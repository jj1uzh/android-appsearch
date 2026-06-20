package net.jj1uzh.appsearcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

import java.io.File

class AppProvider(private val context: Context) {
    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        return resolveInfoList
            .filterNot { it.activityInfo.packageName == context.packageName }
            .map { resolveInfo ->
                val appName = resolveInfo.loadLabel(pm).toString()
                val packageName = resolveInfo.activityInfo.packageName
                AppInfo(packageName = packageName, name = appName)
            }
    }

    fun loadCachedApps(): List<AppInfo> {
        val file = File(context.cacheDir, "apps_cache.tsv")
        if (!file.exists()) return emptyList()
        return try {
            file.readLines().mapNotNull { line ->
                val parts = line.split("\t", limit = 2)
                if (parts.size == 2) {
                    AppInfo(packageName = parts[0], name = parts[1])
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCachedApps(apps: List<AppInfo>) {
        val file = File(context.cacheDir, "apps_cache.tsv")
        try {
            val content = apps.joinToString("\n") { "${it.packageName}\t${it.name}" }
            file.writeText(content)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
