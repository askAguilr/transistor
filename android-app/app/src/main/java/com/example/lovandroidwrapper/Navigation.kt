package com.example.lovandroidwrapper

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.lovandroidwrapper.ui.main.MainScreen
import com.example.lovandroidwrapper.ui.webview.WebViewScreenComponent
import com.example.lovandroidwrapper.ui.settings.AppSettingsScreenComponent

@Composable
fun MainNavigation(startUrl: String? = null) {
  val initialKey = if (!startUrl.isNullOrBlank()) WebViewScreen(startUrl) else Main
  val backStack = rememberNavBackStack(initialKey)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.safeDrawingPadding().padding(16.dp))
        }
        entry<WebViewScreen> { key ->
          WebViewScreenComponent(url = key.url)
        }
        entry<AppSettingsScreen> { key ->
          AppSettingsScreenComponent(url = key.url, onBack = { backStack.removeLastOrNull() })
        }
      },
  )
}
