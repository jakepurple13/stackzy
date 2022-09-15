package com.theapache64.stackzy.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object FrameworkChecker {

    var hasScrcpy by mutableStateOf(false)

    suspend fun init() {
        hasScrcpy()
    }

    private suspend fun hasScrcpy() {
        hasScrcpy = hasFramework("scrcpy -v")
    }

    private suspend fun hasFramework(command: String) = try {
        withContext(Dispatchers.IO) { Runtime.getRuntime().exec(command) }
        true
    } catch (e: IOException) {
        false
    }
}