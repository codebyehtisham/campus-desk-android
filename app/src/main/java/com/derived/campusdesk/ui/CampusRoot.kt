package com.derived.campusdesk.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.derived.campusdesk.attendance.AttendanceViewModel
import com.derived.campusdesk.attendance.ui.AttendanceScreen
import com.derived.campusdesk.auth.ForgotPasswordViewModel
import com.derived.campusdesk.auth.LoginViewModel
import com.derived.campusdesk.auth.SessionPhase
import com.derived.campusdesk.auth.SessionStore
import com.derived.campusdesk.auth.ui.LoginScreen
import com.derived.campusdesk.courses.CourseListViewModel
import com.derived.campusdesk.courses.ui.CourseDetailScreen
import com.derived.campusdesk.courses.ui.CourseListScreen
import com.derived.campusdesk.home.HomeViewModel
import com.derived.campusdesk.home.ui.HomeScreen
import com.derived.campusdesk.networking.api.ApiEnvironmentStore
import com.derived.campusdesk.networking.debug.DevToolsConfig
import com.derived.campusdesk.networking.models.Course
import com.derived.campusdesk.profile.ProfileViewModel
import com.derived.campusdesk.profile.ui.ProfileScreen
import com.derived.campusdesk.sharedui.components.AppTab
import com.derived.campusdesk.sharedui.components.CampusLoadingView
import com.derived.campusdesk.sharedui.components.FloatingDock
import com.derived.campusdesk.sharedui.components.TabCrossfade
import com.derived.campusdesk.ui.debug.ApiLogsSheet
import com.derived.campusdesk.ui.debug.ShakeEvents

@Composable
fun CampusRoot(
    sessionStore: SessionStore,
    devToolsConfig: DevToolsConfig,
    environmentStore: ApiEnvironmentStore,
) {
    val phase by sessionStore.phase.collectAsStateWithLifecycle()
    var apiReloadKey by remember { mutableIntStateOf(0) }
    var showApiLogs by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { sessionStore.restore() }

    if (devToolsConfig.isDevToolsEnabled) {
        LaunchedEffect(Unit) {
            ShakeEvents.shakes.collect { showApiLogs = true }
        }
    }

    when (val current = phase) {
        SessionPhase.Launching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CampusLoadingView()
        }
        SessionPhase.SignedOut -> {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val forgotViewModel: ForgotPasswordViewModel = hiltViewModel()
            // ViewModels are activity-scoped, so clear stale credentials after sign-out.
            LaunchedEffect(Unit) {
                loginViewModel.reset()
                forgotViewModel.reset()
            }
            LoginScreen(
                loginViewModel = loginViewModel,
                forgotPasswordViewModel = forgotViewModel,
                devToolsConfig = devToolsConfig,
                environmentStore = environmentStore,
                onApiEnvironmentChanged = { apiReloadKey++ },
            )
        }
        is SessionPhase.SignedIn -> {
            MainTabShell(
                sessionStore = sessionStore,
                signedIn = current,
                reloadKey = apiReloadKey,
            )
        }
    }

    if (showApiLogs && devToolsConfig.isDevToolsEnabled) {
        ApiLogsSheet(onDismiss = { showApiLogs = false })
    }
}

@Composable
private fun MainTabShell(
    sessionStore: SessionStore,
    signedIn: SessionPhase.SignedIn,
    reloadKey: Int,
) {
    var tab by remember { mutableStateOf(AppTab.Home) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (selectedCourse != null) {
            CourseDetailScreen(course = selectedCourse!!, onBack = { selectedCourse = null })
        } else {
            TabCrossfade(selected = tab, modifier = Modifier.fillMaxSize()) { currentTab ->
                when (currentTab) {
                    AppTab.Home -> {
                        val vm: HomeViewModel = hiltViewModel(key = "home-$reloadKey")
                        HomeScreen(viewModel = vm, user = signedIn.user, onScan = { tab = AppTab.Scan })
                    }
                    AppTab.Courses -> {
                        val vm: CourseListViewModel = hiltViewModel(key = "courses-$reloadKey")
                        CourseListScreen(viewModel = vm, onCourseSelected = { selectedCourse = it })
                    }
                    AppTab.Scan -> {
                        val vm: AttendanceViewModel = hiltViewModel(key = "scan-$reloadKey")
                        LaunchedEffect(signedIn) {
                            vm.configure(signedIn.attendanceLocationEnabled, signedIn.campusFence)
                        }
                        AttendanceScreen(viewModel = vm, attendanceLocationEnabled = signedIn.attendanceLocationEnabled)
                    }
                    AppTab.Profile -> {
                        val vm: ProfileViewModel = hiltViewModel(key = "profile-$reloadKey")
                        ProfileScreen(
                            viewModel = vm,
                            sessionStore = sessionStore,
                            user = signedIn.user,
                            organization = signedIn.organization,
                        )
                    }
                }
            }
            FloatingDock(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
