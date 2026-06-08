package com.aegis.ielts.features.reading.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.ielts.features.reading.ReadingUiState
import com.aegis.ielts.features.reading.ReadingViewModel
import com.aegis.ielts.features.reading.data.*
import com.aegis.ielts.ui.theme.*

/**
 * Responsive IELTS Reading Assessment screen with split-pane viewport.
 */
@Composable
fun ReadingAssessmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReadingViewModel,
    testId: String
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeLeft by viewModel.timeLeftSeconds.collectAsStateWithLifecycle()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val validationError by viewModel.validationError.collectAsStateWithLifecycle()

    var activeStemForPicker by remember { mutableStateOf<SentenceStem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceSlate)
            .systemBarsPadding()
    ) {
        // Main State Routing
        Crossfade(targetState = uiState, label = "ReadingStateCrossfade") { state ->
            when (state) {
                is ReadingUiState.Idle -> {
                    IdleStateContent(
                        onStartTest = { viewModel.startMockExam() },
                        onNavigateBack = onNavigateBack
                    )
                }
                is ReadingUiState.MockTestActive -> {
                    ActiveStateContent(
                        timeLeft = timeLeft,
                        answers = answers,
                        isFrozen = state.isFrozen,
                        passage = viewModel.passage,
                        stems = viewModel.stems,
                        onNavigateBack = { viewModel.resetToIdle() },
                        onSelectEnding = { stem -> activeStemForPicker = stem },
                        onClearEnding = { stemId -> viewModel.mapStemToEnding(stemId, null) },
                        onSubmit = { viewModel.submitAssessment() }
                    )
                }
                is ReadingUiState.EvaluationComplete -> {
                    EvaluationCompleteContent(
                        report = state.report,
                        stems = viewModel.stems,
                        endings = viewModel.endings,
                        onRestart = { viewModel.resetToIdle() },
                        onNavigateBack = onNavigateBack
                    )
                }
                is ReadingUiState.Error -> {
                    ErrorStateContent(
                        message = state.message,
                        onRetry = { viewModel.resetToIdle() }
                    )
                }
            }
        }

        // Custom Floating Error Banner for Grammatical/Syntactic Mismatch
        AnimatedVisibility(
            visible = validationError != null,
            enter = slideInVertically(animationSpec = tween(300)) { -it } + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(300)) { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            validationError?.let { errorText ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentError),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = TextLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorText,
                            color = TextLight,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearValidationError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextLight
                            )
                        }
                    }
                }
            }
        }

        // Option Picker Dialog for Sentence Endings
        if (activeStemForPicker != null) {
            EndingPicker(
                stem = activeStemForPicker!!,
                endings = viewModel.endings,
                currentSelectionId = answers[activeStemForPicker!!.id],
                onDismiss = { activeStemForPicker = null },
                onSelect = { endingId ->
                    viewModel.mapStemToEnding(activeStemForPicker!!.id, endingId)
                    activeStemForPicker = null
                }
            )
        }
    }
}

// ─── Screen Components ────────────────────────────────────────────────────────

@Composable
private fun IdleStateContent(
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
                .background(AccentBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Reading Module",
                tint = AccentBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "IELTS READING SIMULATOR",
            color = TextLight,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Academic Reading Practice Test",
            color = TextMuted,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Instruction details
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSlateLight),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Test Instructions",
                    color = SurfaceCyan,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                InstructionRow(icon = Icons.Default.Timer, text = "Un-skippable 60-minute countdown simulator.")
                InstructionRow(icon = Icons.AutoMirrored.Filled.CompareArrows, text = "Matching Sentence Endings task ($5$ questions vs $8$ options).")
                InstructionRow(icon = Icons.Default.CheckCircle, text = "On-device grammatical cohesion check: subject-verb and syntax alignment are strictly enforced before mapping is saved.")
                InstructionRow(icon = Icons.Default.Lock, text = "Timer expiry freezes all interactions and triggers auto-grading.")
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
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f).height(50.dp)
            ) {
                Text(
                    text = "Start Assessment",
                    color = SurfaceSlate,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InstructionRow(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
private fun ActiveStateContent(
    timeLeft: Int,
    answers: Map<String, String?>,
    isFrozen: Boolean,
    passage: Passage,
    stems: List<SentenceStem>,
    onNavigateBack: () -> Unit,
    onSelectEnding: (SentenceStem) -> Unit,
    onClearEnding: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Active Header / Navigation Bar
        ActiveHeaderBar(
            timeLeft = timeLeft,
            isFrozen = isFrozen,
            onQuit = onNavigateBack,
            onSubmit = onSubmit
        )

        HorizontalDivider(color = SurfaceSlateLight, thickness = 1.dp)

        // Responsive Viewport Split
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 768.dp

            if (isCompact) {
                // Stacked Viewport for phones
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top: Passage (Isolated scroll)
                    Box(modifier = Modifier.weight(1.1f)) {
                        PassagePanel(passage = passage)
                    }

                    HorizontalDivider(color = SurfaceSlateLight, thickness = 2.dp)

                    // Bottom: Interactive nodes
                    Box(modifier = Modifier.weight(0.9f)) {
                        InteractiveQuestionsPanel(
                            stems = stems,
                            answers = answers,
                            isFrozen = isFrozen,
                            onSelectEnding = onSelectEnding,
                            onClearEnding = onClearEnding
                        )
                    }
                }
            } else {
                // Strict 1.2f to 0.8f Split Viewport for tablets/foldables
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        PassagePanel(passage = passage)
                    }

                    VerticalDivider(color = SurfaceSlateLight, thickness = 1.dp)

                    Box(modifier = Modifier.weight(0.8f)) {
                        InteractiveQuestionsPanel(
                            stems = stems,
                            answers = answers,
                            isFrozen = isFrozen,
                            onSelectEnding = onSelectEnding,
                            onClearEnding = onClearEnding
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveHeaderBar(
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
                    contentDescription = "Quit test",
                    tint = TextLight
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AEGIS IELTS Reading",
                color = TextLight,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Live Timer
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
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Submit Test",
                color = if (isFrozen) TextMuted else SurfaceSlate,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Isolated scrollable panel for the reading passage to enforce composition-skip.
 */
@Composable
private fun PassagePanel(
    passage: Passage,
    modifier: Modifier = Modifier
) {
    // Scroll state is completely isolated inside the sub-composable
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceSlate)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = "READING PASSAGE",
            color = AccentBlue,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = passage.title,
            color = TextLight,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        passage.paragraphs.forEachIndexed { index, paragraph ->
            Text(
                text = paragraph,
                color = TextLight.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 26.sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

/**
 * Isolated panel for questions and stems to enforce composition-skip.
 */
@Composable
private fun InteractiveQuestionsPanel(
    stems: List<SentenceStem>,
    answers: Map<String, String?>,
    isFrozen: Boolean,
    onSelectEnding: (SentenceStem) -> Unit,
    onClearEnding: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceSlateLight)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = "QUESTIONS 1-5",
            color = SurfaceCyan,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Complete each sentence by selecting the correct ending from the options list. Mappings are evaluated for grammatical cohesion on-device.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        stems.forEachIndexed { idx, stem ->
            val mappedEndingId = answers[stem.id]

            SentenceStemCard(
                index = idx + 1,
                stem = stem,
                mappedEndingId = mappedEndingId,
                isFrozen = isFrozen,
                onSelectClick = { onSelectEnding(stem) },
                onClearClick = { onClearEnding(stem.id) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SentenceStemCard(
    index: Int,
    stem: SentenceStem,
    mappedEndingId: String?,
    isFrozen: Boolean,
    onSelectClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(
            width = 1.dp,
            color = if (mappedEndingId != null) SurfaceCyan.copy(alpha = 0.3f) else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Stem Header Text
            Text(
                text = "$index. ${stem.text} ...",
                color = TextLight,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selection Node UI
            if (mappedEndingId == null) {
                // Interactive select CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceSlate)
                        .clickable(enabled = !isFrozen) { onSelectClick() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Select matching sentence ending...",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = SurfaceCyan
                        )
                    }
                }
            } else {
                // Mapped selection view
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSlate)
                            .clickable(enabled = !isFrozen) { onSelectClick() }
                            .border(1.dp, SurfaceCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid Match",
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getEndingTextStub(mappedEndingId),
                                color = SurfaceCyan,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2
                            )
                        }
                    }

                    if (!isFrozen) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onClearClick,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = AccentError)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear mapping"
                            )
                        }
                    }
                }
            }
        }
    }
}

// Temporary mapping for display in selection node (avoids passing complete ending pool down)
private fun getEndingTextStub(endingId: String): String {
    return when (endingId) {
        "ending_1" -> "withstand temperatures exceeding three hundred degrees."
        "ending_2" -> "offers a continuous and stable supply of electricity."
        "ending_3" -> "is the high initial capital investment required."
        "ending_4" -> "are causing environmental disruptions in local ecosystems."
        "ending_5" -> "provide crucial details about subterranean heat flow."
        "ending_6" -> "to reduce carbon emissions globally by fifty percent."
        "ending_7" -> "harnessing heat from dry hot rock formations."
        "ending_8" -> "remains highly dependent on weather patterns."
        else -> ""
    }
}

/**
 * Dialog overlay to select sentence endings.
 */
@Composable
private fun EndingPicker(
    stem: SentenceStem,
    endings: List<SentenceEnding>,
    currentSelectionId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Match Sentence Ending",
                        color = TextLight,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stem text display
                Text(
                    text = "Stem: \"${stem.text} ...\"",
                    color = SurfaceCyan,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = SurfaceSlateLight)
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable choices
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    endings.forEach { ending ->
                        val isSelected = ending.id == currentSelectionId

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SurfaceCyan.copy(alpha = 0.12f) else SurfaceSlate)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SurfaceCyan else SurfaceSlateLight,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelect(ending.id) }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = ending.text,
                                color = if (isSelected) SurfaceCyan else TextLight,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaluationCompleteContent(
    report: ReadingGradingReport,
    stems: List<SentenceStem>,
    endings: List<SentenceEnding>,
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
            text = "DIAGNOSTIC REPORT",
            color = SurfaceCyan,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Band Score Circle
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
                // Draw arc for score progress
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

        // Raw Score card
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
                    text = "${report.rawScore} / ${report.totalQuestions} Questions",
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (report.isTimeOut) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Assessment ended due to time limits.",
                color = AccentError,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Detailed Breakdown",
            color = TextLight,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Question breakdown details
        stems.forEachIndexed { index, stem ->
            val userAnsId = report.userAnswers[stem.id]
            val isCorrect = userAnsId == stem.correctAnswerId

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCorrect) AccentGreen.copy(alpha = 0.3f) else AccentError.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Question ${index + 1}",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = if (isCorrect) "Correct" else "Incorrect",
                                tint = if (isCorrect) AccentGreen else AccentError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCorrect) "Correct" else "Incorrect",
                                color = if (isCorrect) AccentGreen else AccentError,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${stem.text} ...",
                        color = TextLight,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // User Selection
                    Text(
                        text = "Your Answer: " + (userAnsId?.let { getEndingTextStub(it) } ?: "(No Answer Chosen)"),
                        color = if (isCorrect) SurfaceCyan else AccentError,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    if (!isCorrect) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Correct Answer: " + getEndingTextStub(stem.correctAnswerId),
                            color = AccentGreen,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = SurfaceSlateLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Grammatical justification
                    Text(
                        text = "Grammatical Analysis: " + getGrammarJustification(stem.expectedType),
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                border = BorderStroke(1.dp, SurfaceSlateLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.0f).height(50.dp)
            ) {
                Text("Home")
            }

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.0f).height(50.dp)
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

private fun getGrammarJustification(expectedType: ExpectedType): String {
    return when (expectedType) {
        ExpectedType.SINGULAR_VERB -> "Requires subject-verb agreement with a singular noun phrase. The selected clause starts with a singular third-person verb ('is', 'offers')."
        ExpectedType.PLURAL_VERB -> "Requires subject-verb agreement with a plural noun phrase. The selected clause starts with a plural verb ('provide')."
        ExpectedType.BASE_VERB -> "Following the modal auxiliary verb 'must', the clause expects a bare infinitive verb form ('withstand')."
        ExpectedType.GERUND -> "Following the preposition 'focused on', the complement requires a gerund ('-ing' form) to function as a noun phrase ('harnessing')."
    }
}

@Composable
private fun ErrorStateContent(
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


