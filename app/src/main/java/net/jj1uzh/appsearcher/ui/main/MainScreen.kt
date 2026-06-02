package net.jj1uzh.appsearcher.ui.main

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.jj1uzh.appsearcher.AppInfo
import net.jj1uzh.appsearcher.AppSearchUiState
import net.jj1uzh.appsearcher.AppSearchViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: AppSearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val launchApp: (AppInfo) -> Unit = { app ->
        viewModel.onAppLaunched(app.packageName)
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        intent?.let { context.startActivity(it) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { (context as? android.app.Activity)?.finish() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .systemBarsPadding(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = {
                        val state = uiState
                        if (state is AppSearchUiState.Success) {
                            val topApp = state.recentApps.firstOrNull() ?: state.apps.firstOrNull()
                            topApp?.let(launchApp)
                        }
                    },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Enter) {
                                val state = uiState
                                if (state is AppSearchUiState.Success) {
                                    val topApp = state.recentApps.firstOrNull() ?: state.apps.firstOrNull()
                                    topApp?.let(launchApp)
                                }
                                true
                            } else {
                                false
                            }
                        }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 8.dp, end = 8.dp),
            content = {}
        )

        when (val state = uiState) {
            is AppSearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AppSearchUiState.Success -> {
                val topApp = state.recentApps.firstOrNull() ?: state.apps.firstOrNull()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.recentApps.isNotEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                            Text(
                                text = "Recent Apps",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(state.recentApps) { app ->
                            AppItem(app = app, onClick = { launchApp(app) }, isTopApp = app == topApp)
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                            Text(
                                text = "All Apps",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    items(state.apps) { app ->
                        AppItem(app = app, onClick = { launchApp(app) }, isTopApp = app == topApp)
                    }
                }
            }
        }
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo, onClick: () -> Unit, isTopApp: Boolean = false) {
    val bgColor = if (isTopApp) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(bgColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        val bitmap = remember(app.icon) {
            app.icon.toBitmap().asImageBitmap()
        }
        Image(
            bitmap = bitmap,
            contentDescription = app.name,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
