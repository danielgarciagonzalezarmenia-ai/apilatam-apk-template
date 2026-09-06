package com.apilatam.wrap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
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
    registerToken()
    intent?.extras?.getString("url")?.let { redirect(it) }
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
    Thread {
      try {
        FirebaseMessaging.getInstance().token
          .addOnCompleteListener { t ->
            if (t.isSuccessful) postToken(t.result)
          }
      } catch (_: Exception) {
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
      val url = URL("${BuildConfig.API_URL}/api/token")
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
}