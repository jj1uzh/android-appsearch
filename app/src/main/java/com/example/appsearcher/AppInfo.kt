package com.example.appsearcher

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    val searchKeys: List<String> = emptyList()
)
