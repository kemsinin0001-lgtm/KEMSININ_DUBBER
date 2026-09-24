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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kemsinin.dubber.R
import com.kemsinin.dubber.ui.DubberEngine
import com.kemsinin.dubber.ui.DubberState
import com.kemsinin.dubber.ui.LANGUAGE_OPTIONS
import com.kemsinin.dubber.ui.Stage
import com.kemsinin.dubber.ui.components.AppCard
import com.kemsinin.dubber.ui.components.ChipRow
import com.kemsinin.dubber.ui.components.GradientButton
import com.kemsinin.dubber.ui.components.SectionTitle
import com.kemsinin.dubber.ui.components.SoftButton
import com.kemsinin.dubber.ui.theme.CyanAccent
import com.kemsinin.dubber.ui.theme.DarkSurface
import com.kemsinin.dubber.ui.theme.DarkSurfaceHigh
import com.kemsinin.dubber.ui.theme.Emerald
import com.kemsinin.dubber.ui.theme.StrokeSoft
import com.kemsinin.dubber.ui.theme.TextMuted
import com.kemsinin.dubber.ui.theme.Violet
import com.kemsinin.dubber.ui.theme.VioletBright
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EditScreen(state: DubberState, scope: CoroutineScope) {
    val context = LocalContext.current
    var engineInfo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        engineInfo = DubberEngine.describeEngine()
    }

    val languageLabels = LANGUAGE_OPTIONS.map { it.label }
    val selectedLanguage = LANGUAGE_OPTIONS.firstOrNull { it.value == state.targetLang }?.label
        ?: languageLabels.first()

    AppCard(glow = Violet) {
        SectionTitle(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.edit_api_title),
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = state.apiKey,
            onValueChange = { state.apiKey = it },
            label = { Text(stringResource(R.string.edit_api_key), fontSize = 12.sp) },
            placeholder = {
                Text(stringResource(R.string.edit_api_hint), color = TextMuted, fontSize = 12.sp)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet,
                unfocusedBorderColor = StrokeSoft,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )

        Spacer(Modifier.height(10.dp))

        SoftButton(
            text = stringResource(R.string.edit_test_key),
            icon = Icons.Default.VpnKey,
            modifier = Modifier.fillMaxWidth(),
            container = DarkSurfaceHigh,
            contentColor = CyanAccent,
            enabled = !state.busy,
            onClick = { scope.launch { DubberEngine.testApiKey(state) } },
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.edit_target_lang),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(8.dp))

        ChipRow(
            options = languageLabels,
            selected = selectedLanguage,
            onSelect = { label ->
                LANGUAGE_OPTIONS.firstOrNull { it.label == label }?.let { state.targetLang = it.value }
            },
        )

        Spacer(Modifier.height(16.dp))

        GradientButton(
            text = if (state.busy) {
                stringResource(R.string.status_processing)
            } else {
                stringResource(R.string.edit_run)
            },
            icon = Icons.Default.Mic,
            enabled = !state.busy,
            onClick = { scope.launch { DubberEngine.runPipeline(state, context) } },
        )
    }

    Spacer(Modifier.height(14.dp))

    AppCard(glow = CyanAccent) {
        SectionTitle(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.edit_pipeline),
        )

        Spacer(Modifier.height(12.dp))

        val stepIndex = when (state.stage) {
            Stage.IDLE, Stage.ERROR -> 0
            Stage.RESOLVING, Stage.DOWNLOADING -> 1
            Stage.EXTRACTING, Stage.TRANSCRIBING -> 2
            Stage.TRANSLATING -> 3
            Stage.SPEAKING -> 4
            Stage.DONE -> 6
        }

        StepRow(1, stringResource(R.string.step1_label), stepIndex >= 1, stepIndex > 1)
        StepRow(2, stringResource(R.string.step2_label), stepIndex >= 2, stepIndex > 2)
        StepRow(3, stringResource(R.string.step3_label), stepIndex >= 3, stepIndex > 3)
        StepRow(4, stringResource(R.string.step4_label), stepIndex >= 4, stepIndex > 4)

        Spacer(Modifier.height(12.dp))

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
                    text = stringResource(R.string.edit_engine),
                    color = TextMuted,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = engineInfo ?: stringResource(R.string.edit_engine_unknown),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    AppCard(glow = Violet) {
        SectionTitle(
            icon = Icons.Default.List,
            title = stringResource(R.string.edit_log),
            trailing = {
                Text(
                    text = "${state.log.size}",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            },
        )

        Spacer(Modifier.height(10.dp))

        if (state.log.isEmpty()) {
            Text(
                text = stringResource(R.string.status_idle),
                color = TextMuted,
                fontSize = 12.sp,
            )
        } else {
            state.log.forEach { line ->
                Text(
                    text = "• $line",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun StepRow(step: Int, title: String, active: Boolean, done: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = when {
                done -> Emerald
                active -> CyanAccent
                else -> TextMuted
            },
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = if (active || done) Color.White else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.height(8.dp))
}
