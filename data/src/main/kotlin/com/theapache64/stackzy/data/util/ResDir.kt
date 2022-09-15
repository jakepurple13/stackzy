package com.theapache64.stackzy.data.util

object ResDir {
    val dir: String by lazy {
        System.getProperty("compose.application.resources.dir")
            ?: System.getProperty("user.dir")
            ?: System.getProperty("user.home")
    }
}