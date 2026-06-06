package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: MainViewModel,
    onNavigateToStudio: () -> Unit
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val transcriptionInput by viewModel.transcriptionInput.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedMood by viewModel.selectedMood.collectAsState()
    val selectedMusicIndex by viewModel.selectedMusicIndex.collectAsState()
    val isPlayingSound by viewModel.isPlayingSound.collectAsState()

    val generatedTitle by viewModel.generatedTitle.collectAsState()

    // Recording Permission handler
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasMicPermission = granted }
    )

    // Trigger studio screen once storyboard successfully generates a title
    LaunchedEffect(generatedTitle) {
        if (generatedTitle.isNotEmpty()) {
            onNavigateToStudio()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Loom Chamber",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = CosmicTextPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Waking thoughts stitched into cinema",
                    fontSize = 14.sp,
                    color = CosmicTextSecondary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Error Message Banner
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicGlowRed.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, "error", tint = CosmicGlowRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 13.sp,
                            color = CosmicGlowRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Section 1: The physical Voice Dictation terminal
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicMidnightCard.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(28.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = if (isRecording) listOf(CosmicSecondaryCyan, CosmicPrimaryPurple)
                                else listOf(CosmicPrimaryPurple.copy(0.15f), Color.Transparent)
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DREAM DICTATOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = CosmicSecondaryCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pulse dictation key button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .testTag("dictate_trigger_button")
                    ) {
                        if (isRecording) {
                            // Atmospheric throb ripples
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val outerScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.6f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "wave"
                            )
                            val outerAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(outerScale)
                                    .clip(CircleShape)
                                    .background(CosmicPrimaryPurple.copy(alpha = outerAlpha))
                            )
                        }

                        // Physical Core controller button
                        Surface(
                            onClick = {
                                if (isRecording) {
                                    viewModel.stopVoiceRecording()
                                } else {
                                    if (hasMicPermission) {
                                        viewModel.startVoiceRecording(context)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = if (isRecording) CosmicSecondaryCyan else CosmicMidnightSurface,
                            modifier = Modifier
                                .size(96.dp)
                                .shadow(16.dp, CircleShape),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(
                                    colors = listOf(CosmicPrimaryPurple, CosmicTertiaryPink)
                                )
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Voice recorder",
                                    tint = if (isRecording) CosmicDeepSpace else CosmicPrimaryPurple,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isRecording) "DICTATING SUB-CONSCIOUS... ${recordingDurationSec}s" else "Tap Orb to speak messy wake-up memories",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) CosmicSecondaryCyan else CosmicTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Section 2: Text Reviewer Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SUB-CONSCIOUS TRANSCRIPT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextMuted,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = transcriptionInput,
                    onValueChange = { viewModel.updateTranscriptionInput(it) },
                    placeholder = {
                        Text(
                            "Transcription results will flow here... or click inside to type your messy dream details naturally.",
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("transcription_editor"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CosmicMidnightCard,
                        unfocusedContainerColor = CosmicMidnightCard,
                        focusedTextColor = CosmicTextPrimary,
                        unfocusedTextColor = CosmicTextPrimary,
                        focusedBorderColor = CosmicPrimaryPurple,
                        unfocusedBorderColor = CosmicMidnightSurface
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Section 3: Visual Art style cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "GENERATIVE ART STYLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextMuted,
                    letterSpacing = 1.sp
                )

                val styles = listOf(
                    "Surrealist Salvador Dalí oil masterpiece",
                    "Cyberpunk neon future digital painting",
                    "Ghibli hand-drawn cozy anime aesthetic",
                    "Vaporwave fluid glass sculpting",
                    "Monochrome mystical ink sketch",
                    "Hyper-realistic epic detail cinematic VFX"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    styles.forEach { style ->
                        val active = selectedStyle == style
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) CosmicPrimaryPurple else CosmicMidnightCard)
                                .clickable { viewModel.setStyle(style) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("style_chip_$style")
                        ) {
                            Text(
                                text = style,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) CosmicDeepSpace else CosmicTextPrimary
                            )
                        }
                    }
                }
            }

            // Section 4: Atmospheric Synthesizer Tuning
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicMidnightCard.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(CosmicMidnightSurface, Color.Transparent)
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AMBIENT SYNTH SOUNDSCAPE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = CosmicTextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Oscillator Frequency Tuning",
                                fontSize = 12.sp,
                                color = CosmicSecondaryCyan
                            )
                        }

                        // Play/Pause test drone
                        IconButton(
                            onClick = {
                                if (isPlayingSound) {
                                    viewModel.stopAmbientSynth()
                                } else {
                                    viewModel.startAmbientSynth()
                                }
                            },
                            modifier = Modifier
                                .background(CosmicMidnightSurface, CircleShape)
                                .testTag("ambient_audio_toggle")
                        ) {
                            Icon(
                                imageVector = if (isPlayingSound) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Listen to ambient frequencies",
                                tint = if (isPlayingSound) CosmicTertiaryPink else CosmicTextPrimary
                            )
                        }
                    }

                    val moods = listOf(
                        "Mysterious" to 0,
                        "Calm" to 1,
                        "Terrifying" to 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        moods.forEach { (moodName, ind) ->
                            val active = selectedMood == moodName
                            Button(
                                onClick = { viewModel.setMood(moodName, ind) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) CosmicMidnightSurface else Color.Transparent
                                ),
                                border = if (active) ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(CosmicSecondaryCyan, CosmicPrimaryPurple)
                                    )
                                ) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mood_selector_$moodName")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = when (moodName) {
                                            "Calm" -> Icons.Default.Star
                                            "Terrifying" -> Icons.Default.Warning
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = moodName,
                                        tint = if (active) CosmicSecondaryCyan else CosmicTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = moodName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) CosmicTextPrimary else CosmicTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BIG ACTION: DISPATCH STORYBOARD WEAVER
            Button(
                onClick = { viewModel.generateDreamBlueprint() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .testTag("weave_blueprint_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimaryPurple),
                shape = RoundedCornerShape(24.dp),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CosmicDeepSpace,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "STITCHING STAR SPLINTERS...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = CosmicDeepSpace
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Weave",
                            tint = CosmicDeepSpace
                        )
                        Text(
                            text = "WEAVE STORYBOARD",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = CosmicDeepSpace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
