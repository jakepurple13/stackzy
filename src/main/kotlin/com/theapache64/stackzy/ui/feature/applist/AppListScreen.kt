package com.theapache64.stackzy.ui.feature.applist

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.theapache64.stackzy.data.util.calladapter.flow.Resource
import com.theapache64.stackzy.model.AndroidAppWrapper
import com.theapache64.stackzy.ui.common.*
import com.theapache64.stackzy.ui.common.loading.LoadingAnimation
import com.theapache64.stackzy.util.R
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants

/**
 * To select an application from the selected device
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun SelectAppScreen(
    appListViewModel: AppListViewModel,
    onBackClicked: () -> Unit,
    onAppSelected: (AndroidAppWrapper) -> Unit
) {

    val searchKeyword by appListViewModel.searchKeyword.collectAsState()
    val appsResponse by appListViewModel.apps.collectAsState()
    val selectedTabIndex by appListViewModel.selectedTabIndex.collectAsState()

    val hasData = appsResponse is Resource.Success &&
            (appsResponse as Resource.Success<List<AndroidAppWrapper>>).data.isNotEmpty()

    var installApk by remember { mutableStateOf(false) }

    if (installApk) {
        FileDialog(
            mode = FileDialogMode.Load,
        ) {
            it?.let { it1 -> appListViewModel.installApk(it1) }
            installApk = false
        }
    }

    appListViewModel.error?.let {
        AlertDialog(
            onDismissRequest = { appListViewModel.error = null },
            title = { Text("Something went wrong") },
            text = { Text(it) },
            buttons = { TextButton(onClick = { appListViewModel.error = null }) { Text("Close") } }
        )
    }

    DragDropHandler(
        dragEnter = { appListViewModel.uiState = AppListState.DragDrop },
        drop = { event ->
            event.acceptDrop(DnDConstants.ACTION_COPY)
            val draggedFileName = event.transferable.getTransferData(DataFlavor.javaFileListFlavor)
            println(draggedFileName)
            when (draggedFileName) {
                is List<*> -> {
                    draggedFileName.firstOrNull()?.toString()?.let {
                        if (it.endsWith(".apk")) {
                            appListViewModel.installApk(it)
                        }
                    }
                }
            }
            event.dropComplete(true)
        },
        dragExit = { appListViewModel.uiState = AppListState.Default }
    )

    Crossfade(appListViewModel.uiState) { installing ->
        when (installing) {
            AppListState.DragDrop -> {
                Card(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Drag-n-Drop to import")
                        Text("Please note that this will overwrite the current data")
                    }
                }
            }

            AppListState.Installing -> LoadingAnimation("Installing Apk", appListViewModel.installingProgress)
            AppListState.Default -> {
                CustomScaffold(
                    title = R.string.select_app_title,
                    onBackClicked = onBackClicked,
                    bottomGradient = hasData, // only for success
                    topRightSlot = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (appListViewModel.hasScrcpy) {
                                TooltipBox("Open Scrcpy") {
                                    IconButton(onClick = { appListViewModel.openScrcpy() }) {
                                        Icon(
                                            painterResource("drawables/scrcpy_icon.svg"),
                                            null,
                                            tint = Color.Unspecified
                                        )
                                    }
                                }
                            }

                            TooltipBox("Take Screen Shot") {
                                IconButton(
                                    onClick = { appListViewModel.screenshot() }
                                ) { Icon(Icons.Default.Screenshot, null) }
                            }

                            TooltipBox("Install Apk") {
                                IconButton(
                                    onClick = { installApk = true }
                                ) { Icon(Icons.Default.InstallMobile, null) }
                            }

                            // SearchBox
                            OutlinedTextField(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = ""
                                    )
                                },
                                singleLine = true,
                                value = searchKeyword,
                                label = { Text(text = R.string.select_app_label_search) },
                                onValueChange = { appListViewModel.onSearchKeywordChanged(it) },
                                modifier = Modifier
                                    .width(300.dp)
                            )
                        }
                    }
                ) {

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Column {
                        if (selectedTabIndex != AppListViewModel.TAB_NO_TAB) {
                            TabRow(
                                selectedTabIndex = selectedTabIndex,
                                backgroundColor = Color.Transparent,
                                contentColor = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(
                                    top = 5.dp,
                                    bottom = 10.dp,
                                )
                            ) {
                                AppListViewModel.tabsMap.entries.forEach { tabEntry ->
                                    Tab(
                                        selected = tabEntry.key == selectedTabIndex,
                                        onClick = { appListViewModel.onTabClicked(tabEntry.key) },
                                        text = { Text(tabEntry.value) }
                                    )
                                }
                            }
                        }

                        when (appsResponse) {
                            is Resource.Loading -> {
                                val message = (appsResponse as Resource.Loading<List<AndroidAppWrapper>>).message ?: ""
                                LoadingAnimation(message, funFacts = null)
                            }

                            is Resource.Error -> {
                                Box {
                                    ErrorSnackBar(
                                        (appsResponse as Resource.Error<List<AndroidAppWrapper>>).errorData
                                    )
                                }
                            }

                            is Resource.Success -> {
                                val apps = (appsResponse as Resource.Success<List<AndroidAppWrapper>>).data

                                if (apps.isNotEmpty()) {
                                    // Grid
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(items = apps, { it.appPackage }) { app ->
                                            Column(modifier = Modifier.animateItemPlacement()) {
                                                // GridItem
                                                Selectable(
                                                    data = app,
                                                    onSelected = onAppSelected
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(10.dp)
                                                )
                                            }
                                        }
                                    }

                                } else {
                                    // No app found
                                    FullScreenError(
                                        title = "App not found",
                                        message = "Couldn't find any app with $searchKeyword",
                                        image = painterResource("drawables/woman_desk.png"),
                                        action = {
                                            Button(
                                                onClick = {
                                                    appListViewModel.onOpenMarketClicked()
                                                },
                                            ) {
                                                Text(text = R.string.app_detail_action_open_market)
                                            }
                                        }
                                    )
                                }
                            }

                            null -> {
                                LoadingAnimation("Preparing apps...", funFacts = null)
                            }
                        }
                    }

                }
            }
        }
    }

}