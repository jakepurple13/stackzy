package com.theapache64.stackzy.ui.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.essenty.parcelable.Parcelable
import com.github.theapache64.gpa.model.Account
import com.theapache64.stackzy.di.AppComponent
import com.theapache64.stackzy.di.DaggerAppComponent
import com.theapache64.stackzy.model.AndroidAppWrapper
import com.theapache64.stackzy.model.AndroidDeviceWrapper
import com.theapache64.stackzy.model.LibraryWrapper
import com.theapache64.stackzy.ui.feature.appdetail.AppDetailScreenComponent
import com.theapache64.stackzy.ui.feature.applist.AppListScreenComponent
import com.theapache64.stackzy.ui.feature.devicelist.DeviceListScreenComponent
import com.theapache64.stackzy.ui.feature.libdetail.LibraryDetailScreenComponent
import com.theapache64.stackzy.ui.feature.liblist.LibraryListScreenComponent
import com.theapache64.stackzy.ui.feature.login.LogInScreenComponent
import com.theapache64.stackzy.ui.feature.pathway.PathwayScreenComponent
import com.theapache64.stackzy.ui.feature.splash.SplashScreenComponent
import com.theapache64.stackzy.ui.feature.update.UpdateScreenComponent
import com.theapache64.stackzy.util.ApkSource
import com.toxicbakery.logging.Arbor
import java.awt.Desktop
import java.net.URI

/**
 * All navigation decisions are made from here
 */
class NavHostComponent(
    private val componentContext: ComponentContext
) : Component, ComponentContext by componentContext {

    /**
     * Available screensSelectApp
     */
    private sealed class Config : Parcelable {
        object Splash : Config()
        object SelectPathway : Config()
        data class LogIn(val shouldGoToPlayStore: Boolean) : Config()
        object DeviceList : Config()
        data class AppList(
            val apkSource: ApkSource<AndroidDeviceWrapper, Account>
        ) : Config()

        data class AppDetail(
            val apkSource: ApkSource<AndroidDeviceWrapper, Account>,
            val androidApp: AndroidAppWrapper
        ) : Config()

        object Update : Config()
        object LibraryList : Config()
        data class LibraryDetail(
            val libraryWrapper: LibraryWrapper
        ) : Config()
    }

    private val appComponent: AppComponent = DaggerAppComponent
        .create()

    private val navigator = StackNavigation<Config>()

    /**
     * Router configuration
     */
    private val router = childStack(
        source = navigator,
        initialConfiguration = Config.Splash,
        childFactory = ::createScreenComponent
    )

    /**
     * When a new navigation request made, the screen will be created by this method.
     */
    private fun createScreenComponent(config: Config, componentContext: ComponentContext): Component {
        return when (config) {
            is Config.Splash -> SplashScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext,
                onSyncFinished = ::onSplashSyncFinished,
                onUpdateNeeded = ::onUpdateNeeded
            )
            is Config.SelectPathway -> PathwayScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext,
                onAdbSelected = ::onPathwayAdbSelected,
                onLogInNeeded = ::onLogInNeeded,
                onPlayStoreSelected = ::onPathwayPlayStoreSelected,
                onLibrariesSelected = ::onPathwayLibrariesSelected
            )
            is Config.LogIn -> LogInScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext,
                onLoggedIn = ::onLoggedIn,
                onBackClicked = ::onBackClicked,
                shouldGoToPlayStore = config.shouldGoToPlayStore
            )

            is Config.DeviceList -> DeviceListScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext,
                onDeviceSelected = ::onDeviceSelected,
                onBackClicked = ::onBackClicked,
            )
            is Config.AppList -> AppListScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext,
                apkSource = config.apkSource,
                onAppSelected = ::onAppSelected,
                onBackClicked = ::onBackClicked
            )

            is Config.AppDetail -> AppDetailScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext,
                selectedApp = config.androidApp,
                apkSource = config.apkSource,
                onLibrarySelected = ::onLibrarySelected,
                onBackClicked = ::onBackClicked
            )

            is Config.Update -> UpdateScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext
            )
            is Config.LibraryList -> LibraryListScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext,
                onLibraryClicked = ::onLibraryClicked,
                onBackClicked = ::onBackClicked
            )

            is Config.LibraryDetail -> LibraryDetailScreenComponent(
                appComponent = appComponent,
                componentContext = componentContext,
                onBackClicked = ::onBackClicked,
                onAppClicked = ::onAppSelected,
                libraryWrapper = config.libraryWrapper,
                onLogInNeeded = ::onLogInNeeded
            )
        }
    }

    /**
     * Invoked when a library clicked
     */
    private fun onLibraryClicked(libraryWrapper: LibraryWrapper) {
        navigator.push(Config.LibraryDetail(libraryWrapper))
    }


    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun render() {
        Children(
            stack = router,
            animation = stackAnimation(scale() + fade())
        ) { child ->
            child.instance.render()
        }
    }

    /**
     * Invoked when splash finish data sync
     */
    private fun onSplashSyncFinished() {
        navigator.replaceCurrent(Config.SelectPathway)
        /*navigator.push(
            Config.AppDetail(
                AndroidDevice(
                    "Samsung",
                    "someModel",
                    Device(
                        "R52M604X18E",
                        DeviceState.DEVICE
                    )
                ),
                AndroidApp(
                    // Package("a.i"),
                    Package("com.theapache64.topcorn"),
                )
            )
        )*/
    }

    /**
     * Invoked when play store selected from the pathway screen
     */
    private fun onPathwayPlayStoreSelected(account: Account) {
        Arbor.d("Showing select app")
        navigator.push(Config.AppList(ApkSource.PlayStore(account)))
    }

    /**
     * This method will be invoked when login is needed (either login pressed or authentication failed)
     */
    private fun onLogInNeeded(shouldGoToPlayStore: Boolean) {
        navigator.push(Config.LogIn(shouldGoToPlayStore))
    }

    /**
     * Invoked when login succeeded.
     */
    private fun onLoggedIn(shouldGoToPlayStore: Boolean, account: Account) {
        if (shouldGoToPlayStore) {
            navigator.replaceCurrent(Config.AppList(ApkSource.PlayStore(account)))
        } else {
            Arbor.d("onLoggedIn: Moving back...")
            navigator.pop()
        }
    }

    /**
     * Invoked when adb selected from the pathway screen
     */
    private fun onPathwayAdbSelected() {
        navigator.push(Config.DeviceList)
    }

    private fun onPathwayLibrariesSelected() {
        navigator.push(Config.LibraryList)
    }

    /**
     * Invoked when a device selected
     */
    private fun onDeviceSelected(androidDevice: AndroidDeviceWrapper) {
        navigator.push(Config.AppList(ApkSource.Adb(androidDevice)))
    }

    /**
     * Invoked when the app got selected
     */
    private fun onAppSelected(
        apkSource: ApkSource<AndroidDeviceWrapper, Account>,
        androidAppWrapper: AndroidAppWrapper
    ) {
        navigator.push(
            Config.AppDetail(
                apkSource = apkSource,
                androidApp = androidAppWrapper
            )
        )
    }

    /**
     * Invoked when library selected
     */
    private fun onLibrarySelected(libraryWrapper: LibraryWrapper) {
        Desktop.getDesktop().browse(URI(libraryWrapper.website))
    }

    /**
     * Invoked when an update is necessary
     */
    private fun onUpdateNeeded() {
        println("Update needed")
        navigator.push(Config.Update)
    }

    /**
     * Invoked when back arrow pressed
     */
    private fun onBackClicked() {
        Arbor.d("Back clicked popping")
        navigator.pop()
    }


}