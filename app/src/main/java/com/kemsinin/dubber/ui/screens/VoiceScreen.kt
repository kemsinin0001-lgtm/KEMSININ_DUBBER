package com.kemsinin.dubber.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kemsinin.dubber.R
import com.kemsinin.dubber.ui.DubberEngine
import com.kemsinin.dubber.ui.DubberState
import com.kemsinin.dubber.ui.VOICE_OPTIONS
import com.kemsinin.dubber.ui.VoiceOption
import com.kemsinin.dubber.ui.components.AppCard
import com.kemsinin.dubber.ui.components.GradientButton
import com.kemsinin.dubber.ui.components.SectionTitle
import com.kemsinin.dubber.ui.components.SoftButton
import com.kemsinin.dubber.ui.components.ValueSlider
import com.kemsinin.dubber.ui.components.rememberAudioPreview
import com.kemsinin.dubber.ui.hertzArg
import com.kemsinin.dubber.ui.percentArg
import com.kemsinin.dubber.ui.theme.CyanAccent
import com.kemsinin.dubber.ui.theme.DarkSurfaceHigh
import com.kemsinin.dubber.ui.theme.StrokeSoft
import com.kemsinin.dubber.ui.theme.TextMuted
import com.kemsinin.dubber.ui.theme.Violet
import com.kemsinin.dubber.ui.theme.VioletBright
import com.kemsinin.dubber.ui.theme.VioletDeep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun VoiceScreen(state: DubberState, scope: CoroutineScope) {
    val context = LocalContext.current
    val preview = rememberAudioPreview()
    val sample = stringResource(R.string.voice_sample)

    AppCard(glow = Violet) {
        SectionTitle(
            icon = Icons.Default.GraphicEq,
            title = stringResource(R.string.voice_title),
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.voice_choose),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(8.dp))

        VOICE_OPTIONS.forEach { option ->
            VoiceRow(
                option = option,
                selected = state.voice == option.value,
                onSelect = { state.voice = option.value },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        ValueSlider(
            label = stringResource(R.string.voice_speed),
            value = state.ratePercent,
            valueLabel = percentArg(state.ratePercent),
            range = -50f..100f,
            onValueChange = { state.ratePercent = it },
        )
        ValueSlider(
            label = stringResource(R.string.voice_pitch),
            value = state.pitchHz,
            valueLabel = hertzArg(state.pitchHz),
            range = -50f..50f,
            onValueChange = { state.pitchHz = it },
        )
        ValueSlider(
            label = stringResource(R.string.voice_volume),
            value = state.volumePercent,
            valueLabel = percentArg(state.volumePercent),
            range = -100f..100f,
            onValueChange = { state.volumePercent = it },
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SoftButton(
                text = stringResource(R.string.voice_preview),
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.weight(1f),
                container = DarkSurfaceHigh,
                contentColor = CyanAccent,
                enabled = !state.busy,
                onClick = {
                    scope.launch {
                        DubberEngine.previewVoice(state, context, sample)
                        if (!state.failed) {
                            preview.toggle(state.artifactPath)?.let { state.note(it) }
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(10.dp))

        GradientButton(
            text = stringResource(R.string.voice_generate),
            icon = Icons.Default.Mic,
            enabled = !state.busy && state.cues.isNotEmpty(),
            onClick = { scope.launch { DubberEngine.generateVoiceover(state, context) } },
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.voice_hint),
            color = TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun VoiceRow(option: VoiceOption, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) VioletDeep else DarkSurfaceHigh)
            .border(
                width = 1.dp,
                color = if (selected) VioletBright else StrokeSoft,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = option.value,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = option.label,
            color = if (selected) CyanAccent else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = VioletBright,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
