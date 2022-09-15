package com.theapache64.stackzy.ui.feature

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.theapache64.cyclone.core.Activity
import com.theapache64.cyclone.core.Intent
import com.theapache64.stackzy.App
import com.theapache64.stackzy.ui.common.LocalWindow
import com.theapache64.stackzy.ui.navigation.NavHostComponent
import com.theapache64.stackzy.ui.theme.R
import com.theapache64.stackzy.ui.theme.StackzyTheme
import com.theapache64.stackzy.ui.util.FrameworkChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.awt.Desktop
import java.awt.Taskbar
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO
import androidx.compose.ui.window.Window as setContent

class MainActivity : Activity() {
    companion object {
        fun getStartIntent(): Intent {
            return Intent(MainActivity::class).apply {
                // data goes here
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate() {
        super.onCreate()
        try {
            /*
             *TODO : Temp fix for https://github.com/theapache64/stackzy/issues/72
             *  Should be updated once resolved :
             */
            Taskbar.getTaskbar().iconImage = getAppIcon()
        } catch (e: UnsupportedOperationException) {
            e.printStackTrace()
        }

        val lifecycle = LifecycleRegistry()
        val root = NavHostComponent(DefaultComponentContext(lifecycle))

        application {

            val state = rememberWindowState(
                size = DpSize(
                    width = 1224.dp,
                    height = 900.dp
                )
            )

            setContent(
                onCloseRequest = ::exitApplication,
                title = "${App.appArgs.appName} (${App.appArgs.version})",
                icon = painterResource(R.drawables.appIcon),
                state = state,
                undecorated = true,
                transparent = true
            ) {
                CompositionLocalProvider(LocalWindow provides this) {
                    StackzyTheme {
                        MenuBar(root)
                        Surface(
                            shape = when (hostOs) {
                                OS.Linux -> RoundedCornerShape(8.dp)
                                OS.Windows -> RectangleShape
                                OS.MacOS -> RoundedCornerShape(8.dp)
                                else -> RoundedCornerShape(8.dp)
                            },
                            modifier = Modifier.animateContentSize()
                        ) {
                            Scaffold(
                                topBar = {
                                    Column {
                                        WindowDraggableArea(
                                            modifier = Modifier.combinedClickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = {},
                                                onDoubleClick = {
                                                    state.placement =
                                                        if (state.placement != WindowPlacement.Maximized) {
                                                            WindowPlacement.Maximized
                                                        } else {
                                                            WindowPlacement.Floating
                                                        }
                                                }
                                            )
                                        ) {
                                            TopAppBar(
                                                backgroundColor = MaterialTheme.colors.surface,
                                                elevation = 0.dp,
                                            ) {
                                                when (hostOs) {
                                                    OS.Linux -> LinuxTopBar(state)
                                                    OS.Windows -> WindowsTopBar(state)
                                                    OS.MacOS -> MacOsTopBar(state)
                                                    else -> {}
                                                }
                                            }
                                        }
                                        Divider(color = MaterialTheme.colors.onSurface)
                                    }
                                },
                                backgroundColor = MaterialTheme.colors.surface
                            ) { padding ->
                                Surface(modifier = Modifier.padding(padding)) {
                                    // Igniting navigation
                                    root.render()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameWindowScope.MenuBar(root: NavHostComponent) {

    val menuViewModel = remember { MenuViewModel() }

    val scope = rememberCoroutineScope()
    LaunchedEffect(menuViewModel) {
        menuViewModel.init(scope)
    }

    MenuBar {
        Menu("Addons") {
            CheckboxItem("Scrcpy", menuViewModel.hasScrcpy) { menuViewModel.openScrcpy() }
            Separator()
            Item("Refresh") { menuViewModel.refresh() }
        }

        Menu("Settings") {
            Item("Open Settings") { root.onSettingsScreen() }
        }
    }
}

class MenuViewModel {

    private lateinit var viewModelScope: CoroutineScope

    private val frameworkChecker = FrameworkChecker

    val hasScrcpy by derivedStateOf { frameworkChecker.hasScrcpy }

    fun init(scope: CoroutineScope) {
        viewModelScope = scope
        scope.launch { frameworkChecker.init() }
    }

    fun refresh() {
        viewModelScope.launch { frameworkChecker.init() }
    }

    fun openScrcpy() {
        Desktop.getDesktop().browse(URI("https://github.com/Genymobile/scrcpy"))
    }

}

@Composable
private fun ApplicationScope.LinuxTopBar(state: WindowState) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = ::exitApplication) { Icon(Icons.Default.Close, null) }
            IconButton(onClick = { state.isMinimized = !state.isMinimized }) { Icon(Icons.Default.Minimize, null) }
            IconButton(
                onClick = {
                    state.placement = if (state.placement != WindowPlacement.Maximized) WindowPlacement.Maximized
                    else WindowPlacement.Floating
                }
            ) { Icon(Icons.Default.Maximize, null) }
        }

        Text(
            "${App.appArgs.appName} (${App.appArgs.version})",
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@Composable
private fun ApplicationScope.WindowsTopBar(state: WindowState) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = ::exitApplication) { Icon(Icons.Default.Close, null) }
            IconButton(onClick = { state.isMinimized = !state.isMinimized }) { Icon(Icons.Default.Minimize, null) }
            IconButton(
                onClick = {
                    state.placement = if (state.placement != WindowPlacement.Maximized) WindowPlacement.Maximized
                    else WindowPlacement.Floating
                }
            ) { Icon(Icons.Default.Maximize, null) }
        }

        Text(
            "${App.appArgs.appName} (${App.appArgs.version})",
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ApplicationScope.MacOsTopBar(state: WindowState) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = ::exitApplication) { Icon(Icons.Default.Close, null) }
            IconButton(onClick = { state.isMinimized = !state.isMinimized }) { Icon(Icons.Default.Minimize, null) }
            IconButton(
                onClick = {
                    state.placement = if (state.placement != WindowPlacement.Fullscreen) WindowPlacement.Fullscreen
                    else WindowPlacement.Floating
                }
            ) {
                Icon(
                    if (state.placement != WindowPlacement.Fullscreen) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                    null
                )
            }
        }

        Text(
            "${App.appArgs.appName} (${App.appArgs.version})",
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * To get app icon for toolbar and system tray
 */
private fun getAppIcon(): BufferedImage {

    // Retrieving image
    val resourceFile = MainActivity::class.java.classLoader.getResourceAsStream(R.drawables.appIcon)
    val imageInput = ImageIO.read(resourceFile)

    val newImage = BufferedImage(
        imageInput.width,
        imageInput.height,
        BufferedImage.TYPE_INT_ARGB
    )

    // Drawing
    val canvas = newImage.createGraphics()
    canvas.drawImage(imageInput, 0, 0, null)
    canvas.dispose()

    return newImage
}
