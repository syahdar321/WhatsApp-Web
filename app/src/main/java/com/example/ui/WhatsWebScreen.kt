package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DirectChatSheet
import com.example.ui.components.GuideDialog
import com.example.ui.components.SettingsSheet
import com.example.ui.components.WHATSAPP_WEB_URL
import com.example.ui.components.WhatsWebView
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppDeepTeal
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsWebScreen(
    onShowFileChooser: (ValueCallback<Array<Uri>>?, Intent?) -> Unit,
    onPermissionRequest: (PermissionRequest) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isDesktopMode by remember { mutableStateOf(true) }
    var zoomLevel by remember { mutableIntStateOf(100) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog & Sheets states
    var showDirectChatSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val directChatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle Back Press
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            showExitDialog = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = !isFullscreen,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WhatsAppDarkGreen)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF25D366), CircleShape)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isDesktopMode) "Mode Desktop Aktif" else "Mode Seluler",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = WhatsAppDarkGreen,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        actions = {
                            // Direct Chat Quick Action
                            IconButton(
                                onClick = { showDirectChatSheet = true },
                                modifier = Modifier.testTag("action_direct_chat")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = stringResource(R.string.title_direct_chat)
                                )
                            }

                            // Quick QR Guide Action
                            IconButton(
                                onClick = { showGuideDialog = true },
                                modifier = Modifier.testTag("action_guide")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = stringResource(R.string.title_guide)
                                )
                            }

                            // Refresh
                            IconButton(
                                onClick = {
                                    errorMessage = null
                                    webViewInstance?.reload()
                                },
                                modifier = Modifier.testTag("action_refresh")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.btn_reload)
                                )
                            }

                            // Fullscreen Mode
                            IconButton(
                                onClick = {
                                    isFullscreen = true
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Mode layar penuh diaktifkan. Ketuk tombol mengambang untuk keluar.",
                                            withDismissAction = true
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("action_fullscreen")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Layar Penuh"
                                )
                            }

                            // Settings / Menu
                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier.testTag("action_settings")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.title_settings)
                                )
                            }
                        }
                    )

                    // Loading Progress Indicator
                    if (isLoading && loadProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { loadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = Color(0xFF25D366),
                            trackColor = WhatsAppDeepTeal
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(
                    if (isFullscreen) WindowInsets.navigationBars else WindowInsets(0, 0, 0, 0)
                )
        ) {
            // Main Web View
            WhatsWebView(
                isDesktopMode = isDesktopMode,
                zoomLevel = zoomLevel,
                onProgressChanged = { progress ->
                    loadProgress = progress
                    isLoading = progress < 100
                },
                onPageStarted = {
                    isLoading = true
                    errorMessage = null
                },
                onPageFinished = {
                    isLoading = false
                },
                onErrorReceived = { err ->
                    errorMessage = err
                    isLoading = false
                },
                onShowFileChooser = onShowFileChooser,
                onPermissionRequest = onPermissionRequest,
                onWebViewCreated = { webViewInstance = it }
            )

            // Floating Fullscreen Exit & Quick Access Pill
            if (isFullscreen) {
                Surface(
                    onClick = { isFullscreen = false },
                    color = WhatsAppDarkGreen.copy(alpha = 0.85f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp)
                        .testTag("fullscreen_exit_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Keluar Layar Penuh",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tampilkan Menu",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Error / Offline Screen
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SignalWifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = stringResource(R.string.offline_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.offline_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    webViewInstance?.loadUrl(WHATSAPP_WEB_URL)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppDarkGreen
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("offline_retry_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_retry))
                            }

                            OutlinedButton(
                                onClick = { showGuideDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("offline_guide_button")
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Panduan")
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct Chat Bottom Sheet
    if (showDirectChatSheet) {
        DirectChatSheet(
            sheetState = directChatSheetState,
            onDismiss = { showDirectChatSheet = false },
            onOpenInWebView = { fullPhone, msg ->
                val encodedMsg = try {
                    URLEncoder.encode(msg, StandardCharsets.UTF_8.name())
                } catch (_: Exception) {
                    ""
                }
                val url = if (encodedMsg.isNotEmpty()) {
                    "https://web.whatsapp.com/send?phone=$fullPhone&text=$encodedMsg"
                } else {
                    "https://web.whatsapp.com/send?phone=$fullPhone"
                }
                errorMessage = null
                webViewInstance?.loadUrl(url)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Membuka obrolan dengan +$fullPhone di WhatsWeb...")
                }
            },
            onOpenExternal = { fullPhone, msg ->
                try {
                    val encodedMsg = try {
                        URLEncoder.encode(msg, StandardCharsets.UTF_8.name())
                    } catch (_: Exception) {
                        ""
                    }
                    val url = "https://api.whatsapp.com/send?phone=$fullPhone&text=$encodedMsg"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Aplikasi WhatsApp tidak ditemukan pada perangkat ini.")
                    }
                }
            }
        )
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsSheet(
            sheetState = settingsSheetState,
            isDesktopMode = isDesktopMode,
            onDesktopModeChange = { enabled ->
                isDesktopMode = enabled
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (enabled) "Mode Desktop diaktifkan" else "Mode Seluler diaktifkan"
                    )
                }
            },
            zoomLevel = zoomLevel,
            onZoomChange = { newZoom ->
                zoomLevel = newZoom
            },
            onReload = {
                errorMessage = null
                webViewInstance?.reload()
            },
            onClearCache = {
                showClearCacheDialog = true
            },
            onOpenGuide = {
                showGuideDialog = true
            },
            onOpenDirectChat = {
                showDirectChatSheet = true
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    // How-To Guide Dialog
    if (showGuideDialog) {
        GuideDialog(onDismiss = { showGuideDialog = false })
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Hapus Sesi & Data Cache?") },
            text = {
                Text(
                    "Tindakan ini akan menghapus sesi login WhatsApp Web, cookie, dan data cache lokal. Anda perlu memindai kode QR kembali setelah dihapus."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        webViewInstance?.clearCache(true)
                        webViewInstance?.clearFormData()
                        webViewInstance?.clearHistory()
                        WebStorage.getInstance().deleteAllData()
                        CookieManager.getInstance().removeAllCookies {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Cache dan sesi berhasil dibersihkan.")
                            }
                            webViewInstance?.loadUrl(WHATSAPP_WEB_URL)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_clear_cache_button")
                ) {
                    Text("Hapus & Muat Ulang")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearCacheDialog = false },
                    modifier = Modifier.testTag("cancel_clear_cache_button")
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.dialog_exit_title)) },
            text = { Text(stringResource(R.string.dialog_exit_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen),
                    modifier = Modifier.testTag("confirm_exit_button")
                ) {
                    Text(stringResource(R.string.btn_exit_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false },
                    modifier = Modifier.testTag("cancel_exit_button")
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
