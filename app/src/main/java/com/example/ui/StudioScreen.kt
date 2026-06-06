package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DreamScene
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StudioScreen(
    viewModel: MainViewModel,
    onBackToLoom: () -> Unit,
    isViewingSavedDream: Boolean = false,
    savedTitle: String = "",
    savedNarrative: String = "",
    savedInterpretation: String = "",
    savedScenes: List<DreamScene> = emptyList(),
    savedMood: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Screen content source
    val title = if (isViewingSavedDream) savedTitle else viewModel.generatedTitle.collectAsState().value
    val narrative = if (isViewingSavedDream) savedNarrative else viewModel.generatedNarrative.collectAsState().value
    val scenes = if (isViewingSavedDream) savedScenes else viewModel.generatedScenes.collectAsState().value
    val interpretation = if (isViewingSavedDream) savedInterpretation else viewModel.generatedInterpretation.collectAsState().value
    val mood = if (isViewingSavedDream) savedMood else viewModel.selectedMood.collectAsState().value

    val isProcessing by viewModel.isProcessing.collectAsState()
    val isPlayingSound by viewModel.isPlayingSound.collectAsState()

    // Synthesis oscillator selection
    LaunchedEffect(key1 = true) {
        if (!isViewingSavedDream) {
            viewModel.startAmbientSynth()
        }
    }

    // TTS Voiceover setup
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isTtsSpeaking by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.ENGLISH
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
            viewModel.stopAmbientSynth()
        }
    }

    // Carousel controller state
    var currentSceneIdx by remember { mutableStateOf(0) }
    var isAudiobookPlaying by remember { mutableStateOf(false) }

    // TTS speaker helper
    val speakScript: (String) -> Unit = { text ->
        if (isTtsReady && tts != null) {
            isTtsSpeaking = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "story_voice")
        }
    }

    // Dynamic slideshow ticker
    LaunchedEffect(isAudiobookPlaying, currentSceneIdx) {
        if (isAudiobookPlaying && scenes.isNotEmpty()) {
            val scene = scenes[currentSceneIdx]
            speakScript(scene.scriptText)
            
            // Wait for 7 seconds, then transition to next scene
            delay(8000)
            if (currentSceneIdx < scenes.size - 1) {
                currentSceneIdx++
            } else {
                currentSceneIdx = 0
                isAudiobookPlaying = false
                isTtsSpeaking = false
            }
        }
    }

    // Ken burns zoom factor
    val zoomFactor by animateFloatAsState(
        targetValue = if (isAudiobookPlaying) 1.25f else 1.0f,
        animationSpec = tween(8000, easing = LinearOutSlowInEasing),
        label = "ken_burns_zoom"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        tts?.stop()
                        viewModel.stopAmbientSynth()
                        onBackToLoom()
                    },
                    modifier = Modifier.background(CosmicMidnightSurface, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "back", tint = CosmicTextPrimary)
                }

                Text(
                    text = "DREAM REEL PLAYER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = CosmicSecondaryCyan,
                    letterSpacing = 1.sp
                )

                // Sync status indicator
                Box(
                    modifier = Modifier
                        .background(
                            if (isPlayingSound) CosmicSecondaryCyan.copy(0.15f)
                            else CosmicMidnightSurface, CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SYNTH ${mood.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlayingSound) CosmicSecondaryCyan else CosmicTextSecondary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main title header
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = CosmicTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            // Cinematic Stage: Viewport
            if (scenes.isNotEmpty()) {
                val currentScene = scenes[currentSceneIdx]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, CosmicPrimaryPurple.copy(0.3f), RoundedCornerShape(28.dp))
                            .background(CosmicMidnightCard)
                            .testTag("cinematic_stage_viewport"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Soft scaling Ken Burns image loading
                        Base64OrProceduralView(
                            imageBase64 = currentScene.imageBase64,
                            zoomScale = zoomFactor,
                            prompt = currentScene.visualPrompt
                        )

                        // Top-left Scene label
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .background(CosmicDeepSpace.copy(0.7f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "SCENE ${currentScene.sceneNumber} OF ${scenes.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = CosmicSecondaryCyan
                            )
                        }

                        // Style indicator overlay on bottom right
                        if (currentScene.imageBase64 == null && !isProcessing) {
                            Button(
                                onClick = { viewModel.generateVisualForScene(currentSceneIdx) },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicDeepSpace.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(14.dp)
                                    .testTag("compile_visuals_btn_$currentSceneIdx")
                            ) {
                                Icon(Icons.Default.MovieFilter, "vfx", tint = CosmicPrimaryPurple, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("COMPILING PIXELS", fontSize = 10.sp, color = CosmicPrimaryPurple)
                            }
                        }
                    }

                    // Narration playback overlay
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CosmicMidnightCard),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "narration",
                                tint = if (isTtsSpeaking) CosmicSecondaryCyan else CosmicTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            AnimatedContent(
                                targetState = currentScene.scriptText,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(400)) with fadeOut(animationSpec = tween(400))
                                },
                                label = "narration_script"
                            ) { text ->
                                Text(
                                    text = text,
                                    fontSize = 14.sp,
                                    color = CosmicTextPrimary,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Controller button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Scene
                        IconButton(
                            onClick = {
                                if (currentSceneIdx > 0) {
                                    currentSceneIdx--
                                } else {
                                    currentSceneIdx = scenes.size - 1
                                }
                            },
                            modifier = Modifier.background(CosmicMidnightSurface, CircleShape)
                        ) {
                            Icon(Icons.Default.SkipPrevious, "prev", tint = CosmicTextPrimary)
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Unified Slideshow Play/Pause Button
                        Surface(
                            onClick = {
                                isAudiobookPlaying = !isAudiobookPlaying
                                if (!isAudiobookPlaying) {
                                    tts?.stop()
                                    isTtsSpeaking = false
                                }
                            },
                            shape = CircleShape,
                            color = if (isAudiobookPlaying) CosmicTertiaryPink else CosmicPrimaryPurple,
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(8.dp, CircleShape)
                                .testTag("reel_play_control")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isAudiobookPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "play-pause-show",
                                    tint = CosmicDeepSpace,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Next Scene
                        IconButton(
                            onClick = {
                                if (currentSceneIdx < scenes.size - 1) {
                                    currentSceneIdx++
                                } else {
                                    currentSceneIdx = 0
                                }
                            },
                            modifier = Modifier.background(CosmicMidnightSurface, CircleShape)
                        ) {
                            Icon(Icons.Default.SkipNext, "next", tint = CosmicTextPrimary)
                        }
                    }
                }
            }

            // Cinematic Full narrative text summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicMidnightCard.copy(0.4f)),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        colors = listOf(CosmicMidnightSurface, Color.Transparent)
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "THE JOURNEY NARRATIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondaryCyan,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = narrative,
                        fontSize = 13.sp,
                        color = CosmicTextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // Psychological Interpretation Drawer
            if (interpretation.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicMidnightCard.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness3,
                                contentDescription = "psych",
                                tint = CosmicPrimaryPurple
                            )
                            Text(
                                text = "AI PSYCHOLOGICAL DICTIONARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = interpretation,
                            fontSize = 13.sp,
                            color = CosmicTextSecondary,
                            lineHeight = 22.sp,
                            modifier = Modifier.testTag("dream_interpretation_text")
                        )
                    }
                }
            }

            // Interactive save mechanics
            if (!isViewingSavedDream) {
                Button(
                    onClick = {
                        viewModel.saveDreamToJournal()
                        onBackToLoom()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_dream_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSecondaryCyan),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Save, "save", tint = CosmicDeepSpace)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ARCHIVE IN JOURNAL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = CosmicDeepSpace
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// Custom decoding component loaded locally with fallback canvas drawing
@Composable
fun Base64OrProceduralView(
    imageBase64: String?,
    zoomScale: Float = 1.0f,
    prompt: String
) {
    if (imageBase64 != null) {
        // Base64 decode rendering blocks
        val bitmap = remember(imageBase64) {
            try {
                val clean = imageBase64.replace("\n", "").trim()
                val decoded = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "AI Dream scene",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(zoomScale)
            )
        } else {
            FractalDreamCanvas(prompt = prompt)
        }
    } else {
        // Standard interactive canvas display
        FractalDreamCanvas(prompt = prompt)
    }
}

// Procedural, gorgeous background animated canvas
@Composable
fun FractalDreamCanvas(prompt: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "fractal")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Render gorgeous, glowing cyber/mystic vector dream meshes dynamically!
            val center = this.center
            for (i in 1..4) {
                val radius = (width.coerceAtMost(height) / 5f) * i
                val strokeWidth = 2f + i
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CosmicPrimaryPurple.copy(alpha = 0.5f / i),
                            CosmicSecondaryCyan.copy(alpha = 0.2f / i)
                        ),
                        center = center,
                        radius = radius * (1.2f + (0.1f * Math.sin(Math.toRadians(waveOffset.toDouble() + i * 45)).toFloat()))
                    ),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        Text(
            text = prompt,
            fontSize = 11.sp,
            color = CosmicTextSecondary.copy(0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .background(CosmicDeepSpace.copy(0.6f), RoundedCornerShape(10.dp))
                .padding(8.dp)
        )
    }
}
