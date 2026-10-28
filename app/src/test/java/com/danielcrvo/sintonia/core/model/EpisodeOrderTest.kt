package com.danielcrvo.sintonia.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EpisodeOrderTest {
    private fun episode(id: String, season: Int, number: Int) = Episode(id, season, number, "E$number", null, null, "mp4")

    private val seasons = listOf(
        Season(2, listOf(episode("s2e1", 2, 1))),
        Season(1, listOf(episode("s1e2", 1, 2), episode("s1e1", 1, 1))),
    )

    @Test
    fun `next episode in the same season`() {
        assertEquals("s1e2", EpisodeOrder.next(seasons, episode("s1e1", 1, 1))!!.externalId)
    }

    @Test
    fun `end of season moves to the first of the next`() {
        assertEquals("s2e1", EpisodeOrder.next(seasons, episode("s1e2", 1, 2))!!.externalId)
    }

    @Test
    fun `end of the series has no next`() {
        assertNull(EpisodeOrder.next(seasons, episode("s2e1", 2, 1)))
    }

    private fun progress(id: String, fraction: Float, updatedAt: Long) =
        WatchProgress(ProgressKind.Episode, id, (fraction * 1000).toLong(), 1000, updatedAt, null, null, "serie", null, null, null)

    @Test
    fun `resumes the last started episode`() {
        val resume = EpisodeOrder.resume(seasons, listOf(progress("s1e1", 1f, 1), progress("s1e2", 0.4f, 2)))
        assertEquals("s1e2", resume!!.externalId)
    }

    @Test
    fun `if the last one finished it moves to the next`() {
        val resume = EpisodeOrder.resume(seasons, listOf(progress("s1e2", 0.99f, 5)))
        assertEquals("s2e1", resume!!.externalId)
    }

    @Test
    fun `with no progress it starts at the first`() {
        assertEquals("s1e1", EpisodeOrder.resume(seasons, emptyList())!!.externalId)
    }
}
