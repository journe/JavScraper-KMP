package javscraper.io.metadata.multipart

import javscraper.io.NfoWriter
import javscraper.io.metadata.VideoMetadataEditResult
import javscraper.io.metadata.VideoMetadataEditor
import javscraper.models.Video
import javscraper.settings.MultiPartSuffix
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VideoMetadataEditorMultiPartTest {
    @Test
    fun `editing metadata without title change renames part assets in place`() {
        val root = Files.createTempDirectory("javscraper-multipart-in-place")
        try {
            val folder = root.resolve("[FC2-4694056] Same title")
            Files.createDirectories(folder)
            val base = "[FC2-4694056] Same title"
            listOf("cd1", "cd2").forEach { part ->
                Files.writeString(folder.resolve("FC2-4694056-$part.mp4"), part)
            }
            Files.writeString(
                folder.resolve("movie.nfo"),
                NfoWriter.generate(Video(number = "FC2-4694056", title = "Same title"))
            )
            Files.writeString(
                folder.resolve("FC2-4694056-cd2.nfo"),
                "<movie><title>cd2</title><num>FC2-4694056</num></movie>"
            )

            val result = VideoMetadataEditor.update(
                video = Video(
                    number = "FC2-4694056",
                    title = "Same title",
                    path = folder.resolve("FC2-4694056-cd1.mp4").toString()
                ),
                lockData = false,
                folderLayers = listOf("[{num}] {title}"),
                scanDir = root.toString()
            )

            val success = assertIs<VideoMetadataEditResult.Success>(result)
            assertEquals(folder.resolve("$base - cd1.mp4").toString(), success.video.path)
            assertTrue(Files.isRegularFile(folder.resolve("$base - cd2.mp4")))
            assertTrue(Files.isRegularFile(folder.resolve("$base - cd2.nfo")))
            assertFalse(Files.exists(folder.resolve("FC2-4694056-cd2.nfo")))
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `editing title renames multi-part folder videos nfos and part images`() {
        val root = Files.createTempDirectory("javscraper-multipart-rename")
        try {
            val oldFolder = root.resolve("[FC2-4694056] Old title")
            Files.createDirectories(oldFolder)
            val oldBase = "[FC2-4694056] Old title"
            val oldVideos = listOf("cd1", "cd2", "cd3").map { part ->
                oldFolder.resolve("$oldBase - $part.mp4").also(Files::createFile)
            }
            val masterNfo = oldFolder.resolve("movie.nfo")
            Files.writeString(
                masterNfo,
                NfoWriter.generate(Video(number = "FC2-4694056", title = "Old title"))
            )
            val partNfos = listOf("cd2", "cd3").map { part ->
                oldFolder.resolve("$oldBase - $part.nfo").also {
                    Files.writeString(it, "<movie><title>$part</title><num>FC2-4694056</num></movie>")
                }
            }
            listOf("cd2", "cd3").forEach { part ->
                Files.writeString(oldFolder.resolve("$oldBase - $part-poster.jpg"), "$part poster")
                Files.writeString(oldFolder.resolve("$oldBase - $part-fanart.jpg"), "$part fanart")
            }
            Files.writeString(oldFolder.resolve("poster.jpg"), "generic poster")
            Files.writeString(oldFolder.resolve("fanart.jpg"), "generic fanart")

            val result = VideoMetadataEditor.update(
                video = Video(
                    number = "FC2-4694056",
                    title = "马尾美女",
                    path = oldVideos[0].toString()
                ),
                lockData = false,
                folderLayers = listOf("[{num}] {title}"),
                multiPartSuffix = MultiPartSuffix.DISC,
                scanDir = root.toString()
            )

            val success = assertIs<VideoMetadataEditResult.Success>(result)
            val newFolder = root.resolve("[FC2-4694056] 马尾美女")
            val newBase = "[FC2-4694056] 马尾美女"
            assertEquals(newFolder.resolve("$newBase - disc1.mp4").toString(), success.video.path)
            assertFalse(Files.exists(oldFolder))
            listOf("disc1", "disc2", "disc3").forEach { part ->
                assertTrue(Files.isRegularFile(newFolder.resolve("$newBase - $part.mp4")))
            }
            assertTrue(Files.readString(newFolder.resolve("movie.nfo")).contains("<title>马尾美女</title>"))
            listOf("disc2" to "cd2", "disc3" to "cd3").forEachIndexed { index, (part, originalPart) ->
                val oldNfo = partNfos[index]
                assertFalse(Files.exists(oldNfo))
                val newNfo = newFolder.resolve("$newBase - $part.nfo")
                assertEquals("<movie><title>$originalPart</title><num>FC2-4694056</num></movie>", Files.readString(newNfo))
                assertEquals("$originalPart poster", Files.readString(newFolder.resolve("$newBase - $part-poster.jpg")))
                assertEquals("$originalPart fanart", Files.readString(newFolder.resolve("$newBase - $part-fanart.jpg")))
            }
            assertEquals("generic poster", Files.readString(newFolder.resolve("poster.jpg")))
            assertEquals("generic fanart", Files.readString(newFolder.resolve("fanart.jpg")))
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
