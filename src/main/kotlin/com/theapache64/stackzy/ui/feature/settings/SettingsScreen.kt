package com.theapache64.stackzy.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theapache64.stackzy.ui.common.CustomScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackClicked: () -> Unit
) {
    CustomScaffold(
        title = "Settings",
        onBackClicked = onBackClicked
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { settingsViewModel.changeDownloadApkLocation() }
            ) {
                ListItem(
                    headlineText = { Text("Download Apk Location") },
                    trailingContent = {
                        Text(
                            settingsViewModel.downloadApkLocation,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                )
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { settingsViewModel.changeScreenshotLocation() }
            ) {
                ListItem(
                    headlineText = { Text("Screenshot Location") },
                    trailingContent = {
                        Text(
                            settingsViewModel.screenshotLocation,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                )
            }
        }
    }
}