package net.jj1uzh.appsearcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val appProvider = AppProvider(application)
    private val normalizer = SearchNormalizer()
    private val recentAppsRepo = RecentAppsRepository(application)
    
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    init {
        loadApps()
    }
    
    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appProvider.getInstalledApps().map { app ->
                val keys = normalizer.normalize(app.name)
                app.copy(searchKeys = keys)
            }
            _allApps.value = apps.sortedBy { it.name.lowercase() }
        }
    }
    
    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    val uiState: StateFlow<AppSearchUiState> = combine(
        _allApps,
        _searchQuery,
        recentAppsRepo.recentAppsFlow
    ) { allApps, query, recentPackages ->
        if (allApps.isEmpty()) {
            return@combine AppSearchUiState.Loading
        }
        
        if (query.isBlank()) {
            val recentApps = recentPackages.mapNotNull { pkg -> 
                allApps.find { it.packageName == pkg }
            }
            AppSearchUiState.Success(
                isSearch = false,
                recentApps = recentApps,
                apps = allApps
            )
        } else {
            val normalizedQuery = normalizer.normalizeQuery(query)
            val filteredApps = allApps.filter { app ->
                app.searchKeys.any { key -> key.contains(normalizedQuery) } ||
                app.packageName.lowercase().contains(normalizedQuery)
            }.sortedByDescending { app ->
                app.searchKeys.any { key -> key.startsWith(normalizedQuery) } ||
                app.packageName.lowercase().startsWith(normalizedQuery)
            }
            AppSearchUiState.Success(
                isSearch = true,
                recentApps = emptyList(),
                apps = filteredApps
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSearchUiState.Loading
    )
    
    fun onAppLaunched(packageName: String) {
        viewModelScope.launch {
            recentAppsRepo.addRecentApp(packageName)
        }
    }
}

sealed class AppSearchUiState {
    object Loading : AppSearchUiState()
    data class Success(
        val isSearch: Boolean,
        val recentApps: List<AppInfo>,
        val apps: List<AppInfo>
    ) : AppSearchUiState()
}
