@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.aegis.ielts.features.speaking.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.ielts.features.speaking.ExaminerEngineState
import com.aegis.ielts.features.speaking.SpeakingUiState
import com.aegis.ielts.features.speaking.SpeakingViewModel
import com.aegis.ielts.features.speaking.ui.components.AudioWaveformVisualizer
import com.aegis.ielts.ui.components.shared.ParticleBlobOrb
import com.aegis.ielts.ui.components.shared.StarfieldBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IeltsSpeakingAssessmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpeakingViewModel,
    testId: String
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Decoupled Smart-Flow State Subscriptions
    val currentUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val rawAmplitudeDb by viewModel.currentAmplitudeDb.collectAsStateWithLifecycle()

    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    DisposableEffect(ctx) {
        val ttsEngine = android.speech.tts.TextToSpeech(ctx) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ttsInitialized = true
            }
        }
        ttsEngine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "EXAMINER_UTTERANCE") {
                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        viewModel.onExaminerSpeakingCompleted()
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId == "EXAMINER_UTTERANCE") {
                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        viewModel.onExaminerSpeakingCompleted()
                    }
                }
            }
        })
        tts = ttsEngine
        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }

    LaunchedEffect(currentUiState, ttsInitialized) {
        val state = currentUiState
        if (state is SpeakingUiState.MockTestActive &&
            state.engineState == ExaminerEngineState.EXAMINER_SPEAKING &&
            ttsInitialized
        ) {
            val params = android.os.Bundle().apply {
                putInt(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
            }
            tts?.speak(state.promptText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "EXAMINER_UTTERANCE")
        }
    }

    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    var showAbortDialog by remember { mutableStateOf(false) }

    // Hard-locked system back interceptors to protect continuous testing session integrity
    BackHandler {
        if (currentUiState is SpeakingUiState.MockTestActive) {
            showAbortDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Design Tokens
    val surfaceSlate = Color(0xFF1A1C1E)
    val surfaceCyan = Color(0xFF00F5FF)

    if (showAbortDialog) {
        AlertDialog(
            onDismissRequest = { showAbortDialog = false },
            title = { Text("Abort Official Mock Exam?", fontWeight = FontWeight.Bold) },
            text = { Text("Exiting now will terminate the active session. Your grading trajectory will be lost.") },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            confirmButton = {
                TextButton(onClick = {
                    showAbortDialog = false
                    viewModel.terminateExamSession()
                    onNavigateBack()
                }) { Text("Abort Test", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showAbortDialog = false }) { Text("Resume Practice", color = surfaceCyan) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aegis Computer-Based IELTS Simulation", color = Color.White, fontSize = 16.sp) },
                navigationIcon = {
                    if (currentUiState !is SpeakingUiState.MockTestActive) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    if (currentUiState is SpeakingUiState.MockTestActive) {
                        IconButton(onClick = { showAbortDialog = true }) {
                            Icon(Icons.Filled.Close, "Cancel Session", tint = Color.Gray)
                        }
                    }
                }
            )
        },
        containerColor = surfaceSlate
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            StarfieldBackground()

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = currentUiState) {
                    is SpeakingUiState.Idle -> {
                        Spacer(Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            ParticleBlobOrb(isThinking = false, isTalking = false, isListening = false)
                        }
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = "Official Speaking Module Simulation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "The system will guide you through Part 1, Part 2 (Cue Card),\nand Part 3. Complete structural evaluation parameters apply.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(40.dp))
                        Button(
                            onClick = {
                                if (hasMicPermission) {
                                    viewModel.startMockExamPipeline(testId)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.65f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2F33)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Initialize Mock Test", color = surfaceCyan, letterSpacing = 1.2.sp)
                        }
                        Spacer(Modifier.weight(1f))
                    }

                    is SpeakingUiState.MockTestActive -> {
                        // Strict Time Limit Visualizations
                        val minutes = elapsedSeconds / 60
                        val seconds = elapsedSeconds % 60
                        Text(
                            text = String.format("ELAPSED TIME: %02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (elapsedSeconds > 660) MaterialTheme.colorScheme.error else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        val partText = when (state.currentPart) {
                            1 -> "PART 1 - Question ${state.currentQuestionIndex + 1} of 3"
                            2 -> "PART 2 - Cue Card Topic"
                            3 -> "PART 3 - Question ${state.currentQuestionIndex - 3} of 3"
                            else -> "PART ${state.currentPart}"
                        }
                        Text(
                            text = partText,
                            style = MaterialTheme.typography.titleMedium,
                            color = surfaceCyan,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(Modifier.weight(0.5f))

                        // Interactive Orb Driver mapping to high-precision telemetry states
                        Box(modifier = Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                            ParticleBlobOrb(
                                isThinking = state.engineState == ExaminerEngineState.ANALYZING || state.engineState == ExaminerEngineState.CONNECTING,
                                isTalking = state.engineState == ExaminerEngineState.EXAMINER_SPEAKING,
                                isListening = state.engineState == ExaminerEngineState.CANDIDATE_RECORDING
                            )
                        }

                        Spacer(Modifier.weight(0.5f))

                        // Responsive Status Sub-Labels
                        val diagnosticText = when (state.engineState) {
                            ExaminerEngineState.CONNECTING -> "CONNECTING TO EVALUATION SERVER..."
                            ExaminerEngineState.EXAMINER_SPEAKING -> "EXAMINER DELIVERY ACTIVE"
                            ExaminerEngineState.CANDIDATE_RECORDING -> "LIVE AUDIO PIPELINE RECORDING"
                            ExaminerEngineState.ANALYZING -> "PARSING CRITERIA WEIGHTS..."
                        }

                        val diagnosticColor by animateColorAsState(
                            targetValue = when (state.engineState) {
                                ExaminerEngineState.CONNECTING -> Color(0xFF8B5CF6)
                                ExaminerEngineState.EXAMINER_SPEAKING -> surfaceCyan
                                ExaminerEngineState.CANDIDATE_RECORDING -> Color(0xFF10B981)
                                ExaminerEngineState.ANALYZING -> Color(0xFFD4AF37)
                            }, label = "DiagnosticColor"
                        )

                        Text(
                            text = diagnosticText,
                            style = MaterialTheme.typography.labelMedium,
                            color = diagnosticColor,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(Modifier.height(24.dp))

                        // Active physics-driven waveform rendering during live candidate speech
                        if (state.engineState == ExaminerEngineState.CANDIDATE_RECORDING) {
                            AudioWaveformVisualizer(
                                amplitudeDb = rawAmplitudeDb,
                                modifier = Modifier.padding(horizontal = 32.dp).bottomSpace()
                            )
                        } else {
                            Spacer(Modifier.height(250.dp)) // Maintain canvas constraints to block layout shifts
                        }
                    }

                    is SpeakingUiState.EvaluationComplete -> {
                        // Route to your existing decoupled Tabbed Evaluation Layout mapping
                        DiagnosticReportPanel(
                            report = state.assessmentResponse,
                            onReturn = { viewModel.resetToIdle() }
                        )
                    }

                    is SpeakingUiState.Error -> {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Evaluation Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.resetToIdle() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2F33))
                        ) { Text("Return", color = surfaceCyan) }
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Extension to cleanly isolate layout spacing padding allocations
private fun Modifier.bottomSpace() = this.padding(bottom = 48.dp)