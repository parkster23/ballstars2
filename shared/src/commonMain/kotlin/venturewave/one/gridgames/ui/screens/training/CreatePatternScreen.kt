@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package venturewave.one.gridgames.ui.screens.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import grid_games_mobile.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.data.repository.PatternRepository
import venturewave.one.gridgames.data.storage.FileStorageProvider
import venturewave.one.gridgames.model.TrainingPattern
import venturewave.one.gridgames.ui.components.pattern.DifficultyStars
import venturewave.one.gridgames.ui.components.pattern.MiniGridPreview
import venturewave.one.gridgames.ui.components.navigation.FloatingNavIcons
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing
import venturewave.one.gridgames.platform.exitApp
import kotlin.uuid.Uuid

/**
 * Screen for creating custom training patterns
 *
 * Users can:
 * - Enter pattern name and description
 * - Select difficulty level (1-5 stars)
 * - Tap out sequence on interactive 3x3 grid
 * - Preview their pattern
 * - Save to persist as custom pattern
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePatternScreen(
    onPatternCreated: () -> Unit,
    onBack: () -> Unit = {},
    onHome: () -> Unit = {},
    onProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var patternName by remember { mutableStateOf("") }
    var patternDescription by remember { mutableStateOf("") }
    var difficultyLevel by remember { mutableIntStateOf(1) }
    var selectedSequence by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val repository = remember { PatternRepository(FileStorageProvider.get()) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background image matching training selection screen
        Image(
            painter = painterResource(Res.drawable.mainmenu_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top navigation
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BallStarsColor.GlowCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Heading (full width)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BallStarsSpacing.lg),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF08263A),
                border = BorderStroke(2.dp, BallStarsColor.GlowCyan)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Custom Pattern",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = BallStarsColor.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Scrollable form content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = BallStarsSpacing.lg, vertical = BallStarsSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.md)
            ) {
                // Pattern Name
                item {
                    FormField(
                        label = "Pattern Name",
                        value = patternName,
                        onValueChange = { patternName = it },
                        placeholder = "e.g. My Custom Pattern"
                    )
                }

                // Pattern Description
                item {
                    FormField(
                        label = "Description",
                        value = patternDescription,
                        onValueChange = { patternDescription = it },
                        placeholder = "Describe your pattern...",
                        minLines = 3
                    )
                }

                // Difficulty Level
                item {
                    DifficultySelector(
                        selectedDifficulty = difficultyLevel,
                        onDifficultySelected = { difficultyLevel = it }
                    )
                }

                // Interactive Grid
                item {
                    InteractiveGridSection(
                        sequence = selectedSequence,
                        onSequenceChanged = { selectedSequence = it }
                    )
                }

                // Preview
                if (selectedSequence.isNotEmpty()) {
                    item {
                        PatternPreviewSection(sequence = selectedSequence)
                    }
                }

                // Error message
                if (showError) {
                    item {
                        Text(
                            text = errorMessage,
                            color = BallStarsColor.Error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = BallStarsSpacing.sm)
                        )
                    }
                }

                // Save Button
                item {
                    Button(
                        onClick = {
                            // Validate
                            when {
                                patternName.isBlank() -> {
                                    showError = true
                                    errorMessage = "Please enter a pattern name"
                                }
                                patternDescription.isBlank() -> {
                                    showError = true
                                    errorMessage = "Please enter a description"
                                }
                                selectedSequence.size < 3 -> {
                                    showError = true
                                    errorMessage = "Pattern must have at least 3 positions"
                                }
                                else -> {
                                    showError = false
                                    // Save pattern
                                    scope.launch {
                                        try {
                                            val newPattern = TrainingPattern(
                                                id = Uuid.random().toString(),
                                                name = patternName,
                                                description = patternDescription,
                                                targetSequence = selectedSequence,
                                                difficultyLevel = difficultyLevel,
                                                estimatedDuration = selectedSequence.size * 2, // 2 seconds per position
                                                isCustom = true
                                            )
                                            repository.addPattern(newPattern)
                                            onPatternCreated()
                                        } catch (e: Exception) {
                                            showError = true
                                            errorMessage = "Failed to save pattern: ${e.message}"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BallStarsColor.GlowCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Pattern",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = BallStarsColor.BgDeep
                        )
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(BallStarsSpacing.lg))
                }
            }
        }

        // Floating navigation icons (top-right)
        FloatingNavIcons(
            onHomeClick = onHome,
            onProfileClick = onProfile,
            onExitClick = { exitApp() },
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * Form field component
 */
@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = BallStarsColor.TextPrimary
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = BallStarsColor.TextSecondary.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BallStarsColor.Surface,
                unfocusedContainerColor = BallStarsColor.Surface,
                focusedTextColor = BallStarsColor.TextPrimary,
                unfocusedTextColor = BallStarsColor.TextPrimary,
                cursorColor = BallStarsColor.GlowCyan,
                focusedIndicatorColor = BallStarsColor.GlowCyan,
                unfocusedIndicatorColor = BallStarsColor.Outline
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = minLines
        )
    }
}

/**
 * Difficulty selector with star rating
 */
@Composable
private fun DifficultySelector(
    selectedDifficulty: Int,
    onDifficultySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Difficulty Level",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = BallStarsColor.TextPrimary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..5).forEach { level ->
                Surface(
                    onClick = { onDifficultySelected(level) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedDifficulty == level) BallStarsColor.GlowCyan else BallStarsColor.Surface,
                    border = BorderStroke(
                        1.dp,
                        if (selectedDifficulty == level) BallStarsColor.GlowCyan else BallStarsColor.Outline
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = level.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedDifficulty == level) BallStarsColor.BgDeep else BallStarsColor.TextPrimary
                        )
                    }
                }
            }
        }

        // Show star preview
        DifficultyStars(rating = selectedDifficulty)
    }
}

/**
 * Interactive 3x3 grid where users tap to create sequence
 */
@Composable
private fun InteractiveGridSection(
    sequence: List<Int>,
    onSequenceChanged: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tap Grid to Create Sequence",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = BallStarsColor.TextPrimary
        )

        Text(
            text = "Tap positions in the order you want them lit. Minimum 3 positions.",
            style = MaterialTheme.typography.bodySmall,
            color = BallStarsColor.TextSecondary
        )

        // 3x3 Grid
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BallStarsColor.Surface,
            border = BorderStroke(2.dp, BallStarsColor.Outline)
        ) {
            Column(
                modifier = Modifier.padding(BallStarsSpacing.md),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..2) {
                            val position = row * 3 + col
                            val sequenceIndex = sequence.indexOf(position)
                            val isInSequence = sequenceIndex != -1

                            GridCell(
                                position = position,
                                sequenceNumber = if (isInSequence) sequenceIndex + 1 else null,
                                onClick = {
                                    if (isInSequence) {
                                        // Remove from sequence
                                        onSequenceChanged(sequence.filter { it != position })
                                    } else {
                                        // Add to sequence
                                        onSequenceChanged(sequence + position)
                                    }
                                }
                            )
                        }
                    }
                }

                // Clear button
                if (sequence.isNotEmpty()) {
                    TextButton(
                        onClick = { onSequenceChanged(emptyList()) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Clear Sequence",
                            color = BallStarsColor.GlowCyan
                        )
                    }
                }
            }
        }

        // Sequence count
        Text(
            text = "Sequence length: ${sequence.size} position${if (sequence.size != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = BallStarsColor.TextSecondary
        )
    }
}

/**
 * Individual grid cell
 */
@Composable
private fun GridCell(
    position: Int,
    sequenceNumber: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
                if (sequenceNumber != null) BallStarsColor.GlowCyan else Color(0xFF1A3A4A)
            )
            .border(
                width = 2.dp,
                color = if (sequenceNumber != null) BallStarsColor.GlowCyan else BallStarsColor.Outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (sequenceNumber != null) {
            Text(
                text = sequenceNumber.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = BallStarsColor.BgDeep
            )
        }
    }
}

/**
 * Pattern preview section
 */
@Composable
private fun PatternPreviewSection(
    sequence: List<Int>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = BallStarsColor.TextPrimary
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = BallStarsColor.Surface,
            border = BorderStroke(1.dp, BallStarsColor.Outline)
        ) {
            Row(
                modifier = Modifier.padding(BallStarsSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(BallStarsSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniGridPreview(
                    sequence = sequence,
                    size = 80.dp,
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    text = "This is how your pattern will appear in the list",
                    style = MaterialTheme.typography.bodySmall,
                    color = BallStarsColor.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
