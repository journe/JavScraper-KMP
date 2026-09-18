package javscraper.ui.screens.detail

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveVideoInfoCardTest {

    @Test
    fun `hidden poster keeps its last measured space`() {
        val lastMeasured = PosterSpace(width = 320, height = 452)

        assertEquals(lastMeasured, reservedPosterSpace(measured = null, cached = lastMeasured))
        assertEquals(
            PosterSpace(width = 280, height = 452),
            reservedPosterSpace(
                measured = PosterSpace(width = 280, height = 452),
                cached = lastMeasured
            )
        )
    }
    @Test
    fun `video info width uses maximum when poster is hidden`() {
        assertEquals(360.dp, adaptiveVideoInfoCardWidth(500.dp, 0.dp))
        assertEquals(200.dp, adaptiveVideoInfoCardWidth(200.dp, 0.dp))
    }

    @Test
    fun `video info width consumes remaining width when it fits beside poster`() {
        assertEquals(360.dp, adaptiveVideoInfoCardWidth(800.dp, 200.dp))
        assertEquals(284.dp, adaptiveVideoInfoCardWidth(500.dp, 200.dp))
        assertEquals(184.dp, adaptiveVideoInfoCardWidth(400.dp, 200.dp))
    }

    @Test
    fun `video info width uses maximum when it must wrap`() {
        assertEquals(360.dp, adaptiveVideoInfoCardWidth(400.dp, 260.dp))
        assertEquals(350.dp, adaptiveVideoInfoCardWidth(350.dp, 200.dp))
        assertEquals(300.dp, adaptiveVideoInfoCardWidth(300.dp, 200.dp))
    }
}
