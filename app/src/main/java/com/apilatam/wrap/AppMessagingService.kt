package com.apilatam.wrap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL

class AppMessagingService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    postToken(token)
  }

  override fun onMessageReceived(remote: RemoteMessage) {
    val title = remote.notification?.title ?: remote.data["title"] ?: "AppForge"
    val body = remote.notification?.body ?: remote.data["body"] ?: ""
    val url = remote.data["url"] ?: BuildConfig.APP_URL
    show(title, body, url)
  }

  private fun show(title: String, body: String, url: String) {
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= 26) {
      val ch = NotificationChannel(CHANNEL, "Notificaciones de la app", NotificationManager.IMPORTANCE_HIGH)
      nm.createNotificationChannel(ch)
    }
    val intent = Intent(this, MainActivity::class.java).apply {
      action = Intent.ACTION_VIEW
      data = Uri.parse(url)
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra("url", url)
    }
    val pi = PendingIntent.getActivity(this, url.hashCode() and 0x7fffffff, intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val n = NotificationCompat.Builder(this, CHANNEL)
      .setSmallIcon(R.drawable.ic_app)
      .setContentTitle(title)
      .setContentText(body)
      .setAutoCancel(true)
      .setContentIntent(pi)
      .build()
    nm.notify(url.hashCode(), n)
  }

  private fun postToken(token: String) {
    try {
      val out = org.json.JSONObject()
        .put("appId", BuildConfig.APP_ID)
        .put("token", token)
        .put("platform", "android")
        .toString()
      val url = URL(BuildConfig.API_URL + "/api/token")
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
      }
      conn.outputStream.use { it.write(out.toByteArray()) }
      conn.inputStream.use { it.close() }
    } catch (_: Exception) {
    }
  }

  companion object {
    private const val CHANNEL = "appforge_push"
  }
}