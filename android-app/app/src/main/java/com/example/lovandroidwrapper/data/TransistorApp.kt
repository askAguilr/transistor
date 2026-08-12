package com.example.lovandroidwrapper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transistor_apps")
data class TransistorApp(
    @PrimaryKey val url: String,
    val title: String,
    val iconUrl: String? = null,
    val lastAccessed: Long = System.currentTimeMillis(),
    val toastEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val fileChooserEnabled: Boolean = true,
    val offlineMode: Boolean = false,
    val orientation: String = "UNSPECIFIED", // "UNSPECIFIED", "PORTRAIT", "LANDSCAPE"
    val immersiveMode: Boolean = false,
    val themeColor: String? = null,
    val appsEnabled: Boolean = true
)
