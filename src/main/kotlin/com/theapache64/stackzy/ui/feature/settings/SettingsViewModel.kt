package com.theapache64.stackzy.ui.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.theapache64.stackzy.data.repo.ConfigRepo
import com.theapache64.stackzy.ui.common.chooseDirectorySwing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val configRepo: ConfigRepo
) {

    private lateinit var viewModelScope: CoroutineScope

    var downloadApkLocation by mutableStateOf("")
    var screenshotLocation by mutableStateOf("")

    fun init(
        viewModelScope: CoroutineScope,
    ) {
        this.viewModelScope = viewModelScope
        downloadApkLocation = configRepo.getLocalConfig()?.downloadApkLocation
            ?: (System.getProperty("user.home") + "/Downloads")
        screenshotLocation = configRepo.getLocalConfig()?.screenshotLocation
            ?: (System.getProperty("user.home") + "/Desktop")
    }

    fun changeDownloadApkLocation() {
        viewModelScope.launch {
            try {
                chooseDirectorySwing(downloadApkLocation)?.let { folder ->
                    downloadApkLocation = folder
                    configRepo.getLocalConfig()
                        ?.copy(downloadApkLocation = downloadApkLocation)
                        ?.let { configRepo.saveConfigToLocal(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changeScreenshotLocation() {
        viewModelScope.launch {
            try {
                chooseDirectorySwing(screenshotLocation)?.let { folder ->
                    screenshotLocation = folder
                    configRepo.getLocalConfig()
                        ?.copy(screenshotLocation = screenshotLocation)
                        ?.let { configRepo.saveConfigToLocal(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}