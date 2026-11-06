package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.WhatsWebScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
                var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

                // File Chooser launcher for image/document attachments in WhatsApp Web
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data
                        val uris: Array<Uri>? = when {
                            data?.clipData != null -> {
                                val clip = data.clipData!!
                                Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                            }
                            data?.data != null -> arrayOf(data.data!!)
                            else -> null
                        }
                        pendingFileCallback?.onReceiveValue(uris)
                    } else {
                        pendingFileCallback?.onReceiveValue(null)
                    }
                    pendingFileCallback = null
                }

                // Media Permission launcher for Camera & Microphone in WhatsApp Web
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    val req = pendingPermissionRequest
                    if (req != null) {
                        val grantedList = mutableListOf<String>()
                        val cameraGranted = grants[Manifest.permission.CAMERA] == true ||
                                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true ||
                                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

                        for (r in req.resources) {
                            if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE && cameraGranted) {
                                grantedList.add(r)
                            } else if (r == PermissionRequest.RESOURCE_AUDIO_CAPTURE && audioGranted) {
                                grantedList.add(r)
                            }
                        }

                        if (grantedList.isNotEmpty()) {
                            req.grant(grantedList.toTypedArray())
                        } else {
                            req.deny()
                        }
                        pendingPermissionRequest = null
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    WhatsWebScreen(
                        onShowFileChooser = { callback, intent ->
                            pendingFileCallback = callback
                            val chooserIntent = intent ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                            }
                            try {
                                filePickerLauncher.launch(chooserIntent)
                            } catch (_: Exception) {
                                callback?.onReceiveValue(null)
                                pendingFileCallback = null
                            }
                        },
                        onPermissionRequest = { req ->
                            pendingPermissionRequest = req
                            val needsCamera = req.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                            val needsAudio = req.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

                            val permissionsToRequest = mutableListOf<String>()
                            if (needsCamera && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                permissionsToRequest.add(Manifest.permission.CAMERA)
                            }
                            if (needsAudio && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                            }

                            if (permissionsToRequest.isNotEmpty()) {
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            } else {
                                req.grant(req.resources)
                                pendingPermissionRequest = null
                            }
                        }
                    )
                }
            }
        }
    }
}
