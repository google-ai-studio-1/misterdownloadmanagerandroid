package com.misterdownloadmanager.port.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.port.R
import com.misterdownloadmanager.port.core.model.Settings
import com.misterdownloadmanager.port.core.tools.TreeUriPath
import com.misterdownloadmanager.port.ui.icons.MdmIcons

@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var folderError by remember { mutableStateOf<String?>(null) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val path = TreeUriPath.treeUriToPath(uri, context)
        if (path == null) {
            folderError = context.getString(R.string.settings_folder_not_supported)
            return@rememberLauncherForActivityResult
        }
        if (TreeUriPath.isFolderWritable(path)) {
            folderError = null
            onSettingsChange(settings.copy(downloadFolder = path))
        } else {
            folderError = context.getString(R.string.settings_folder_not_writable)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.settings_general), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSwitch(
            label = stringResource(R.string.settings_auto_start),
            checked = settings.autoStartDownloads,
            onChange = { onSettingsChange(settings.copy(autoStartDownloads = it)) }
        )
        SettingsSwitch(
            label = stringResource(R.string.settings_resume_on_start),
            checked = settings.resumeOnStart,
            onChange = { onSettingsChange(settings.copy(resumeOnStart = it)) }
        )
        SettingsSwitch(
            label = stringResource(R.string.settings_notifications),
            checked = settings.showNotifications,
            onChange = { onSettingsChange(settings.copy(showNotifications = it)) }
        )
        SettingsSwitch(
            label = stringResource(R.string.settings_theme),
            checked = settings.followSystemTheme,
            onChange = { onSettingsChange(settings.copy(followSystemTheme = it)) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.settings_download), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = settings.maxSimultaneousDownloads.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let { onSettingsChange(settings.copy(maxSimultaneousDownloads = it)) }
            },
            label = { Text(stringResource(R.string.settings_max_downloads)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = settings.maxConnectionsPerDownload.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let { onSettingsChange(settings.copy(maxConnectionsPerDownload = it)) }
            },
            label = { Text(stringResource(R.string.settings_max_connections)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = settings.downloadFolder,
                onValueChange = { onSettingsChange(settings.copy(downloadFolder = it)) },
                label = { Text(stringResource(R.string.settings_download_folder)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { folderError = null; folderPicker.launch(null) }) {
                Icon(MdmIcons.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_browse))
            }
        }
        folderError?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.settings_speed), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSwitch(
            label = stringResource(R.string.label_speed_limit),
            checked = settings.speedLimitOn,
            onChange = { onSettingsChange(settings.copy(speedLimitOn = it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = settings.speedLimitKbPerSecond.toString(),
            onValueChange = { text ->
                text.toLongOrNull()?.let { onSettingsChange(settings.copy(speedLimitKbPerSecond = it)) }
            },
            label = { Text(stringResource(R.string.label_speed_limit) + " (KB/s)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.settings_connection), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSwitch(
            label = stringResource(R.string.settings_use_proxy),
            checked = settings.proxy.enabled,
            onChange = { onSettingsChange(settings.copy(proxy = settings.proxy.copy(enabled = it))) }
        )
        SettingsSwitch(
            label = stringResource(R.string.settings_checksum_verify),
            checked = settings.verifyChecksumAfterDownload,
            onChange = { onSettingsChange(settings.copy(verifyChecksumAfterDownload = it)) }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
