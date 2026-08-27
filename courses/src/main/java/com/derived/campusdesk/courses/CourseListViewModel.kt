package com.derived.campusdesk.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.models.Course
import com.derived.campusdesk.networking.services.CampusService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CourseListViewModel @Inject constructor(
    private val campusService: CampusService,
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<String>> = _courses.map { list ->
        listOf("All") + list.map { it.displayCategory }.distinct().sorted()
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), listOf("All"))

    val visibleCourses: StateFlow<List<Course>> = combine(
        _courses,
        _selectedCategory,
    ) { courses, category ->
        if (category == "All") courses else courses.filter { it.displayCategory == category }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun selectCategory(category: String) { _selectedCategory.value = category }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _courses.value = campusService.courses()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not load courses."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
