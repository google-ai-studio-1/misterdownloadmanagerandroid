package com.misterdownloadmanager.port.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.port.R
import com.misterdownloadmanager.port.core.model.Settings

@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = stringResource(R.string.settings_general), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
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
        Text(text = stringResource(R.string.settings_download), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = settings.maxSimultaneousDownloads.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let { onSettingsChange(settings.copy(maxSimultaneousDownloads = it)) }
            },
            label = { Text(stringResource(R.string.settings_max_downloads)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = settings.downloadFolder,
            onValueChange = { onSettingsChange(settings.copy(downloadFolder = it)) },
            label = { Text(stringResource(R.string.settings_download_folder)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.settings_speed), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        SettingsSwitch(
            label = stringResource(R.string.label_speed_limit),
            checked = settings.speedLimitOn,
            onChange = { onSettingsChange(settings.copy(speedLimitOn = it)) }
        )
        OutlinedTextField(
            value = settings.speedLimitKbPerSecond.toString(),
            onValueChange = { text ->
                text.toLongOrNull()?.let { onSettingsChange(settings.copy(speedLimitKbPerSecond = it)) }
            },
            label = { Text(stringResource(R.string.label_speed_limit) + " (KB/s)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.settings_connection), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
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
    }
}

@Composable
fun SettingsSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
