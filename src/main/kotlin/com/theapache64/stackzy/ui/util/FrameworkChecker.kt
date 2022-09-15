package com.theapache64.stackzy.ui.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class FrameworkChecker {

    suspend fun hasScrcpy() = hasFramework("scrcpy -v")

    private suspend fun hasFramework(command: String) = try {
        withContext(Dispatchers.IO) { Runtime.getRuntime().exec(command) }
        true
    } catch (e: IOException) {
        false
    }

}