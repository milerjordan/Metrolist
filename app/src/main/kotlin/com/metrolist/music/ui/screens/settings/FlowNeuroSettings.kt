package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import android.widget.Toast
import kotlinx.coroutines.launch
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.FlowNeuroAllowAlternativesKey
import com.metrolist.music.constants.FlowNeuroAvoidUnrelatedKey
import com.metrolist.music.constants.FlowNeuroArtistDiversityKey
import com.metrolist.music.constants.FlowNeuroDiversityEnabledKey
import com.metrolist.music.constants.FlowNeuroArtistRotationEnabledKey
import com.metrolist.music.constants.FlowNeuroConfirmationKey
import com.metrolist.music.constants.FlowNeuroConfirmationEnabledKey
import com.metrolist.music.constants.FlowNeuroContinuityKey
import com.metrolist.music.constants.FlowNeuroContinuityEnabledKey
import com.metrolist.music.constants.FlowNeuroEnabledKey
import com.metrolist.music.constants.FlowNeuroExcludeQueueKey
import com.metrolist.music.constants.FlowNeuroExcludeLiveRemixesKey
import com.metrolist.music.constants.FlowNeuroLearningLevelKey
import com.metrolist.music.constants.FlowNeuroLearningEnabledKey
import com.metrolist.music.constants.FlowNeuroNegativeSignalsKey
import com.metrolist.music.constants.FlowNeuroInjectedCountKey
import com.metrolist.music.constants.FlowNeuroProfileVersionKey
import com.metrolist.music.constants.FlowNeuroRecommendationReasonsKey
import com.metrolist.music.constants.FlowNeuroRepeatWindowKey
import com.metrolist.music.constants.FlowNeuroRepeatWindowEnabledKey
import com.metrolist.music.constants.FlowNeuroNetworkKey
import com.metrolist.music.constants.FlowNeuroNetworkEnabledKey
import com.metrolist.music.constants.FlowNeuroPositiveSignalsKey
import com.metrolist.music.constants.FlowNeuroSimilarityKey
import com.metrolist.music.constants.FlowNeuroSimilarityEnabledKey
import com.metrolist.music.constants.FlowNeuroTransparencyEnabledKey
import com.metrolist.music.constants.FlowNeuroWhitelistEnabledKey
import com.metrolist.music.constants.FlowNeuroWhitelistKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.flowneuro.FlowNeuroBackup
import com.metrolist.music.utils.rememberPreference

private val similarityOptions = listOf("90%", "80%", "70%", "60%")
private val diversityOptions = listOf("Sin límite", "Equilibrada", "Alta")
private val confirmationOptions = listOf("Inmediata", "Tras 60%", "Tras 80%")
private val continuityOptions = listOf("FlowNeuro dominante", "Conservar mezcla")
private val networkOptions = listOf("Wi‑Fi + datos móviles", "Solo Wi‑Fi")
private val repeatWindowOptions = listOf("24 horas", "48 horas", "7 días", "Siempre")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowNeuroSettings(navController: NavController) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            FlowNeuroBackup.export(context, uri).onSuccess {
                Toast.makeText(context, "Perfil FlowNeuro respaldado", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "No se pudo crear el respaldo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            FlowNeuroBackup.import(context, uri).onSuccess {
                Toast.makeText(context, "Perfil FlowNeuro restaurado", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Respaldo FlowNeuro no válido", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val (enabled, setEnabled) = rememberPreference(FlowNeuroEnabledKey, true)
    val (excludeQueue, setExcludeQueue) = rememberPreference(FlowNeuroExcludeQueueKey, true)
    val (similarity, setSimilarity) = rememberPreference(FlowNeuroSimilarityKey, "90%")
    val (similarityEnabled, setSimilarityEnabled) = rememberPreference(FlowNeuroSimilarityEnabledKey, true)
    val (allowAlternatives, setAllowAlternatives) = rememberPreference(FlowNeuroAllowAlternativesKey, false)
    val (excludeLiveRemixes, setExcludeLiveRemixes) = rememberPreference(FlowNeuroExcludeLiveRemixesKey, true)
    val (diversity, setDiversity) = rememberPreference(FlowNeuroArtistDiversityKey, "Equilibrada")
    val (diversityEnabled, setDiversityEnabled) = rememberPreference(FlowNeuroDiversityEnabledKey, true)
    val (artistRotationEnabled, setArtistRotationEnabled) = rememberPreference(FlowNeuroArtistRotationEnabledKey, true)
    val (whitelistEnabled, setWhitelistEnabled) = rememberPreference(FlowNeuroWhitelistEnabledKey, false)
    val (whitelist, setWhitelist) = rememberPreference(FlowNeuroWhitelistKey, "")
    val (avoidUnrelated, setAvoidUnrelated) = rememberPreference(FlowNeuroAvoidUnrelatedKey, true)
    val (confirmation, setConfirmation) = rememberPreference(FlowNeuroConfirmationKey, "Tras 60%")
    val (confirmationEnabled, setConfirmationEnabled) = rememberPreference(FlowNeuroConfirmationEnabledKey, true)
    val (continuity, setContinuity) = rememberPreference(FlowNeuroContinuityKey, "FlowNeuro dominante")
    val (continuityEnabled, setContinuityEnabled) = rememberPreference(FlowNeuroContinuityEnabledKey, true)
    val (network, setNetwork) = rememberPreference(FlowNeuroNetworkKey, "Wi‑Fi + datos móviles")
    val (networkEnabled, setNetworkEnabled) = rememberPreference(FlowNeuroNetworkEnabledKey, true)
    val (repeatWindow, setRepeatWindow) = rememberPreference(FlowNeuroRepeatWindowKey, "48 horas")
    val (repeatWindowEnabled, setRepeatWindowEnabled) = rememberPreference(FlowNeuroRepeatWindowEnabledKey, true)
    val (learningEnabled, setLearningEnabled) = rememberPreference(FlowNeuroLearningEnabledKey, true)
    val (transparencyEnabled, setTransparencyEnabled) = rememberPreference(FlowNeuroTransparencyEnabledKey, true)
    val learningLevel by rememberPreference(FlowNeuroLearningLevelKey, 1)
    val positiveSignals by rememberPreference(FlowNeuroPositiveSignalsKey, 0)
    val negativeSignals by rememberPreference(FlowNeuroNegativeSignalsKey, 0)
    val injectedCount by rememberPreference(FlowNeuroInjectedCountKey, 0)
    val profileVersion by rememberPreference(FlowNeuroProfileVersionKey, 1)
    val lastReason by rememberPreference(FlowNeuroRecommendationReasonsKey, "Sin señales todavía")
    var dialog by remember { mutableStateOf<Selection?>(null) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Material3SettingsGroup(
            title = "FlowNeuro",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text("Activar FlowNeuro") },
                    description = { Text("Añade recomendaciones locales después de la pista actual") },
                    trailingContent = { Switch(checked = enabled, onCheckedChange = setEnabled) },
                    onClick = { setEnabled(!enabled) },
                ),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Aprendizaje continuo",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.backup),
                    title = { Text("Respaldar aprendizaje") },
                    description = { Text("Guarda preferencias y progreso en un archivo local") },
                    onClick = { exportLauncher.launch("flowneuro-profile.json") },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.restore),
                    title = { Text("Restaurar aprendizaje") },
                    description = { Text("Importa el perfil después de cambiar de teléfono") },
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.delete),
                    title = { Text("Borrar aprendizaje") },
                    description = { Text("Elimina señales, historial FLOW y nivel local") },
                    onClick = {
                        scope.launch {
                            FlowNeuroBackup.clearLearning(context).onSuccess {
                                Toast.makeText(context, "Aprendizaje FlowNeuro borrado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                ),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Estado de aprendizaje",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text("Nivel $learningLevel/100") },
                    description = { Text("$positiveSignals positivas · $negativeSignals saltos · $injectedCount inyecciones FLOW · perfil v$profileVersion") },
                    trailingContent = { Switch(checked = learningEnabled, onCheckedChange = setLearningEnabled) },
                    onClick = { setLearningEnabled(!learningEnabled) },
                ),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Transparencia",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text(if (transparencyEnabled) "Última señal: $lastReason" else "Transparencia desactivada") },
                    description = { Text(if (transparencyEnabled) "Combina afinidad de artista y álbum, contexto horario, duración y diversidad. La etiqueta FLOW identifica cada inyección." else "Activa el interruptor para mostrar la explicación del aprendizaje.") },
                    trailingContent = { Switch(checked = transparencyEnabled, onCheckedChange = setTransparencyEnabled) },
                    onClick = { setTransparencyEnabled(!transparencyEnabled) },
                ),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Afinidad y versiones",
            items = listOf(
                optionToggleItem("Similitud mínima", "Exige una relación fuerte antes de inyectar", similarity, similarityEnabled, setSimilarityEnabled) { dialog = Selection.Similarity },
                toggleItem("Permitir versiones alternativas", "Permite remixes, directos, acústicas y covers", allowAlternatives, setAllowAlternatives),
                toggleItem("Excluir directos y remixes", "Bloquea Live, En Vivo y Remix", excludeLiveRemixes, setExcludeLiveRemixes),
                optionToggleItem("Diversidad de artistas", "Controla cuántas veces se repite un artista", diversity, diversityEnabled, setDiversityEnabled) { dialog = Selection.Diversity },
                toggleItem("Rotación de artistas y álbumes", "Evita repetir artistas de las últimas 20 inyecciones FLOW; los dúos cuentan con todos sus participantes", artistRotationEnabled, setArtistRotationEnabled),
                toggleItem("No usar canciones de la cola original", "Busca una canción FLOW independiente y excluye las canciones que ya están en la cola de Metrolist/YouTube Music", excludeQueue, setExcludeQueue),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Preferencias personales",
            items = listOf(
                toggleItem("Lista blanca de artistas", "Limita las inyecciones a tu lista personal", whitelistEnabled, setWhitelistEnabled),
            ),
        )
        OutlinedTextField(
            value = whitelist,
            onValueChange = { value -> setWhitelist(value.split(Regex("[,;\\n]")).take(20000).joinToString("\\n")) },
            enabled = whitelistEnabled,
            label = { Text("Artistas permitidos") },
            supportingText = { Text("Un artista por línea, coma o punto y coma · máximo 20.000") },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            minLines = 3,
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Escucha y continuidad",
            items = listOf(
                optionToggleItem("Confirmación de escucha", "Cuándo cuenta como señal positiva", confirmation, confirmationEnabled, setConfirmationEnabled) { dialog = Selection.Confirmation },
                optionToggleItem("Continuidad de cola", "Cómo se encadenan las inyecciones", continuity, continuityEnabled, setContinuityEnabled) { dialog = Selection.Continuity },
                optionToggleItem("Uso de red para recomendaciones", "Origen de los datos de afinidad", network, networkEnabled, setNetworkEnabled) { dialog = Selection.Network },
                optionToggleItem("No repetir canciones durante", "Evita una canción FLOW ya inyectada dentro de este periodo", repeatWindow, repeatWindowEnabled, setRepeatWindowEnabled) { dialog = Selection.RepeatWindow },
            ),
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Cómo funciona",
            items = listOf(
                toggleItem(
                    title = "Evita música sin relación",
                    description = "Exige la afinidad configurada, excluye duplicados, bloquea versiones no permitidas y limita artistas repetidos por lote.",
                    checked = avoidUnrelated,
                    onCheckedChange = setAvoidUnrelated,
                ),
            ),
        )
        Spacer(Modifier.height(16.dp))
    }
    TopAppBar(
        title = { Text("FlowNeuro") },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                androidx.compose.material3.Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )

    dialog?.let { selection ->
        val values = when (selection) {
            Selection.Similarity -> similarityOptions
            Selection.Diversity -> diversityOptions
            Selection.Confirmation -> confirmationOptions
            Selection.Continuity -> continuityOptions
            Selection.Network -> networkOptions
            Selection.RepeatWindow -> repeatWindowOptions
        }
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(selection.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    values.forEach { value ->
                        TextButton(
                            onClick = {
                                when (selection) {
                                    Selection.Similarity -> setSimilarity(value)
                                    Selection.Diversity -> setDiversity(value)
                                    Selection.Confirmation -> setConfirmation(value)
                                    Selection.Continuity -> setContinuity(value)
                                    Selection.Network -> setNetwork(value)
                                    Selection.RepeatWindow -> setRepeatWindow(value)
                                }
                                dialog = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(value, color = if (value == selection.current(similarity, diversity, confirmation, continuity, network, repeatWindow)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

private enum class Selection(val title: String) {
    Similarity("Similitud mínima"), Diversity("Diversidad de artistas"), Confirmation("Confirmación de escucha"), Continuity("Continuidad de cola"), Network("Uso de red"), RepeatWindow("No repetir canciones durante")
}

private fun Selection.current(similarity: String, diversity: String, confirmation: String, continuity: String, network: String, repeatWindow: String) = when (this) {
    Selection.Similarity -> similarity
    Selection.Diversity -> diversity
    Selection.Confirmation -> confirmation
    Selection.Continuity -> continuity
    Selection.Network -> network
    Selection.RepeatWindow -> repeatWindow
}

@Composable
private fun toggleItem(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) = Material3SettingsItem(
    icon = painterResource(R.drawable.tune), title = { Text(title) }, description = { Text(description) },
    trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }, onClick = { onCheckedChange(!checked) },
)

@Composable
private fun optionItem(title: String, description: String, value: String, onClick: () -> Unit) = Material3SettingsItem(
    icon = painterResource(R.drawable.tune),
    title = { Text(title) },
    description = { Text("$description\nToca para cambiar") },
    trailingContent = { Text(value, color = MaterialTheme.colorScheme.primary) },
    onClick = onClick,
)

@Composable
private fun optionToggleItem(
    title: String,
    description: String,
    value: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onValueClick: () -> Unit,
) = Material3SettingsItem(
    icon = painterResource(R.drawable.tune),
    title = { Text(title) },
    description = { Text("$description\nValor: $value · toca el valor para cambiarlo") },
    trailingContent = {
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    },
    onClick = onValueClick,
)
