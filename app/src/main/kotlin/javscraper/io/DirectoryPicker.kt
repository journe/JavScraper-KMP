package javscraper.io

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Opens a native OS directory selection dialog.
 * On macOS uses the built-in directory picker mode.
 * On Windows/Linux opens the native file dialog and returns the directory path.
 *
 * @param title Dialog window title
 * @param initialDir Optional initial directory to open the dialog to
 * @return Absolute path of the selected directory, or null if cancelled
 */
fun pickDirectory(title: String, initialDir: String? = null): String? {
    System.setProperty("apple.awt.fileDialogForDirectories", "true")
    return try {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.isModal = true
        if (!initialDir.isNullOrBlank()) {
            dialog.directory = initialDir
        }
        dialog.isVisible = true
        dialog.directory
    } finally {
        System.clearProperty("apple.awt.fileDialogForDirectories")
    }
}
