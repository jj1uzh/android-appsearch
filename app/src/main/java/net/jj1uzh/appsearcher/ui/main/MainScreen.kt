package net.jj1uzh.appsearcher.ui.main

import android.content.Intent
import androidx.compose.foundation.Image
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

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: AppSearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Search apps...") },
            singleLine = true
        )

        when (val state = uiState) {
            is AppSearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AppSearchUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
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
                            AppItem(app = app, onClick = {
                                viewModel.onAppLaunched(app.packageName)
                                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                intent?.let { context.startActivity(it) }
                            })
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
                        AppItem(app = app, onClick = {
                            viewModel.onAppLaunched(app.packageName)
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.let { context.startActivity(it) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
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
