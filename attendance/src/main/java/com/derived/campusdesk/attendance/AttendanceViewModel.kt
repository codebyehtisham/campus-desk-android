package com.derived.campusdesk.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.models.AttendanceSession
import com.derived.campusdesk.networking.models.CampusFence
import com.derived.campusdesk.networking.models.LocationFix
import com.derived.campusdesk.networking.models.QRScanPayload
import com.derived.campusdesk.networking.services.AttendanceService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

enum class ScanPhase { Idle, Confirming, Success }

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceService: AttendanceService,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _phase = MutableStateFlow(ScanPhase.Idle)
    val phase: StateFlow<ScanPhase> = _phase.asStateFlow()

    private val _manualCode = MutableStateFlow("")
    val manualCode: StateFlow<String> = _manualCode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _session = MutableStateFlow<AttendanceSession?>(null)
    val session: StateFlow<AttendanceSession?> = _session.asStateFlow()

    private val _lastFix = MutableStateFlow<LocationFix?>(null)
    val lastFix: StateFlow<LocationFix?> = _lastFix.asStateFlow()

    /** UI should request location permissions when this becomes true. */
    private val _needsLocationPermission = MutableStateFlow(false)
    val needsLocationPermission: StateFlow<Boolean> = _needsLocationPermission.asStateFlow()

    private var attendanceLocationEnabled = false
    private var campusFence: CampusFence? = null
    private var lastScannedToken: String? = null
    private var pendingQrToken: String? = null

    fun configure(attendanceLocationEnabled: Boolean, campusFence: CampusFence?) {
        this.attendanceLocationEnabled = attendanceLocationEnabled
        this.campusFence = campusFence
    }

    fun onManualCodeChange(value: String) {
        _manualCode.value = value
    }

    fun handleScan(raw: String) {
        if (_isLoading.value || _phase.value == ScanPhase.Success) return
        val payload = QRScanPayload.parse(raw) ?: run {
            _errorMessage.value = "That QR code is not a valid attendance session."
            return
        }
        if (payload.qrToken == lastScannedToken && _phase.value == ScanPhase.Success) return
        confirm(payload.qrToken)
    }

    fun submitManual() {
        val code = _manualCode.value.trim()
        if (code.isBlank()) return
        handleScan(code)
    }

    /** Call after the system location permission dialog returns. */
    fun onLocationPermissionResult(granted: Boolean) {
        _needsLocationPermission.value = false
        val pending = pendingQrToken
        pendingQrToken = null
        if (!granted) {
            _errorMessage.value = "Location permission is required to mark attendance on this campus."
            _phase.value = ScanPhase.Idle
            _isLoading.value = false
            return
        }
        if (pending != null) {
            confirm(pending, locationAlreadyGranted = true)
        }
    }

    fun reset() {
        _phase.value = ScanPhase.Idle
        _errorMessage.value = null
        _successMessage.value = null
        _session.value = null
        _lastFix.value = null
        lastScannedToken = null
        pendingQrToken = null
        _needsLocationPermission.value = false
    }

    private fun confirm(qrToken: String, locationAlreadyGranted: Boolean = false) {
        viewModelScope.launch {
            _phase.value = ScanPhase.Confirming
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val fix = if (attendanceLocationEnabled) {
                    if (!locationAlreadyGranted && !locationProvider.hasLocationPermission()) {
                        pendingQrToken = qrToken
                        _needsLocationPermission.value = true
                        return@launch
                    }
                    locationProvider.currentFix(campusFence)
                        ?: throw IllegalStateException(
                            "Couldn't read your location. Turn on location services and try again.",
                        )
                } else {
                    null
                }
                _lastFix.value = fix
                val result = attendanceService.scanQr(qrToken, fix)
                _successMessage.value = result.displayMessage
                _session.value = result.session
                _lastFix.value = fix?.copy(
                    onCampus = result.onCampus ?: fix.onCampus,
                    distanceMeters = result.distanceMeters ?: fix.distanceMeters,
                ) ?: _lastFix.value
                lastScannedToken = qrToken
                _phase.value = ScanPhase.Success
            } catch (e: SecurityException) {
                pendingQrToken = qrToken
                _needsLocationPermission.value = true
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not mark attendance."
                _phase.value = ScanPhase.Idle
            } finally {
                if (!_needsLocationPermission.value) {
                    _isLoading.value = false
                }
            }
        }
    }
}

interface LocationProvider {
    fun hasLocationPermission(): Boolean
    suspend fun currentFix(fence: CampusFence?): LocationFix?
}

class FusedLocationProvider(private val context: Context) : LocationProvider {
    override fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun currentFix(fence: CampusFence?): LocationFix? {
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission not granted")
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val token = CancellationTokenSource()
        val location: Location? = withTimeoutOrNull(8_000) {
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
            } catch (_: SecurityException) {
                null
            } ?: runCatching {
                client.lastLocation.await()
            }.getOrNull()
        }
        return location?.let {
            LocationFix(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy.toDouble(),
            ).withFence(fence)
        }
    }
}
