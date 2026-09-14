/* FlowNeuro/Flow attribution: https://github.com/A-EDev/Flow · GPL-3.0 */
package com.metrolist.music.flowneuro

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.FlowNeuroAllowAlternativesKey
import com.metrolist.music.constants.FlowNeuroArtistDiversityKey
import com.metrolist.music.constants.FlowNeuroDiversityEnabledKey
import com.metrolist.music.constants.FlowNeuroArtistRotationEnabledKey
import com.metrolist.music.constants.FlowNeuroAvoidUnrelatedKey
import com.metrolist.music.constants.FlowNeuroConfirmationKey
import com.metrolist.music.constants.FlowNeuroConfirmationEnabledKey
import com.metrolist.music.constants.FlowNeuroContinuityKey
import com.metrolist.music.constants.FlowNeuroContinuityEnabledKey
import com.metrolist.music.constants.FlowNeuroEnabledKey
import com.metrolist.music.constants.FlowNeuroExcludeQueueKey
import com.metrolist.music.constants.FlowNeuroExcludeLiveRemixesKey
import com.metrolist.music.constants.FlowNeuroLastLearningDayKey
import com.metrolist.music.constants.FlowNeuroLastProfileMaintenanceDayKey
import com.metrolist.music.constants.FlowNeuroLearningLevelKey
import com.metrolist.music.constants.FlowNeuroLearningEnabledKey
import com.metrolist.music.constants.FlowNeuroNegativeSignalsKey
import com.metrolist.music.constants.FlowNeuroInjectedCountKey
import com.metrolist.music.constants.FlowNeuroNetworkKey
import com.metrolist.music.constants.FlowNeuroNetworkEnabledKey
import com.metrolist.music.constants.FlowNeuroPositiveSignalsKey
import com.metrolist.music.constants.FlowNeuroRecentIdsKey
import com.metrolist.music.constants.FlowNeuroRepeatWindowKey
import com.metrolist.music.constants.FlowNeuroRepeatWindowEnabledKey
import com.metrolist.music.constants.FlowNeuroProfileVersionKey
import com.metrolist.music.constants.FlowNeuroArtistAffinitiesKey
import com.metrolist.music.constants.FlowNeuroAlbumAffinitiesKey
import com.metrolist.music.constants.FlowNeuroTimeAffinitiesKey
import com.metrolist.music.constants.FlowNeuroRecommendationReasonsKey
import com.metrolist.music.constants.FlowNeuroProfileLastUpdatedKey
import com.metrolist.music.constants.FlowNeuroSimilarityKey
import com.metrolist.music.constants.FlowNeuroSimilarityEnabledKey
import com.metrolist.music.constants.FlowNeuroTransparencyEnabledKey
import com.metrolist.music.constants.FlowNeuroWhitelistEnabledKey
import com.metrolist.music.constants.FlowNeuroWhitelistKey
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Portable, local-only FlowNeuro profile backup. No account or network is involved. */
object FlowNeuroBackup {
    private const val FORMAT = "flowneuro-profile"
    private const val VERSION = 1

    suspend fun export(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = context.dataStore.data.first()
            val root = JSONObject().apply {
                put("format", FORMAT)
                put("version", VERSION)
                put("exportedAt", System.currentTimeMillis())
                put("enabled", prefs[FlowNeuroEnabledKey] ?: true)
                put("excludeQueue", prefs[FlowNeuroExcludeQueueKey] ?: true)
                put("excludeQueue", prefs[FlowNeuroExcludeQueueKey] ?: true)
                put("similarity", prefs[FlowNeuroSimilarityKey] ?: "90%")
                put("similarityEnabled", prefs[FlowNeuroSimilarityEnabledKey] ?: true)
                put("allowAlternatives", prefs[FlowNeuroAllowAlternativesKey] ?: false)
                put("excludeLiveRemixes", prefs[FlowNeuroExcludeLiveRemixesKey] ?: true)
                put("artistDiversity", prefs[FlowNeuroArtistDiversityKey] ?: "Equilibrada")
                put("diversityEnabled", prefs[FlowNeuroDiversityEnabledKey] ?: true)
                put("artistRotationEnabled", prefs[FlowNeuroArtistRotationEnabledKey] ?: true)
                put("whitelistEnabled", prefs[FlowNeuroWhitelistEnabledKey] ?: false)
                put("whitelist", prefs[FlowNeuroWhitelistKey] ?: "")
                put("avoidUnrelated", prefs[FlowNeuroAvoidUnrelatedKey] ?: true)
                put("confirmation", prefs[FlowNeuroConfirmationKey] ?: "Tras 60%")
                put("confirmationEnabled", prefs[FlowNeuroConfirmationEnabledKey] ?: true)
                put("continuity", prefs[FlowNeuroContinuityKey] ?: "FlowNeuro dominante")
                put("continuityEnabled", prefs[FlowNeuroContinuityEnabledKey] ?: true)
                put("network", prefs[FlowNeuroNetworkKey] ?: "Wi‑Fi + datos móviles")
                put("networkEnabled", prefs[FlowNeuroNetworkEnabledKey] ?: true)
                put("learningEnabled", prefs[FlowNeuroLearningEnabledKey] ?: true)
                put("transparencyEnabled", prefs[FlowNeuroTransparencyEnabledKey] ?: true)
                put("learningLevel", prefs[FlowNeuroLearningLevelKey] ?: 1)
                put("positiveSignals", prefs[FlowNeuroPositiveSignalsKey] ?: 0)
                put("negativeSignals", prefs[FlowNeuroNegativeSignalsKey] ?: 0)
                put("injectedCount", prefs[FlowNeuroInjectedCountKey] ?: 0)
                put("recentIds", prefs[FlowNeuroRecentIdsKey] ?: "")
                put("repeatWindow", prefs[FlowNeuroRepeatWindowKey] ?: "48 horas")
                put("repeatWindowEnabled", prefs[FlowNeuroRepeatWindowEnabledKey] ?: true)
                put("profileVersion", prefs[FlowNeuroProfileVersionKey] ?: 1)
                put("artistAffinities", prefs[FlowNeuroArtistAffinitiesKey] ?: "{}")
                put("albumAffinities", prefs[FlowNeuroAlbumAffinitiesKey] ?: "{}")
                put("timeAffinities", prefs[FlowNeuroTimeAffinitiesKey] ?: "{}")
                put("recommendationReasons", prefs[FlowNeuroRecommendationReasonsKey] ?: "")
                put("profileLastUpdated", prefs[FlowNeuroProfileLastUpdatedKey] ?: 0L)
                put("lastLearningDay", prefs[FlowNeuroLastLearningDayKey] ?: "")
                put("lastProfileMaintenanceDay", prefs[FlowNeuroLastProfileMaintenanceDayKey] ?: "")
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray()) }
                ?: error("No se pudo abrir el archivo de respaldo")
        }
    }

    suspend fun import(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                ?: error("No se pudo leer el archivo de respaldo")
            val json = JSONObject(root)
            require(json.optString("format") == FORMAT) { "El archivo no es un perfil FlowNeuro" }
            context.dataStore.edit { prefs ->
                prefs[FlowNeuroEnabledKey] = json.optBoolean("enabled", true)
                prefs[FlowNeuroExcludeQueueKey] = json.optBoolean("excludeQueue", true)
                prefs[FlowNeuroExcludeQueueKey] = json.optBoolean("excludeQueue", true)
                prefs[FlowNeuroSimilarityKey] = json.optString("similarity", "90%")
                prefs[FlowNeuroSimilarityEnabledKey] = json.optBoolean("similarityEnabled", true)
                prefs[FlowNeuroAllowAlternativesKey] = json.optBoolean("allowAlternatives", false)
                prefs[FlowNeuroExcludeLiveRemixesKey] = json.optBoolean("excludeLiveRemixes", true)
                prefs[FlowNeuroArtistDiversityKey] = json.optString("artistDiversity", "Equilibrada")
                prefs[FlowNeuroDiversityEnabledKey] = json.optBoolean("diversityEnabled", true)
                prefs[FlowNeuroArtistRotationEnabledKey] = json.optBoolean("artistRotationEnabled", true)
                prefs[FlowNeuroWhitelistEnabledKey] = json.optBoolean("whitelistEnabled", false)
                prefs[FlowNeuroWhitelistKey] = json.optString("whitelist", "")
                prefs[FlowNeuroAvoidUnrelatedKey] = json.optBoolean("avoidUnrelated", true)
                prefs[FlowNeuroConfirmationKey] = json.optString("confirmation", "Tras 60%")
                prefs[FlowNeuroConfirmationEnabledKey] = json.optBoolean("confirmationEnabled", true)
                prefs[FlowNeuroContinuityKey] = json.optString("continuity", "FlowNeuro dominante")
                prefs[FlowNeuroContinuityEnabledKey] = json.optBoolean("continuityEnabled", true)
                prefs[FlowNeuroNetworkKey] = json.optString("network", "Wi‑Fi + datos móviles")
                prefs[FlowNeuroNetworkEnabledKey] = json.optBoolean("networkEnabled", true)
                prefs[FlowNeuroLearningEnabledKey] = json.optBoolean("learningEnabled", true)
                prefs[FlowNeuroTransparencyEnabledKey] = json.optBoolean("transparencyEnabled", true)
                prefs[FlowNeuroLearningLevelKey] = json.optInt("learningLevel", 1).coerceIn(1, 100)
                prefs[FlowNeuroPositiveSignalsKey] = json.optInt("positiveSignals", 0).coerceAtLeast(0)
                prefs[FlowNeuroNegativeSignalsKey] = json.optInt("negativeSignals", 0).coerceAtLeast(0)
                prefs[FlowNeuroInjectedCountKey] = json.optInt("injectedCount", 0).coerceAtLeast(0)
                prefs[FlowNeuroRecentIdsKey] = json.optString("recentIds", "")
                prefs[FlowNeuroRepeatWindowKey] = json.optString("repeatWindow", "48 horas")
                prefs[FlowNeuroRepeatWindowEnabledKey] = json.optBoolean("repeatWindowEnabled", true)
                prefs[FlowNeuroProfileVersionKey] = json.optInt("profileVersion", 1)
                prefs[FlowNeuroArtistAffinitiesKey] = json.optString("artistAffinities", "{}")
                prefs[FlowNeuroAlbumAffinitiesKey] = json.optString("albumAffinities", "{}")
                prefs[FlowNeuroTimeAffinitiesKey] = json.optString("timeAffinities", "{}")
                prefs[FlowNeuroRecommendationReasonsKey] = json.optString("recommendationReasons", "")
                prefs[FlowNeuroProfileLastUpdatedKey] = json.optLong("profileLastUpdated", 0L)
                prefs[FlowNeuroLastLearningDayKey] = json.optString("lastLearningDay", "")
                prefs[FlowNeuroLastProfileMaintenanceDayKey] = json.optString("lastProfileMaintenanceDay", "")
            }
            Unit
        }
    }

    suspend fun clearLearning(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.dataStore.edit { prefs ->
                prefs.remove(FlowNeuroLearningLevelKey)
                prefs.remove(FlowNeuroPositiveSignalsKey)
                prefs.remove(FlowNeuroNegativeSignalsKey)
                prefs.remove(FlowNeuroInjectedCountKey)
                prefs.remove(FlowNeuroRecentIdsKey)
                prefs.remove(FlowNeuroRepeatWindowKey)
                prefs.remove(FlowNeuroProfileVersionKey)
                prefs.remove(FlowNeuroArtistAffinitiesKey)
                prefs.remove(FlowNeuroAlbumAffinitiesKey)
                prefs.remove(FlowNeuroTimeAffinitiesKey)
                prefs.remove(FlowNeuroRecommendationReasonsKey)
                prefs.remove(FlowNeuroProfileLastUpdatedKey)
                prefs.remove(FlowNeuroLastLearningDayKey)
                prefs.remove(FlowNeuroLastProfileMaintenanceDayKey)
            }
            Unit
        }
    }
}
