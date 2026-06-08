package com.aegis.ielts.features.writing.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.aegis.ielts.core.domain.WritingAssessmentResponse
import com.aegis.ielts.features.writing.WritingUiState
import com.aegis.ielts.features.writing.WritingViewModel
import com.aegis.ielts.features.writing.data.*
import com.aegis.ielts.ui.theme.*
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WritingAssessmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: WritingViewModel,
    testId: String
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeLeft by viewModel.timeLeftSeconds.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceSlate)
            .systemBarsPadding()
    ) {
        Crossfade(targetState = uiState, label = "WritingStateCrossfade") { state ->
            when (state) {
                is WritingUiState.Idle -> {
                    WritingIdleContent(
                        onStartTest = { taskType -> viewModel.startWritingTest(taskType) },
                        onNavigateBack = onNavigateBack
                    )
                }
                is WritingUiState.MockTestActive -> {
                    WritingActiveWorkspace(
                        state = state,
                        timeLeft = timeLeft,
                        essayTextFlow = viewModel.essayText,
                        onEssayChange = { viewModel.updateEssay(it) },
                        onNavigateBack = { viewModel.resetToIdle() },
                        onSubmit = { viewModel.submitEssay() }
                    )
                }
                is WritingUiState.Analyzing -> {
                    WritingAnalyzingContent()
                }
                is WritingUiState.EvaluationComplete -> {
                    WritingEvaluationCompleteContent(
                        task = state.task,
                        response = state.response,
                        onRestart = { viewModel.resetToIdle() },
                        onNavigateBack = onNavigateBack
                    )
                }
                is WritingUiState.Error -> {
                    WritingErrorContent(
                        message = state.message,
                        onRetry = { viewModel.resetToIdle() }
                    )
                }
            }
        }
    }
}

// ─── Idle Instructions Screen ──────────────────────────────────────────────────

@Composable
private fun WritingIdleContent(
    onStartTest: (Int) -> Unit,
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
                .background(AccentGold.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = "Writing Module",
                tint = AccentGold,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "IELTS WRITING WORKSPACE",
            color = TextLight,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Academic Task 1 and Task 2 Practice",
            color = TextMuted,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Info cards
        Row(
            modifier = Modifier.fillMaxWidth(0.95f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TaskSelectCard(
                taskNumber = 1,
                title = "Academic Task 1",
                description = "Report on informational data charts.\n(Min 150 words)",
                accentColor = SurfaceCyan,
                modifier = Modifier.weight(1f),
                onClick = { onStartTest(1) }
            )

            TaskSelectCard(
                taskNumber = 2,
                title = "Academic Task 2",
                description = "Formal argumentative research essay.\n(Min 250 words)",
                accentColor = AccentGold,
                modifier = Modifier.weight(1f),
                onClick = { onStartTest(2) }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedButton(
            onClick = onNavigateBack,
            border = BorderStroke(1.dp, SurfaceSlateLight),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(50.dp)
        ) {
            Text("Back to Dashboard")
        }
    }
}

@Composable
private fun TaskSelectCard(
    taskNumber: Int,
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(1.dp, SurfaceSlateLight, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "TASK $taskNumber",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = TextLight,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Select",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Active Essay Workspace Screen ─────────────────────────────────────────────

@Composable
private fun WritingActiveWorkspace(
    state: WritingUiState.MockTestActive,
    timeLeft: Int,
    essayTextFlow: StateFlow<String>,
    onEssayChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val task = state.task

    Column(modifier = Modifier.fillMaxSize()) {
        // Active Header Bar
        ActiveWritingHeader(
            taskType = task.taskType,
            timeLeft = timeLeft,
            isFrozen = state.isFrozen,
            onQuit = onNavigateBack,
            onSubmit = onSubmit
        )

        HorizontalDivider(color = SurfaceSlateLight, thickness = 1.dp)

        // Split Layout (Keystroke performance optimized)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 768.dp

            if (isCompact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top: Prompt & Multi-view tables
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        WritingPromptPanel(task = task)
                    }

                    HorizontalDivider(color = SurfaceSlateLight, thickness = 2.dp)

                    // Bottom: Text Editor and Decoupled Word Counter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(SurfaceSlateLight)
                            .imePadding()
                    ) {
                        WritingEditorPanel(
                            essayTextFlow = essayTextFlow,
                            minWords = task.minWords,
                            isFrozen = state.isFrozen,
                            onEssayChange = onEssayChange
                        )
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: Prompt Detail
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        WritingPromptPanel(task = task)
                    }

                    VerticalDivider(color = SurfaceSlateLight, thickness = 1.dp)

                    // Right Pane: Text Editor Workspace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(SurfaceSlateLight)
                    ) {
                        WritingEditorPanel(
                            essayTextFlow = essayTextFlow,
                            minWords = task.minWords,
                            isFrozen = state.isFrozen,
                            onEssayChange = onEssayChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveWritingHeader(
    taskType: Int,
    timeLeft: Int,
    isFrozen: Boolean,
    onQuit: () -> Unit,
    onSubmit: () -> Unit
) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)
    val isLowTime = timeLeft < 300 // Red color if < 5 mins

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSlate)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQuit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quit",
                    tint = TextLight
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Writing Task $taskType Workspace",
                color = TextLight,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Timer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    if (isLowTime) AccentError.copy(alpha = 0.15f) else SurfaceSlateLight,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                tint = if (isLowTime) AccentError else SurfaceCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timerText,
                color = if (isLowTime) AccentError else TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Submit Button
        Button(
            onClick = onSubmit,
            enabled = !isFrozen,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                disabledContainerColor = SurfaceSlateLight
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Submit Essay",
                color = if (isFrozen) TextMuted else SurfaceSlate,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WritingPromptPanel(task: WritingTask) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceSlate)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(AccentGold.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ACADEMIC TASK ${task.taskType}",
                color = AccentGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = task.title,
            color = TextLight,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = task.prompt,
            color = TextLight.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 24.sp
        )

        // Renders visual metrics table for Task 1 side-by-side/stacked
        if (task.taskType == 1 && task.detailsText != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Multi-View Informational Prompt Data:",
                color = SurfaceCyan,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SurfaceSlateLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Task 1 Energy Table Data Model mapping
                    EnergySourceTable()
                }
            }
        }
    }
}

@Composable
private fun EnergySourceTable() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSlate)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Energy Source", color = SurfaceCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
            Text(text = "2015 Share", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(text = "2025 Share", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
        HorizontalDivider(color = SurfaceSlateLight)

        EnergyRow("Coal", "30%", "22%")
        EnergyRow("Gas", "23%", "25%")
        EnergyRow("Oil", "33%", "28%")
        EnergyRow("Renewables", "14%", "25%")
    }
}

@Composable
private fun EnergyRow(source: String, val15: String, val25: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = source, color = TextLight, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
        Text(text = val15, color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(text = val25, color = SurfaceCyan, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = SurfaceSlate.copy(alpha = 0.4f))
}

/**
 * Text Editor workspace panel.
 * WordCountDisplay is isolated and reads state flow internally, skipping parent recomposition.
 */
@Composable
private fun WritingEditorPanel(
    essayTextFlow: StateFlow<String>,
    minWords: Int,
    isFrozen: Boolean,
    onEssayChange: (String) -> Unit
) {
    // Collect the state flow locally in the editor panel
    val textState by essayTextFlow.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Text Input Box
        OutlinedTextField(
            value = textState,
            onValueChange = onEssayChange,
            readOnly = isFrozen,
            placeholder = { Text("Write your response here...", color = TextMuted, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                disabledTextColor = TextMuted,
                focusedContainerColor = SurfaceSlate,
                unfocusedContainerColor = SurfaceSlate,
                disabledContainerColor = SurfaceSlate,
                focusedBorderColor = SurfaceCyan,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Decoupled Word Counter (Performance optimized)
        WordCounterDisplay(
            essayTextFlow = essayTextFlow,
            minWords = minWords
        )
    }
}

/**
 * Isolated word counter component that collects the state flow internally.
 * This guarantees the parent layout is NOT recomposed on single key entries.
 */
@Composable
private fun WordCounterDisplay(
    essayTextFlow: StateFlow<String>,
    minWords: Int
) {
    val text by essayTextFlow.collectAsStateWithLifecycle()
    val wordCount = remember(text) {
        text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    }
    val isComplete = wordCount >= minWords

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSlate, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Edit,
                contentDescription = null,
                tint = if (isComplete) AccentGreen else AccentGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$wordCount words",
                color = if (isComplete) AccentGreen else TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Text(
            text = "Required: min $minWords words",
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}

// ─── AI Analyzing Screen ────────────────────────────────────────────────────────

@Composable
private fun WritingAnalyzingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceSlate),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = SurfaceCyan,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "AEGIS EXAMINER GRADING...",
            color = SurfaceCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Evaluating grammar structures & template plagiarism indices",
            color = TextMuted,
            fontSize = 12.sp
        )
    }
}

// ─── Evaluation Completed Panel ──────────────────────────────────────────────────

@Composable
private fun WritingEvaluationCompleteContent(
    task: WritingTask,
    response: WritingAssessmentResponse,
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
            text = "WRITING EVALUATION REPORT",
            color = SurfaceCyan,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Glowing overall Band Score
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
                drawCircle(color = SurfaceSlateLight, style = Stroke(width = 8.dp.toPx()))
                val sweep = (response.overallScore.overall.band / 9.0f) * 360f
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
                    text = response.overallScore.overall.toString(),
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

        // Template Warning Banner
        if (response.templateDetected) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentError.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentError),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AccentError, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Template Plagiarism Warning", color = AccentError, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "Examiner similarity index: %.0f%%. Rigid formulaic structures detected.".format(response.templateSimilarityScore * 100),
                            color = TextLight,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Originality Check Passed: Organic transitions and sentence cohesive flows detected.",
                        color = TextLight,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Subscores Grid breakdown
        Text(
            text = "Criteria Scorecard",
            color = TextLight,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SubScoreCard("Task Achievement", response.taskAchievementScore, modifier = Modifier.weight(1f))
            SubScoreCard("Coherence", response.coherenceScore, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SubScoreCard("Lexical Resource", response.lexicalScore, modifier = Modifier.weight(1f))
            SubScoreCard("Grammar & Range", response.grammarScore, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Examiner Critique
        Text(
            text = "Examiner Critique",
            color = TextLight,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = response.feedback,
                color = TextLight,
                fontSize = 13.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // CTA Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                border = BorderStroke(1.dp, SurfaceSlateLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Home")
            }

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(
                    text = "Retry Task",
                    color = SurfaceSlate,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SubScoreCard(
    label: String,
    score: Float,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = TextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "%.1f".format(score),
                color = SurfaceCyan,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Error Screen ─────────────────────────────────────────────────────────────

@Composable
private fun WritingErrorContent(
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
