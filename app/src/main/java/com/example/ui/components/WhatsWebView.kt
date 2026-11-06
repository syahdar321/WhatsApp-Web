package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

const val WHATSAPP_WEB_URL = "https://web.whatsapp.com/"
const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WhatsWebView(
    modifier: Modifier = Modifier,
    isDesktopMode: Boolean,
    zoomLevel: Int,
    onProgressChanged: (Int) -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onErrorReceived: (String) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, Intent?) -> Unit,
    onPermissionRequest: (PermissionRequest) -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    val context = LocalContext.current
    val mobileUserAgent = remember { WebSettings.getDefaultUserAgent(context) }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .testTag("whats_web_view"),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Enable Cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                // Configure WebSettings for optimal WhatsApp Web rendering
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    textZoom = zoomLevel
                    userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else mobileUserAgent
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.let { onPermissionRequest(it) }
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        val intent = fileChooserParams?.createIntent()
                        onShowFileChooser(filePathCallback, intent)
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onPageFinished()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            onErrorReceived(error?.description?.toString() ?: "Gagal memuat halaman")
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        val urlStr = uri.toString()

                        return when {
                            urlStr.startsWith("whatsapp:") ||
                            urlStr.startsWith("intent:") ||
                            urlStr.startsWith("tel:") ||
                            urlStr.startsWith("mailto:") -> {
                                try {
                                    val intent = Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME)
                                    ctx.startActivity(intent)
                                } catch (_: Exception) {
                                }
                                true
                            }
                            urlStr.contains("web.whatsapp.com") || urlStr.contains("whatsapp.com") -> {
                                false
                            }
                            else -> {
                                // External link
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                                    ctx.startActivity(browserIntent)
                                    true
                                } catch (_: Exception) {
                                    false
                                }
                            }
                        }
                    }
                }

                loadUrl(WHATSAPP_WEB_URL)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            val targetUserAgent = if (isDesktopMode) DESKTOP_USER_AGENT else mobileUserAgent
            if (webView.settings.userAgentString != targetUserAgent) {
                webView.settings.userAgentString = targetUserAgent
                webView.reload()
            }
            if (webView.settings.textZoom != zoomLevel) {
                webView.settings.textZoom = zoomLevel
            }
        }
    )
}
