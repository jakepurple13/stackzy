package com.theapache64.stackzy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.FrameWindowScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

enum class FileDialogMode(internal val id: Int) { Load(FileDialog.LOAD), Save(FileDialog.SAVE) }

@Composable
fun FileDialog(
    mode: FileDialogMode,
    title: String = "Choose a file",
    parent: Frame? = null,
    block: FileDialog.() -> Unit = {},
    onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, title, mode.id) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(directory + File.separator + file)
                }
            }
        }.apply(block)
    },
    dispose = FileDialog::dispose
)

suspend fun chooseDirectorySwing(
    startingFolder: String
) = withContext(Dispatchers.IO) {
    val chooser = JFileChooser(startingFolder).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isVisible = true
    }

    when (val code = chooser.showOpenDialog(null)) {
        JFileChooser.APPROVE_OPTION -> chooser.selectedFile.absolutePath
        JFileChooser.CANCEL_OPTION -> null
        JFileChooser.ERROR_OPTION -> error("Something went wrong")
        else -> error("Unknown return code '${code}' from JFileChooser::showOpenDialog")
    }
}

val LocalWindow: ProvidableCompositionLocal<FrameWindowScope> = staticCompositionLocalOf { error("None Yet") }