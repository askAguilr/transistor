package com.example.lovandroidwrapper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lovandroidwrapper.data.AppDatabase
import com.example.lovandroidwrapper.data.TransistorApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreenComponent(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = db.appDao()

    var transistorApp by remember { mutableStateOf<TransistorApp?>(null) }

    // State variables
    var title by remember { mutableStateOf("") }
    var toastEnabled by remember { mutableStateOf(true) }
    var locationEnabled by remember { mutableStateOf(true) }
    var fileChooserEnabled by remember { mutableStateOf(true) }
    var appsEnabled by remember { mutableStateOf(true) }
    var offlineMode by remember { mutableStateOf(false) }
    var orientation by remember { mutableStateOf("UNSPECIFIED") }
    var immersiveMode by remember { mutableStateOf(false) }

    var isScraping by remember { mutableStateOf(false) }
    var scrapeProgress by remember { mutableStateOf("") }

    // Load initial data
    LaunchedEffect(url) {
        val app = dao.getAppByUrl(url)
        if (app != null) {
            transistorApp = app
            title = app.title
            toastEnabled = app.toastEnabled
            locationEnabled = app.locationEnabled
            fileChooserEnabled = app.fileChooserEnabled
            appsEnabled = app.appsEnabled
            offlineMode = app.offlineMode
            orientation = app.orientation
            immersiveMode = app.immersiveMode
        } else {
            // Default settings for new app
            title = "New Web App"
            transistorApp = TransistorApp(url = url, title = title)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val updatedApp = TransistorApp(
                                url = url,
                                title = title.ifBlank { "Web App" },
                                iconUrl = transistorApp?.iconUrl,
                                lastAccessed = System.currentTimeMillis(),
                                toastEnabled = toastEnabled,
                                locationEnabled = locationEnabled,
                                fileChooserEnabled = fileChooserEnabled,
                                offlineMode = offlineMode,
                                orientation = orientation,
                                immersiveMode = immersiveMode,
                                appsEnabled = appsEnabled
                            )
                            dao.insertApp(updatedApp)
                            onBack()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("App Info", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("App Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = {},
                        label = { Text("App URL (Read-only)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false
                    )
                }
            }

            // Sandbox Permissions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Sandbox Permissions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Toast Plugin", style = MaterialTheme.typography.bodyLarge)
                            Text("Allow native Toast alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = toastEnabled, onCheckedChange = { toastEnabled = it })
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Location Access", style = MaterialTheme.typography.bodyLarge)
                            Text("Allow access to geolocation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = locationEnabled, onCheckedChange = { locationEnabled = it })
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("File Chooser", style = MaterialTheme.typography.bodyLarge)
                            Text("Allow uploading native files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = fileChooserEnabled, onCheckedChange = { fileChooserEnabled = it })
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Installed Apps Launcher", style = MaterialTheme.typography.bodyLarge)
                            Text("Allow listing, launching & uninstalling device apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = appsEnabled, onCheckedChange = { appsEnabled = it })
                    }
                }
            }

            // Offline Caching & Scraper Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Offline Archiver", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offline Mode", style = MaterialTheme.typography.bodyLarge)
                            Text("Serve assets from local cache", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = offlineMode, onCheckedChange = { offlineMode = it })
                    }

                    Button(
                        onClick = {
                            isScraping = true
                            scrapeProgress = "Initializing scraper..."
                            scope.launch {
                                try {
                                    val success = com.example.lovandroidwrapper.utils.WebScraper.scrapeAndCache(context, url) { progress ->
                                        scrapeProgress = progress
                                    }
                                    if (success) {
                                        scrapeProgress = "Archive built successfully!"
                                    } else {
                                        scrapeProgress = "Archive failed to download completely."
                                    }
                                } catch (e: Exception) {
                                    scrapeProgress = "Scraping failed: ${e.localizedMessage}"
                                } finally {
                                    isScraping = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScraping
                    ) {
                        if (isScraping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                        Text(if (isScraping) "Archiving..." else "Download for Offline Use")
                    }

                    if (scrapeProgress.isNotEmpty()) {
                        Text(
                            text = scrapeProgress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (scrapeProgress.contains("failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Screen Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Display Options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Immersive Fullscreen", style = MaterialTheme.typography.bodyLarge)
                            Text("Hide status and navigation bars", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = immersiveMode, onCheckedChange = { immersiveMode = it })
                    }

                    HorizontalDivider()

                    Text("Orientation Lock", style = MaterialTheme.typography.bodyMedium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf("UNSPECIFIED", "PORTRAIT", "LANDSCAPE")
                        options.forEach { opt ->
                            FilterChip(
                                selected = orientation == opt,
                                onClick = { orientation = opt },
                                label = { Text(opt) }
                            )
                        }
                    }
                }
            }

            // Danger Zone
            if (transistorApp != null) {
                Button(
                    onClick = {
                        scope.launch {
                            dao.deleteApp(transistorApp!!)
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete app")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove App from Library")
                }
            }
        }
    }
}
