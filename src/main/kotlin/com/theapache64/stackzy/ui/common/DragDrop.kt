package com.theapache64.stackzy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import java.awt.dnd.*

@Composable
fun DragDropHandler(
    dragEnter: (DropTargetDragEvent?) -> Unit = {},
    dragOver: (DropTargetDragEvent?) -> Unit = {},
    drop: (DropTargetDropEvent) -> Unit = {},
    dragExit: (DropTargetEvent?) -> Unit = {}
) {
    val window = LocalWindow.current

    LaunchedEffect(Unit) {
        window.window.dropTarget = DropTarget().apply {
            addDropTargetListener(object : DropTargetAdapter() {
                override fun dragEnter(dtde: DropTargetDragEvent?) {
                    dragEnter(dtde)
                }

                override fun drop(event: DropTargetDropEvent) {
                    drop(event)
                }

                override fun dragExit(dte: DropTargetEvent?) {
                    dragExit(dte)
                }

                override fun dragOver(dtde: DropTargetDragEvent?) {
                    dragOver(dtde)
                }
            })
        }
    }
}