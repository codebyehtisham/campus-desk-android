package com.derived.campusdesk.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.api.ApiConfigProvider
import com.derived.campusdesk.networking.models.CampusSettings
import com.derived.campusdesk.networking.models.FacultyMember
import com.derived.campusdesk.networking.models.NewsItem
import com.derived.campusdesk.networking.services.CampusService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val campusService: CampusService,
    private val apiConfigProvider: ApiConfigProvider,
) : ViewModel() {
    private val instituteSlug get() = apiConfigProvider.defaultInstituteSlug()
    private val _settings = MutableStateFlow<CampusSettings?>(null)
    val settings: StateFlow<CampusSettings?> = _settings.asStateFlow()

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    val news: StateFlow<List<NewsItem>> = _news.asStateFlow()

    private val _faculty = MutableStateFlow<List<FacultyMember>>(emptyList())
    val faculty: StateFlow<List<FacultyMember>> = _faculty.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val settingsDeferred = async { runCatching { campusService.settings(instituteSlug) }.getOrNull() }
                val newsDeferred = async { runCatching { campusService.news() }.getOrNull().orEmpty() }
                val facultyDeferred = async { runCatching { campusService.faculty() }.getOrNull().orEmpty() }
                val nextSettings = settingsDeferred.await()
                val nextNews = newsDeferred.await()
                val nextFaculty = facultyDeferred.await()
                // Assign together so the UI stays on the branded loader until everything is ready.
                _settings.value = nextSettings ?: _settings.value
                _news.value = nextNews
                _faculty.value = nextFaculty
                if (_settings.value == null && _news.value.isEmpty() && _faculty.value.isEmpty()) {
                    _errorMessage.value = "Could not load campus data."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
