package com.metrolist.music.flowneuro

import com.metrolist.music.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowNeuroBadgeTest {
    @Test
    fun injectedMetadataKeepsFlowBadge() {
        val metadata = MediaMetadata(
            id = "song-id",
            title = "Test song",
            artists = listOf(MediaMetadata.Artist(id = "artist-id", name = "Artist")),
            duration = 180,
            suggestedBy = "FLOW",
        )

        assertEquals("FLOW", metadata.suggestedBy)
    }
}
