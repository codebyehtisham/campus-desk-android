package co.archer.sdk

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Optional FCM stub. Host apps that depend on `firebase-messaging` can register this
 * service (or subclass it) and forward tokens / data messages to Archer.
 *
 * Declare in the host manifest when firebase-messaging is on the classpath:
 * ```xml
 * <service
 *   android:name="co.archer.sdk.ArcherFirebaseMessagingService"
 *   android:exported="false">
 *   <intent-filter>
 *     <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *   </intent-filter>
 * </service>
 * ```
 */
open class ArcherFirebaseMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d("Archer", "FCM token refresh len=${token.length}")
    Archer.setPushToken(token)
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val data = message.data
    if (data.isEmpty()) return
    val payload = ArcherPushPayloadParser.parseFcmData(data)
      ?: Archer.handlePushNotification(data)
    if (payload != null) {
      Log.d("Archer", "FCM Archer push messageId=${payload.messageId}")
    }
  }
}
