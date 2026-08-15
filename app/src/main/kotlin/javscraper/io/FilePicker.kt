package javscraper.io

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import java.io.File

/**
 * Opens a native OS file selection dialog using FileKit.
 *
 * On Windows this invokes the modern `IFileOpenDialog` (the same file
 * picker seen in native Windows apps). On macOS it uses `NSOpenPanel`.
 * On Linux it uses the portal-based or XDG native dialog.
 *
 * @param title Dialog title.
 * @param extensions Optional file extensions to filter (without leading dot), e.g. ["exe"].
 * @return Absolute path of the selected file, or null if cancelled.
 */
suspend fun pickFile(title: String, extensions: List<String>? = null): String? {
    val type = extensions?.let { FileKitType.File(it) }
    val file = FileKit.openFilePicker(
        type = type ?: FileKitType.File(),
        dialogSettings = FileKitDialogSettings(title = title)
    )?.file ?: return null
    return file.absolutePath.trimEnd(File.separatorChar)
}