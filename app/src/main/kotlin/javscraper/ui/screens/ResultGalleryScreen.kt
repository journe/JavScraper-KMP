package javscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.models.Video
import javscraper.ui.components.PosterCard

@Composable
fun ResultGalleryScreen(results: List<Video>, onClear: () -> Unit, onOpenOutputDir: () -> Unit, outputDir: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Scrape Results", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); if (results.isNotEmpty()) Text("${results.size} movies scraped", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (results.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onOpenOutputDir) { Icon(Icons.Default.Folder, null); Spacer(Modifier.width(8.dp)); Text("Open Output") }; Button(onClick = onClear, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Clear") } }
        }
        Spacer(Modifier.height(16.dp))
        if (results.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)); Spacer(Modifier.height(12.dp)); Text("No results yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        else { LazyVerticalGrid(columns = GridCells.Adaptive(190.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) { items(results) { PosterCard(it) } } }
    }
}