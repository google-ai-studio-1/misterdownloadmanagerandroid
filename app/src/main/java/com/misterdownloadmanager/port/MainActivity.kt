package com.misterdownloadmanager.port

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.misterdownloadmanager.port.core.model.Category
import com.misterdownloadmanager.port.ui.DownloadViewModel
import com.misterdownloadmanager.port.ui.icons.MdmIcons
import com.misterdownloadmanager.port.ui.screens.AddDownloadScreen
import com.misterdownloadmanager.port.ui.screens.CategoriesScreen
import com.misterdownloadmanager.port.ui.screens.DownloadDetailScreen
import com.misterdownloadmanager.port.ui.screens.DownloadListScreen
import com.misterdownloadmanager.port.ui.screens.SettingsScreen
import com.misterdownloadmanager.port.ui.theme.MisterDownloadManagerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            MisterDownloadManagerTheme {
                MainScreen(viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }
}

enum class Destination {
    DOWNLOADS,
    ADD,
    CATEGORIES,
    SETTINGS
}

@Composable
fun MainScreen(viewModel: DownloadViewModel) {
    var destination by remember { mutableStateOf(Destination.DOWNLOADS) }
    var selectedDownloadId by remember { mutableStateOf<Long?>(null) }
    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedDownload = items.firstOrNull { it.id == selectedDownloadId }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination == Destination.DOWNLOADS,
                    onClick = { destination = Destination.DOWNLOADS; selectedDownloadId = null },
                    icon = { Icon(MdmIcons.Download, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_downloads)) }
                )
                NavigationBarItem(
                    selected = destination == Destination.ADD,
                    onClick = { destination = Destination.ADD },
                    icon = { Icon(MdmIcons.Add, contentDescription = null) },
                    label = { Text(stringResource(R.string.action_add)) }
                )
                NavigationBarItem(
                    selected = destination == Destination.CATEGORIES,
                    onClick = { destination = Destination.CATEGORIES },
                    icon = { Icon(MdmIcons.Category, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_categories)) }
                )
                NavigationBarItem(
                    selected = destination == Destination.SETTINGS,
                    onClick = { destination = Destination.SETTINGS },
                    icon = { Icon(MdmIcons.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        if (selectedDownload != null) {
            DownloadDetailScreen(
                item = selectedDownload,
                onPause = { viewModel.pauseDownload(selectedDownload) },
                onResume = { viewModel.resumeDownload(selectedDownload) },
                onRetry = { viewModel.retryDownload(selectedDownload) },
                onDelete = { viewModel.deleteDownload(selectedDownload, deleteFile = false); selectedDownloadId = null },
                modifier = modifier
            )
        } else {
            when (destination) {
                Destination.DOWNLOADS -> DownloadListScreen(
                    items = viewModel.filteredItems(),
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onPause = { viewModel.pauseDownload(it) },
                    onResume = { viewModel.resumeDownload(it) },
                    onDelete = { viewModel.deleteDownload(it, deleteFile = false) },
                    onSelect = { selectedDownloadId = it.id },
                    modifier = modifier
                )
                Destination.ADD -> AddDownloadScreen(
                    onAdd = { viewModel.addDownload(it, null, null); destination = Destination.DOWNLOADS },
                    onAddTorrent = { viewModel.addDownload(it, null, Category.TORRENTS.id); destination = Destination.DOWNLOADS },
                    modifier = modifier
                )
                Destination.CATEGORIES -> CategoriesScreen(
                    categories = Category.DEFAULTS,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = { viewModel.selectCategory(it); destination = Destination.DOWNLOADS },
                    modifier = modifier
                )
                Destination.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onSettingsChange = { viewModel.updateSettings(it) },
                    modifier = modifier
                )
            }
        }
    }
}
