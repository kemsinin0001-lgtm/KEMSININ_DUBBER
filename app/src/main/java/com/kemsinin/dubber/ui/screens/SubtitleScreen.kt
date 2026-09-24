package com.kemsinin.dubber.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kemsinin.dubber.R
import com.kemsinin.dubber.ui.Cue
import com.kemsinin.dubber.ui.DubberEngine
import com.kemsinin.dubber.ui.DubberState
import com.kemsinin.dubber.ui.Stage
import com.kemsinin.dubber.ui.components.AppCard
import com.kemsinin.dubber.ui.components.ChipRow
import com.kemsinin.dubber.ui.components.GradientButton
import com.kemsinin.dubber.ui.components.SectionTitle
import com.kemsinin.dubber.ui.components.SoftButton
import com.kemsinin.dubber.ui.components.TimeTag
import com.kemsinin.dubber.ui.formatClock
import com.kemsinin.dubber.ui.theme.CyanAccent
import com.kemsinin.dubber.ui.theme.DarkSurface
import com.kemsinin.dubber.ui.theme.DarkSurfaceHigh
import com.kemsinin.dubber.ui.theme.StrokeSoft
import com.kemsinin.dubber.ui.theme.TextMuted
import com.kemsinin.dubber.ui.theme.Violet
import com.kemsinin.dubber.ui.theme.VioletBright
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SubtitleScreen(
    state: DubberState,
    scope: CoroutineScope,
    onImportSrt: () -> Unit,
    onDictate: () -> Unit,
) {
    val context = LocalContext.current
    val originalLabel = stringResource(R.string.subtitle_show_original)
    val translatedLabel = stringResource(R.string.subtitle_show_translated)
    val listening = state.busy &&
        (state.stage == Stage.EXTRACTING || state.stage == Stage.TRANSCRIBING)

    AppCard(glow = CyanAccent) {
        SectionTitle(
            icon = Icons.Default.Subtitles,
            title = stringResource(R.string.subtitle_title),
            trailing = {
                Text(
                    text = stringResource(R.string.subtitle_lines, state.cues.size),
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
        )

        Spacer(Modifier.height(14.dp))

        GradientButton(
            text = if (listening) {
                stringResource(R.string.status_processing)
            } else {
                stringResource(R.string.subtitle_auto)
            },
            icon = Icons.Default.Mic,
            enabled = !state.busy,
            onClick = { scope.launch { DubberEngine.autoSubtitles(state, context) } },
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SoftButton(
                text = stringResource(R.string.subtitle_dictate),
                icon = Icons.Default.GraphicEq,
                modifier = Modifier.weight(1f),
                container = DarkSurfaceHigh,
                contentColor = CyanAccent,
                enabled = !state.busy,
                fontSize = 13,
                onClick = onDictate,
            )
            SoftButton(
                text = stringResource(R.string.subtitle_import),
                icon = Icons.Default.Upload,
                modifier = Modifier.weight(1f),
                container = DarkSurfaceHigh,
                contentColor = VioletBright,
                enabled = !state.busy,
                fontSize = 13,
                onClick = onImportSrt,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.subtitle_audio_hint),
            color = TextMuted,
            fontSize = 11.sp,
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = state.scriptText,
            onValueChange = { state.scriptText = it },
            placeholder = {
                Text(
                    text = stringResource(R.string.subtitle_script_hint),
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = StrokeSoft,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )

        Spacer(Modifier.height(10.dp))

        SoftButton(
            text = stringResource(R.string.subtitle_build),
            modifier = Modifier.fillMaxWidth(),
            container = Violet,
            contentColor = Color.White,
            borderColor = VioletBright,
            fontSize = 13,
            onClick = { state.cuesFromScript(state.scriptText) },
        )

        Spacer(Modifier.height(12.dp))

        GradientButton(
            text = stringResource(R.string.subtitle_translate),
            icon = Icons.Default.Bolt,
            enabled = !state.busy && state.cues.isNotEmpty(),
            onClick = { scope.launch { DubberEngine.translateSubtitles(state, context) } },
        )
    }

    Spacer(Modifier.height(14.dp))

    AppCard(glow = Violet) {
        ChipRow(
            options = listOf(originalLabel, translatedLabel),
            selected = if (state.showOriginal) originalLabel else translatedLabel,
            onSelect = { state.showOriginal = it == originalLabel },
        )

        Spacer(Modifier.height(12.dp))

        val cues: List<Cue> = if (state.showOriginal && state.originalCues.isNotEmpty()) {
            state.originalCues.toList()
        } else {
            state.cues.toList()
        }

        if (cues.isEmpty()) {
            Text(
                text = stringResource(R.string.subtitle_empty),
                color = TextMuted,
                fontSize = 13.sp,
            )
        } else {
            cues.forEach { cue ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, StrokeSoft, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.width(70.dp)) {
                        TimeTag(text = formatClock(cue.startMs))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "#${cue.index}",
                            color = TextMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = cue.text,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
