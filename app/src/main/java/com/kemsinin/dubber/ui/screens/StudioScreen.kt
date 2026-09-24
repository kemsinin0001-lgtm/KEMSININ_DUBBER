package com.kemsinin.dubber.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kemsinin.dubber.R
import com.kemsinin.dubber.ui.DubberEngine
import com.kemsinin.dubber.ui.DubberState
import com.kemsinin.dubber.ui.MediaItem
import com.kemsinin.dubber.ui.components.AppCard
import com.kemsinin.dubber.ui.components.GradientButton
import com.kemsinin.dubber.ui.components.InnerShape
import com.kemsinin.dubber.ui.components.PanelCard
import com.kemsinin.dubber.ui.components.PillBadge
import com.kemsinin.dubber.ui.components.SectionTitle
import com.kemsinin.dubber.ui.components.SoftButton
import com.kemsinin.dubber.ui.components.TimerPill
import com.kemsinin.dubber.ui.components.rememberAudioPreview
import com.kemsinin.dubber.ui.formatClock
import com.kemsinin.dubber.ui.theme.CyanAccent
import com.kemsinin.dubber.ui.theme.DarkSurface
import com.kemsinin.dubber.ui.theme.DarkSurfaceHigh
import com.kemsinin.dubber.ui.theme.PinkHot
import com.kemsinin.dubber.ui.theme.Rose
import com.kemsinin.dubber.ui.theme.StrokeSoft
import com.kemsinin.dubber.ui.theme.TextMuted
import com.kemsinin.dubber.ui.theme.Violet
import com.kemsinin.dubber.ui.theme.VioletBright
import com.kemsinin.dubber.ui.theme.VioletDeep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StudioScreen(
    state: DubberState,
    scope: CoroutineScope,
    onImportVideo: () -> Unit,
    onImportFolder: () -> Unit,
    onSaveToPc: () -> Unit,
) {
    val context = LocalContext.current
    val preview = rememberAudioPreview()

    var elapsed by remember { mutableStateOf(0L) }

    LaunchedEffect(preview.isPlaying) {
        if (preview.isPlaying) {
            elapsed = 0L
            while (true) {
                delay(500L)
                elapsed += 500L
            }
        } else {
            elapsed = 0L
        }
    }

    val timerText = "%s / %s".format(
        formatClock(if (preview.isPlaying) elapsed else 0L),
        formatClock(state.durationMs),
    )

    AppCard(glow = CyanAccent) {
        SectionTitle(
            icon = Icons.Default.PlayArrow,
            title = stringResource(R.string.studio_preview),
            trailing = { TimerPill(text = timerText) },
        )
        Spacer(Modifier.height(12.dp))

        PanelCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(336.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.cover.isNotBlank()) {
                    AsyncImage(
                        model = state.cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(InnerShape)
                            .alpha(0.32f),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(InnerShape)
                        .background(DarkSurface.copy(alpha = 0.94f))
                        .border(1.dp, StrokeSoft, InnerShape)
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PillBadge(
                            text = stringResource(R.string.studio_badge_douyin),
                            container = Brush.horizontalGradient(listOf(Color(0xFF0B0F1A), Color(0xFF0B0F1A))),
                            contentColor = CyanAccent,
                            icon = Icons.Default.MusicNote,
                        )
                        Spacer(Modifier.width(8.dp))
                        PillBadge(
                            text = stringResource(R.string.studio_badge_quality),
                            container = Brush.horizontalGradient(listOf(PinkHot, Rose)),
                            contentColor = Color.White,
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = state.linkInput,
                            onValueChange = { state.linkInput = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.studio_link_hint),
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = StrokeSoft,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        CopyButton(onClick = { copyToClipboard(context, state.linkInput) })
                    }

                    Spacer(Modifier.height(12.dp))

                    GradientButton(
                        text = if (state.busy) {
                            stringResource(R.string.status_processing)
                        } else {
                            stringResource(R.string.studio_action_translate)
                        },
                        icon = Icons.Default.Bolt,
                        enabled = !state.busy,
                        onClick = {
                            preview.stop()
                            scope.launch { DubberEngine.runPipeline(state, context) }
                        },
                    )

                    Spacer(Modifier.height(10.dp))

                    SoftButton(
                        text = stringResource(R.string.studio_action_save_pc),
                        icon = Icons.Default.Monitor,
                        modifier = Modifier.fillMaxWidth(),
                        container = DarkSurfaceHigh,
                        contentColor = VioletBright,
                        onClick = onSaveToPc,
                    )

                    if (state.busy) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.status_processing),
                            color = CyanAccent,
                            fontSize = 12.sp,
                        )
                    }
                }

                if (state.cover.isBlank() && state.detail.isNotBlank() && !state.busy) {
                    Text(
                        text = state.detail,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    AppCard(glow = Violet) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SoftButton(
                text = stringResource(R.string.studio_action_import_video),
                icon = Icons.Default.VideoFile,
                modifier = Modifier.weight(1f),
                container = Violet,
                contentColor = Color.White,
                borderColor = VioletBright,
                onClick = onImportVideo,
            )
            SoftButton(
                text = stringResource(R.string.studio_action_import_folder),
                icon = Icons.Default.Folder,
                modifier = Modifier.weight(1f),
                container = VioletDeep,
                contentColor = VioletBright,
                borderColor = Violet,
                onClick = onImportFolder,
            )
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle(
            icon = Icons.Default.Movie,
            title = stringResource(R.string.studio_group),
            trailing = {
                Text(
                    text = stringResource(R.string.subtitle_lines, state.group.size),
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            },
        )

        Spacer(Modifier.height(10.dp))

        if (state.group.isEmpty()) {
            Text(
                text = stringResource(R.string.studio_group_empty),
                color = TextMuted,
                fontSize = 13.sp,
            )
        } else {
            state.group.forEach { item ->
                MediaRow(
                    item = item,
                    active = state.localVideoPath == item.path,
                    canPlay = state.artifactPath.isNotBlank() && !state.busy,
                    playing = preview.isPlaying,
                    onSelect = { state.select(item) },
                    onPlay = {
                        preview.toggle(state.artifactPath)?.let { state.note(it) }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CyanAccent.copy(alpha = 0.16f))
            .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = stringResource(R.string.studio_action_copy),
            tint = CyanAccent,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun MediaRow(
    item: MediaItem,
    active: Boolean,
    canPlay: Boolean,
    playing: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) DarkSurfaceHigh else DarkSurface)
            .border(
                width = 1.dp,
                color = if (active) CyanAccent.copy(alpha = 0.6f) else StrokeSoft,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onSelect)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(VioletDeep),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = VioletBright,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${formatClock(item.durationMs)} · " + stringResource(
                    if (item.remote) R.string.studio_source_douyin else R.string.studio_source_device
                ),
                color = TextMuted,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(if (playing) CyanAccent.copy(alpha = 0.25f) else DarkSurfaceHigh)
                .clickable(enabled = canPlay, onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.studio_action_play),
                tint = if (canPlay) CyanAccent else TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    if (text.isBlank()) return
    try {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("douyin", text))
    } catch (_: Throwable) {
        // Clipboard is best effort.
    }
}
