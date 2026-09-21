package com.derived.campusdesk.analytics

import android.app.Activity
import android.util.Log
import co.archer.sdk.Archer
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Archer push activation + FCM token registration.
 * Requires `app/google-services.json` (same Firebase project as Archer Console → Push).
 */
object ArcherPushBootstrap {
    private const val TAG = "ArcherPush"

    fun activate(activity: Activity) {
        Archer.activatePush(activity, requestAuthorization = true)
        registerFcmToken(activity)
    }

    fun registerFcmToken(activity: Activity) {
        try {
            if (FirebaseApp.getApps(activity).isEmpty()) {
                Log.w(TAG, "Firebase not configured — add app/google-services.json to enable FCM")
                return
            }
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (token.isNullOrBlank()) return@addOnSuccessListener
                    Log.d(TAG, "FCM token len=${token.length}")
                    Archer.setPushToken(token)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "FCM token fetch failed", e)
                }
        } catch (t: Throwable) {
            Log.w(TAG, "FCM unavailable", t)
        }
    }
}
