package com.example.lovandroidwrapper

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data class WebViewScreen(val url: String) : NavKey

@Serializable data class AppSettingsScreen(val url: String) : NavKey


