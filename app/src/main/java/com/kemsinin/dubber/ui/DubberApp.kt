package com.kemsinin.dubber.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kemsinin.dubber.R
import com.kemsinin.dubber.ui.screens.EditScreen
import com.kemsinin.dubber.ui.screens.StudioScreen
import com.kemsinin.dubber.ui.screens.SubtitleScreen
import com.kemsinin.dubber.ui.screens.TimelineScreen
import com.kemsinin.dubber.ui.screens.VoiceScreen
import com.kemsinin.dubber.ui.theme.Amber
import com.kemsinin.dubber.ui.theme.CyanAccent
import com.kemsinin.dubber.ui.theme.DarkBackground
import com.kemsinin.dubber.ui.theme.DarkSurface
import com.kemsinin.dubber.ui.theme.DarkSurfaceHigh
import com.kemsinin.dubber.ui.theme.GreenOK
import com.kemsinin.dubber.ui.theme.OrangeFlame
import com.kemsinin.dubber.ui.theme.Rose
import com.kemsinin.dubber.ui.theme.TextMuted
import com.kemsinin.dubber.ui.theme.VioletBright
import com.kemsinin.dubber.ui.theme.VioletDeep
import kotlinx.coroutines.launch

private data class NavItem(val labelRes: Int, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem(R.string.tab_studio, Icons.Default.Layers),
    NavItem(R.string.tab_voice, Icons.Default.Mic),
    NavItem(R.string.tab_text, Icons.Default.ClosedCaption),
    NavItem(R.string.tab_edit, Icons.Default.AutoAwesome),
    NavItem(R.string.tab_timeline, Icons.Default.VideoLibrary),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DubberApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { DubberState() }

    var exportSrtOnly by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            persistPermission(context, uri)
            scope.launch { DubberEngine.importVideo(state, context, uri) }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            persistPermission(context, uri)
            scope.launch { DubberEngine.importFolder(state, context, uri) }
        }
    }

    val srtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { DubberEngine.importSrt(state, context, uri) }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                if (exportSrtOnly) {
                    DubberEngine.exportSrt(state, context, uri)
                } else {
                    DubberEngine.exportArtifact(state, context, uri)
                }
            }
        }
    }

    fun exportArtifact() {
        exportSrtOnly = false
        exportPicker.launch(state.artifactName.ifBlank { "phorn-dubber-export" })
    }

    fun exportSrt() {
        exportSrtOnly = true
        exportPicker.launch(state.artifactName.ifBlank { "phorn-dubber" } + ".srt")
    }

    // On-device dictation: the system speech recognizer turns speech into cues.
    val dictateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = spoken?.firstOrNull().orEmpty().trim()
            if (text.isEmpty()) {
                state.fail("មិនបានឮសំឡេងអ្វីទេ")
            } else {
                state.appendCues(text)
                state.note("បានបន្ថែមអក្សររត់ដោយសំឡេង (${state.cues.size} បន្ទាត់)")
            }
        }
    }

    fun startDictation() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.subtitle_dictate))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            dictateLauncher.launch(intent)
        } catch (_: Throwable) {
            state.fail("ទូរស័ព្ទនេះមិនមានកម្មវិធីស្តាប់សំឡេងទេ")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark()
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.header_title),
                            color = VioletBright,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.header_trial),
                            color = Amber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                },
                actions = { QualityBadge() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 0.dp,
            ) {
                NAV_ITEMS.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = state.tab == index,
                        onClick = { state.tab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(item.labelRes),
                                modifier = Modifier.size(21.dp),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(item.labelRes),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanAccent,
                            selectedTextColor = CyanAccent,
                            indicatorColor = VioletDeep,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                        ),
                    )
                }
            }
        },
        containerColor = DarkBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            StatusStrip(state)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                when (state.tab) {
                    0 -> StudioScreen(
                        state = state,
                        scope = scope,
                        onImportVideo = { videoPicker.launch(arrayOf("video/*")) },
                        onImportFolder = { folderPicker.launch(null) },
                        onSaveToPc = { exportArtifact() },
                    )

                    1 -> VoiceScreen(state = state, scope = scope)

                    2 -> SubtitleScreen(
                        state = state,
                        scope = scope,
                        onImportSrt = { srtPicker.launch(arrayOf("text/*", "application/x-subrip", "*/*")) },
                        onDictate = { startDictation() },
                    )

                    3 -> EditScreen(state = state, scope = scope)

                    else -> TimelineScreen(
                        state = state,
                        onExportArtifact = { exportArtifact() },
                        onExportSrt = { exportSrt() },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(OrangeFlame, Rose)))
            .padding(2.dp)
            .clip(CircleShape)
            .background(DarkBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "P",
            color = OrangeFlame,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun QualityBadge() {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(36.dp)
            .clip(CircleShape)
            .border(2.dp, GreenOK, CircleShape)
            .background(DarkSurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.header_badge),
            color = GreenOK,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun StatusStrip(state: DubberState) {
    val accent = when (state.stage) {
        Stage.ERROR -> Rose
        Stage.DONE -> GreenOK
        Stage.IDLE -> TextMuted
        else -> CyanAccent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(stageLabelRes(state.stage)),
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        if (state.detail.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.detail,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun stageLabelRes(stage: Stage): Int = when (stage) {
    Stage.IDLE -> R.string.status_idle
    Stage.RESOLVING -> R.string.status_resolving
    Stage.EXTRACTING -> R.string.status_extracting
    Stage.TRANSCRIBING -> R.string.status_transcribing
    Stage.DOWNLOADING -> R.string.status_downloading
    Stage.TRANSLATING -> R.string.status_translating
    Stage.SPEAKING -> R.string.status_speaking
    Stage.DONE -> R.string.status_done
    Stage.ERROR -> R.string.status_error
}

private fun persistPermission(context: android.content.Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    } catch (_: Throwable) {
        // Not all providers grant persistable permissions.
    }
}
