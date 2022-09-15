package com.theapache64.stackzy.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.arkivanov.decompose.ComponentContext
import com.theapache64.stackzy.di.AppComponent
import com.theapache64.stackzy.ui.navigation.Component
import javax.inject.Inject

class SettingsComponent(
    componentContext: ComponentContext,
    appComponent: AppComponent,
    val onBackClicked: () -> Unit,
) : Component, ComponentContext by componentContext {

    @Inject
    lateinit var settingsViewModel: SettingsViewModel

    init {
        appComponent.inject(this)
    }

    @Composable
    override fun render() {
        val scope = rememberCoroutineScope()
        LaunchedEffect(settingsViewModel) {
            settingsViewModel.init(scope)
        }

        SettingsScreen(
            settingsViewModel = settingsViewModel,
            onBackClicked = onBackClicked
        )
    }
}