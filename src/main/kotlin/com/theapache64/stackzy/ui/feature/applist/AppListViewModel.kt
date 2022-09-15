package com.theapache64.stackzy.ui.feature.applist

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.theapache64.gpa.api.Play
import com.github.theapache64.gpa.model.Account
import com.theapache64.stackzy.data.local.AndroidApp
import com.theapache64.stackzy.data.repo.AdbRepo
import com.theapache64.stackzy.data.repo.ConfigRepo
import com.theapache64.stackzy.data.repo.InstallResource
import com.theapache64.stackzy.data.repo.PlayStoreRepo
import com.theapache64.stackzy.data.util.calladapter.flow.Resource
import com.theapache64.stackzy.model.AndroidAppWrapper
import com.theapache64.stackzy.model.AndroidDeviceWrapper
import com.theapache64.stackzy.ui.util.FrameworkChecker
import com.theapache64.stackzy.util.ApkSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

class AppListViewModel @Inject constructor(
    private val adbRepo: AdbRepo,
    private val playStoreRepo: PlayStoreRepo,
    private val configRepo: ConfigRepo
) {

    companion object {
        const val TAB_NO_TAB = -1
        private const val TAB_THIRD_PARTY_APPS_ID = 0
        private const val TAB_SYSTEM_APPS_ID = 1

        val tabsMap = mapOf(
            TAB_THIRD_PARTY_APPS_ID to "3rd Party Apps",
            TAB_SYSTEM_APPS_ID to "System Apps"
        )

        private const val MSG_LOADING_TRENDING_APPS = "Loading trending apps..."


        private val playStoreUrlRegEx by lazy {
            "^https:\\/\\/play\\.google\\.com\\/store\\/apps\\/details\\?id=(?<packageName>[\\w\\.]+)\$".toRegex()
        }

        fun isPlayStoreUrl(input: String): Boolean {
            return playStoreUrlRegEx.matches(input)
        }

        fun parsePackageName(playStoreUrl: String): String? {
            val matches = playStoreUrlRegEx.find(playStoreUrl)
            return matches?.groupValues?.getOrNull(1)
        }
    }

    private lateinit var viewModelScope: CoroutineScope
    private var searchJob: Job? = null

    /**
     * APK source can be either from an AndroidDevice or from a google Account
     */
    private lateinit var apkSource: ApkSource<AndroidDeviceWrapper, Account>
    private var selectedDevice: AndroidDeviceWrapper? = null

    /**
     * To store all apps instaled in the device (used for search filtering)
     */
    private var fullApps: List<AndroidApp>? = null
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword

    private val _selectedTabIndex = MutableStateFlow(TAB_NO_TAB)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex

    /**
     * Filtered apps
     */
    private val _apps = MutableStateFlow<Resource<List<AndroidAppWrapper>>?>(null)
    val apps: StateFlow<Resource<List<AndroidAppWrapper>>?> = _apps

    var installingProgress by mutableStateOf(0.0)
    var uiState by mutableStateOf(AppListState.Default)
    var error: String? by mutableStateOf(null)

    private val frameworkChecker = FrameworkChecker

    val hasScrcpy by derivedStateOf { frameworkChecker.hasScrcpy }

    fun init(
        scope: CoroutineScope,
        apkSource: ApkSource<AndroidDeviceWrapper, Account>
    ) {
        this.viewModelScope = scope
        this.apkSource = apkSource
        scope.launch { frameworkChecker.init() }
    }

    fun openScrcpy() {
        try {
            Runtime.getRuntime().exec("scrcpy -s ${selectedDevice?.androidDevice?.device?.serial}")
        } catch (e: IOException) {
            error = e.message
        }
    }

    fun loadApps() {

        // Updating state
        _apps.value = Resource.Loading(MSG_LOADING_TRENDING_APPS)

        when (apkSource) {
            is ApkSource.Adb -> {
                // ### ADB ###
                this.selectedDevice = (apkSource as ApkSource.Adb<AndroidDeviceWrapper>).value
                viewModelScope.launch {
                    fullApps = adbRepo.getInstalledApps(selectedDevice!!.device)
                    val tab = if (selectedTabIndex.value == TAB_NO_TAB) {
                        TAB_THIRD_PARTY_APPS_ID // first time
                    } else {
                        _selectedTabIndex.value // going back from detail page
                    }
                    onTabClicked(tab)
                }
            }

            is ApkSource.PlayStore -> {

                // ### PLAY STORE ###
                /*viewModelScope.launch {
                    val api = Play.getApi((apkSource as ApkSource.PlayStore<Account>).value)
                    val apps = playStoreRepo.search(" ", api)
                    _apps.value = Resource.Success( apps)
                }*/
                onSearchKeywordChanged(_searchKeyword.value)
            }
        }
    }

    fun onSearchKeywordChanged(_newKeyword: String) {
        val newKeyword = _newKeyword.replace("\n", "")
        _searchKeyword.value = newKeyword

        viewModelScope.launch {
            when (apkSource) {
                is ApkSource.Adb -> {
                    // ### ADB ###

                    _apps.value = Resource.Loading()
                    delay(1)

                    // Filtering apps
                    val filteredApps = fullApps
                        ?.filter {
                            // search with keyword
                            it.appPackage.name.lowercase(Locale.getDefault()).contains(newKeyword, ignoreCase = true)
                        }
                        ?.filter {
                            // filter for active tab
                            it.isSystemApp == (selectedTabIndex.value == TAB_SYSTEM_APPS_ID)
                        }
                        ?.sortedBy { it.appPackage.name }
                        ?: listOf()

                    _apps.value = Resource.Success(filteredApps.map { AndroidAppWrapper(it) })
                }

                is ApkSource.PlayStore -> {
                    if (isPlayStoreUrl(newKeyword)) {
                        // Pasted a play store url
                        val packageName = parsePackageName(newKeyword)!!
                        viewModelScope.launch {
                            val account = (apkSource as ApkSource.PlayStore<Account>).value
                            val api = Play.getApi(account)
                            val app = playStoreRepo.find(packageName, api)
                            if (app != null) {
                                // found app in playstore
                                _apps.value = Resource.Success(listOf(AndroidAppWrapper(app)))
                            } else {
                                _apps.value = Resource.Error("Invalid PlayStore URL")
                            }
                        }
                    } else {

                        // Play Store
                        searchJob?.cancel()

                        searchJob = viewModelScope.launch {

                            delay(500)

                            val account = (apkSource as ApkSource.PlayStore<Account>).value
                            val api = Play.getApi(account)
                            val keyword = searchKeyword.value.let {
                                it.ifBlank {
                                    " "
                                }
                            }
                            val loadingMsg = if (keyword.isBlank()) {
                                MSG_LOADING_TRENDING_APPS
                            } else {
                                "Searching for '$keyword'"
                            }

                            _apps.value = Resource.Loading(loadingMsg)

                            val apps = playStoreRepo.search(keyword, api)
                            _apps.value = Resource.Success(apps.map { AndroidAppWrapper(it) })
                        }
                    }


                }
            }
        }


    }

    fun onOpenMarketClicked() {
        if (apkSource is ApkSource.Adb) {
            viewModelScope.launch {
                val androidDevice = (apkSource as ApkSource.Adb<AndroidDeviceWrapper>).value.androidDevice
                adbRepo.launchMarket(androidDevice, searchKeyword.value)
            }
        }
    }

    /**
     * Invoked when any of the tabs clicked
     */
    fun onTabClicked(tabIndex: Int) {
        _selectedTabIndex.value = tabIndex

        // utilising existing filter logic : since we've already filter logic inside search,
        // passing empty string would filter the tab accordingly and passing search keyword would
        // filter the apps accordingly.
        onSearchKeywordChanged(searchKeyword.value)
    }

    fun installApk(file: String) {
        val androidDeviceWrapper = (apkSource as ApkSource.Adb).value
        viewModelScope.launch {
            uiState = AppListState.Installing
            adbRepo.installApk(
                androidDeviceWrapper.androidDevice,
                File(file)
            ) {
                when (it) {
                    is InstallResource.Error -> {
                        println(it.errorData)
                        uiState = AppListState.Default
                        error = it.errorData
                    }

                    is InstallResource.Loading -> installingProgress = it.progress
                    is InstallResource.Success -> {
                        loadApps()
                        uiState = AppListState.Default
                    }
                }
            }
        }
    }

    fun screenshot() {
        val androidDeviceWrapper = (apkSource as ApkSource.Adb).value
        viewModelScope.launch {
            try {
                adbRepo.screenshot(androidDeviceWrapper.androidDevice, configRepo.getLocalConfig()!!.screenshotLocation)
            } catch (e: Exception) {
                error = "Something went wrong"
            }
        }
    }

    fun onAppDownload(appWrapper: AndroidAppWrapper) {
        viewModelScope.launch {
            uiState = AppListState.Pulling
            val androidDeviceWrapper = (apkSource as ApkSource.Adb).value
            val apkRemotePath = adbRepo.getApkPath(androidDeviceWrapper.androidDevice, appWrapper.androidApp)
            if (apkRemotePath != null) {
                val downloads = configRepo.getLocalConfig()!!.downloadApkLocation

                val apkFile = File("$downloads/${appWrapper.appTitle ?: appWrapper.appPackage.name}.apk")

                adbRepo.pullFile(
                    androidDeviceWrapper.androidDevice,
                    apkRemotePath,
                    apkFile
                ).distinctUntilChanged()
                    .catch { error = it.message ?: "Something went wrong while pulling APK" }
                    .onCompletion { uiState = AppListState.Default }
                    .collect { downloadPercentage -> installingProgress = downloadPercentage.toDouble() / 100 }
            }
        }
    }
}

enum class AppListState { Installing, DragDrop, Default, Pulling }