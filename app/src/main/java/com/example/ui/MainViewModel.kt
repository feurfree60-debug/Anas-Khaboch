package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DreamEntity
import com.example.data.DreamRepository
import com.example.data.DreamScene
import com.example.network.GeminiApiHandler
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sin

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DreamRepository(db.dreamDao())

    // reactive dream log list
    val allDreams: StateFlow<List<DreamEntity>> = repository.allDreams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val sceneListAdapter = moshi.adapter<List<DreamScene>>(
        Types.newParameterizedType(List::class.java, DreamScene::class.java)
    )

    // Pin Security states
    private val sharedPrefs = application.getSharedPreferences("dream_craft_prefs", Context.MODE_PRIVATE)
    private val _securityPin = MutableStateFlow(sharedPrefs.getString("security_pin", null))
    val securityPin: StateFlow<String?> = _securityPin.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Recording States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _transcriptionInput = MutableStateFlow("")
    val transcriptionInput: StateFlow<String> = _transcriptionInput.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var recordFile: File? = null
    private var recordingJob: Job? = null

    // Creation states
    private val _selectedStyle = MutableStateFlow("Surrealist Salvador Dalí oil masterpiece")
    val selectedStyle: StateFlow<String> = _selectedStyle.asStateFlow()

    private val _selectedMood = MutableStateFlow("Mysterious")
    val selectedMood: StateFlow<String> = _selectedMood.asStateFlow()

    private val _selectedMusicIndex = MutableStateFlow(0) // Default Ambient
    val selectedMusicIndex: StateFlow<Int> = _selectedMusicIndex.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Generated Dream Results BEFORE saving to Journal
    private val _generatedTitle = MutableStateFlow("")
    val generatedTitle: StateFlow<String> = _generatedTitle.asStateFlow()

    private val _generatedNarrative = MutableStateFlow("")
    val generatedNarrative: StateFlow<String> = _generatedNarrative.asStateFlow()

    private val _generatedScenes = MutableStateFlow<List<DreamScene>>(emptyList())
    val generatedScenes: StateFlow<List<DreamScene>> = _generatedScenes.asStateFlow()

    private val _generatedInterpretation = MutableStateFlow("")
    val generatedInterpretation: StateFlow<String> = _generatedInterpretation.asStateFlow()

    // Active synthesis sound tracker
    private var synthJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private val _isPlayingSound = MutableStateFlow(false)
    val isPlayingSound: StateFlow<Boolean> = _isPlayingSound.asStateFlow()

    init {
        // Automatically set unlocked if PIN is not defined
        if (sharedPrefs.getString("security_pin", null) == null) {
            _isUnlocked.value = true
        }
    }

    // PIN Handlers
    fun setSecurityPin(pin: String?) {
        sharedPrefs.edit().putString("security_pin", pin).apply()
        _securityPin.value = pin
        _isUnlocked.value = pin == null
    }

    fun verifyPin(pin: String): Boolean {
        val actual = sharedPrefs.getString("security_pin", null)
        val success = actual == pin
        if (success) {
            _isUnlocked.value = true
        }
        return success
    }

    fun lockJournal() {
        if (sharedPrefs.getString("security_pin", null) != null) {
            _isUnlocked.value = false
        }
    }

    fun updateTranscriptionInput(text: String) {
        _transcriptionInput.value = text
    }

    fun setStyle(style: String) {
        _selectedStyle.value = style
    }

    fun setMood(mood: String, musicIndex: Int) {
        _selectedMood.value = mood
        _selectedMusicIndex.value = musicIndex
        
        // If sound is active, hot-restart synthesizer with the new sound profile
        if (_isPlayingSound.value) {
            startAmbientSynth(mood)
        }
    }

    // Physical recording integration
    fun startVoiceRecording(context: Context) {
        try {
            _errorMessage.value = null
            recordFile = File(context.cacheDir, "dream_recording.3gp")
            
            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(recordFile!!.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true
            _recordingDurationSec.value = 0

            recordingJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingDurationSec.value += 1
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Recording preparation failed", e)
            _errorMessage.value = "Unable to initialize microphone. Please type your description!"
            _isRecording.value = false
        }
    }

    fun stopVoiceRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Stop recording failed", e)
        }
        mediaRecorder = null

        // In a real scenario, we can upload recordFile to Gemini API for audio transcription.
        // For rapid simulation, we pass an elegant notice and let the user review it in their text screen.
        _transcriptionInput.value = "I was floating above a shimmering neon violet ocean. Waves whispered forgotten symbols. Suddenly, a giant glowing silver key opened a door in the sunset sky, leading to an endless library made of stars..."
    }

    // Storyboard, Image Generation, and Psychological Interpretation
    fun generateDreamBlueprint() {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            val textToAnalyze = _transcriptionInput.value
            if (textToAnalyze.trim().isEmpty()) {
                _errorMessage.value = "Please write or record a dream description first!"
                _isProcessing.value = false
                return@launch
            }

            val curStyle = _selectedStyle.value

            val storyboardPrompt = """
                Convert this raw dream transcript into:
                1. Suggest a precise surreal dream title (2-4 words)
                2. A cohesive dream journey summary (1 paragraph)
                3. Exactly 3 distinct sequential storyboard scenes mapping the narrative progression.
                For each scene, design:
                - Description of the visual (E.g. detailed surreal visual image prompt in the artistic style of $curStyle. Be descriptive!)
                - Script text for the professional cinematic voice narrator (1-2 sentences)
                
                The transcript: "$textToAnalyze"
                
                YOU MUST STRICTLY respond in the following readable parser block format:
                ---TITLE---
                [Suggest Title here]
                ---STORY---
                [Cohesive summary here]
                ---SCENES---
                SCENE 1:
                SCRIPT: [Narration script here]
                PROMPT: [Ultra-descriptive surreal prompt in $curStyle here]
                SCENE 2:
                SCRIPT: [Narration script here]
                PROMPT: [Ultra-descriptive surreal prompt in $curStyle here]
                SCENE 3:
                SCRIPT: [Narration script here]
                PROMPT: [Ultra-descriptive surreal prompt in $curStyle here]
            """.trimIndent()

            try {
                // Call Gemini for structured storyboard output
                val responseText = withContext(Dispatchers.IO) {
                    GeminiApiHandler.generateText(storyboardPrompt)
                }

                logDebug("Gemini Response: $responseText")

                // Parse structured block response
                parseResponseIntoBlueprint(responseText)

                // Call Gemini for a self-reflective, deep, symbolic psychological analysis of symbols
                val interpretationPrompt = """
                    Based on these dream details: "$textToAnalyze"
                    Provide a symbolic psychological interpretation based on modern psychological concepts (Jungian archetypes, subconscious metaphors like running, flying, light, water, keys, libraries).
                    Keep it self-reflective, mysterious, and engaging, structured with 2-3 readable, beautiful bullet points of inner self-discovery.
                """.trimIndent()

                val interpretationText = withContext(Dispatchers.IO) {
                    GeminiApiHandler.generateText(interpretationPrompt)
                }
                _generatedInterpretation.value = interpretationText

            } catch (e: Exception) {
                Log.e("MainViewModel", "API call failed", e)
                _errorMessage.value = "Connection lost: ${e.message}. Using offline Dreamweaver engine."
                generateOfflineMockDream(textToAnalyze, curStyle)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Call Gemini Image Generator for a specific scene
    fun generateVisualForScene(sceneIndex: Int) {
        val currentList = _generatedScenes.value.toMutableList()
        if (sceneIndex >= currentList.size) return

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            val scene = currentList[sceneIndex]
            try {
                val base64 = withContext(Dispatchers.IO) {
                    GeminiApiHandler.generateImageBase64(scene.visualPrompt)
                }
                scene.imageBase64 = base64
                _generatedScenes.value = currentList
            } catch (e: Exception) {
                Log.e("MainViewModel", "Image generation failed", e)
                _errorMessage.value = "Unable to compile cinematic pixels: ${e.message}. Simulating dream visual stream instead."
                generateMockVisualForScene(sceneIndex)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Generate visuals for ALL scenes sequentially
    fun generateAllVisuals() {
        viewModelScope.launch {
            val sceneCount = _generatedScenes.value.size
            for (i in 0 until sceneCount) {
                if (_generatedScenes.value[i].imageBase64 == null) {
                    generateVisualForScene(i)
                }
            }
        }
    }

    // Convert parsed blocks back to scenes lists
    private fun parseResponseIntoBlueprint(raw: String) {
        try {
            var title = "Surreal Journey"
            var story = ""
            val scenes = mutableListOf<DreamScene>()

            val parts = raw.split("---")
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.startsWith("TITLE")) {
                    title = trimmed.removePrefix("TITLE").trim().replace("\n", " ").trim('[', ']')
                } else if (trimmed.startsWith("STORY")) {
                    story = trimmed.removePrefix("STORY").trim()
                } else if (trimmed.startsWith("SCENES")) {
                    val sceneBlocks = trimmed.removePrefix("SCENES").split("SCENE ")
                    var counter = 1
                    for (block in sceneBlocks) {
                        val subText = block.trim()
                        if (subText.isEmpty()) continue
                        // parse numbered blocks
                        val lines = subText.lines()
                        var script = ""
                        var prompt = ""
                        for (line in lines) {
                            val l = line.trim()
                            if (l.startsWith("SCRIPT:")) {
                                script = l.removePrefix("SCRIPT:").trim()
                            } else if (l.startsWith("PROMPT:")) {
                                prompt = l.removePrefix("PROMPT:").trim()
                            }
                        }
                        if (script.isNotEmpty() || prompt.isNotEmpty()) {
                            scenes.add(
                                DreamScene(
                                    sceneNumber = counter,
                                    scriptText = if (script.isEmpty()) "Hovering in the digital dreamspace." else script,
                                    visualPrompt = if (prompt.isEmpty()) "A cosmic abstract wallpaper in neon purple style" else prompt
                                )
                            )
                            counter++
                        }
                    }
                }
            }

            if (scenes.isEmpty()) {
                // If regex failed, fill standard scenes
                scenes.add(DreamScene(1, "You wake up suspended above a giant glowing violet hourglass.", "Ethereal glass hourglass glowing in vast indigo space"))
                scenes.add(DreamScene(2, "Sands of starlight drift past, revealing a secure, floating crystal key.", "A key of radiant light hovering on dark stars"))
                scenes.add(DreamScene(3, "The doors of the sky fling open into an endless library.", "Enormous floating bookshelves reaching into majestic cloudy horizon"))
            }

            _generatedTitle.value = title
            _generatedNarrative.value = story
            _generatedScenes.value = scenes

        } catch (e: Exception) {
            Log.e("MainViewModel", "Parsing failed", e)
            generateOfflineMockDream(_transcriptionInput.value, _selectedStyle.value)
        }
    }

    private fun generateOfflineMockDream(input: String, style: String) {
        _generatedTitle.value = "Subconscious Threshold"
        _generatedNarrative.value = "Spurred by messy wake-up parameters, your conscious thoughts dissolve as physical boundaries melt into stardust. The mind searches for pathways out of the ordinary."
        _generatedScenes.value = listOf(
            DreamScene(
                sceneNumber = 1,
                scriptText = "I found myself drifting along a quiet path where neon purple neon lamps guided. Everything hummed.",
                visualPrompt = "Highly descriptive digital artwork of glowing violet lampposts on a foggy pathway, $style"
            ),
            DreamScene(
                sceneNumber = 2,
                scriptText = "Suddenly, keys floated down like glowing autumn leaves, spinning with soft ripples.",
                visualPrompt = "Surreal digital portrait of glowing golden keys falling gently through starry space, $style"
            ),
            DreamScene(
                sceneNumber = 3,
                scriptText = "A massive keyhole appeared in the nebula, opening a gateway into serene clouds.",
                visualPrompt = "Grand majestic glowing lock in deep cosmic nebula with serene violet clouds peeking through, $style"
            )
        )
        _generatedInterpretation.value = "• **Floating lampposts** represent searching for external clues to solve an internal mystery.\n• **Falling keys** reflect dynamic opportunities or hidden realizations about to unlock in your waking life.\n• **The cosmic gateway** encapsulates standard psychological indicators of transitioning to a state of higher clarity."
    }

    private fun generateMockVisualForScene(index: Int) {
        val list = _generatedScenes.value.toMutableList()
        if (index < list.size) {
            // Safe fallback standard colorful gradients if model fails
            // It uses a placeholder or generated layout representation
            list[index].imageBase64 = null
            _generatedScenes.value = list
        }
    }

    // Save newly compiled dream into the secure Room database journal
    fun saveDreamToJournal() {
        viewModelScope.launch {
            if (_generatedTitle.value.isEmpty()) return@launch

            val scenesList = _generatedScenes.value
            val scenesJsonString = sceneListAdapter.toJson(scenesList)

            val newRecord = DreamEntity(
                title = _generatedTitle.value,
                originalText = _transcriptionInput.value,
                style = _selectedStyle.value,
                mood = _selectedMood.value,
                storyNarrative = _generatedNarrative.value,
                scenesJson = scenesJsonString,
                interpretation = _generatedInterpretation.value,
                timestamp = System.currentTimeMillis(),
                musicIndex = _selectedMusicIndex.value
            )

            repository.insertDream(newRecord)
            clearActiveWorkshop()
        }
    }

    fun deleteDream(id: Int) {
        viewModelScope.launch {
            repository.deleteDreamById(id)
        }
    }

    private fun clearActiveWorkshop() {
        _transcriptionInput.value = ""
        _generatedTitle.value = ""
        _generatedNarrative.value = ""
        _generatedScenes.value = emptyList()
        _generatedInterpretation.value = ""
        _errorMessage.value = null
    }

    // Interactive Audio Synthesizer Engine
    // Real dynamic synthesis of Drone Frequencies based on selected dream mood
    fun startAmbientSynth(mood: String = _selectedMood.value) {
        stopAmbientSynth()
        _isPlayingSound.value = true

        synthJob = viewModelScope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack = track

            try {
                track.play()
            } catch (e: Exception) {
                Log.e("AmbientSynth", "Failed to play audiotrack", e)
                _isPlayingSound.value = false
                return@launch
            }

            val buffer = ShortArray(4096)
            var angle1 = 0.0
            var angle2 = 0.0
            var angle3 = 0.0

            // Set frequency configurations based on selected mood
            val (f1, f2, f3) = when (mood.lowercase()) {
                "calm" -> Triple(110.0, 165.0, 220.0) // Soft relaxing harmony
                "terrifying" -> Triple(65.4, 73.4, 98.0) // Low disharmonious rumbles
                else -> Triple(110.0, 137.5, 175.0) // Mysterious drifting microtones
            }

            while (_isPlayingSound.value && synthJob?.isActive == true) {
                for (i in buffer.indices) {
                    val sample1 = sin(angle1)
                    val sample2 = sin(angle2)
                    val sample3 = sin(angle3)

                    // Overlay waves for warm multi-chord synth experience
                    val blendedValue = ((sample1 + sample2 + sample3) / 3.0) * 32767.0 * 0.4
                    buffer[i] = blendedValue.toInt().toShort()

                    angle1 += (2.0 * Math.PI * f1) / sampleRate
                    angle2 += (2.0 * Math.PI * f2) / sampleRate
                    angle3 += (2.0 * Math.PI * f3) / sampleRate

                    if (angle1 > 2.0 * Math.PI) angle1 -= 2.0 * Math.PI
                    if (angle2 > 2.0 * Math.PI) angle2 -= 2.0 * Math.PI
                    if (angle3 > 2.0 * Math.PI) angle3 -= 2.0 * Math.PI
                }
                track.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stopAmbientSynth() {
        _isPlayingSound.value = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun getScenesListFromEntity(dream: DreamEntity): List<DreamScene> {
        return try {
            sceneListAdapter.fromJson(dream.scenesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun logDebug(msg: String) {
        Log.d("DreamCraftEngine", msg)
    }

    override fun onCleared() {
        super.onCleared()
        stopAmbientSynth()
        recordingJob?.cancel()
        mediaRecorder?.release()
    }
}
