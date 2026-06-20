package net.jj1uzh.appsearcher

data class AppInfo(
    val packageName: String,
    val name: String,
    val searchKeys: List<String> = emptyList()
)
