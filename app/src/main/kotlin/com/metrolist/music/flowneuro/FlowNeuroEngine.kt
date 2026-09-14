/*
 * FlowNeuro integration inspired by Flow Android Client.
 * Original project: https://github.com/A-EDev/Flow
 * Distributed under GPL-3.0; keep this attribution in derivative works.
 */
package com.metrolist.music.flowneuro

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.YouTube.SearchFilter
import com.metrolist.innertube.models.SongItem
import com.metrolist.lastfm.LastFM
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.constants.FlowNeuroAllowAlternativesKey
import com.metrolist.music.constants.FlowNeuroArtistDiversityKey
import com.metrolist.music.constants.FlowNeuroDiversityEnabledKey
import com.metrolist.music.constants.FlowNeuroArtistRotationEnabledKey
import com.metrolist.music.constants.FlowNeuroAvoidUnrelatedKey
import com.metrolist.music.constants.FlowNeuroContinuityKey
import com.metrolist.music.constants.FlowNeuroContinuityEnabledKey
import com.metrolist.music.constants.FlowNeuroConfirmationKey
import com.metrolist.music.constants.FlowNeuroConfirmationEnabledKey
import com.metrolist.music.constants.FlowNeuroEnabledKey
import com.metrolist.music.constants.FlowNeuroExcludeQueueKey
import com.metrolist.music.constants.FlowNeuroExcludeLiveRemixesKey
import com.metrolist.music.constants.FlowNeuroNetworkKey
import com.metrolist.music.constants.FlowNeuroNetworkEnabledKey
import com.metrolist.music.constants.FlowNeuroLearningEnabledKey
import com.metrolist.music.constants.FlowNeuroLearningLevelKey
import com.metrolist.music.constants.FlowNeuroLastLearningDayKey
import com.metrolist.music.constants.FlowNeuroLastProfileMaintenanceDayKey
import com.metrolist.music.constants.FlowNeuroNegativeSignalsKey
import com.metrolist.music.constants.FlowNeuroInjectedCountKey
import com.metrolist.music.constants.FlowNeuroArtistAffinitiesKey
import com.metrolist.music.constants.FlowNeuroAlbumAffinitiesKey
import com.metrolist.music.constants.FlowNeuroTimeAffinitiesKey
import com.metrolist.music.constants.FlowNeuroProfileVersionKey
import com.metrolist.music.constants.FlowNeuroRecommendationReasonsKey
import com.metrolist.music.constants.FlowNeuroProfileLastUpdatedKey
import com.metrolist.music.constants.FlowNeuroPositiveSignalsKey
import com.metrolist.music.constants.FlowNeuroRecentIdsKey
import com.metrolist.music.constants.FlowNeuroRecentArtistsKey
import com.metrolist.music.constants.FlowNeuroRecentAlbumsKey
import com.metrolist.music.constants.FlowNeuroRepeatWindowKey
import com.metrolist.music.constants.FlowNeuroRepeatWindowEnabledKey
import com.metrolist.music.constants.FlowNeuroSimilarityKey
import com.metrolist.music.constants.FlowNeuroSimilarityEnabledKey
import com.metrolist.music.constants.FlowNeuroWhitelistEnabledKey
import com.metrolist.music.constants.FlowNeuroWhitelistKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import org.json.JSONObject

/** Selects and ranks local/online candidates for the internal FlowNeuro queue injector. */
class FlowNeuroEngine(
    private val context: Context,
    private val database: MusicDatabase,
) {
    suspend fun recommend(current: MediaMetadata, queuedIds: Set<String>): Result =
        runCatching {
            maintainProfileDaily()
            recommendInternal(current, queuedIds)
        }.getOrElse { Result.empty() }

    private suspend fun recommendInternal(current: MediaMetadata, queuedIds: Set<String>): Result {
        val prefs = context.dataStore.data.first()
        if (prefs[FlowNeuroEnabledKey] ?: true != true) return Result.empty()

        val local = withContext(Dispatchers.IO) {
            database.relatedSongs(current.id).map { it.toMediaMetadata() }
        }
        val online = if (shouldUseOnline(prefs)) fetchOnline(current.id) else emptyList()
        val lastFm = fetchLastFmCandidates(current)
        val candidates = (local + online + lastFm).distinctBy { it.id }
        val excludeQueue = prefs[FlowNeuroExcludeQueueKey] ?: true
        val minimum = similarityThreshold(prefs[FlowNeuroSimilarityKey] ?: "90%")
        val whitelistEnabled = prefs[FlowNeuroWhitelistEnabledKey] ?: false
        val whitelist = parseArtists(prefs[FlowNeuroWhitelistKey].orEmpty())
        val allowAlternatives = prefs[FlowNeuroAllowAlternativesKey] ?: false
        val excludeLiveRemixes = prefs[FlowNeuroExcludeLiveRemixesKey] ?: true
        val avoidUnrelated = prefs[FlowNeuroAvoidUnrelatedKey] ?: true
        val diversity = if (prefs[FlowNeuroDiversityEnabledKey] ?: true) prefs[FlowNeuroArtistDiversityKey] ?: "Equilibrada" else "Sin límite"
        val now = System.currentTimeMillis()
        val repeatWindow = if (prefs[FlowNeuroRepeatWindowEnabledKey] ?: true) prefs[FlowNeuroRepeatWindowKey] ?: "48 horas" else "Sin límite"
        val recentIds = parseRecentEntries(prefs[FlowNeuroRecentIdsKey], now, repeatWindow).map { it.id }.toSet()
        val rotationEnabled = prefs[FlowNeuroArtistRotationEnabledKey] ?: true
        val recentArtists = if (rotationEnabled) parseRecentLabels(prefs[FlowNeuroRecentArtistsKey], now) else emptySet()
        val recentAlbums = if (rotationEnabled) parseRecentLabels(prefs[FlowNeuroRecentAlbumsKey], now) else emptySet()
        val artistAffinities = decodeScores(prefs[FlowNeuroArtistAffinitiesKey])
        val albumAffinities = decodeScores(prefs[FlowNeuroAlbumAffinitiesKey])
        val timeAffinities = decodeScores(prefs[FlowNeuroTimeAffinitiesKey])
        val timeAffinity = timeAffinities[timeBucket()] ?: 0.0
        val selectedArtists = mutableSetOf<String>()
        val selectedAlbums = mutableSetOf<String>()
        val currentAlbum = current.album?.title?.let(::normalize)

        val ranked = candidates.asSequence()
            .filter { it.id != current.id && (!excludeQueue || it.id !in queuedIds) }
            .filter { it.id !in recentIds }
            .filter { candidate ->
                val artists = candidate.artists.map { normalize(it.name) }.filter(String::isNotBlank)
                val album = candidate.album?.title?.let(::normalize)
                // Recent artists are a diversity preference, not a hard stop:
                // making them fatal can exhaust the candidate pool after the
                // first FLOW injection. Song IDs and albums remain hard stops.
                (artists.none { it in recentArtists } || artists.any { it in current.artists.map { artist -> normalize(artist.name) } }) &&
                    (album == null || (album !in recentAlbums && album != currentAlbum && album !in selectedAlbums))
            }
            .filter { !whitelistEnabled || it.artists.any { artist -> normalize(artist.name) in whitelist } }
            .filter { allowAlternatives || !isAlternative(it.title) }
            .filter { !excludeLiveRemixes || !isLiveOrRemix(it.title) }
            .map {
                it to score(
                    current,
                    it,
                    localIds = local.mapTo(HashSet()) { song -> song.id },
                    artistAffinities = artistAffinities,
                    albumAffinities = albumAffinities,
                    timeAffinity = timeAffinity,
                )
            }
            .filter { !(prefs[FlowNeuroSimilarityEnabledKey] ?: true) || !avoidUnrelated || it.second >= minimum }
            .sortedByDescending { it.second }
            .mapNotNull { (candidate, score) ->
                val artist = candidate.artists.firstOrNull()?.name?.let(::normalize) ?: return@mapNotNull null
                val blockedByDiversity = when (diversity) {
                    "Alta" -> artist in selectedArtists || artist == current.artists.firstOrNull()?.name?.let(::normalize)
                    "Equilibrada" -> artist in selectedArtists
                    else -> false
                }
                val album = candidate.album?.title?.let(::normalize)
                if (blockedByDiversity || (album != null && album in selectedAlbums)) null else candidate.also {
                    selectedArtists += artist
                    album?.let(selectedAlbums::add)
                }
            }
            // Fill a real discovery block instead of behaving like YouTube Music's
            // single safe recommendation. Diversity and the persistent rotation
            // filters decide which candidates survive this batch.
            // Una sola inyección por transición: la cola original no se sustituye.
            .take(1)
            .map { it.copy(suggestedBy = "FLOW").toMediaItem() }
            .toList()

        return Result(
            ranked,
            candidates.size,
            local.size,
            if (prefs[FlowNeuroContinuityEnabledKey] ?: true) prefs[FlowNeuroContinuityKey] ?: "FlowNeuro dominante" else "FlowNeuro dominante",
        )
    }

    private suspend fun fetchLastFmCandidates(current: MediaMetadata): List<MediaMetadata> = withContext(Dispatchers.IO) {
        if (!LastFM.hasPublicApiKey()) return@withContext emptyList()
        val artist = current.artists.firstOrNull()?.name ?: return@withContext emptyList()
        val resolved = mutableListOf<MediaMetadata>()
        for (candidate in LastFM.similarTracks(artist, current.title, limit = 24).getOrDefault(emptyList()).take(12)) {
                val songs = YouTube.search("${candidate.artist} ${candidate.track}", SearchFilter.FILTER_SONG)
                    .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                songs.firstOrNull { song ->
                    val artistMatches = song.artists.any { normalize(it.name) == normalize(candidate.artist) }
                    val titleMatches = normalize(song.title).contains(normalize(candidate.track)) ||
                        normalize(candidate.track).contains(normalize(song.title))
                    artistMatches && titleMatches && !isAlternative(song.title)
                }?.toMediaMetadata()?.takeIf { it.duration > 0 }?.let(resolved::add)
        }
        resolved
    }

    suspend fun requiredCompletionRatio(): Float =
        when (context.dataStore.data.first().let { prefs -> if (prefs[FlowNeuroConfirmationEnabledKey] ?: true) prefs[FlowNeuroConfirmationKey] else "Inmediata" }) {
            "Inmediata" -> 0f
            "Tras 80%" -> 0.80f
            else -> 0.60f
        }

    suspend fun recordPlaybackSignal(
        songId: String,
        completionRatio: Float,
        manuallySkipped: Boolean,
        artists: List<String> = emptyList(),
        album: String? = null,
    ) {
        context.safeDataStoreEdit { prefs ->
            if (prefs[FlowNeuroLearningEnabledKey] ?: true != true) return@safeDataStoreEdit
            val confirmation = when (if (prefs[FlowNeuroConfirmationEnabledKey] ?: true) prefs[FlowNeuroConfirmationKey] else "Inmediata") {
                "Inmediata" -> 0f
                "Tras 80%" -> 0.80f
                else -> 0.60f
            }
            val ratio = completionRatio.coerceIn(0f, 1f)
            if (manuallySkipped && ratio < confirmation.coerceAtLeast(0.20f)) {
                prefs[FlowNeuroNegativeSignalsKey] = (prefs[FlowNeuroNegativeSignalsKey] ?: 0) + 1
                updateProfile(prefs, artists, album, positive = false)
                return@safeDataStoreEdit
            }
            if (ratio < confirmation) return@safeDataStoreEdit
            prefs[FlowNeuroPositiveSignalsKey] = (prefs[FlowNeuroPositiveSignalsKey] ?: 0) + 1
            prefs[FlowNeuroInjectedCountKey] = (prefs[FlowNeuroInjectedCountKey] ?: 0) + 1
            updateProfile(prefs, artists, album, positive = true)
            val today = LocalDate.now().toString()
            if (prefs[FlowNeuroLastLearningDayKey] != today) {
                prefs[FlowNeuroLastLearningDayKey] = today
                prefs[FlowNeuroLearningLevelKey] = ((prefs[FlowNeuroLearningLevelKey] ?: 1) + 1).coerceAtMost(100)
            }
        }
    }

    suspend fun recordInjected(songId: String, artists: List<String> = emptyList(), album: String? = null) {
        context.safeDataStoreEdit { prefs ->
            val now = System.currentTimeMillis()
            val window = if (prefs[FlowNeuroRepeatWindowEnabledKey] ?: true) prefs[FlowNeuroRepeatWindowKey] ?: "48 horas" else "48 horas"
            val entries = parseRecentEntries(prefs[FlowNeuroRecentIdsKey], now, window)
                .filter { it.id != songId }
                .let { if (window == "Siempre") it else it.takeLast(499) }
                .toMutableList()
            entries += RecentEntry(songId, now)
            prefs[FlowNeuroRecentIdsKey] = entries.joinToString(",") { "${it.id}:${it.injectedAt}" }
            recordRecentLabels(prefs, FlowNeuroRecentArtistsKey, artists.map(::normalize), now)
            album?.takeIf(String::isNotBlank)?.let { recordRecentLabels(prefs, FlowNeuroRecentAlbumsKey, listOf(normalize(it)), now) }
        }
    }

    suspend fun clearLearning() {
        context.safeDataStoreEdit { prefs ->
            prefs.remove(FlowNeuroPositiveSignalsKey)
            prefs.remove(FlowNeuroNegativeSignalsKey)
            prefs.remove(FlowNeuroInjectedCountKey)
            prefs.remove(FlowNeuroRecentIdsKey)
            prefs.remove(FlowNeuroRecentArtistsKey)
            prefs.remove(FlowNeuroRecentAlbumsKey)
            prefs.remove(FlowNeuroProfileVersionKey)
            prefs.remove(FlowNeuroArtistAffinitiesKey)
            prefs.remove(FlowNeuroAlbumAffinitiesKey)
            prefs.remove(FlowNeuroTimeAffinitiesKey)
            prefs.remove(FlowNeuroRecommendationReasonsKey)
            prefs.remove(FlowNeuroProfileLastUpdatedKey)
            prefs.remove(FlowNeuroLearningLevelKey)
            prefs.remove(FlowNeuroLastLearningDayKey)
            prefs.remove(FlowNeuroLastProfileMaintenanceDayKey)
        }
    }

    private suspend fun maintainProfileDaily() {
        val today = LocalDate.now().toString()
        context.safeDataStoreEdit { prefs ->
            if (prefs[FlowNeuroLastProfileMaintenanceDayKey] == today) return@safeDataStoreEdit
            fun decay(key: androidx.datastore.preferences.core.Preferences.Key<String>) {
                val scores = decodeScores(prefs[key])
                    .mapValues { (_, value) -> (value * 0.98).coerceIn(-1.0, 1.0) }
                    .filterValues { abs(it) >= 0.02 }
                prefs[key] = JSONObject(scores).toString()
            }
            decay(FlowNeuroArtistAffinitiesKey)
            decay(FlowNeuroAlbumAffinitiesKey)
            decay(FlowNeuroTimeAffinitiesKey)
            prefs[FlowNeuroLastProfileMaintenanceDayKey] = today
        }
    }

    private suspend fun fetchOnline(songId: String): List<MediaMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val next = YouTube.next(WatchEndpoint(videoId = songId)).getOrNull()
            val direct = next?.items?.map { it.toMediaMetadata() }.orEmpty()
            val related = next?.relatedEndpoint?.let { YouTube.related(it).getOrNull()?.songs?.map { song -> song.toMediaMetadata() } }.orEmpty()
            (direct + related).distinctBy { it.id }
        }.getOrDefault(emptyList())
    }

    private fun shouldUseOnline(prefs: Preferences): Boolean {
        if (prefs[FlowNeuroNetworkEnabledKey] ?: true != true) return false
        return when (prefs[FlowNeuroNetworkKey] ?: "Wi‑Fi + datos móviles") {
            "Solo Wi‑Fi" -> isWifiAvailable()
            else -> isNetworkAvailable()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        return manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun isWifiAvailable(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        return manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    private fun score(
        current: MediaMetadata,
        candidate: MediaMetadata,
        localIds: Set<String>,
        artistAffinities: Map<String, Double>,
        albumAffinities: Map<String, Double>,
        timeAffinity: Double,
    ): Float {
        val currentArtists = current.artists.map { normalize(it.name) }.toSet()
        val candidateArtists = candidate.artists.map { normalize(it.name) }.toSet()
        val affinityScore = candidateArtists.maxOfOrNull { artistAffinities[it] ?: 0.0 } ?: 0.0
        val artistScore = (if (currentArtists.intersect(candidateArtists).isNotEmpty()) 0.58 else 0.0) + affinityScore * 0.15
        val albumAffinity = candidate.album?.id?.let { albumAffinities[it] } ?: 0.0
        val albumScore = (if (current.album?.id != null && current.album.id == candidate.album?.id) 0.20 else 0.0) + albumAffinity * 0.10
        val yearScore = if (current.album != null && candidate.album != null) 0.07f else 0f
        val localScore = if (candidate.id in localIds) 0.15f else 0f
        val durationScore = if (current.duration > 0 && candidate.duration > 0) {
            (1f - (abs(current.duration - candidate.duration).toFloat() / current.duration).coerceIn(0f, 1f)) * 0.10f
        } else 0f
        return (artistScore + albumScore + yearScore + localScore + durationScore + timeAffinity * 0.05).toFloat().coerceIn(0f, 1f)
    }

    private fun decodeScores(value: String?): Map<String, Double> = runCatching {
        val json = JSONObject(value ?: "{}")
        json.keys().asSequence().associateWith { json.optDouble(it, 0.0) }
    }.getOrDefault(emptyMap())

    private data class RecentEntry(val id: String, val injectedAt: Long)

    private fun parseRecentEntries(value: String?, now: Long, window: String): List<RecentEntry> {
        val cutoff = when (window) {
            "Sin límite" -> 0L
            "24 horas" -> 24L * 60 * 60 * 1000
            "7 días" -> 7L * 24 * 60 * 60 * 1000
            "Siempre" -> Long.MAX_VALUE
            else -> 48L * 60 * 60 * 1000
        }
        return value.orEmpty().split(',').asSequence().filter(String::isNotBlank).mapNotNull { raw ->
            val separator = raw.lastIndexOf(':')
            val id = if (separator > 0) raw.substring(0, separator) else raw
            val timestamp = if (separator > 0) raw.substring(separator + 1).toLongOrNull() ?: now else now
            RecentEntry(id, timestamp)
        }.filter { cutoff > 0L && (cutoff == Long.MAX_VALUE || now - it.injectedAt < cutoff) }.toList()
    }

    private fun parseRecentLabels(value: String?, now: Long): Set<String> = runCatching {
        val json = JSONObject(value ?: "{}")
        val timestamps = json.keys().asSequence().map { json.optLong(it, now) }.distinct().sortedDescending().take(20).toSet()
        json.keys().asSequence().filter { json.optLong(it, now) in timestamps }.toSet()
    }.getOrDefault(emptySet())

    private fun recordRecentLabels(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        labels: List<String>,
        now: Long,
    ) {
        if (labels.isEmpty()) return
        val json = JSONObject(prefs[key] ?: "{}")
        labels.filter(String::isNotBlank).distinct().forEach { json.put(it, now) }
        val timestamps = json.keys().asSequence().map { json.optLong(it, now) }.distinct().sortedDescending().take(20).toSet()
        val trimmed = JSONObject()
        json.keys().asSequence().filter { json.optLong(it, now) in timestamps }.forEach { trimmed.put(it, json.optLong(it, now)) }
        prefs[key] = trimmed.toString()
    }

    private fun timeBucket(): String = (LocalTime.now().hour / 4).toString()

    private fun updateProfile(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        artists: List<String>,
        album: String?,
        positive: Boolean,
    ) {
        val delta = if (positive) 0.08 else -0.12
        fun update(key: androidx.datastore.preferences.core.Preferences.Key<String>, values: List<String>) {
            if (values.isEmpty()) return
            val scores = decodeScores(prefs[key]).toMutableMap()
            values.map(::normalize).filter(String::isNotBlank).distinct().forEach { name ->
                scores[name] = ((scores[name] ?: 0.0) + delta).coerceIn(-1.0, 1.0)
            }
            prefs[key] = JSONObject(scores.filterValues { abs(it) >= 0.02 }.toMap()).toString()
        }
        update(FlowNeuroArtistAffinitiesKey, artists)
        update(FlowNeuroAlbumAffinitiesKey, listOfNotNull(album))
        update(FlowNeuroTimeAffinitiesKey, listOf(timeBucket()))
        prefs[FlowNeuroProfileVersionKey] = 1
        prefs[FlowNeuroProfileLastUpdatedKey] = System.currentTimeMillis()
        prefs[FlowNeuroRecommendationReasonsKey] = if (positive) "afinidad+escucha" else "salto+rechazo"
    }

    private fun parseArtists(value: String): Set<String> = value.split(',', ';', '\n').asSequence().map(::normalize).filter(String::isNotBlank).take(20_000).toSet()
    private fun similarityThreshold(value: String): Float = when (value) {
        "90%" -> 0.90f
        "80%" -> 0.80f
        "70%" -> 0.70f
        "60%" -> 0.60f
        else -> 0.90f
    }
    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
    private fun isLiveOrRemix(title: String): Boolean = listOf("live", "en vivo", "remix").any { title.contains(it, ignoreCase = true) }
    private fun isAlternative(title: String): Boolean = isLiveOrRemix(title) || listOf("acoustic", "acústica", "cover", "karaoke", "version").any { title.contains(it, ignoreCase = true) }

    data class Result(val items: List<MediaItem>, val candidateCount: Int, val localCount: Int, val continuity: String) {
        companion object { fun empty() = Result(emptyList(), 0, 0, "FlowNeuro dominante") }
    }
}
