package com.apilatam.wrap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

  private lateinit var web: WebView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    web = findViewById(R.id.web)
    setupWeb()
    web.loadUrl(BuildConfig.APP_URL)
    initFirebase()
    registerToken()
    intent?.extras?.getString("url")?.let { redirect(it) }
  }

  private var permAsked = false

  override fun onResume() {
    super.onResume()
    if (!permAsked) {
      permAsked = true
      Handler(Looper.getMainLooper()).postDelayed({ askNotifications() }, 700)
    }
  }

  private fun askNotifications() {
    if (Build.VERSION.SDK_INT >= 33 &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    val granted = requestCode == 1001 && grantResults.isNotEmpty() &&
      grantResults[0] == PackageManager.PERMISSION_GRANTED
    postToWeb(if (granted) "perm-ok" else "perm-denied")
  }

  private fun initFirebase() {
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
          .setApplicationId("1:352238980841:android:a1b2c3d4e5f6a7b8")
          .setApiKey("AIzaSyA4lFjAcn7ebAZF9SkVfpm1RPYnThN8roA")
          .setProjectId("appforge-20549")
          .setGcmSenderId("352238980841")
          .setStorageBucket("appforge-20549.appspot.com")
          .build())
      }
    } catch (_: Exception) {
    }
  }

  private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= 33 &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.extras?.getString("url")?.let { redirect(it) }
  }

  private fun setupWeb() {
    val s = web.settings
    s.javaScriptEnabled = true
    s.domStorageEnabled = true
    s.loadWithOverviewMode = true
    s.useWideViewPort = true
    s.cacheMode = WebSettings.LOAD_DEFAULT
    web.webViewClient = object : WebViewClient() {
      override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) {
          runOnUiThread {
            try {
              Toast.makeText(this@MainActivity, "No se pudo cargar la app: " + (error.description ?: "error de red"), Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
            }
          }
        }
      }
      override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        when {
          url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:")
              || url.startsWith("sms:") || url.startsWith("geo:") -> {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            return true
          }
          else -> {
            view.loadUrl(url)
            return true
          }
        }
      }
    }
  }

  override fun onBackPressed() {
    if (web.canGoBack()) web.goBack() else super.onBackPressed()
  }

  private fun redirect(url: String?) {
    if (!url.isNullOrBlank()) web.loadUrl(url)
  }

  private fun registerToken() {
    postToWeb("iniciando")
    Thread {
      try {
        FirebaseMessaging.getInstance().token
          .addOnCompleteListener { t ->
            if (t.isSuccessful) {
              postToWeb("token-ok")
              postToken(t.result)
            } else {
              postToWeb("token-err: " + (t.exception?.message ?: "desconocido"))
            }
          }
      } catch (e: Exception) {
        postToWeb("fcm-excep: " + (e.message ?: e.toString()))
      }
    }.start()
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
      val status = conn.responseCode
      conn.inputStream.use { it.close() }
      if (status in 200..299) postToWeb("registrado") else postToWeb("registro-" + status)
    } catch (e: Exception) {
      postToWeb("registro-err: " + (e.message ?: ""))
    }
  }

  private fun postToWeb(msg: String) {
    val safe = msg.replace("'", " ").replace("\\", " ").take(120)
    runOnUiThread {
      try {
        Toast.makeText(this, "FCM: $safe", Toast.LENGTH_LONG).show()
      } catch (_: Exception) {
      }
    }
    Thread {
      var i = 0
      while (i < 10) {
        runOnUiThread {
          try { web.evaluateJavascript("try{window.__fcmStatus=window.__fcmStatus||function(){};window.__fcmStatus('$safe')}catch(e){}", null) } catch (_: Exception) {}
        }
        i++
        try { Thread.sleep(2000) } catch (_: Exception) {}
      }
    }.start()
  }
}