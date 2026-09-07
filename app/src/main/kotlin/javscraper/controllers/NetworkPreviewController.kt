package javscraper.controllers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.models.Video
import javscraper.models.mergeNetworkPreviewCandidates

/** Accumulates network search candidates for the network preview screen. */
class NetworkPreviewController {
    var candidates by mutableStateOf<List<Video>>(emptyList())
        private set

    fun add(incoming: List<Video>) {
        candidates = mergeNetworkPreviewCandidates(candidates, incoming)
    }

    fun clear() {
        candidates = emptyList()
    }
}