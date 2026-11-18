package com.soumit.technovision.ui.common

import com.google.firebase.Timestamp
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.LogModel
import com.soumit.technovision.data.model.StaffModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import java.util.Date

/**
 * Centralized, offline mock data fixture for Jetpack Compose @Preview rendering.
 * Does NOT interact with Firebase or make network calls.
 */
object PreviewData {

    // ==========================================
    // 1. Staff Members (Owner, Faculty, Receptionist)
    // ==========================================

    val ownerStaff = StaffModel(
        id = "staff_owner_01",
        fullName = "Bishal Sha",
        email = "owner.bishal@technovision.com",
        role = "owner",
        phone = "9876543210"
    )

    val facultyStaff1 = StaffModel(
        id = "staff_faculty_01",
        fullName = "Soumit Das",
        email = "soumit.faculty@technovision.com",
        role = "faculty",
        phone = "9876543211"
    )

    val facultyStaff2 = StaffModel(
        id = "staff_faculty_02",
        fullName = "Rinki Das",
        email = "rinki.faculty@technovision.com",
        role = "faculty",
        phone = "9876543212"
    )

    val facultyStaff3 = StaffModel(
        id = "staff_faculty_03",
        fullName = "Priya Sengupta",
        email = "priya.faculty@technovision.com",
        role = "faculty",
        phone = "9876543213"
    )

    val receptionistStaff = StaffModel(
        id = "staff_reception_01",
        fullName = "Soma Ghosh",
        email = "soma.reception@technovision.com",
        role = "receptionist",
        phone = "9876543214"
    )

    val allStaff: List<StaffModel> = listOf(
        ownerStaff,
        facultyStaff1,
        facultyStaff2,
        facultyStaff3,
        receptionistStaff
    )

    val userMap: Map<String, String> = mapOf(
        ownerStaff.id to ownerStaff.fullName,
        ownerStaff.email to ownerStaff.fullName,
        ownerStaff.email.lowercase() to ownerStaff.fullName,
        facultyStaff1.id to facultyStaff1.fullName,
        facultyStaff1.email to facultyStaff1.fullName,
        facultyStaff1.email.lowercase() to facultyStaff1.fullName,
        facultyStaff2.id to facultyStaff2.fullName,
        facultyStaff2.email to facultyStaff2.fullName,
        facultyStaff2.email.lowercase() to facultyStaff2.fullName,
        facultyStaff3.id to facultyStaff3.fullName,
        facultyStaff3.email to facultyStaff3.fullName,
        facultyStaff3.email.lowercase() to facultyStaff3.fullName,
        receptionistStaff.id to receptionistStaff.fullName,
        receptionistStaff.email to receptionistStaff.fullName,
        receptionistStaff.email.lowercase() to receptionistStaff.fullName
    )

    // ==========================================
    // 2. 10 Curated Students with Diverse Data
    // ==========================================

    val students: List<StudentModel> = listOf(
        // 1. Aarav Sharma - Class 10, Today's Birthday, Standard dues
        StudentModel(
            id = "student_01",
            fullName = "Aarav Sharma",
            studentClass = "10",
            school = "St. Xavier's School",
            dob = "18/11/2010",
            gender = "Male",
            fatherName = "Rajesh Sharma",
            motherName = "Sunita Sharma",
            monthlyInstallment = 500L,
            phone = "9876543201",
            altPhone = "9876543202",
            email = "aarav.sharma@example.com",
            batchTimeSlot = "9:00 AM - 11:00 AM",
            weeklySchedule = mapOf(
                "Monday" to "9:00 AM - 11:00 AM",
                "Wednesday" to "9:00 AM - 11:00 AM",
                "Friday" to "9:00 AM - 11:00 AM"
            ),
            admissionDate = "15/01/2024",
            totalPaid = 3500L
        ),

        // 2. Priya Das - Class 12, Tomorrow's Birthday, Senior batch
        StudentModel(
            id = "student_02",
            fullName = "Priya Das",
            studentClass = "12",
            school = "Alipurduar Girls High School",
            dob = "19/11/2008",
            gender = "Female",
            fatherName = "Bikash Das",
            motherName = "Mita Das",
            monthlyInstallment = 700L,
            phone = "9876543203",
            altPhone = "9876543204",
            email = "priya.das@example.com",
            batchTimeSlot = "11:00 AM - 1:00 PM",
            weeklySchedule = mapOf(
                "Tuesday" to "11:00 AM - 1:00 PM",
                "Thursday" to "11:00 AM - 1:00 PM",
                "Saturday" to "11:00 AM - 1:00 PM"
            ),
            admissionDate = "10/02/2024",
            totalPaid = 4900L
        ),

        // 3. Rahul Mukherjee - Class 11, Birthday next week, Evening batch
        StudentModel(
            id = "student_03",
            fullName = "Rahul Mukherjee",
            studentClass = "11",
            school = "Kendriya Vidyalaya",
            dob = "24/11/2009",
            gender = "Male",
            fatherName = "Anirban Mukherjee",
            motherName = "Priyanka Mukherjee",
            monthlyInstallment = 600L,
            phone = "9876543205",
            email = "rahul.m@example.com",
            batchTimeSlot = "5:00 PM - 7:00 PM",
            weeklySchedule = mapOf(
                "Monday" to "5:00 PM - 7:00 PM",
                "Wednesday" to "5:00 PM - 7:00 PM",
                "Friday" to "5:00 PM - 7:00 PM"
            ),
            admissionDate = "01/03/2024",
            totalPaid = 4200L
        ),

        // 4. Ananya Roy - Class 9, Junior, Weekend batch
        StudentModel(
            id = "student_04",
            fullName = "Ananya Roy",
            studentClass = "9",
            school = "Railway Higher Secondary School",
            dob = "05/12/2011",
            gender = "Female",
            fatherName = "Subhas Roy",
            motherName = "Rina Roy",
            monthlyInstallment = 500L,
            phone = "9876543206",
            batchTimeSlot = "9:00 AM - 11:00 AM",
            weeklySchedule = mapOf(
                "Saturday" to "9:00 AM - 11:00 AM",
                "Sunday" to "9:00 AM - 11:00 AM"
            ),
            admissionDate = "15/04/2024",
            totalPaid = 3000L
        ),

        // 5. Debashis Sen - Class 10, Leap Day birthday edge case (29 Feb)
        StudentModel(
            id = "student_05",
            fullName = "Debashis Sen",
            studentClass = "10",
            school = "McWilliam High School",
            dob = "29/02/2008",
            gender = "Male",
            fatherName = "Partha Sen",
            motherName = "Gita Sen",
            monthlyInstallment = 500L,
            phone = "9876543207",
            batchTimeSlot = "3:00 PM - 5:00 PM",
            weeklySchedule = mapOf(
                "Tuesday" to "3:00 PM - 5:00 PM",
                "Thursday" to "3:00 PM - 5:00 PM",
                "Saturday" to "3:00 PM - 5:00 PM"
            ),
            admissionDate = "22/05/2024",
            totalPaid = 2500L
        ),

        // 6. Siddharth Roy - Class 12, High outstanding dues
        StudentModel(
            id = "student_06",
            fullName = "Siddharth Roy",
            studentClass = "12",
            school = "Delhi Public School",
            dob = "14/04/2008",
            gender = "Male",
            fatherName = "Alok Roy",
            motherName = "Mamata Roy",
            monthlyInstallment = 800L,
            phone = "9876543208",
            batchTimeSlot = "7:00 PM - 9:00 PM",
            weeklySchedule = mapOf(
                "Monday" to "7:00 PM - 9:00 PM",
                "Wednesday" to "7:00 PM - 9:00 PM",
                "Friday" to "7:00 PM - 9:00 PM"
            ),
            admissionDate = "10/01/2024",
            totalPaid = 800L // Heavy dues!
        ),

        // 7. Sneha Paul - Class 8, Zero dues (Fully paid up to date)
        StudentModel(
            id = "student_07",
            fullName = "Sneha Paul",
            studentClass = "8",
            school = "St. Joseph's Convent",
            dob = "12/08/2012",
            gender = "Female",
            fatherName = "Dipak Paul",
            motherName = "Shampa Paul",
            monthlyInstallment = 450L,
            phone = "9876543209",
            batchTimeSlot = "1:00 PM - 3:00 PM",
            weeklySchedule = mapOf(
                "Tuesday" to "1:00 PM - 3:00 PM",
                "Thursday" to "1:00 PM - 3:00 PM"
            ),
            admissionDate = "15/06/2024",
            totalPaid = 2700L
        ),

        // 8. Souvik Banerjee - Class 11, Advance payment made
        StudentModel(
            id = "student_08",
            fullName = "Souvik Banerjee",
            studentClass = "11",
            school = "Modern English Academy",
            dob = "03/01/2009",
            gender = "Male",
            fatherName = "Soumen Banerjee",
            motherName = "Debolina Banerjee",
            monthlyInstallment = 600L,
            phone = "9876543210",
            batchTimeSlot = "7:00 PM - 9:00 PM",
            weeklySchedule = mapOf(
                "Monday" to "7:00 PM - 9:00 PM",
                "Thursday" to "7:00 PM - 9:00 PM"
            ),
            admissionDate = "18/07/2024",
            totalPaid = 4800L // Paid in advance
        ),

        // 9. Tanushree Ghosh - Class 10, Partial dues
        StudentModel(
            id = "student_09",
            fullName = "Tanushree Ghosh",
            studentClass = "10",
            school = "Alipurduar High School",
            dob = "20/09/2010",
            gender = "Female",
            fatherName = "Tarun Ghosh",
            motherName = "Jharna Ghosh",
            monthlyInstallment = 500L,
            phone = "9876543211",
            batchTimeSlot = "9:00 AM - 11:00 AM",
            weeklySchedule = mapOf(
                "Wednesday" to "9:00 AM - 11:00 AM",
                "Friday" to "9:00 AM - 11:00 AM"
            ),
            admissionDate = "05/08/2024",
            totalPaid = 1000L
        ),

        // 10. Vikramaditya Roy Choudhury - Class 12, Long name edge case
        StudentModel(
            id = "student_10",
            fullName = "Vikramaditya Roy Choudhury",
            studentClass = "12",
            school = "Scottish Church Collegiate School",
            dob = "15/03/2008",
            gender = "Male",
            fatherName = "Rameshwar Roy Choudhury",
            motherName = "Aparna Roy Choudhury",
            monthlyInstallment = 750L,
            phone = "9876543212",
            batchTimeSlot = "5:00 PM - 7:00 PM",
            weeklySchedule = mapOf(
                "Tuesday" to "5:00 PM - 7:00 PM",
                "Thursday" to "5:00 PM - 7:00 PM",
                "Saturday" to "5:00 PM - 7:00 PM"
            ),
            admissionDate = "15/01/2024",
            totalPaid = 5250L
        )
    )

    val sampleStudent: StudentModel = students[0]

    val studentsMap: Map<String, StudentModel> = students.associateBy { it.id }

    val outstandingBalances: Map<String, Long> = mapOf(
        "student_01" to 500L,
        "student_02" to 700L,
        "student_03" to 600L,
        "student_04" to 500L,
        "student_05" to 500L,
        "student_06" to 5600L, // High overdue
        "student_07" to 0L,    // Cleared
        "student_08" to 0L,    // Advance paid
        "student_09" to 1000L,
        "student_10" to 750L
    )

    // ==========================================
    // 3. Sample Fee Payments
    // ==========================================

    val feePayments: List<FeePaymentModel> = listOf(
        FeePaymentModel(
            id = "pay_01",
            studentId = "student_01",
            studentName = "Aarav Sharma",
            amountPaid = 500L,
            paymentMethod = "Cash",
            remarks = "October fee",
            receivedBy = "Pooja Saha",
            timestamp = Timestamp(Date())
        ),
        FeePaymentModel(
            id = "pay_02",
            studentId = "student_02",
            studentName = "Priya Das",
            amountPaid = 700L,
            paymentMethod = "UPI",
            remarks = "October fee - GPay",
            receivedBy = "Pooja Saha",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 86400000L))
        ),
        FeePaymentModel(
            id = "pay_03",
            studentId = "student_03",
            studentName = "Rahul Mukherjee",
            amountPaid = 600L,
            paymentMethod = "Cash",
            remarks = "Regular payment",
            receivedBy = "Soumit Das",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 172800000L))
        ),
        FeePaymentModel(
            id = "pay_04",
            studentId = "student_06",
            studentName = "Siddharth Roy",
            amountPaid = 800L,
            paymentMethod = "Bank Transfer",
            remarks = "Partial dues clearing",
            receivedBy = "Soumit Das",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 259200000L))
        ),
        FeePaymentModel(
            id = "pay_05",
            studentId = "student_08",
            studentName = "Souvik Banerjee",
            amountPaid = 1200L,
            paymentMethod = "UPI",
            remarks = "Advance payment (2 months)",
            receivedBy = "Pooja Saha",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 345600000L))
        )
    )

    // ==========================================
    // 4. Sample Attendance Records
    // ==========================================

    val attendanceRecords: List<AttendanceRecordModel> = listOf(
        AttendanceRecordModel(
            id = "att_01",
            date = "18-11-2026",
            timeSlot = "9:00 AM - 11:00 AM",
            presentStudentIds = listOf("student_01", "student_04", "student_09"),
            absentStudentIds = listOf("student_07"),
            additionalStudentIds = listOf("student_04"),
            totalPresent = 3,
            totalAbsent = 1,
            submittedBy = "Amitabha Roy",
            submittedByUid = "staff_faculty_01",
            submittedByEmail = "amitabha.faculty@technovision.com",
            timestamp = Timestamp(Date())
        ),
        AttendanceRecordModel(
            id = "att_02",
            date = "17-11-2026",
            timeSlot = "11:00 AM - 1:00 PM",
            presentStudentIds = listOf("student_02", "student_05"),
            absentStudentIds = emptyList(),
            additionalStudentIds = emptyList(),
            totalPresent = 2,
            totalAbsent = 0,
            submittedBy = "Rahul Sharma",
            submittedByUid = "staff_faculty_02",
            submittedByEmail = "rahul.faculty@technovision.com",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 86400000L))
        )
    )

    // ==========================================
    // 5. Sample Faculty Attendance Requests
    // ==========================================

    val teacherAttendanceRequests: List<TeacherAttendanceRequestModel> = listOf(
        TeacherAttendanceRequestModel(
            id = "tar_01",
            teacherId = "staff_faculty_01",
            teacherName = "Amitabha Roy",
            date = "18-11-2026",
            timeSlot = "9:00 AM - 11:00 AM",
            status = "Verified",
            timestamp = Timestamp(Date()),
            verifiedBy = "Soumit Das"
        ),
        TeacherAttendanceRequestModel(
            id = "tar_02",
            teacherId = "staff_faculty_02",
            teacherName = "Rahul Sharma",
            date = "18-11-2026",
            timeSlot = "11:00 AM - 1:00 PM",
            status = "Pending",
            timestamp = Timestamp(Date())
        ),
        TeacherAttendanceRequestModel(
            id = "tar_03",
            teacherId = "staff_faculty_03",
            teacherName = "Priya Sengupta",
            date = "17-11-2026",
            timeSlot = "5:00 PM - 7:00 PM",
            status = "Verified",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 86400000L)),
            verifiedBy = "Soumit Das"
        )
    )

    // ==========================================
    // 6. Sample System Activity Logs
    // ==========================================

    val systemLogs: List<LogModel> = listOf(
        LogModel(
            id = "log_01",
            action = "Student Enrolled",
            details = "Enrolled new student: Aarav Sharma (Class 10)",
            performedBy = "Soumit Das",
            timestamp = Timestamp(Date())
        ),
        LogModel(
            id = "log_02",
            action = "Fee Collected",
            details = "Collected ₹500 via Cash for Aarav Sharma",
            performedBy = "Pooja Saha",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 3600000L))
        ),
        LogModel(
            id = "log_03",
            action = "Attendance Verified",
            details = "Verified batch [9:00 AM - 11:00 AM] for teacher Amitabha Roy",
            performedBy = "Soumit Das",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 7200000L))
        ),
        LogModel(
            id = "log_04",
            action = "Staff Added",
            details = "Registered new faculty: Priya Sengupta",
            performedBy = "Soumit Das",
            timestamp = Timestamp(Date(System.currentTimeMillis() - 86400000L))
        )
    )
}
