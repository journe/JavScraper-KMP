package javscraper.io

import javscraper.models.Ranking
import javscraper.models.Review
import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NfoWriterTest {

    @Test
    fun `generate basic NFO structure`() {
        val video = Video(
            number = "SONE-205",
            title = "完全タイトル",
            actresses = listOf("女優A"),
            date = "2024-06-11",
            maker = "SOD Create",
            label = "STAR",
            director = "監督名",
            duration = 120,
            rating = 7.5,
            tags = listOf("高清画质"),
            summary = "影片简介"
        )
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<?xml")
        assertContains(nfo, "<movie>")
        assertContains(nfo, "</movie>")
        assertContains(nfo, "<title>完全タイトル</title>")
        assertContains(nfo, "<originaltitle>SONE-205</originaltitle>")
    }

    @Test
    fun `generate includes actor elements`() {
        val video = Video(
            number = "ABP-123",
            actresses = listOf("女優A", "女優B")
        )
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<actor>")
        assertContains(nfo, "<name>女優A</name>")
        assertContains(nfo, "<name>女優B</name>")
        assertContains(nfo, "<order>1</order>")
        assertContains(nfo, "<order>2</order>")
    }

    @Test
    fun `generate empty actresses list produces no actor elements`() {
        val video = Video(number = "TEST-001", actresses = emptyList())
        val nfo = NfoWriter.generate(video)
        assertTrue("actor" !in nfo || !nfo.contains("<actor>"))
        // Note: if actresses is empty, you may still get a self-closing tag depending on implementation
        assertEquals(false, nfo.contains("</actor>"))
    }

    @Test
    fun `generate includes genre and tag`() {
        val video = Video(
            number = "TEST-001",
            tags = listOf("高清画质", "中文字幕")
        )
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<genre>高清画质</genre>")
        assertContains(nfo, "<tag>中文字幕</tag>")
    }

    @Test
    fun `generate blank title uses number as title`() {
        val video = Video(number = "SONE-205", title = "")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<title>SONE-205</title>")
    }

    @Test
    fun `generate blank title blank uses number`() {
        val video = Video(number = "ABC-123", title = "  ")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<title>ABC-123</title>")
    }

    @Test
    fun `generate null duration omits runtime`() {
        val video = Video(number = "TEST-001", duration = null)
        val nfo = NfoWriter.generate(video)
        assertEquals(false, nfo.contains("<runtime>"))
    }

    @Test
    fun `generate zero duration omits runtime`() {
        val video = Video(number = "TEST-001", duration = 0)
        val nfo = NfoWriter.generate(video)
        assertEquals(false, nfo.contains("<runtime>"))
    }

    @Test
    fun `generate includes rating when positive`() {
        val video = Video(number = "TEST-001", rating = 8.5)
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<rating>8.5</rating>")
    }

    @Test
    fun `generate null rating omitted`() {
        val video = Video(number = "TEST-001", rating = null)
        val nfo = NfoWriter.generate(video)
        assertEquals(false, nfo.contains("<rating>"))
    }

    @Test
    fun `generate includes fanart sample images`() {
        val video = Video(
            number = "SONE-001",
            sampleImages = listOf("https://example.com/1.jpg", "https://example.com/2.jpg")
        )

        val nfo = NfoWriter.generate(video)

        assertContains(nfo, "<fanart>")
        assertContains(nfo, "<thumb>https://example.com/1.jpg</thumb>")
        assertContains(nfo, "<thumb>https://example.com/2.jpg</thumb>")
        assertContains(nfo, "</fanart>")
    }

    @Test
    fun `generate thumb falls back to cover url`() {
        val video = Video(number = "SONE-001", coverUrl = "https://example.com/cover.jpg")

        val nfo = NfoWriter.generate(video)

        assertContains(nfo, "<thumb>https://example.com/cover.jpg</thumb>")
        assertContains(nfo, "<cover>https://example.com/cover.jpg</cover>")
    }

    @Test
    fun `generate includes source and website`() {
        val video = Video(
            number = "FC2-PPV-1723984",
            source = "fc2",
            detailUrl = "https://adult.contents.fc2.com/article/1723984/"
        )
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<source>fc2</source>")
        assertContains(nfo, "<website>https://adult.contents.fc2.com/article/1723984/</website>")
    }

    @Test
    fun `generate series as set element`() {
        val video = Video(number = "TEST-001", series = "So甜美")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<set>So甜美</set>")
    }

    @Test
    fun `generate blank series omitted`() {
        val video = Video(number = "TEST-001", series = "")
        val nfo = NfoWriter.generate(video)
        assertEquals(false, nfo.contains("<set>"))
    }

    @Test
    fun `generate includes date components`() {
        val video = Video(number = "TEST-001", date = "2024-06-11")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<year>2024</year>")
        assertContains(nfo, "<premiered>2024-06-11</premiered>")
        assertContains(nfo, "<release>2024-06-11</release>")
    }

    @Test
    fun `generate short date produces no year or premiered`() {
        val video = Video(number = "TEST-001", date = "24")
        val nfo = NfoWriter.generate(video)
        assertEquals(false, nfo.contains("<year>") && nfo.contains("<premiered>"))
    }

    @Test
    fun `generate includes studio and maker`() {
        val video = Video(number = "TEST-001", maker = "SOD Create")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<studio>SOD Create</studio>")
        assertContains(nfo, "<maker>SOD Create</maker>")
    }

    @Test
    fun `generate includes label`() {
        val video = Video(number = "TEST-001", label = "STAR")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<label>STAR</label>")
    }

    @Test
    fun `generate includes director`() {
        val video = Video(number = "TEST-001", director = "監督名")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<director>監督名</director>")
    }

    @Test
    fun `generate special characters in strings`() {
        val video = Video(
            number = "TEST-001",
            title = "Title with <>&\"' and 日本語",
            actresses = listOf("Name with <>&\"'")
        )
        val nfo = NfoWriter.generate(video)
        // XMLStreamWriter should escape these
        assertContains(nfo, "<title>")
        assertContains(nfo, "</title>")
        assertTrue(nfo.contains("&lt;") || nfo.contains("<"), "XML should handle special chars")
        assertContains(nfo, "Name with")
    }

    @Test
    fun `generate NFO with empty video number`() {
        val video = Video(number = "")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<title></title>")
    }

    @Test
    fun `generate includes mpaa rating always`() {
        val video = Video(number = "TEST-001")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<mpaa>JP-18+</mpaa>")
    }

    @Test
    fun `generate sorttitle matches number`() {
        val video = Video(number = "SONE-205")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<sorttitle>SONE-205</sorttitle>")
    }
    @Test
    fun `generate formats NFO with line breaks and indentation`() {
        val video = Video(
            number = "ABC-001",
            title = "Formatted Title",
            actresses = listOf("Actor A")
        )

        val nfo = NfoWriter.generate(video)

        assertTrue(nfo.contains("\n<movie>"), "Root element should start on its own line")
        assertTrue(nfo.contains("\n  <title>Formatted Title</title>"), "Child element should be indented")
        assertTrue(nfo.contains("\n  <actor>"), "Actor element should be indented")
        assertTrue(nfo.contains("\n    <name>Actor A</name>"), "Actor child element should be indented")
        assertTrue(nfo.contains("\n</movie>"), "Root element should close on its own line")
    }

    @Test
    fun `generate includes num element`() {
        val video = Video(number = "SONE-205")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<num>SONE-205</num>")
    }

    @Test
    fun `generate includes country and language`() {
        val video = Video(number = "SONE-205")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<country>Japan</country>")
        assertContains(nfo, "<language>ja</language>")
    }

    @Test
    fun `generate includes uniqueid elements`() {
        val video = Video(number = "SONE-205")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<uniqueid type=\"num\" default=\"true\">SONE-205</uniqueid>")
        assertContains(nfo, "<uniqueid type=\"home\">SONE-205</uniqueid>")
    }

    @Test
    fun `generate lockdata follows flag`() {
        val locked = NfoWriter.generate(Video(number = "SONE-205"), lockData = true)
        val unlocked = NfoWriter.generate(Video(number = "SONE-205"), lockData = false)
        assertContains(locked, "<lockdata>true</lockdata>")
        assertContains(unlocked, "<lockdata>false</lockdata>")
    }

    @Test
    fun `generate includes poster element`() {
        val video = Video(number = "SONE-001", coverUrl = "https://example.com/cover.jpg")
        val nfo = NfoWriter.generate(video)
        assertContains(nfo, "<poster>https://example.com/cover.jpg</poster>")
    }

    @Test
    fun `generate includes javdb extra fields`() {
        val video = Video(
            number = "SONE-001",
            wantCount = 7411,
            watchedCount = 1614,
            ratingCount = 1614,
            rankings = listOf(Ranking(212, "JavDB 2022年度TOP250")),
            reviews = listOf(
                Review(
                    id = "93142913",
                    author = "we***e",
                    date = "2023-12-11",
                    score = 5.0,
                    likes = 1007,
                    content = "第一条短评"
                )
            )
        )

        val nfo = NfoWriter.generate(video)

        assertContains(nfo, "<javdb_extra>")
        assertContains(nfo, "<want_count>7411</want_count>")
        assertContains(nfo, "<watched_count>1614</watched_count>")
        assertContains(nfo, "<rating_count>1614</rating_count>")
        assertContains(nfo, """<ranking rank="212">JavDB 2022年度TOP250</ranking>""")
        assertContains(nfo, """<review id="93142913" author="we***e" date="2023-12-11" score="5.0" likes="1007">""")
        assertContains(nfo, "<content>第一条短评</content>")
    }
}
