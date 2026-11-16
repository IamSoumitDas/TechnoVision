package com.soumit.technovision.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.FeePaymentModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface FeeRepository {
    fun getFeePaymentsStream(): Flow<List<FeePaymentModel>>
    fun getPaymentsForStudentStream(studentId: String): Flow<List<FeePaymentModel>>
    suspend fun recordPayment(payment: FeePaymentModel): Result<String>
    suspend fun deletePayment(paymentId: String): Result<Unit>
}

class FirestoreFeeRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FeeRepository {

    override fun getFeePaymentsStream(): Flow<List<FeePaymentModel>> = callbackFlow {
        val subscription = firestore.collection("fee_payments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val payments = snapshot.documents.map { doc ->
                        FeePaymentModel(
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
                    trySend(payments)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getPaymentsForStudentStream(studentId: String): Flow<List<FeePaymentModel>> = callbackFlow {
        if (studentId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("fee_payments")
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val payments = snapshot.documents.map { doc ->
                        FeePaymentModel(
                            id = doc.id,
                            studentId = doc.getString("studentId") ?: "",
                            studentName = doc.getString("studentName") ?: "",
                            amountPaid = doc.getLong("amountPaid") ?: 0L,
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            remarks = doc.getString("remarks") ?: "",
                            receivedBy = doc.getString("receivedBy") ?: "",
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    }.sortedByDescending { it.timestamp?.seconds ?: 0L }
                    trySend(payments)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun recordPayment(payment: FeePaymentModel): Result<String> {
        return try {
            val paymentData = hashMapOf(
                "studentId" to payment.studentId,
                "studentName" to payment.studentName,
                "amountPaid" to payment.amountPaid,
                "paymentMethod" to payment.paymentMethod,
                "remarks" to payment.remarks,
                "receivedBy" to payment.receivedBy,
                "timestamp" to (payment.timestamp ?: Timestamp.now())
            )
            val docRef = firestore.collection("fee_payments").add(paymentData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePayment(paymentId: String): Result<Unit> {
        return try {
            firestore.collection("fee_payments").document(paymentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
