package venturewave.one.gridgames.ui.screens.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import grid_games_mobile.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch
import venturewave.one.gridgames.data.repository.PatternRepository
import venturewave.one.gridgames.data.storage.FileStorageProvider
import venturewave.one.gridgames.model.TrainingPattern
import venturewave.one.gridgames.ui.components.pattern.DifficultyStars
import venturewave.one.gridgames.ui.components.pattern.MiniGridPreview
import venturewave.one.gridgames.ui.components.navigation.FloatingNavIcons
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing
import venturewave.one.gridgames.platform.exitApp

/**
 * Difficulty filter options
 */
enum class DifficultyFilter {
    ALL, BEGINNER, INTERMEDIATE, ADVANCED
}

/**
 * Maps pattern ID to thumbnail drawable resource
 */
private fun getPatternThumbnail(patternId: String): DrawableResource {
    return when (patternId) {
        "triangles" -> Res.drawable.pattern_thumb_triangles
        "the_cruff" -> Res.drawable.pattern_thumb_cruff
        "rollers" -> Res.drawable.pattern_thumb_rollers
        "penguin_feet" -> Res.drawable.pattern_thumb_penguin_feet
        "vs" -> Res.drawable.pattern_thumb_vs
        "box_run" -> Res.drawable.pattern_thumb_box_run
        "zig_zag" -> Res.drawable.pattern_thumb_zig_zag
        "cross_pattern" -> Res.drawable.pattern_thumb_cross_pattern
        "diamond" -> Res.drawable.pattern_thumb_diamond
        "figure_8" -> Res.drawable.pattern_thumb_figure_8
        "ladder" -> Res.drawable.pattern_thumb_ladder
        "spiral" -> Res.drawable.pattern_thumb_spiral
        "corner_to_centre" -> Res.drawable.pattern_thumb_corner_to_centre
        "edge_to_centre" -> Res.drawable.pattern_thumb_edge_to_centre
        "combo_flow" -> Res.drawable.pattern_thumb_combo_flow
        else -> Res.drawable.pattern_thumb_triangles // fallback
    }
}

/**
 * Training Pattern Selection Screen
 *
 * Modern BallStars design with:
 * - Header artwork (boy + girl)
 * - Difficulty filter chips
 * - Pattern cards with thumbnails, info, and mini grid previews
 * - Dynamic Canvas-based grid rendering
 *
 * @param onPatternSelected Callback when pattern is selected
 * @param onBack Callback to navigate back
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPatternSelectionScreen(
    onPatternSelected: (TrainingPattern) -> Unit,
    onBack: () -> Unit = {},
    onCreatePattern: () -> Unit = {},
    onHome: () -> Unit = {},
    onProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(DifficultyFilter.ALL) }
    var allPatterns by remember { mutableStateOf<List<TrainingPattern>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val repository = remember { PatternRepository(FileStorageProvider.get()) }

    // Load patterns from JSON file storage
    fun loadPatterns() {
        scope.launch {
            try {
                println("Loading patterns from JSON storage...")
                allPatterns = repository.getAllPatterns()
                println("Loaded ${allPatterns.size} patterns")
                isLoading = false
            } catch (e: Exception) {
                println("Error loading patterns: ${e.message}")
                e.printStackTrace()
                // Fallback to bundled patterns
                allPatterns = TrainingPattern.getBundledPatterns()
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPatterns()
    }

    // Filter patterns based on difficulty
    val filteredPatterns = when (selectedFilter) {
        DifficultyFilter.ALL -> allPatterns
        DifficultyFilter.BEGINNER -> allPatterns.filter { it.difficultyLevel == 1 }
        DifficultyFilter.INTERMEDIATE -> allPatterns.filter { it.difficultyLevel in 2..3 }
        DifficultyFilter.ADVANCED -> allPatterns.filter { it.difficultyLevel >= 4 }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background image matching main menu
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
            // Top navigation with back button
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

            // Heading with background lozenge (full width)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BallStarsSpacing.lg),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF08263A), // Dark blue
                border = BorderStroke(2.dp, BallStarsColor.GlowCyan)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select Training Pattern",
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

            // Difficulty filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = BallStarsSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(BallStarsSpacing.sm)
            ) {
                item {
                    DifficultyFilterChip(
                        label = "All",
                        selected = selectedFilter == DifficultyFilter.ALL,
                        onClick = { selectedFilter = DifficultyFilter.ALL }
                    )
                }
                item {
                    DifficultyFilterChip(
                        label = "Beginner",
                        selected = selectedFilter == DifficultyFilter.BEGINNER,
                        onClick = { selectedFilter = DifficultyFilter.BEGINNER }
                    )
                }
                item {
                    DifficultyFilterChip(
                        label = "Intermediate",
                        selected = selectedFilter == DifficultyFilter.INTERMEDIATE,
                        onClick = { selectedFilter = DifficultyFilter.INTERMEDIATE }
                    )
                }
                item {
                    DifficultyFilterChip(
                        label = "Advanced",
                        selected = selectedFilter == DifficultyFilter.ADVANCED,
                        onClick = { selectedFilter = DifficultyFilter.ADVANCED }
                    )
                }
            }

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Pattern list
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = BallStarsSpacing.lg, vertical = BallStarsSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.md)
            ) {
                items(
                    items = filteredPatterns,
                    key = { it.id }
                ) { pattern ->
                    TrainingPatternCard(
                        pattern = pattern,
                        onClick = { onPatternSelected(pattern) },
                        onDelete = if (pattern.isCustom) {
                            {
                                scope.launch {
                                    try {
                                        repository.deletePattern(pattern.id)
                                        loadPatterns()
                                    } catch (e: Exception) {
                                        println("Error deleting pattern: ${e.message}")
                                    }
                                }
                            }
                        } else null
                    )
                }
            }
        }

        // Floating Action Button for creating new pattern
        FloatingActionButton(
            onClick = onCreatePattern,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(BallStarsSpacing.lg),
            containerColor = BallStarsColor.GlowCyan,
            contentColor = BallStarsColor.BgDeep
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Pattern"
            )
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
 * Difficulty filter chip component
 */
@Composable
private fun DifficultyFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) BallStarsColor.GlowCyan else BallStarsColor.Surface
    val textColor = if (selected) BallStarsColor.BgDeep else BallStarsColor.TextSecondary
    val borderColor = if (selected) BallStarsColor.GlowCyan else BallStarsColor.Outline

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Training pattern card component
 *
 * Layout: [MiniGridPreview] [Name/Description/Difficulty/Duration] [Delete (if custom)]
 */
@Composable
private fun TrainingPatternCard(
    pattern: TrainingPattern,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = BallStarsColor.Surface,
        border = BorderStroke(4.dp, BallStarsColor.GlowCyan)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BallStarsSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(BallStarsSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Mini grid preview showing pattern
            MiniGridPreview(
                sequence = pattern.targetSequence,
                size = 80.dp,
                modifier = Modifier.size(80.dp)
            )

            // RIGHT: Pattern info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pattern name with custom badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pattern.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BallStarsColor.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (pattern.isCustom) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BallStarsColor.GlowCyan.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BallStarsColor.GlowCyan)
                        ) {
                            Text(
                                text = "CUSTOM",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BallStarsColor.GlowCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = pattern.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = BallStarsColor.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Difficulty and duration
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyStars(rating = pattern.difficultyLevel)

                    Text(
                        text = "${pattern.estimatedDuration}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = BallStarsColor.GlowCyan
                    )
                }
            }

            // Delete button for custom patterns
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Pattern",
                        tint = BallStarsColor.Error
                    )
                }
            }
        }
    }
}
