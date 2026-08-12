package com.example.lovandroidwrapper.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.example.lovandroidwrapper.AppSettingsScreen
import com.example.lovandroidwrapper.WebViewScreen
import com.example.lovandroidwrapper.data.AppDatabase
import com.example.lovandroidwrapper.data.TransistorApp
import com.example.lovandroidwrapper.utils.ShortcutHelper
import com.example.lovandroidwrapper.utils.IconLoader
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val db = remember { AppDatabase.getDatabase(context) }
  val dao = db.appDao()

  val apps by dao.getAllApps().collectAsState(initial = emptyList())
  var inputUrl by remember { mutableStateOf("http://localhost:5173") }
  var inputTitle by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = "Transistor Library",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Your custom web apps, native capabilities",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline
        )
      }
      IconButton(onClick = { showAddDialog = true }) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add new app")
      }
    }

    // Quick Launch Address Bar
    Card(
      modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Quick Launch", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = inputUrl,
            onValueChange = { inputUrl = it },
            label = { Text("Enter Web URL") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          Button(
            onClick = {
              if (inputUrl.isNotBlank()) {
                val formattedUrl = formatUrl(inputUrl)
                scope.launch {
                  // Ensure it exists in db
                  val existing = dao.getAppByUrl(formattedUrl)
                  if (existing == null) {
                    val name = extractHost(formattedUrl)
                    dao.insertApp(TransistorApp(url = formattedUrl, title = name))
                  } else {
                    dao.insertApp(existing.copy(lastAccessed = System.currentTimeMillis()))
                  }
                  onItemClick(WebViewScreen(formattedUrl))
                }
              }
            },
            modifier = Modifier.height(56.dp)
          ) {
            Text("Go")
          }
        }
      }
    }

    // Grid of apps
    if (apps.isEmpty()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Public,
            contentDescription = "Empty",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text("No apps in library yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
          Text("Add one or quick launch a URL above", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
      }
    } else {
      Text(
        text = "Pinned & Recent Apps",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.onSurface
      )
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(apps) { app ->
          AppCard(
            app = app,
            onLaunch = {
              scope.launch {
                dao.insertApp(app.copy(lastAccessed = System.currentTimeMillis()))
                onItemClick(WebViewScreen(app.url))
              }
            },
            onSettings = { onItemClick(AppSettingsScreen(app.url)) },
            onShortcut = { ShortcutHelper.createShortcut(context, app) },
            onDelete = { scope.launch { dao.deleteApp(app) } }
          )
        }
      }
    }
  }

  // Add App Dialog
  if (showAddDialog) {
    var newUrl by remember { mutableStateOf("http://") }
    var newTitle by remember { mutableStateOf("") }
    var isUrlError by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add Web App to Library") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("App Title (e.g. My Demo)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          OutlinedTextField(
            value = newUrl,
            onValueChange = {
              newUrl = it
              isUrlError = false
            },
            label = { Text("Web App URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isUrlError
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newUrl.isBlank() || newUrl == "http://" || newUrl == "https://") {
              isUrlError = true
            } else {
              val formattedUrl = formatUrl(newUrl)
              val finalTitle = newTitle.ifBlank { extractHost(formattedUrl) }
              scope.launch {
                dao.insertApp(TransistorApp(url = formattedUrl, title = finalTitle))
                showAddDialog = false
              }
            }
          }
        ) {
          Text("Add App")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun AppCard(
  app: TransistorApp,
  onLaunch: () -> Unit,
  onSettings: () -> Unit,
  onShortcut: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onLaunch() },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
  ) {
    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
      // Menu Icon at top right
      Box(modifier = Modifier.align(Alignment.TopEnd)) {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false }
        ) {
          DropdownMenuItem(
            text = { Text("App Settings") },
            onClick = {
              menuExpanded = false
              onSettings()
            },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
          )
          DropdownMenuItem(
            text = { Text("Pin to Home Screen") },
            onClick = {
              menuExpanded = false
              onShortcut()
            },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
          )
          DropdownMenuItem(
            text = { Text("Remove App") },
            onClick = {
              menuExpanded = false
              onDelete()
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
          )
        }
      }

      // Card Content
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
      ) {
        // Icon / Favicon Loader
        var iconBitmap by remember(app.iconUrl) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(app.iconUrl) {
          if (!app.iconUrl.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
              iconBitmap = IconLoader.loadIcon(app.iconUrl, 128, 128)
            }
          } else {
            iconBitmap = null
          }
        }

        if (iconBitmap != null) {
          Image(
            bitmap = iconBitmap!!.asImageBitmap(),
            contentDescription = "App Icon",
            modifier = Modifier.size(48.dp).padding(bottom = 8.dp)
          )
        } else {
          Icon(
            imageVector = Icons.Default.Public,
            contentDescription = "App Icon",
            modifier = Modifier.size(48.dp).padding(bottom = 8.dp),
            tint = MaterialTheme.colorScheme.primary
          )
        }

        Text(
          text = app.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Text(
          text = app.url,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    }
  }
}

// Helpers
private fun formatUrl(url: String): String {
  var formatted = url.trim()
  if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
    formatted = "http://$formatted"
  }
  return formatted
}

private fun extractHost(url: String): String {
  return try {
    val uri = URI(url)
    val host = uri.host
    if (!host.isNullOrEmpty()) {
      if (host.startsWith("www.")) host.substring(4) else host
    } else {
      url
    }
  } catch (e: Exception) {
    url
  }
}
