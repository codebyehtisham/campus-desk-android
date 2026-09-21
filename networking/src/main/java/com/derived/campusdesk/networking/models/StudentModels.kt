package com.derived.campusdesk.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - Dashboard

@Serializable
data class StudentDashboard(
    val pendingAssignments: Int? = null,
    val pendingLeaves: Int? = null,
    val classCount: Int? = null,
    val recentQuizMarks: List<QuizMarkSummary>? = null,
    val openSessions: List<OpenAttendanceSession>? = null,
)

@Serializable
data class QuizMarkSummary(
    val id: FlexibleId = FlexibleId(""),
    val title: String? = null,
    val name: String? = null,
    val marks: Double? = null,
    val marksObtained: Double? = null,
    val maxMarks: Double? = null,
    val className: String? = null,
    val classId: FlexibleId? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Quiz"
    val displayMarks: String
        get() {
            val obtained = marksObtained ?: marks ?: return "—"
            return if (maxMarks != null) {
                "${formatMark(obtained)} / ${formatMark(maxMarks)}"
            } else {
                formatMark(obtained)
            }
        }
}

@Serializable
data class OpenAttendanceSession(
    val id: FlexibleId,
    val className: String? = null,
    val name: String? = null,
    val title: String? = null,
) {
    val displayName: String get() = className ?: name ?: title ?: "Open session"
}

// MARK: - Classes

@Serializable
data class StudentClass(
    val id: FlexibleId,
    val name: String? = null,
    val title: String? = null,
    val code: String? = null,
    val teacher: TeacherRef? = null,
    val contents: List<ClassContent>? = null,
    val assignments: List<StudentAssignment>? = null,
    val quizzes: List<StudentQuiz>? = null,
    val leaves: List<StudentLeave>? = null,
) {
    val displayName: String get() = name ?: title ?: code ?: "Class"
    val teacherName: String get() = teacher?.displayName ?: "Faculty"
}

@Serializable
data class TeacherRef(
    val id: FlexibleId? = null,
    val name: String? = null,
    val title: String? = null,
) {
    val displayName: String
        get() {
            val trimmed = name?.trim().orEmpty()
            return trimmed.ifBlank { title ?: "Teacher" }
        }
}

@Serializable
data class ClassContent(
    val id: FlexibleId = FlexibleId(""),
    val week: Int? = null,
    val title: String? = null,
    val body: String? = null,
)

// MARK: - Timetable

@Serializable
data class TimetableSlot(
    val id: FlexibleId = FlexibleId(""),
    val day: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val className: String? = null,
    val classId: FlexibleId? = null,
    val room: String? = null,
) {
    val timeRange: String
        get() = listOfNotNull(startTime, endTime).joinToString(" – ")
}

// MARK: - Assignments

enum class AssignmentStatus {
    MISSING,
    SUBMITTED,
    GRADED,
    PENDING,
    ;

    companion object {
        fun from(raw: String?): AssignmentStatus = when (raw?.lowercase()) {
            "submitted" -> SUBMITTED
            "graded" -> GRADED
            "pending" -> PENDING
            else -> MISSING
        }
    }
}

@Serializable
data class StudentAssignment(
    val id: FlexibleId,
    val title: String? = null,
    val body: String? = null,
    val dueDate: String? = null,
    val status: String? = null,
    val marksObtained: Double? = null,
    val feedback: String? = null,
    val classId: FlexibleId? = null,
    val className: String? = null,
) {
    val assignmentStatus: AssignmentStatus get() = AssignmentStatus.from(status)
    val displayTitle: String get() = title ?: "Assignment"
}

@Serializable
data class AssignmentSubmitPayload(
    val body: String,
    val file: String? = null,
    val fileName: String? = null,
)

@Serializable
data class AssignmentSubmitResponse(
    val file: AssignmentSubmittedFileRef? = null,
)

@Serializable
data class AssignmentSubmittedFileRef(
    val url: String? = null,
)

data class AssignmentSubmissionFile(
    val fileName: String,
    val mimeType: String,
    val dataURI: String,
) {
    companion object {
        val allowedMimeTypes: Set<String> = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        )
    }
}

// MARK: - Quizzes & Exams

@Serializable
data class StudentQuiz(
    val id: FlexibleId,
    val title: String? = null,
    val name: String? = null,
    val marks: Double? = null,
    val marksObtained: Double? = null,
    val maxMarks: Double? = null,
    val className: String? = null,
    val classId: FlexibleId? = null,
    val date: String? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Quiz"
    val displayMarks: String
        get() {
            val obtained = marksObtained ?: marks ?: return "—"
            return if (maxMarks != null) {
                "${formatMark(obtained)} / ${formatMark(maxMarks)}"
            } else {
                formatMark(obtained)
            }
        }
}

@Serializable
data class StudentExam(
    val id: FlexibleId,
    val title: String? = null,
    val name: String? = null,
    val grade: String? = null,
    val marks: Double? = null,
    val marksObtained: Double? = null,
    val maxMarks: Double? = null,
    val className: String? = null,
    val classId: FlexibleId? = null,
    val date: String? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Exam"
}

// MARK: - Attendance history

@Serializable
data class AttendanceDayRecord(
    val id: FlexibleId = FlexibleId(""),
    val date: String? = null,
    val dateLabel: String? = null,
    val status: String? = null,
    val className: String? = null,
) {
    val displayDate: String get() = dateLabel ?: date?.take(10) ?: "—"
}

@Serializable
data class AttendanceSessionRecord(
    val id: FlexibleId,
    val className: String? = null,
    val date: String? = null,
    val dateLabel: String? = null,
    val status: String? = null,
) {
    val displayDate: String get() = dateLabel ?: date?.take(10) ?: "—"
}

@Serializable
data class StudentAttendanceHistory(
    val daily: List<AttendanceDayRecord>? = null,
    val sessions: List<AttendanceSessionRecord>? = null,
    val register: List<AttendanceDayRecord>? = null,
) {
    val dailyRecords: List<AttendanceDayRecord> get() = daily ?: register ?: emptyList()
}

// MARK: - Leave

enum class LeaveType(val rawValue: String) {
    SICK("sick"),
    CASUAL("casual"),
    MATERNITY("maternity"),
    ANNUAL("annual"),
    ;

    val label: String
        get() = when (this) {
            SICK -> "Sick"
            CASUAL -> "Casual"
            MATERNITY -> "Maternity"
            ANNUAL -> "Annual"
        }

    companion object {
        fun from(raw: String?): LeaveType? =
            entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }
    }
}

enum class LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    ;

    companion object {
        fun from(raw: String?): LeaveStatus = when (raw?.lowercase()) {
            "approved", "accepted" -> APPROVED
            "rejected", "declined" -> REJECTED
            else -> PENDING
        }
    }
}

@Serializable
data class StudentLeave(
    val id: FlexibleId,
    val classId: FlexibleId? = null,
    val className: String? = null,
    val type: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val reason: String? = null,
    val status: String? = null,
    val reviewNotes: String? = null,
    val reviewedAt: String? = null,
    val createdAt: String? = null,
    @SerialName("class") val classRef: LeaveClassRef? = null,
    val teacher: LeaveTeacherRef? = null,
) {
    val leaveStatus: LeaveStatus get() = LeaveStatus.from(status)
    val leaveType: LeaveType? get() = LeaveType.from(type)
    val resolvedClassId: FlexibleId? get() = classId ?: classRef?.id
    val displayClassName: String get() = classRef?.name ?: className ?: classRef?.code ?: "Class"
    val displayTeacherName: String get() = teacher?.name ?: "Teacher"
    val dateRangeLabel: String
        get() {
            val start = startDate?.take(10) ?: "—"
            val end = endDate?.take(10) ?: "—"
            return if (start == end) start else "$start – $end"
        }
}

@Serializable
data class LeaveClassRef(
    val id: FlexibleId,
    val name: String? = null,
    val code: String? = null,
)

@Serializable
data class LeaveTeacherRef(
    val id: FlexibleId,
    val name: String? = null,
)

@Serializable
data class LeaveCreatePayload(
    val classId: String,
    val type: String,
    val startDate: String,
    val endDate: String,
    val reason: String? = null,
) {
    companion object {
        fun create(
            classId: String,
            type: LeaveType,
            startDate: String,
            endDate: String,
            reason: String?,
        ): LeaveCreatePayload {
            val trimmed = reason?.trim().orEmpty()
            return LeaveCreatePayload(
                classId = classId,
                type = type.rawValue,
                startDate = startDate,
                endDate = endDate,
                reason = trimmed.ifBlank { null },
            )
        }
    }
}

// MARK: - Notifications

@Serializable
data class StudentNotification(
    val id: FlexibleId,
    val title: String? = null,
    val body: String? = null,
    val message: String? = null,
    val read: Boolean? = null,
    val createdAt: String? = null,
    @SerialName("type") val notificationType: String? = null,
    val data: StudentNotificationData? = null,
) {
    val leaveId: FlexibleId? get() = data?.leaveId
    val displayBody: String get() = body ?: message ?: ""
    val isLeaveDecision: Boolean get() = notificationType == "student_leave_decision"
}

@Serializable
data class StudentNotificationData(
    val leaveId: FlexibleId? = null,
)

private fun formatMark(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
