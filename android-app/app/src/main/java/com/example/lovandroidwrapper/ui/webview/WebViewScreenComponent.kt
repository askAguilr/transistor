package com.example.lovandroidwrapper.ui.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lovandroidwrapper.data.AppDatabase
import com.example.lovandroidwrapper.data.TransistorApp
import com.example.lovandroidwrapper.plugins.CorePlugin
import com.example.lovandroidwrapper.plugins.ToastPlugin
import com.example.lovandroidwrapper.plugins.MetadataPlugin
import com.example.lovandroidwrapper.plugins.AppsPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreenComponent(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    val db = remember { AppDatabase.getDatabase(context) }
    val dao = db.appDao()

    val appSettingsState = dao.getAppByUrlFlow(url).collectAsState(initial = null)
    val appSettings = appSettingsState.value ?: TransistorApp(url = url, title = "Web App")

    // UI and Logic states
    var isLoading by remember { mutableStateOf(true) }
    var extractedThemeColor by remember { mutableStateOf<Int?>(null) }
    var devHudOpen by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }

    // File Chooser state
    var activeFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            activeFilePathCallback?.onReceiveValue(uris.toTypedArray())
        } else {
            activeFilePathCallback?.onReceiveValue(null)
        }
        activeFilePathCallback = null
    }

    // Orientation Lock effect
    DisposableEffect(appSettings.orientation) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = when (appSettings.orientation) {
            "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Immersive Mode effect
    DisposableEffect(appSettings.immersiveMode) {
        if (appSettings.immersiveMode) {
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
        onDispose {
            if (appSettings.immersiveMode) {
                activity?.window?.let { window ->
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // WebView container
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    if (appSettings.offlineMode) {
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                    } else {
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        clearCache(true)
                    }

                    webViewClient = TransistorWebViewClient(
                        context = ctx,
                        offlineMode = appSettings.offlineMode,
                        onPageFinishedCallback = {
                            isLoading = false
                        },
                        onThemeColorExtracted = { color ->
                            extractedThemeColor = color
                            if (color != null) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val hexColor = String.format("#%06X", 0xFFFFFF and color)
                                        val existingApp = dao.getAppByUrl(url)
                                        if (existingApp != null && existingApp.themeColor != hexColor) {
                                            dao.insertApp(existingApp.copy(themeColor = hexColor))
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    )

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            if (appSettings.fileChooserEnabled) {
                                activeFilePathCallback = filePathCallback
                                filePickerLauncher.launch("*/*")
                                return true
                            }
                            return false
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                val level = it.messageLevel().name
                                val msg = it.message()
                                val src = it.sourceId().substringAfterLast("/")
                                val line = it.lineNumber()
                                val logLine = "[$level] $msg ($src:$line)"
                                scope.launch(Dispatchers.Main) {
                                    logs.add(logLine)
                                }
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    // Sandbox permissions for JS interfaces
                    addJavascriptInterface(
                        CorePlugin(ctx, buildList {
                            add("TransistorCore")
                            if (appSettings.toastEnabled) add("TransistorToast")
                            if (appSettings.appsEnabled) add("TransistorApps")
                        }),
                        "TransistorCore"
                    )

                    if (appSettings.toastEnabled) {
                        addJavascriptInterface(ToastPlugin(ctx), "TransistorToast")
                    }

                    if (appSettings.appsEnabled) {
                        addJavascriptInterface(AppsPlugin(ctx), "TransistorApps")
                    }

                    addJavascriptInterface(
                        MetadataPlugin(ctx, dao, scope, url),
                        "TransistorMetadataBridge"
                    )

                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Splash Screen Overlay
        AnimatedVisibility(
            visible = isLoading,
            exit = fadeOut()
        ) {
            val splashBg = extractedThemeColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
            val splashContentColor = if (extractedThemeColor != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(splashBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = appSettings.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = splashContentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Transistor Container Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = splashContentColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = splashContentColor)
                }
            }
        }

        // Floating Dev HUD Toggle Button
        FloatingActionButton(
            onClick = { devHudOpen = !devHudOpen },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = if (devHudOpen) 260.dp else 0.dp), // Move up when HUD open
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Icon(
                imageVector = if (devHudOpen) Icons.Default.Close else Icons.Default.BugReport,
                contentDescription = "Toggle Developer HUD"
            )
        }

        // Developer HUD Overlay Drawer
        if (devHudOpen) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .align(Alignment.BottomCenter)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dev HUD Console",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { logs.clear() }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs")
                            }
                            IconButton(onClick = { devHudOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Log output list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (logs.isEmpty()) {
                            item {
                                Text(
                                    text = "No logs captured yet. Trigger console.log inside the web app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            items(logs) { log ->
                                val color = when {
                                    log.contains("[ERROR]") -> Color(0xFFE57373)
                                    log.contains("[WARNING]") -> Color(0xFFFFB74D)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
