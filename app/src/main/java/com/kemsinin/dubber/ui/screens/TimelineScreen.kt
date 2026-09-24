package com.kemsinin.dubber.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kemsinin.dubber.R
import com.kemsinin.dubber.ui.DubberState
import com.kemsinin.dubber.ui.MediaItem
import com.kemsinin.dubber.ui.components.AppCard
import com.kemsinin.dubber.ui.components.SectionTitle
import com.kemsinin.dubber.ui.components.SoftButton
import com.kemsinin.dubber.ui.components.StatChip
import com.kemsinin.dubber.ui.components.TimeTag
import com.kemsinin.dubber.ui.components.rememberAudioPreview
import com.kemsinin.dubber.ui.formatClock
import com.kemsinin.dubber.ui.theme.CyanAccent
import com.kemsinin.dubber.ui.theme.DarkSurface
import com.kemsinin.dubber.ui.theme.DarkSurfaceHigh
import com.kemsinin.dubber.ui.theme.StrokeSoft
import com.kemsinin.dubber.ui.theme.TextMuted
import com.kemsinin.dubber.ui.theme.Violet
import com.kemsinin.dubber.ui.theme.VioletBright
import com.kemsinin.dubber.ui.theme.VioletDeep

@Composable
fun TimelineScreen(
    state: DubberState,
    onExportArtifact: () -> Unit,
    onExportSrt: () -> Unit,
) {
    val preview = rememberAudioPreview()
    val playable = state.artifactPath.isNotBlank() && state.artifactMime.startsWith("audio")

    AppCard(glow = CyanAccent) {
        SectionTitle(
            icon = Icons.Default.Timeline,
            title = stringResource(R.string.timeline_title),
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatChip(
                label = stringResource(R.string.timeline_total),
                value = formatClock(state.totalDurationMs),
                modifier = Modifier.weight(1f),
            )
            StatChip(
                label = stringResource(R.string.timeline_clips),
                value = "${state.group.size}",
                modifier = Modifier.weight(1f),
            )
            StatChip(
                label = stringResource(R.string.subtitle_title),
                value = "${state.cues.size}",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.timeline_arti),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, StrokeSoft, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.artifactName.ifBlank { stringResource(R.string.timeline_none) },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.artifactPath.ifBlank { "—" },
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (playable) CyanAccent.copy(alpha = 0.18f) else DarkSurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (preview.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.studio_action_play),
                    tint = if (playable) CyanAccent else TextMuted,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SoftButton(
            text = stringResource(R.string.timeline_export),
            icon = Icons.Default.Download,
            modifier = Modifier.fillMaxWidth(),
            container = Violet,
            contentColor = Color.White,
            borderColor = VioletBright,
            enabled = state.artifactPath.isNotBlank(),
            onClick = {
                preview.stop()
                onExportArtifact()
            },
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SoftButton(
                text = stringResource(R.string.timeline_export_srt),
                icon = Icons.Default.Save,
                modifier = Modifier.weight(1f),
                container = DarkSurfaceHigh,
                contentColor = VioletBright,
                enabled = state.cues.isNotEmpty(),
                fontSize = 13,
                onClick = onExportSrt,
            )
            SoftButton(
                text = stringResource(R.string.timeline_clear),
                icon = Icons.Default.Delete,
                modifier = Modifier.weight(1f),
                container = DarkSurfaceHigh,
                contentColor = TextMuted,
                fontSize = 13,
                onClick = {
                    preview.stop()
                    state.group.clear()
                    state.cues.clear()
                    state.originalCues.clear()
                    state.artifactPath = ""
                    state.artifactName = ""
                    state.localVideoPath = ""
                    state.localVideoName = ""
                    state.cover = ""
                    state.durationMs = 0L
                    state.reset()
                },
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    AppCard(glow = Violet) {
        SectionTitle(
            icon = Icons.Default.Timeline,
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
                text = stringResource(R.string.timeline_empty),
                color = TextMuted,
                fontSize = 13.sp,
            )
        } else {
            var offset = 0L
            state.group.forEachIndexed { position, item ->
                TimelineRow(position = position + 1, item = item, offsetMs = offset)
                offset += item.durationMs
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TimelineRow(position: Int, item: MediaItem, offsetMs: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, StrokeSoft, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(VioletDeep),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$position",
                color = VioletBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeTag(text = formatClock(offsetMs))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatClock(item.durationMs),
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
