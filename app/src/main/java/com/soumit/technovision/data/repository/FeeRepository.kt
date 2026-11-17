package com.soumit.technovision.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.TechnoVisionApplication
import com.soumit.technovision.data.local.DeltaSyncManager
import com.soumit.technovision.data.local.LocalDatabase
import com.soumit.technovision.data.model.FeePaymentModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface FeeRepository {
    fun getFeePaymentsStream(): Flow<List<FeePaymentModel>>
    fun getPaymentsForStudentStream(studentId: String): Flow<List<FeePaymentModel>>
    suspend fun recordPayment(payment: FeePaymentModel): Result<String>
    suspend fun deletePayment(paymentId: String): Result<Unit>
    suspend fun syncAllStudentsTotalPaid(): Result<Unit>
}

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreFeeRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localDb: LocalDatabase? = null,
    private val syncManager: DeltaSyncManager? = null
) : FeeRepository {

    private val db: LocalDatabase?
        get() = localDb ?: TechnoVisionApplication.instance?.container?.localDatabase

    private val sync: DeltaSyncManager?
        get() = syncManager ?: TechnoVisionApplication.instance?.container?.deltaSyncManager

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _feePaymentsFlow = MutableStateFlow<List<FeePaymentModel>>(emptyList())
    private val _isLoaded = MutableStateFlow(false)

    init {
        repositoryScope.launch {
            val database = db
            if (database != null) {
                _feePaymentsFlow.value = database.getAllFeePayments()
                _isLoaded.value = true
                database.feesUpdateFlow.collect {
                    _feePaymentsFlow.value = database.getAllFeePayments()
                }
            } else {
                _isLoaded.value = true
            }
        }
    }

    override fun getFeePaymentsStream(): Flow<List<FeePaymentModel>> {
        if (db == null) {
            return callbackFlow {
                val subscription = firestore.collection("fee_payments")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val payments = snapshot.documents
                                .filter { it.getBoolean("isDeleted") != true }
                                .map { doc -> parseFeePayment(doc) }
                            trySend(payments)
                        }
                    }
                awaitClose { subscription.remove() }
            }
        }
        return _isLoaded.filter { it }.flatMapLatest { _feePaymentsFlow }
    }

    override fun getPaymentsForStudentStream(studentId: String): Flow<List<FeePaymentModel>> {
        if (studentId.isBlank()) return flowOf(emptyList())

        if (db == null) {
            return callbackFlow {
                val subscription = firestore.collection("fee_payments")
                    .whereEqualTo("studentId", studentId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val payments = snapshot.documents
                                .filter { it.getBoolean("isDeleted") != true }
                                .map { doc -> parseFeePayment(doc) }
                                .sortedByDescending { it.timestamp?.seconds ?: 0L }
                            trySend(payments)
                        }
                    }
                awaitClose { subscription.remove() }
            }
        }
        return _isLoaded.filter { it }.flatMapLatest {
            _feePaymentsFlow.map { list ->
                list.filter { it.studentId == studentId }.sortedByDescending { it.timestamp?.seconds ?: 0L }
            }
        }
    }

    override suspend fun recordPayment(payment: FeePaymentModel): Result<String> {
        return try {
            val now = Timestamp.now()
            val paymentData = hashMapOf(
                "studentId" to payment.studentId,
                "studentName" to payment.studentName,
                "amountPaid" to payment.amountPaid,
                "paymentMethod" to payment.paymentMethod,
                "remarks" to payment.remarks,
                "receivedBy" to payment.receivedBy,
                "timestamp" to (payment.timestamp ?: now),
                "updatedAt" to now,
                "isDeleted" to false
            )
            val docRef = firestore.collection("fee_payments").document()
            val batch = firestore.batch()
            batch.set(docRef, paymentData)

            if (payment.studentId.isNotBlank() && payment.amountPaid > 0L) {
                val studentDocRef = firestore.collection("students").document(payment.studentId)
                batch.update(studentDocRef, "totalPaid", FieldValue.increment(payment.amountPaid))
                batch.update(studentDocRef, "updatedAt", now)
            }

            batch.commit().await()

            // Save in local SQLite
            val savedPayment = payment.copy(id = docRef.id, timestamp = payment.timestamp ?: now)
            db?.upsertFeePayment(savedPayment, now.toDate().time)
            _feePaymentsFlow.value = (listOf(savedPayment) + _feePaymentsFlow.value.filter { it.id != savedPayment.id })
                .sortedByDescending { it.timestamp?.seconds ?: 0L }

            // Update student totalPaid in local SQLite
            val student = db?.getStudent(payment.studentId)
            if (student != null) {
                db?.upsertStudent(student.copy(totalPaid = student.totalPaid + payment.amountPaid), now.toDate().time)
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePayment(paymentId: String): Result<Unit> {
        return try {
            val now = Timestamp.now()
            val localPayment = db?.getFeePayment(paymentId)
            val studentId: String
            val amountPaid: Long
            if (localPayment != null) {
                studentId = localPayment.studentId
                amountPaid = localPayment.amountPaid
            } else {
                val paymentDoc = firestore.collection("fee_payments").document(paymentId).get().await()
                studentId = paymentDoc.getString("studentId").orEmpty()
                amountPaid = paymentDoc.getLong("amountPaid") ?: 0L
            }

            val batch = firestore.batch()
            batch.update(firestore.collection("fee_payments").document(paymentId), mapOf("isDeleted" to true, "updatedAt" to now))

            if (studentId.isNotBlank() && amountPaid > 0L) {
                val studentDocRef = firestore.collection("students").document(studentId)
                batch.update(studentDocRef, "totalPaid", FieldValue.increment(-amountPaid))
                batch.update(studentDocRef, "updatedAt", now)
            }

            batch.commit().await()

            // Update local SQLite
            db?.markFeePaymentDeleted(paymentId, now.toDate().time)
            _feePaymentsFlow.value = _feePaymentsFlow.value.filter { it.id != paymentId }

            val student = db?.getStudent(studentId)
            if (student != null) {
                db?.upsertStudent(student.copy(totalPaid = (student.totalPaid - amountPaid).coerceAtLeast(0L)), now.toDate().time)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFeePayment(doc: DocumentSnapshot): FeePaymentModel {
        return FeePaymentModel(
            id = doc.id,
            studentId = doc.getString("studentId") ?: "",
            studentName = doc.getString("studentName") ?: "",
            amountPaid = doc.getLong("amountPaid") ?: 0L,
            paymentMethod = doc.getString("paymentMethod") ?: "",
            remarks = doc.getString("remarks") ?: "",
            receivedBy = doc.getString("receivedBy") ?: "",
            timestamp = doc.getTimestamp("timestamp")
        )
    }

    override suspend fun syncAllStudentsTotalPaid(): Result<Unit> {
        return try {
            if (db?.isMigrationDone("sync_total_paid") == true) {
                return Result.success(Unit)
            }

            val paymentsSnapshot = firestore.collection("fee_payments").get().await()
            val studentSums = mutableMapOf<String, Long>()
            for (doc in paymentsSnapshot.documents) {
                if (doc.getBoolean("isDeleted") == true) continue
                val sId = doc.getString("studentId") ?: continue
                val amt = doc.getLong("amountPaid") ?: 0L
                studentSums[sId] = (studentSums[sId] ?: 0L) + amt
            }

            val studentsSnapshot = firestore.collection("students").get().await()
            if (studentsSnapshot.isEmpty) {
                db?.setMigrationDone("sync_total_paid")
                return Result.success(Unit)
            }

            val batches = mutableListOf(firestore.batch())
            var operationCount = 0

            for (doc in studentsSnapshot.documents) {
                if (doc.getBoolean("isDeleted") == true) continue
                val correctTotal = studentSums[doc.id] ?: 0L
                val currentTotal = doc.getLong("totalPaid")
                if (currentTotal == null || currentTotal != correctTotal) {
                    if (operationCount >= 450) {
                        batches.add(firestore.batch())
                        operationCount = 0
                    }
                    batches.last().update(doc.reference, "totalPaid", correctTotal)
                    operationCount++
                }
            }

            for (b in batches) {
                b.commit().await()
            }
            db?.setMigrationDone("sync_total_paid")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
