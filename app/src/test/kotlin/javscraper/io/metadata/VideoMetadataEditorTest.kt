package javscraper.io.metadata

import javscraper.io.NfoWriter
import javscraper.models.Video
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VideoMetadataEditorTest {

    @Test
    fun `update writes edited metadata to matching nfo and reads it back`() {
        val directory = Files.createTempDirectory("javscraper-metadata-edit")
        val videoPath = directory.resolve("ABC-001.mp4")
        val nfoPath = directory.resolve("ABC-001.nfo")
        nfoPath.writeText(
            NfoWriter.generate(
                Video(
                    number = "ABC-001",
                    title = "Old title",
                    actresses = listOf("Old actor"),
                    tags = listOf("Old tag")
                )
            )
        )
        val edited = Video(
            number = "ABC-001",
            title = "New title",
            actresses = listOf("New actor"),
            tags = listOf("New tag"),
            path = videoPath.toString()
        )

        val result = VideoMetadataEditor.update(edited, lockData = true, mergeTags = false)

        val success = assertIs<VideoMetadataEditResult.Success>(result)
        assertEquals("New title", success.video.title)
        assertEquals(listOf("New actor"), success.video.actresses)
        assertEquals(listOf("New tag"), success.video.tags)
        assertEquals(videoPath.toString(), success.video.path)
        val savedNfo = Files.readString(nfoPath)
        assertTrue(savedNfo.contains("<title>New title</title>"))
        assertTrue(savedNfo.contains("<name>New actor</name>"))
        assertTrue(savedNfo.contains("<genre>New tag</genre>"))
        assertTrue(!savedNfo.contains("<genre>Old tag</genre>"))
    }

    @Test
    fun `update merges existing tags when edited tags are unchanged`() {
        val directory = Files.createTempDirectory("javscraper-metadata-merge-tags")
        val videoPath = directory.resolve("ABC-001.mp4")
        val nfoPath = directory.resolve("ABC-001.nfo")
        nfoPath.writeText(
            NfoWriter.generate(
                Video(
                    number = "ABC-001",
                    title = "Old title",
                    tags = listOf("AVC1", "1080P", "Shared tag")
                )
            )
        )
        val edited = Video(
            number = "ABC-001",
            title = "New title",
            tags = listOf("Shared tag", "DMM"),
            path = videoPath.toString()
        )

        val result = VideoMetadataEditor.update(edited, lockData = false, mergeTags = true)

        val success = assertIs<VideoMetadataEditResult.Success>(result)
        assertEquals(listOf("AVC1", "1080P", "Shared tag", "DMM"), success.video.tags)
        val savedNfo = Files.readString(nfoPath)
        assertTrue(savedNfo.contains("<genre>AVC1</genre>"))
        assertTrue(savedNfo.contains("<genre>1080P</genre>"))
        assertTrue(savedNfo.contains("<tag>Shared tag</tag>"))
        assertTrue(savedNfo.contains("<tag>DMM</tag>"))
        assertEquals(1, savedNfo.split("<genre>Shared tag</genre>").size - 1)
    }

    @Test
    fun `update renames movie folder from edited number and title`() {
        val scanDirectory = Files.createTempDirectory("javscraper-metadata-rename")
        try {
            val oldFolder = scanDirectory.resolve("[ABC-001] Old title")
            Files.createDirectory(oldFolder)
            val videoPath = oldFolder.resolve("ABC-001.mp4")
            Files.writeString(videoPath, "video")
            Files.writeString(oldFolder.resolve("ABC-001.nfo"), "<movie><num>ABC-001</num><title>Old title</title></movie>")
            val edited = Video(
                number = "ABC-002",
                title = "New title",
                path = videoPath.toString()
            )

            val result = VideoMetadataEditor.update(
                video = edited,
                lockData = false,
                folderLayers = listOf("[{num}] {title}"),
                scanDir = scanDirectory.toString()
            )

            val newFolder = scanDirectory.resolve("[ABC-002] New title")
            val success = assertIs<VideoMetadataEditResult.Success>(result)
            assertEquals(newFolder.resolve("ABC-001.mp4").toString(), success.video.path)
            assertEquals(videoPath.toString(), success.previousPath)
            assertFalse(Files.exists(oldFolder))
            assertTrue(Files.exists(newFolder.resolve("ABC-001.mp4")))
            assertTrue(Files.readString(newFolder.resolve("ABC-001.nfo")).contains("<title>New title</title>"))
        } finally {
            Files.walk(scanDirectory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update renames folder from edited title only`() {
        val scanDirectory = Files.createTempDirectory("javscraper-metadata-title-rename")
        try {
            val oldFolder = scanDirectory.resolve("[ABC-001] Old title")
            Files.createDirectory(oldFolder)
            val videoPath = oldFolder.resolve("ABC-001.mp4")
            Files.writeString(videoPath, "video")
            Files.writeString(oldFolder.resolve("ABC-001.nfo"), "<movie><num>ABC-001</num><title>Old title</title></movie>")

            val result = VideoMetadataEditor.update(
                video = Video(number = "ABC-001", title = "New title", path = videoPath.toString()),
                lockData = false,
                folderLayers = listOf("[{num}] {title}"),
                scanDir = scanDirectory.toString()
            )

            val newFolder = scanDirectory.resolve("[ABC-001] New title")
            val success = assertIs<VideoMetadataEditResult.Success>(result)
            assertEquals(newFolder.resolve("ABC-001.mp4").toString(), success.video.path)
            assertFalse(Files.exists(oldFolder))
            assertTrue(Files.isRegularFile(newFolder.resolve("ABC-001.mp4")))
            assertTrue(Files.readString(newFolder.resolve("ABC-001.nfo")).contains("<title>New title</title>"))
        } finally {
            Files.walk(scanDirectory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update keeps original folder when target folder already exists`() {
        val scanDirectory = Files.createTempDirectory("javscraper-metadata-rename-conflict")
        try {
            val oldFolder = scanDirectory.resolve("[ABC-001] Old title")
            val targetFolder = scanDirectory.resolve("[ABC-002] New title")
            Files.createDirectory(oldFolder)
            Files.createDirectory(targetFolder)
            val videoPath = oldFolder.resolve("ABC-001.mp4")
            val nfoPath = oldFolder.resolve("ABC-001.nfo")
            Files.writeString(videoPath, "video")
            Files.writeString(nfoPath, "<movie><num>ABC-001</num><title>Old title</title></movie>")
            Files.writeString(targetFolder.resolve("existing.txt"), "keep")

            val result = VideoMetadataEditor.update(
                video = Video(number = "ABC-002", title = "New title", path = videoPath.toString()),
                lockData = false,
                folderLayers = listOf("[{num}] {title}"),
                scanDir = scanDirectory.toString()
            )

            val failed = assertIs<VideoMetadataEditResult.Failed>(result)
            assertTrue(failed.message.contains("Target folder already exists"))
            assertTrue(Files.exists(nfoPath))
            assertTrue(Files.exists(oldFolder.resolve("ABC-001.mp4")))
            assertEquals("keep", Files.readString(targetFolder.resolve("existing.txt")))
        } finally {
            Files.walk(scanDirectory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update rolls back renamed folder when nfo update fails`() {
        val scanDirectory = Files.createTempDirectory("javscraper-metadata-rename-rollback")
        try {
            val oldFolder = scanDirectory.resolve("[ABC-001] Old title")
            Files.createDirectory(oldFolder)
            val videoPath = oldFolder.resolve("ABC-001.mp4")
            val nfoPath = oldFolder.resolve("ABC-001.nfo")
            val invalidNfo = "<movie><num>ABC-001</num>"
            Files.writeString(videoPath, "video")
            Files.writeString(nfoPath, invalidNfo)

            val result = VideoMetadataEditor.update(
                video = Video(number = "ABC-002", title = "New title", path = videoPath.toString()),
                lockData = false,
                folderLayers = listOf("[{num}] {title}"),
                scanDir = scanDirectory.toString()
            )

            assertIs<VideoMetadataEditResult.Failed>(result)
            assertTrue(Files.exists(videoPath))
            assertTrue(Files.exists(oldFolder), "invalid NFO must fail before any folder move")
            assertFalse(Files.exists(scanDirectory.resolve("[ABC-002] New title")))
            assertEquals(invalidNfo, Files.readString(nfoPath))
        } finally {
            Files.walk(scanDirectory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update rejects nfo with bare markup in text before moving folder`() {
        val scanDirectory = Files.createTempDirectory("javscraper-metadata-invalid-text")
        try {
            val oldFolder = scanDirectory.resolve("[ABC-001] Old title")
            Files.createDirectory(oldFolder)
            val videoPath = oldFolder.resolve("ABC-001.mp4")
            val nfoPath = oldFolder.resolve("ABC-001.nfo")
            val invalidNfo = "<movie><num>ABC-001</num><tag>年龄 <18 禁止观看</tag></movie>"
            Files.writeString(videoPath, "video")
            Files.writeString(nfoPath, invalidNfo)

            val result = VideoMetadataEditor.update(
                video = Video(number = "ABC-002", title = "New title", path = videoPath.toString()),
                lockData = false,
                folderLayers = listOf("[{num}] {title}"),
                scanDir = scanDirectory.toString()
            )

            val failed = assertIs<VideoMetadataEditResult.Failed>(result)
            assertTrue(failed.message.contains("ABC-001.nfo"), failed.message)
            assertTrue(failed.message.contains("年龄 <18 禁止观看"), failed.message)
            assertTrue(Files.exists(oldFolder), "folder must stay in place on invalid NFO")
            assertEquals(invalidNfo, Files.readString(nfoPath))
            assertFalse(Files.exists(scanDirectory.resolve("[ABC-002] New title")))
        } finally {
            Files.walk(scanDirectory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update reports missing nfo without creating files`() {
        val directory = Files.createTempDirectory("javscraper-metadata-missing")
        val videoPath = directory.resolve("ABC-001.mp4")

        val result = VideoMetadataEditor.update(
            Video(number = "ABC-001", path = videoPath.toString()),
            lockData = false
        )

        assertIs<VideoMetadataEditResult.NfoMissing>(result)
        assertEquals(0, Files.list(directory).use { it.count() })
    }

    @Test
    fun `update adds fields that were absent in minimal nfo`() {
        val directory = Files.createTempDirectory("javscraper-metadata-add-fields")
        val videoPath = directory.resolve("ABC-001.mp4")
        val nfoPath = directory.resolve("ABC-001.nfo")
        nfoPath.writeText("<movie><title>Old</title><num>ABC-001</num></movie>")
        val edited = Video(
            number = "ABC-001",
            title = "A much longer edited title",
            actresses = listOf("New actor"),
            date = "2026-09-18",
            summary = "New summary",
            maker = "New maker",
            label = "New label",
            series = "New series",
            director = "New director",
            duration = 125,
            rating = 9.5,
            tags = listOf("New tag"),
            source = "JavBus",
            detailUrl = "https://example.com/detail",
            coverUrl = "https://example.com/cover.jpg",
            posterUrl = "https://example.com/poster.jpg",
            sampleImages = listOf("https://example.com/sample.jpg"),
            path = videoPath.toString()
        )

        val result = VideoMetadataEditor.update(edited, lockData = false)

        val savedVideo = assertIs<VideoMetadataEditResult.Success>(result).video
        assertEquals("A much longer edited title", savedVideo.title)
        assertEquals("New actor", savedVideo.actresses.single())
        assertEquals("2026-09-18", savedVideo.date)
        assertEquals("New summary", savedVideo.summary)
        assertEquals("New maker", savedVideo.maker)
        assertEquals("New label", savedVideo.label)
        assertEquals("New series", savedVideo.series)
        assertEquals("New director", savedVideo.director)
        assertEquals(125, savedVideo.duration)
        assertEquals(9.5, savedVideo.rating)
        assertEquals("New tag", savedVideo.tags.single())
        assertEquals("JavBus", savedVideo.source)
        assertEquals("https://example.com/detail", savedVideo.detailUrl)
        assertEquals("https://example.com/cover.jpg", savedVideo.coverUrl)
        assertEquals("https://example.com/poster.jpg", savedVideo.posterUrl)
        assertEquals("https://example.com/sample.jpg", savedVideo.sampleImages.single())
    }

    @Test
    fun `updatePart writes master movie nfo when editing the first part`() {
        val directory = Files.createTempDirectory("javscraper-part-edit")
        val video1 = directory.resolve("ABC-001 - cd1.mp4")
        val video2 = directory.resolve("ABC-001 - cd2.mp4")
        Files.writeString(video1, "v1")
        Files.writeString(video2, "v2")
        val masterNfo = directory.resolve("movie.nfo")
        masterNfo.writeText(NfoWriter.generate(Video(number = "ABC-001", title = "Master")))
        val partNfo1 = directory.resolve("ABC-001 - cd1.nfo")
        val partNfo2 = directory.resolve("ABC-001 - cd2.nfo")
        partNfo1.writeText("<movie><title>cd1</title><num>ABC-001</num></movie>")
        partNfo2.writeText("<movie><title>cd2</title><num>ABC-001</num></movie>")

        val result = VideoMetadataEditor.updatePart(
            video = Video(number = "ABC-001", title = "Edited cd1", path = video1.toString()),
            lockData = false
        )

        val success = assertIs<VideoMetadataEditResult.Success>(result)
        assertEquals("Edited cd1", success.video.title)
        assertTrue(Files.readString(masterNfo).contains("<title>Edited cd1</title>"))
        assertTrue(Files.readString(partNfo1).contains("<title>cd1</title>"))
        assertTrue(Files.readString(partNfo2).contains("<title>cd2</title>"))
    }

    @Test
    fun `updatePart writes part nfo when editing a later part`() {
        val directory = Files.createTempDirectory("javscraper-part-edit-later")
        val video1 = directory.resolve("ABC-001 - cd1.mp4")
        val video2 = directory.resolve("ABC-001 - cd2.mp4")
        Files.writeString(video1, "v1")
        Files.writeString(video2, "v2")
        val masterNfo = directory.resolve("movie.nfo")
        masterNfo.writeText(NfoWriter.generate(Video(number = "ABC-001", title = "Master")))
        val partNfo1 = directory.resolve("ABC-001 - cd1.nfo")
        val partNfo2 = directory.resolve("ABC-001 - cd2.nfo")
        partNfo1.writeText("<movie><title>cd1</title><num>ABC-001</num></movie>")
        partNfo2.writeText("<movie><title>cd2</title><num>ABC-001</num></movie>")

        val result = VideoMetadataEditor.updatePart(
            video = Video(number = "ABC-001", title = "Edited cd2", path = video2.toString()),
            lockData = false
        )

        val success = assertIs<VideoMetadataEditResult.Success>(result)
        assertEquals("Edited cd2", success.video.title)
        assertTrue(Files.readString(masterNfo).contains("<title>Master</title>"))
        assertTrue(Files.readString(partNfo1).contains("<title>cd1</title>"))
        assertTrue(Files.readString(partNfo2).contains("<title>Edited cd2</title>"))
    }

    @Test
    fun `updatePart edits movie nfo when first part nfo absent`() {
        val directory = Files.createTempDirectory("javscraper-part-master-only")
        val videoPath = directory.resolve("ABC-001 - cd1.mp4")
        Files.writeString(videoPath, "v1")
        val masterNfo = directory.resolve("movie.nfo")
        masterNfo.writeText(NfoWriter.generate(Video(number = "ABC-001", title = "Master")))

        val result = VideoMetadataEditor.updatePart(
            video = Video(number = "ABC-001", title = "Edited cd1", path = videoPath.toString()),
            lockData = false
        )

        val success = assertIs<VideoMetadataEditResult.Success>(result)
        assertEquals("Edited cd1", success.video.title)
        assertTrue(Files.readString(masterNfo).contains("<title>Edited cd1</title>"))
    }

    @Test
    fun `update adds a new actress to existing actors and persists in nfo`() {
        val directory = Files.createTempDirectory("javscraper-metadata-add-actress")
        val videoPath = directory.resolve("ABC-001.mp4")
        val nfoPath = directory.resolve("ABC-001.nfo")
        nfoPath.writeText(
            NfoWriter.generate(
                Video(number = "ABC-001", actresses = listOf("Actor A", "Actor B"))
            )
        )

        val result = VideoMetadataEditor.update(
            Video(number = "ABC-001", actresses = listOf("Actor A", "Actor B", "Actor C"), path = videoPath.toString()),
            lockData = false
        )

        val success = assertIs<VideoMetadataEditResult.Success>(result)
        assertEquals(listOf("Actor A", "Actor B", "Actor C"), success.video.actresses)
        val savedNfo = Files.readString(nfoPath)
        assertTrue(savedNfo.contains("<name>Actor C</name>"))
        assertTrue(savedNfo.contains("<order>3</order>"))
    }
}
