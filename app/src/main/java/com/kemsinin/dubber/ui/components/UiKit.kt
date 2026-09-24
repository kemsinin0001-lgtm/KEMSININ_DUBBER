package com.kemsinin.dubber.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kemsinin.dubber.ui.theme.CyanAccent
import com.kemsinin.dubber.ui.theme.DarkPanel
import com.kemsinin.dubber.ui.theme.DarkSurface
import com.kemsinin.dubber.ui.theme.DarkSurfaceHigh
import com.kemsinin.dubber.ui.theme.StrokeSoft
import com.kemsinin.dubber.ui.theme.TextMuted
import com.kemsinin.dubber.ui.theme.Violet
import com.kemsinin.dubber.ui.theme.VioletBright

val CardShape = RoundedCornerShape(20.dp)
val InnerShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(14.dp)

/** Outer panel used for every block of the studio, with a soft tinted glow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    glow: Color = Violet,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = CardShape,
                clip = false,
                ambientColor = glow.copy(alpha = 0.35f),
                spotColor = glow.copy(alpha = 0.35f),
            )
            .clip(CardShape)
            .background(DarkSurface)
            .border(1.dp, StrokeSoft, CardShape)
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

/** Darker inset panel (the video preview surface). */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(InnerShape)
            .background(DarkPanel)
            .border(1.dp, StrokeSoft.copy(alpha = 0.7f), InnerShape)
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

/** Rounded square that holds a section icon, like the play badge in the header. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color = CyanAccent,
    size: Dp = 34.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.linearGradient(listOf(Violet, VioletBright))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size / 1.9f),
        )
    }
}

@Composable
fun SectionTitle(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        }
    }
}

/** Small rounded label, e.g. "Douyin" or "4K No-Watermark". */
@Composable
fun PillBadge(
    text: String,
    container: Brush,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    fontSize: Int = 12,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Monospace timer pill used by the preview header. */
@Composable
fun TimerPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(DarkSurfaceHigh)
            .border(1.dp, StrokeSoft, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = CyanAccent,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Big gradient call-to-action (the "បកប្រែភាសា" button). */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    gradient: List<Color> = listOf(CyanAccent, Color(0xFF3B82F6)),
    contentColor: Color = Color(0xFF050912),
    height: Dp = 54.dp,
) {
    val colors = if (enabled) gradient else gradient.map { it.copy(alpha = 0.32f) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = if (enabled) 14.dp else 0.dp,
                shape = ButtonShape,
                clip = false,
                ambientColor = colors.first().copy(alpha = 0.5f),
                spotColor = colors.last().copy(alpha = 0.5f),
            )
            .clip(ButtonShape)
            .background(Brush.horizontalGradient(colors))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

/** Flat tinted button (import / export actions). */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = DarkSurfaceHigh,
    contentColor: Color = VioletBright,
    enabled: Boolean = true,
    borderColor: Color = StrokeSoft,
    height: Dp = 50.dp,
    fontSize: Int = 14,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(ButtonShape)
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .border(1.dp, borderColor, ButtonShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = text,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Small key/value chip used on the info rows. */
@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceHigh)
            .border(1.dp, StrokeSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Row of selectable chips; scrolls sideways when the labels are long. */
@Composable
fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSurfaceHigh,
                    labelColor = Color.White.copy(alpha = 0.8f),
                    selectedContainerColor = Violet,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

/** Labelled slider with a live value readout. */
@Composable
fun ValueSlider(
    label: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                color = CyanAccent,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = Violet,
                inactiveTrackColor = DarkSurfaceHigh,
            ),
        )
    }
}

/** A compact "label + value" line for the subtitle list. */
@Composable
fun TimeTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = CyanAccent,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
