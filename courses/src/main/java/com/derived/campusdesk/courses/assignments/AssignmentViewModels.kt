package com.derived.campusdesk.courses.assignments

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.models.AssignmentSubmissionFile
import com.derived.campusdesk.networking.models.AssignmentSubmitPayload
import com.derived.campusdesk.networking.models.StudentAssignment
import com.derived.campusdesk.networking.services.StudentService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssignmentClassFilter(
    val id: String,
    val name: String,
)

@HiltViewModel
class AssignmentListViewModel @Inject constructor(
    private val studentService: StudentService,
) : ViewModel() {
    private val _assignments = MutableStateFlow<List<StudentAssignment>>(emptyList())
    val assignments: StateFlow<List<StudentAssignment>> = _assignments.asStateFlow()

    private val _selectedClassId = MutableStateFlow("All")
    val selectedClassId: StateFlow<String> = _selectedClassId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isNotEnrolled = MutableStateFlow(false)
    val isNotEnrolled: StateFlow<Boolean> = _isNotEnrolled.asStateFlow()

    fun classFilters(assignments: List<StudentAssignment> = _assignments.value): List<AssignmentClassFilter> {
        val unique = assignments
            .mapNotNull { item ->
                val name = item.className?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val id = item.classId?.value?.takeIf { it.isNotBlank() } ?: name
                AssignmentClassFilter(id = id, name = name)
            }
            .distinctBy { it.id }
        return listOf(AssignmentClassFilter("All", "All classes")) + unique
    }

    fun visibleAssignments(
        assignments: List<StudentAssignment> = _assignments.value,
        selectedClassId: String = _selectedClassId.value,
    ): List<StudentAssignment> {
        if (selectedClassId == "All") return assignments
        return assignments.filter {
            it.classId?.value == selectedClassId || it.className == selectedClassId
        }
    }

    fun selectClass(id: String) {
        _selectedClassId.value = id
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isNotEnrolled.value = false
            try {
                _assignments.value = studentService.assignments()
                val filters = classFilters(_assignments.value)
                if (filters.none { it.id == _selectedClassId.value }) {
                    _selectedClassId.value = "All"
                }
            } catch (e: Exception) {
                if ((e is NetworkError && e.isNotEnrolled) || e.message?.contains("enroll", true) == true) {
                    _isNotEnrolled.value = true
                } else {
                    _errorMessage.value = e.message ?: "Could not load assignments."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@HiltViewModel
class AssignmentDetailViewModel @Inject constructor(
    private val studentService: StudentService,
) : ViewModel() {
    private val _assignment = MutableStateFlow<StudentAssignment?>(null)
    val assignment: StateFlow<StudentAssignment?> = _assignment.asStateFlow()

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    private val _fileName = MutableStateFlow<String?>(null)
    val fileName: StateFlow<String?> = _fileName.asStateFlow()

    private val _fileDataUri = MutableStateFlow<String?>(null)
    val fileDataUri: StateFlow<String?> = _fileDataUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    fun onBodyChange(value: String) {
        _body.value = value
    }

    fun load(id: String, seed: StudentAssignment? = null) {
        if (seed != null) _assignment.value = seed
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _assignment.value = studentService.assignment(id)
            } catch (e: Exception) {
                if (_assignment.value == null) {
                    _errorMessage.value = e.message ?: "Could not load assignment."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun attachFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri)
                    ?: "application/octet-stream"
                if (mime !in AssignmentSubmissionFile.allowedMimeTypes) {
                    _errorMessage.value = "Please choose a PDF or Word document."
                    return@launch
                }
                val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
                } ?: "submission"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Could not read the selected file.")
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                _fileName.value = name
                _fileDataUri.value = "data:$mime;base64,$encoded"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not attach file."
                _fileName.value = null
                _fileDataUri.value = null
            }
        }
    }

    fun clearFile() {
        _fileName.value = null
        _fileDataUri.value = null
    }

    fun submit(id: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            _submitSuccess.value = false
            try {
                val payload = AssignmentSubmitPayload(
                    body = _body.value.trim(),
                    file = _fileDataUri.value,
                    fileName = _fileName.value,
                )
                studentService.submitAssignment(id, payload)
                _submitSuccess.value = true
                _assignment.value = studentService.assignment(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not submit assignment."
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
