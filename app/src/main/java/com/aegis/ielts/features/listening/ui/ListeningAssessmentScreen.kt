package com.aegis.ielts.features.listening.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.lazy.LazyColumn
import com.aegis.ielts.features.listening.ListeningUiState
import com.aegis.ielts.features.listening.ListeningViewModel
import com.aegis.ielts.features.listening.data.*
import com.aegis.ielts.ui.theme.*

@Composable
fun ListeningAssessmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: ListeningViewModel,
    testId: String
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val inputErrors by viewModel.inputErrors.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.audioPlaybackProgress.collectAsStateWithLifecycle()
    val bufferProgress by viewModel.audioBufferProgress.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceSlate)
            .systemBarsPadding()
            .navigationBarsPadding()
    ) {
        Crossfade(targetState = uiState, label = "ListeningStateCrossfade") { state ->
            when (state) {
                is ListeningUiState.Idle -> {
                    ListeningIdleContent(
                        onStartTest = { viewModel.startListeningAssessment() },
                        onNavigateBack = onNavigateBack
                    )
                }
                is ListeningUiState.PendingStart -> {
                    ListeningPendingStartContent(
                        onStartTest = { viewModel.beginPlaybackFromPending() },
                        onNavigateBack = { viewModel.resetToIdle() }
                    )
                }
                is ListeningUiState.Active -> {
                    ListeningActiveContent(
                        state = state,
                        answers = answers,
                        inputErrors = inputErrors,
                        playbackProgress = playbackProgress,
                        bufferProgress = bufferProgress,
                        onNavigateBack = { viewModel.resetToIdle() },
                        onSaveAnswer = { qId, ans -> viewModel.saveAnswer(qId, ans) },
                        onSubmitTest = { viewModel.submitListeningTest() },
                        onStartSectionAudio = { viewModel.startSectionAudio() }
                    )
                }
                is ListeningUiState.EvaluationComplete -> {
                    ListeningEvaluationCompleteContent(
                        report = state.report,
                        onRestart = { viewModel.resetToIdle() },
                        onNavigateBack = onNavigateBack
                    )
                }
                is ListeningUiState.Error -> {
                    ListeningErrorContent(
                        message = state.message,
                        onRetry = { viewModel.resetToIdle() }
                    )
                }
            }
        }
    }
}

// ─── Idle / Instructions Screen ───────────────────────────────────────────────

@Composable
private fun ListeningIdleContent(
    onStartTest: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(AccentGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Headset,
                contentDescription = "Listening Module",
                tint = AccentGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "IELTS LISTENING SIMULATOR",
            color = TextLight,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Accent Simulation & Rigid Media Playback",
            color = TextMuted,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Rule breakdown card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Testing Regulations",
                    color = SurfaceCyan,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                InstructionItem(icon = Icons.AutoMirrored.Filled.VolumeUp, text = "Accent Distribution: South Asian (25%), African (15%), European (20%), Australian (20%), and Standard (20%) accents sampled across 4 sections.")
                InstructionItem(icon = Icons.Default.Lock, text = "Strict Playback Contract: Media cannot be paused, rewound, or scrubbed. Tracks play exactly once.")
                InstructionItem(icon = Icons.Default.Shuffle, text = "Unpredictable Task Router: Shuffles question types to prevent patterns memorization.")
                InstructionItem(icon = Icons.Default.Spellcheck, text = "Alphanumeric Validation: Real-time word and character count limits on text inputs.")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                border = BorderStroke(1.dp, SurfaceSlateLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Back")
            }

            Button(
                onClick = onStartTest,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f).height(50.dp)
            ) {
                Text(
                    text = "Start Listening Test",
                    color = SurfaceSlate,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InstructionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SurfaceCyan,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = TextLight,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ListeningPendingStartContent(
    onStartTest: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "SOUND CHECK",
            color = TextLight,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please ensure your headphones are connected and the volume is set to a comfortable level.\n\nThe audio will begin immediately and play exactly once. You cannot pause or rewind.",
            color = TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                border = BorderStroke(1.dp, SurfaceSlateLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onStartTest,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f).height(50.dp)
            ) {
                Text(
                    text = "Start Test",
                    color = SurfaceSlate,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Active Test Content ──────────────────────────────────────────────────────

@Composable
private fun ListeningActiveContent(
    state: ListeningUiState.Active,
    answers: Map<String, String>,
    inputErrors: Map<String, String?>,
    playbackProgress: Float,
    bufferProgress: Float,
    onNavigateBack: () -> Unit,
    onSaveAnswer: (String, String) -> Unit,
    onSubmitTest: () -> Unit,
    onStartSectionAudio: () -> Unit
) {
    // Combine questions from all active sections
    val allQuestions = remember(state.sections) {
        state.sections.flatMap { it.questions }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Active Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSlate)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quit",
                            tint = TextLight
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IELTS Listening Simulation Workspace",
                        color = TextLight,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sticky-pinned Top Audio Controller Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(SurfaceSlate)
                    .border(width = 1.dp, color = SurfaceSlateLight)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Play Button
                    Button(
                        onClick = onStartSectionAudio,
                        enabled = !state.isAudioStarted && !state.isFrozen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isAudioStarted) SurfaceSlateLight else AccentGreen,
                            disabledContainerColor = SurfaceSlateLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(140.dp)
                            .height(48.dp)
                    ) {
                        val icon = if (state.isAudioStarted && state.isAudioPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PlayArrow
                        val text = if (state.isAudioStarted) {
                            if (state.isAudioPlaying) "Playing..." else "Completed"
                        } else {
                            "Play Audio"
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (state.isAudioStarted) TextMuted else SurfaceSlate,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = text,
                            color = if (state.isAudioStarted) TextMuted else SurfaceSlate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Progress Indicator Line & metrics domain mapping
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Exam Timeline Progress",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(playbackProgress * 100).toInt()}%",
                                color = SurfaceCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        CanvasIsolatedProgressBar(
                            progress = playbackProgress,
                            bufferProgress = bufferProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }

            // Single, continuous vertically scrollable LazyColumn question pool
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Overall instructions
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "EXAM DIRECTIONS",
                                color = SurfaceCyan,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You may preview all questions below before starting the audio playback. Once ready, click 'Play Audio' in the top controller bar to start the listening track. Answers must be completed as you listen.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                items(allQuestions.size) { index ->
                    val question = allQuestions[index]
                    val answer = answers[question.id].orEmpty()
                    val error = inputErrors[question.id]
                    QuestionCard(
                        index = index,
                        question = question,
                        answer = answer,
                        error = error,
                        isFrozen = state.isFrozen,
                        onSaveAnswer = onSaveAnswer
                    )
                }

                item {
                    Button(
                        onClick = onSubmitTest,
                        enabled = !state.isFrozen,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Submit Exam",
                            color = SurfaceSlate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Isolated progress bar using Canvas to render values exactly with buffer overlay.
 */
@Composable
private fun CanvasIsolatedProgressBar(
    progress: Float,
    bufferProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Track path
        drawRect(
            color = SurfaceSlateLight,
            size = size
        )

        // Buffer path (bytes received)
        if (bufferProgress > 0f) {
            val bufferWidth = width * bufferProgress.coerceIn(0f, 1f)
            drawRect(
                color = SurfaceCyan.copy(alpha = 0.3f),
                size = androidx.compose.ui.geometry.Size(bufferWidth, height)
            )
        }

        // Progress path
        if (progress > 0f) {
            val fillWidth = width * progress.coerceIn(0f, 1f)
            drawRect(
                color = SurfaceCyan,
                size = androidx.compose.ui.geometry.Size(fillWidth, height)
            )
        }
    }
}

@Composable
private fun SectionOverviewPanel(section: ListeningSection) {
    Column {
        Text(
            text = "SECTION OVERVIEW",
            color = SurfaceCyan,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Section: ${section.sectionNumber}",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Environment: ${section.environment.label}",
                    color = AccentGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Speaker Accent: ${section.accent.label}",
                    color = AccentGold,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = section.environment.description,
                    color = TextLight.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    index: Int,
    question: ListeningQuestion,
    answer: String,
    error: String?,
    isFrozen: Boolean,
    onSaveAnswer: (String, String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = question.instruction,
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Question ${index + 1}: ${question.questionText}",
                color = TextLight,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Route to matching sub-widgets based on question types
            when (question) {
                is ListeningQuestion.FormCompletion -> {
                    FormCompletionWidget(
                        value = answer,
                        error = error,
                        isFrozen = isFrozen,
                        onValueChange = { onSaveAnswer(question.id, it) }
                    )
                }
                is ListeningQuestion.MultipleChoice -> {
                    MultipleChoiceWidget(
                        selectedOption = answer,
                        options = question.options,
                        isFrozen = isFrozen,
                        onSelect = { onSaveAnswer(question.id, it) }
                    )
                }
                is ListeningQuestion.MapLabeling -> {
                    MapLabelingWidget(
                        selectedCoordinate = answer,
                        coordinates = question.mapLocations,
                        isFrozen = isFrozen,
                        onSelect = { onSaveAnswer(question.id, it) }
                    )
                }
                is ListeningQuestion.Matching -> {
                    MatchingWidget(
                        questionText = question.questionText,
                        selectedCategory = answer,
                        categories = question.categories,
                        isFrozen = isFrozen,
                        onSelect = { onSaveAnswer(question.id, it) }
                    )
                }
            }
        }
    }
}

// ─── Question Widgets ─────────────────────────────────────────────────────────

@Composable
private fun FormCompletionWidget(
    value: String,
    error: String?,
    isFrozen: Boolean,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Fill-in-the-Blanks (Type Answer):",
            color = SurfaceCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = !isFrozen,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                disabledTextColor = TextMuted,
                focusedContainerColor = SurfaceSlate,
                unfocusedContainerColor = SurfaceSlate,
                disabledContainerColor = SurfaceSlate,
                focusedBorderColor = SurfaceCyan,
                unfocusedBorderColor = SurfaceSlateLight,
                errorContainerColor = SurfaceSlate
            ),
            isError = error != null,
            placeholder = { Text("Type alphanumeric token...", color = TextMuted, fontSize = 13.sp) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AccentError,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = error,
                    color = AccentError,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MultipleChoiceWidget(
    selectedOption: String,
    options: List<String>,
    isFrozen: Boolean,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val letter = option.substringBefore(". ").trim()
            val isSelected = selectedOption == letter

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) SurfaceCyan.copy(alpha = 0.12f) else SurfaceSlate)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) SurfaceCyan else SurfaceSlateLight,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = !isFrozen) { onSelect(letter) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { if (!isFrozen) onSelect(letter) },
                    enabled = !isFrozen,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = SurfaceCyan,
                        unselectedColor = TextMuted
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option,
                    color = if (isSelected) SurfaceCyan else TextLight,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Blueprint map labeling layout drawn on Canvas.
 */
@Composable
private fun MapLabelingWidget(
    selectedCoordinate: String,
    coordinates: List<String>,
    isFrozen: Boolean,
    onSelect: (String) -> Unit
) {
    Column {
        // Draw Blueprint using Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceSlate)
                .border(1.dp, SurfaceSlateLight, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val mapPainter = remember {
                try {
                    val stream = context.assets.open("maps/reserve_tour.png")
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    androidx.compose.ui.graphics.painter.BitmapPainter(bitmap.asImageBitmap())
                } catch (e: Exception) {
                    null
                }
            }

            if (mapPainter != null) {
                Image(
                    painter = mapPainter,
                    contentDescription = "Reserve Tour Map",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw layout grids
                    drawRect(color = SurfaceSlateLight.copy(alpha = 0.5f), size = size, style = Stroke(width = 2.dp.toPx()))

                    // Draw rooms
                    drawRect(
                        color = SurfaceSlateLight,
                        topLeft = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 10.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.4f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawRect(
                        color = SurfaceSlateLight,
                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.65f, 10.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.4f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawRect(
                        color = SurfaceSlateLight,
                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.55f),
                        size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.35f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Coordinates Labels overlay
            Box(modifier = Modifier.fillMaxSize()) {
                // Room 1 (A)
                CoordinateTextOverlay("A", Alignment.TopStart, Modifier.padding(start = 24.dp, top = 20.dp))
                // Corridor (B)
                CoordinateTextOverlay("B", Alignment.Center, Modifier.padding(bottom = 20.dp))
                // Room 2 (C)
                CoordinateTextOverlay("C", Alignment.TopEnd, Modifier.padding(end = 24.dp, top = 20.dp))
                // Pathway (D)
                CoordinateTextOverlay("D", Alignment.BottomStart, Modifier.padding(start = 24.dp, bottom = 20.dp))
                // Room 3 (E)
                CoordinateTextOverlay("E", Alignment.BottomCenter, Modifier.padding(bottom = 12.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Coordinate picker buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            coordinates.forEach { letter ->
                val isSelected = selectedCoordinate == letter

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SurfaceCyan.copy(alpha = 0.12f) else SurfaceSlate)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) SurfaceCyan else SurfaceSlateLight,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = !isFrozen) { onSelect(letter) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        color = if (isSelected) SurfaceCyan else TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CoordinateTextOverlay(
    letter: String,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(SurfaceSlateLight, shape = RoundedCornerShape(4.dp))
                .border(1.dp, SurfaceCyan.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = letter, color = SurfaceCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MatchingWidget(
    questionText: String,
    selectedCategory: String,
    categories: List<String>,
    isFrozen: Boolean,
    onSelect: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SurfaceSlateLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSlateLight)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question Item",
                    color = SurfaceCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1.5f)
                )
                categories.forEach { category ->
                    Text(
                        text = category.replace("_", " "),
                        color = SurfaceCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = SurfaceSlateLight)

            // Data Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = questionText,
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1.5f)
                )
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { if (!isFrozen) onSelect(category) },
                            enabled = !isFrozen,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = SurfaceCyan,
                                unselectedColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─── Evaluation Completed Scorecard ───────────────────────────────────────────

@Composable
private fun ListeningEvaluationCompleteContent(
    report: ListeningGradingReport,
    onRestart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceSlate)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "LISTENING REPORT",
            color = SurfaceCyan,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Circular Band Score
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SurfaceCyan.copy(alpha = 0.15f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                drawCircle(
                    color = SurfaceSlateLight,
                    style = Stroke(width = 8.dp.toPx())
                )
                val sweep = (report.rawScore.toFloat() / report.totalQuestions.toFloat()) * 360f
                drawArc(
                    color = SurfaceCyan,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = report.bandScore.toString(),
                    color = TextLight,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Band Score",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Raw Score Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accuracy score: ",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${report.rawScore} / ${report.totalQuestions} Correct",
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Accent Summary List
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Accent Sampled Environments",
                    color = SurfaceCyan,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                report.sectionAccents.forEachIndexed { i, accent ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Section ${i + 1}", color = TextLight, fontSize = 12.sp)
                        Text(text = accent, color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Answer Script Breakdown",
            color = TextLight,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Question Details Script
        report.userAnswers.forEach { (qId, userAns) ->
            val isCorrect = userAns.trim().uppercase() == getCorrectAnswer(qId).trim().uppercase()

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCorrect) AccentGreen.copy(alpha = 0.3f) else AccentError.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Item ID: ${qId.uppercase()}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isCorrect) AccentGreen else AccentError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCorrect) "Correct" else "Incorrect",
                                color = if (isCorrect) AccentGreen else AccentError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Your Response: " + (if (userAns.isEmpty()) "(Blank)" else userAns),
                        color = if (isCorrect) SurfaceCyan else AccentError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (!isCorrect) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Correct Answer: " + getCorrectAnswer(qId),
                            color = AccentGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom CTA Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                border = BorderStroke(1.dp, SurfaceSlateLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Home")
            }

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text(
                    text = "Retry",
                    color = SurfaceSlate,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

// helper stub matching exact answers
private fun getCorrectAnswer(qId: String): String {
    return when (qId) {
        "q1_form_name" -> "HEMINGWAY"
        "q1_form_phone" -> "07700900077"
        "q2_mcq_1" -> "B"
        "q2_mcq_2" -> "A"
        "q3_map_1" -> "B"
        "q3_map_2" -> "C"
        "q4_match_1" -> "MEDIEVAL"
        "q4_match_2" -> "MODERN"
        "q1_mcq_1" -> "A"
        "q1_mcq_2" -> "B"
        "q2_map_1" -> "C"
        "q2_map_2" -> "E"
        "q3_match_1" -> "PREREQUISITES_REQUIRED"
        "q3_match_2" -> "OPEN_TO_ALL"
        "q4_form_1" -> "TITANIUM"
        "q4_form_2" -> "1500"
        else -> ""
    }
}

// ─── Error Screen ─────────────────────────────────────────────────────────────

@Composable
private fun ListeningErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = AccentError,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Something went wrong",
            color = TextLight,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Try Again", color = SurfaceSlate, fontWeight = FontWeight.Bold)
        }
    }
}
