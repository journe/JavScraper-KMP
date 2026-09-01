package javscraper.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations

enum class ScraperStatus { IDLE, SEARCHING, FOUND, FAILED }

data class ScraperState(
    val id: String,
    val name: String,
    val status: ScraperStatus = ScraperStatus.IDLE,
    val message: String = ""
)

@Composable
fun ScraperStatusBar(scrapers: List<ScraperState>, modifier: Modifier = Modifier) {
    val t = LocalTranslations.current
    Column(modifier = modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(t.commonScraperSites, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        scrapers.forEach { scraper ->
            val c by animateColorAsState(
                targetValue = when (scraper.status) {
                    ScraperStatus.IDLE -> Color.Gray; ScraperStatus.SEARCHING -> Color(0xFFFFA726); ScraperStatus.FOUND -> Color(
                        0xFF66BB6A
                    ); ScraperStatus.FAILED -> Color(0xFFEF5350)
                },
                label = "statusColor"
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(50), color = c, modifier = Modifier.size(10.dp)) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    scraper.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (scraper.status == ScraperStatus.SEARCHING) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
