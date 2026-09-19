package javscraper.io.pickers

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import java.io.File

/**
 * Opens a native OS directory selection dialog using FileKit.
 *
 * On Windows this invokes the modern `IFileOpenDialog` (the same folder
 * picker seen in native Windows apps). On macOS it uses `NSOpenPanel`
 * in directory mode. On Linux it uses the portal-based or XDG native
 * dialog.
 *
 * @param title Dialog title.
 * @param initialDir Optional initial directory for the dialog.
 * @return Absolute path of the selected directory, or null if cancelled.
 */
suspend fun pickDirectory(title: String, initialDir: String? = null): String? {
    val directory = FileKit.openDirectoryPicker()
    val file = directory?.file ?: return null
    return file.absolutePath.trimEnd(File.separatorChar)
}
