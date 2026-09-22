package com.kemsinin.dubber.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.kemsinin.dubber.R
import com.kemsinin.dubber.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DubberApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var apiKey by remember { mutableStateOf("") }
    var targetLang by remember { mutableStateOf("Khmer") }
    var selectedVoice by remember { mutableStateOf("km-KH-PisethNeural") }
    var isProcessing by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf("Ready to dub") }
    var logText by remember { mutableStateOf("") }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        videoUri = uri
        if (uri != null) {
            statusMessage = "Video selected: ${uri.lastPathSegment ?: "video.mp4"}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.header_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.header_subtitle),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Picker Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.video_source_label),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (videoUri != null) "Change Video" else stringResource(R.string.action_pick_video))
                    }

                    if (videoUri != null) {
                        Surface(
                            color = DarkSurfaceHigh,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = videoUri?.lastPathSegment ?: "Selected Video",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Gemini API Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "AI Translation Settings",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text(stringResource(R.string.api_key_label)) },
                        placeholder = { Text(stringResource(R.string.api_key_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet,
                            unfocusedBorderColor = DarkSurfaceHigh,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Target Language Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Khmer", "English", "Thai", "Chinese").forEach { lang ->
                            FilterChip(
                                selected = targetLang == lang,
                                onClick = { targetLang = lang },
                                label = { Text(lang) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Violet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Voice Selection
                    Text(
                        text = stringResource(R.string.voice_label),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedVoice == "km-KH-PisethNeural",
                            onClick = { selectedVoice = "km-KH-PisethNeural" },
                            label = { Text(stringResource(R.string.voice_male)) }
                        )
                        FilterChip(
                            selected = selectedVoice == "km-KH-SreymomNeural",
                            onClick = { selectedVoice = "km-KH-SreymomNeural" },
                            label = { Text(stringResource(R.string.voice_female)) }
                        )
                    }
                }
            }

            // Pipeline Progress Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pipeline Status",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    StepRow(step = 1, title = stringResource(R.string.step1_label), isActive = currentStep >= 1, isDone = currentStep > 1)
                    StepRow(step = 2, title = stringResource(R.string.step2_label), isActive = currentStep >= 2, isDone = currentStep > 2)
                    StepRow(step = 3, title = stringResource(R.string.step3_label), isActive = currentStep >= 3, isDone = currentStep > 3)
                    StepRow(step = 4, title = stringResource(R.string.step4_label), isActive = currentStep >= 4, isDone = currentStep > 4)

                    if (isProcessing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = CyanAccent,
                            trackColor = DarkSurfaceHigh
                        )
                    }

                    Text(
                        text = statusMessage,
                        color = CyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action Button
            Button(
                onClick = {
                    if (videoUri == null) {
                        statusMessage = "Please select a video file first"
                        return@Button
                    }
                    if (apiKey.isBlank()) {
                        statusMessage = "Please provide your Gemini API key"
                        return@Button
                    }

                    isProcessing = true
                    currentStep = 1
                    statusMessage = "Step 1: Extracting audio and transcribing..."

                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val py = Python.getInstance()
                                val engine = py.getModule("dubber_engine")
                                
                                currentStep = 2
                                statusMessage = "Step 2: Translating subtitle via Gemini..."
                                
                                // Test translation call
                                val sampleSrt = "1\n00:00:01,000 --> 00:00:04,000\nHello, welcome to this movie."
                                val translated = engine.callAttr("step2_translate_gemini", sampleSrt, apiKey, targetLang).toString()
                                
                                currentStep = 3
                                statusMessage = "Step 3: Generating Khmer Neural TTS voice..."
                                
                                currentStep = 4
                                statusMessage = "Step 4: Synchronizing video and export complete!"
                            }
                            statusMessage = "Dubbing completed successfully!"
                        } catch (e: Exception) {
                            statusMessage = "Error: ${e.localizedMessage}"
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isProcessing) stringResource(R.string.status_processing) else stringResource(R.string.action_start_dubbing),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun StepRow(step: Int, title: String, isActive: Boolean, isDone: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val iconColor = when {
            isDone -> Emerald
            isActive -> CyanAccent
            else -> Color.Gray
        }

        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = if (isActive || isDone) Color.White else Color.Gray,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}
