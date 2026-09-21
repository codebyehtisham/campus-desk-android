package com.derived.campusdesk.courses.leaves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.models.LeaveCreatePayload
import com.derived.campusdesk.networking.models.LeaveType
import com.derived.campusdesk.networking.models.StudentClass
import com.derived.campusdesk.networking.models.StudentLeave
import com.derived.campusdesk.networking.services.StudentService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LeaveListViewModel @Inject constructor(
    private val studentService: StudentService,
) : ViewModel() {
    private val _leaves = MutableStateFlow<List<StudentLeave>>(emptyList())
    val leaves: StateFlow<List<StudentLeave>> = _leaves.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isNotEnrolled = MutableStateFlow(false)
    val isNotEnrolled: StateFlow<Boolean> = _isNotEnrolled.asStateFlow()

    val pendingCount get() = _leaves.value.count { it.status.equals("pending", true) }
    val approvedCount get() = _leaves.value.count {
        it.status.equals("approved", true) || it.status.equals("accepted", true)
    }
    val rejectedCount get() = _leaves.value.count {
        it.status.equals("rejected", true) || it.status.equals("declined", true)
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isNotEnrolled.value = false
            try {
                _leaves.value = studentService.leaves()
            } catch (e: Exception) {
                if ((e is NetworkError && e.isNotEnrolled) || e.message?.contains("enroll", true) == true) {
                    _isNotEnrolled.value = true
                } else {
                    _errorMessage.value = e.message ?: "Could not load leave requests."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun leave(id: String): StudentLeave? = _leaves.value.firstOrNull { it.id.value == id }
}

@HiltViewModel
class ApplyLeaveViewModel @Inject constructor(
    private val studentService: StudentService,
) : ViewModel() {
    private val _classes = MutableStateFlow<List<StudentClass>>(emptyList())
    val classes: StateFlow<List<StudentClass>> = _classes.asStateFlow()

    private val _selectedClassId = MutableStateFlow<String?>(null)
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

    private val _leaveType = MutableStateFlow(LeaveType.SICK)
    val leaveType: StateFlow<LeaveType> = _leaveType.asStateFlow()

    private val _startDate = MutableStateFlow(LocalDate.now())
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(LocalDate.now())
    val endDate: StateFlow<LocalDate> = _endDate.asStateFlow()

    private val _reason = MutableStateFlow("")
    val reason: StateFlow<String> = _reason.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun bootstrap(presetClassId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val list = studentService.classes()
                _classes.value = list
                _selectedClassId.value = presetClassId?.takeIf { id -> list.any { it.id.value == id } }
                    ?: list.firstOrNull()?.id?.value
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not load classes."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectClass(id: String) { _selectedClassId.value = id }
    fun selectType(type: LeaveType) { _leaveType.value = type }
    fun setStartDate(date: LocalDate) {
        _startDate.value = date
        if (_endDate.value.isBefore(date)) _endDate.value = date
    }
    fun setEndDate(date: LocalDate) {
        _endDate.value = if (date.isBefore(_startDate.value)) _startDate.value else date
    }
    fun setReason(value: String) { _reason.value = value }

    fun submit() {
        val classId = _selectedClassId.value
        if (classId.isNullOrBlank()) {
            _errorMessage.value = "Select a class."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            _success.value = false
            try {
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val payload = LeaveCreatePayload.create(
                    classId = classId,
                    type = _leaveType.value,
                    startDate = _startDate.value.format(formatter),
                    endDate = _endDate.value.format(formatter),
                    reason = _reason.value.trim().ifBlank { null },
                )
                studentService.createLeave(payload)
                _success.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not submit leave request."
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
