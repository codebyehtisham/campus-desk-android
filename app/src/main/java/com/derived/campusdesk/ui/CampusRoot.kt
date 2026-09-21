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
import com.derived.campusdesk.courses.ClassDetailViewModel
import com.derived.campusdesk.courses.ClassListViewModel
import com.derived.campusdesk.courses.assignments.AssignmentDetailViewModel
import com.derived.campusdesk.courses.assignments.AssignmentDetailScreen
import com.derived.campusdesk.courses.assignments.AssignmentListScreen
import com.derived.campusdesk.courses.assignments.AssignmentListViewModel
import com.derived.campusdesk.courses.leaves.ApplyLeaveScreen
import com.derived.campusdesk.courses.leaves.ApplyLeaveViewModel
import com.derived.campusdesk.courses.leaves.LeaveDetailScreen
import com.derived.campusdesk.courses.leaves.LeaveListScreen
import com.derived.campusdesk.courses.leaves.LeaveListViewModel
import com.derived.campusdesk.courses.notifications.NotificationsScreen
import com.derived.campusdesk.courses.notifications.NotificationsViewModel
import com.derived.campusdesk.courses.ui.ClassDetailScreen
import com.derived.campusdesk.courses.ui.ClassListScreen
import com.derived.campusdesk.home.HomeViewModel
import com.derived.campusdesk.home.ui.HomeScreen
import com.derived.campusdesk.networking.analytics.ArcherAnalytics
import com.derived.campusdesk.networking.analytics.ArcherScreenAnalytics
import com.derived.campusdesk.networking.api.ApiEnvironmentStore
import com.derived.campusdesk.networking.debug.DevToolsConfig
import com.derived.campusdesk.networking.models.StudentAssignment
import com.derived.campusdesk.networking.models.StudentClass
import com.derived.campusdesk.networking.models.StudentLeave
import com.derived.campusdesk.profile.ProfileViewModel
import com.derived.campusdesk.profile.ui.ProfileScreen
import com.derived.campusdesk.sharedui.components.AppTab
import com.derived.campusdesk.sharedui.components.CampusLoadingView
import com.derived.campusdesk.sharedui.components.FloatingDock
import com.derived.campusdesk.sharedui.components.TabCrossfade
import com.derived.campusdesk.ui.debug.ApiLogsSheet
import com.derived.campusdesk.ui.debug.ShakeEvents

private sealed interface PushRoute {
    data class ClassDetail(val id: String, val seed: StudentClass? = null) : PushRoute
    data class AssignmentDetail(val id: String, val seed: StudentAssignment? = null) : PushRoute
    data object LeaveList : PushRoute
    data class LeaveDetail(val leave: StudentLeave) : PushRoute
    data class ApplyLeave(val classId: String? = null) : PushRoute
    data object Notifications : PushRoute
}

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
        SessionPhase.Launching -> Box(Modifier.fillMaxSize()) {
            CampusLoadingView(style = com.derived.campusdesk.sharedui.components.DerivedLoaderStyle.Splash)
        }
        SessionPhase.SignedOut -> {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val forgotViewModel: ForgotPasswordViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                ArcherScreenAnalytics.setTab("login")
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
    var push by remember { mutableStateOf<PushRoute?>(null) }
    val leaveListVm: LeaveListViewModel = hiltViewModel(key = "leaves-$reloadKey")

    LaunchedEffect(tab, push) {
        when (val route = push) {
            is PushRoute.ClassDetail -> ArcherScreenAnalytics.push("class_detail")
            is PushRoute.AssignmentDetail -> ArcherScreenAnalytics.push("assignment_detail")
            PushRoute.LeaveList -> ArcherScreenAnalytics.push("leave_list")
            is PushRoute.LeaveDetail -> ArcherScreenAnalytics.push("leave_detail")
            is PushRoute.ApplyLeave -> ArcherScreenAnalytics.push("apply_leave")
            PushRoute.Notifications -> ArcherScreenAnalytics.push("notifications")
            null -> {
                val screen = when (tab) {
                    AppTab.Home -> "home"
                    AppTab.Classes -> "classes"
                    AppTab.Assignments -> "assignments"
                    AppTab.Attendance -> "attendance"
                    AppTab.Profile -> "profile"
                }
                ArcherScreenAnalytics.setTab(screen)
            }
        }
    }

    fun openLeaves() {
        ArcherAnalytics.button("open_leaves")
        push = PushRoute.LeaveList
    }
    fun openNotifications() {
        ArcherAnalytics.button("open_notifications")
        push = PushRoute.Notifications
    }
    fun openAssignment(id: String, seed: StudentAssignment? = null) {
        ArcherAnalytics.button("open_assignment", mapOf("assignment_id" to id))
        push = PushRoute.AssignmentDetail(id, seed)
    }
    fun openClass(item: StudentClass) {
        ArcherAnalytics.button("open_class", mapOf("class_id" to item.id.value))
        push = PushRoute.ClassDetail(item.id.value, item)
    }
    fun openLeaveDetail(leave: StudentLeave) {
        ArcherAnalytics.button("open_leave", mapOf("leave_id" to leave.id.value))
        push = PushRoute.LeaveDetail(leave)
    }
    fun openLeaveById(id: String) {
        val known = leaveListVm.leave(id)
        if (known != null) {
            push = PushRoute.LeaveDetail(known)
        } else {
            push = PushRoute.LeaveList
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (val route = push) {
            is PushRoute.ClassDetail -> {
                val vm: ClassDetailViewModel = hiltViewModel(key = "class-${route.id}-$reloadKey")
                ClassDetailScreen(
                    viewModel = vm,
                    classId = route.id,
                    seed = route.seed,
                    onBack = { push = null },
                    onAssignment = { openAssignment(it) },
                    onApplyLeave = { push = PushRoute.ApplyLeave(it) },
                    onLeaveDetail = { id ->
                        route.seed?.leaves?.firstOrNull { it.id.value == id }?.let { openLeaveDetail(it) }
                            ?: openLeaveById(id)
                    },
                )
            }
            is PushRoute.AssignmentDetail -> {
                val vm: AssignmentDetailViewModel = hiltViewModel(key = "assignment-${route.id}-$reloadKey")
                AssignmentDetailScreen(
                    viewModel = vm,
                    assignmentId = route.id,
                    seed = route.seed,
                    onBack = { push = null },
                )
            }
            PushRoute.LeaveList -> {
                LeaveListScreen(
                    viewModel = leaveListVm,
                    onBack = { push = null },
                    onApply = { push = PushRoute.ApplyLeave() },
                    onLeaveDetail = { openLeaveDetail(it) },
                )
            }
            is PushRoute.LeaveDetail -> {
                LeaveDetailScreen(
                    leave = route.leave,
                    onBack = { push = PushRoute.LeaveList },
                )
            }
            is PushRoute.ApplyLeave -> {
                val vm: ApplyLeaveViewModel = hiltViewModel(key = "apply-leave-$reloadKey")
                ApplyLeaveScreen(
                    viewModel = vm,
                    presetClassId = route.classId,
                    onBack = { push = null },
                    onSuccess = {
                        leaveListVm.load()
                        push = PushRoute.LeaveList
                    },
                )
            }
            PushRoute.Notifications -> {
                val vm: NotificationsViewModel = hiltViewModel(key = "notifications-$reloadKey")
                NotificationsScreen(
                    viewModel = vm,
                    onBack = { push = null },
                    onLeave = { openLeaveById(it) },
                )
            }
            null -> {
                TabCrossfade(selected = tab, modifier = Modifier.fillMaxSize()) { currentTab ->
                    when (currentTab) {
                        AppTab.Home -> {
                            val vm: HomeViewModel = hiltViewModel(key = "home-$reloadKey")
                            HomeScreen(
                                viewModel = vm,
                                user = signedIn.user,
                                onOpenClasses = { tab = AppTab.Classes },
                                onOpenAssignments = { tab = AppTab.Assignments },
                                onOpenLeaves = { openLeaves() },
                                onOpenAttendance = { tab = AppTab.Attendance },
                                onOpenNotifications = { openNotifications() },
                            )
                        }
                        AppTab.Classes -> {
                            val vm: ClassListViewModel = hiltViewModel(key = "classes-$reloadKey")
                            ClassListScreen(viewModel = vm, onClassSelected = { openClass(it) })
                        }
                        AppTab.Assignments -> {
                            val vm: AssignmentListViewModel = hiltViewModel(key = "assignments-$reloadKey")
                            AssignmentListScreen(
                                viewModel = vm,
                                onAssignmentSelected = { openAssignment(it.id.value, it) },
                            )
                        }
                        AppTab.Attendance -> {
                            val vm: AttendanceViewModel = hiltViewModel(key = "attendance-$reloadKey")
                            LaunchedEffect(signedIn) {
                                vm.configure(signedIn.attendanceLocationEnabled, signedIn.campusFence)
                            }
                            AttendanceScreen(
                                viewModel = vm,
                                attendanceLocationEnabled = signedIn.attendanceLocationEnabled,
                            )
                        }
                        AppTab.Profile -> {
                            val vm: ProfileViewModel = hiltViewModel(key = "profile-$reloadKey")
                            ProfileScreen(
                                viewModel = vm,
                                sessionStore = sessionStore,
                                user = signedIn.user,
                                organization = signedIn.organization,
                                onOpenLeaves = { openLeaves() },
                            )
                        }
                    }
                }
                FloatingDock(
                    selected = tab,
                    onSelect = {
                        ArcherAnalytics.button("tab_${it.name.lowercase()}")
                        tab = it
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
