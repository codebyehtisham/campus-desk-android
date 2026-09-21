package com.derived.campusdesk.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.models.StudentClass
import com.derived.campusdesk.networking.models.TimetableSlot
import com.derived.campusdesk.networking.services.StudentService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ClassListViewModel @Inject constructor(
    private val studentService: StudentService,
) : ViewModel() {
    private val _classes = MutableStateFlow<List<StudentClass>>(emptyList())
    val classes: StateFlow<List<StudentClass>> = _classes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isNotEnrolled = MutableStateFlow(false)
    val isNotEnrolled: StateFlow<Boolean> = _isNotEnrolled.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isNotEnrolled.value = false
            try {
                _classes.value = studentService.classes()
            } catch (e: Exception) {
                if ((e is NetworkError && e.isNotEnrolled) || e.message?.contains("enroll", true) == true) {
                    _isNotEnrolled.value = true
                } else {
                    _errorMessage.value = e.message ?: "Could not load classes."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@HiltViewModel
class ClassDetailViewModel @Inject constructor(
    private val studentService: StudentService,
) : ViewModel() {
    private val _studentClass = MutableStateFlow<StudentClass?>(null)
    val studentClass: StateFlow<StudentClass?> = _studentClass.asStateFlow()

    private val _timetable = MutableStateFlow<List<TimetableSlot>>(emptyList())
    val timetable: StateFlow<List<TimetableSlot>> = _timetable.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun load(classId: String, seed: StudentClass? = null) {
        if (seed != null) _studentClass.value = seed
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val detail = studentService.classDetail(classId)
                _studentClass.value = detail
                val slots = runCatching { studentService.timetable() }.getOrDefault(emptyList())
                _timetable.value = slots.filter { it.classId?.value == classId }
            } catch (e: Exception) {
                if (_studentClass.value == null) {
                    _errorMessage.value = e.message ?: "Could not load class."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
