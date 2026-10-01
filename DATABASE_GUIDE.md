# BallStars Database Guide

This guide explains the local database implementation for storing player scores, stats, and configurable training patterns in the BallStars app.

## Overview

The app uses **SQLDelight** for cross-platform local database persistence, supporting both Android and iOS. The database stores:

- **Training Patterns** - Configurable patterns with sequences like "Triangles: 5,7,4,5"
- **Game Sessions** - Individual training session results with scores and stats
- **Player Profiles** - Player information and aggregate statistics
- **Pattern Stats** - Per-pattern performance metrics for each player

## Architecture

### Database Schema

The database consists of 4 main tables:

1. **TrainingPatternEntity** - Stores training patterns
   - Pattern ID, name, description
   - Target sequence (comma-separated grid positions: "5,7,4,5")
   - Difficulty level (1-5 stars)
   - Estimated duration in seconds
   - Thumbnail resource name
   - Accent color for UI
   - Custom vs bundled flag

2. **GameSessionEntity** - Stores individual game results
   - Pattern ID (foreign key)
   - Player ID (foreign key)
   - Score, time, hits, misses, accuracy
   - Completion timestamp

3. **PlayerProfileEntity** - Stores player information
   - Player ID, display name, avatar
   - Total score, games played, time played
   - Favorite pattern, skill level

4. **PlayerStatsEntity** - Stores per-pattern stats
   - Player ID + Pattern ID (composite key)
   - Games played, best score, total score
   - Best time, best accuracy
   - Hit/miss totals, last played timestamp

### File Structure

```
shared/src/
├── commonMain/
│   ├── sqldelight/venturewave/one/gridgames/database/
│   │   ├── TrainingPattern.sq      # Pattern table & queries
│   │   ├── GameSession.sq          # Session table & queries
│   │   ├── PlayerProfile.sq        # Profile table & queries
│   │   └── PlayerStats.sq          # Stats table & queries
│   └── kotlin/venturewave/one/gridgames/
│       ├── database/
│       │   ├── DatabaseDriverFactory.kt      # Expect declaration
│       │   ├── Database.kt                   # Singleton instance
│       │   └── DatabaseInitializer.kt        # Initialization logic
│       └── data/
│           ├── GameResultManager.kt          # High-level result saving
│           └── repository/
│               ├── TrainingPatternRepository.kt
│               ├── GameSessionRepository.kt
│               └── PlayerRepository.kt
├── androidMain/kotlin/venturewave/one/gridgames/database/
│   └── DatabaseDriverFactory.android.kt      # Android SQLite driver
└── iosMain/kotlin/venturewave/one/gridgames/database/
    └── DatabaseDriverFactory.ios.kt          # iOS SQLite driver
```

## Initialization

### Android

The database is initialized in [MainActivity.kt](androidApp/src/main/kotlin/venturewave/one/gridgames/MainActivity.kt):

```kotlin
DatabaseInitializer.initialize(DatabaseDriverFactory(applicationContext))
```

This:
1. Creates the SQLite database at `ballstars.db`
2. Seeds bundled training patterns on first launch
3. Creates a default player profile

### iOS

Add this to your iOS app entry point:

```kotlin
DatabaseInitializer.initialize(DatabaseDriverFactory())
```

## Usage Examples

### 1. Configure a Training Pattern

To configure a new pattern with a sequence like "Triangle: 5, 7, 4, 5":

```kotlin
val repository = TrainingPatternRepository()

// Create a custom pattern
repository.insertPattern(
    pattern = TrainingPattern(
        id = "custom_triangle_variant",
        name = "Triangle Variant",
        description = "A custom triangle pattern",
        targetSequence = listOf(5, 7, 4, 5),  // Configurable sequence
        difficultyLevel = 2,
        estimatedDuration = 10
    ),
    thumbnailResource = "pattern_thumb_triangles",
    accentColor = "#27E6F5",
    isCustom = true  // Mark as user-created
)
```

### 2. Load Patterns in UI

The [TrainingPatternSelectionScreen](shared/src/commonMain/kotlin/venturewave/one/gridgames/ui/screens/training/TrainingPatternSelectionScreen.kt) automatically loads from database:

```kotlin
val repository = TrainingPatternRepository()

LaunchedEffect(Unit) {
    allPatterns = repository.getAllPatterns()
}
```

### 3. Draw Pattern on Selection Tile

The [MiniGridPreview](shared/src/commonMain/kotlin/venturewave/one/gridgames/ui/components/pattern/MiniGridPreview.kt) component automatically draws patterns based on the sequence:

```kotlin
MiniGridPreview(
    sequence = pattern.targetSequence,  // e.g., [5, 7, 4, 5]
    size = 56.dp
)
```

The grid is mapped as:
```
1 2 3
4 5 6
7 8 9
```

So the sequence `[5, 7, 4, 5]` draws:
- Start at center (5)
- Move to bottom-left (7)
- Move to middle-left (4)
- Return to center (5)

### 4. Show Pattern on Explainer Screen

The [PatternExplainerScreen](shared/src/commonMain/kotlin/venturewave/one/gridgames/ui/screens/training/PatternExplainerScreen.kt) shows a large pattern preview:

```kotlin
PatternExplainerScreen(
    pattern = selectedPattern,
    onStartTraining = { /* Start game */ },
    onBack = { /* Navigate back */ }
)
```

This displays:
- Pattern name and description
- Large 280dp grid preview showing the sequence
- Difficulty stars (configurable from database)
- Estimated duration (configurable from database)
- Full sequence as numbers (e.g., "5 → 7 → 4 → 5")
- Instructions

### 5. Save Game Results

After a training session, save results using [GameResultManager](shared/src/commonMain/kotlin/venturewave/one/gridgames/data/GameResultManager.kt):

```kotlin
val result = GameResult(
    pattern = selectedPattern,
    score = 1500,
    totalTime = 8500L,  // Milliseconds
    hitCount = 25,
    missCount = 3,
    accuracy = 89.3f
)

GameResultManager.saveGameResult(
    result = result,
    playerId = "default_player",
    onSuccess = {
        println("Result saved successfully!")
        // Navigate to score screen
    },
    onError = { error ->
        println("Failed to save: $error")
    }
)
```

This automatically:
- Saves the game session to database
- Updates player profile (total score, games played, time)
- Updates pattern-specific stats (best score, best time, accuracy)

### 6. Query Player Stats

```kotlin
// Get stats for a specific pattern
val stats = GameResultManager.getPatternStats("triangles")
stats?.let {
    println("Total sessions: ${it.totalSessions}")
    println("Best score: ${it.maxScore}")
    println("Average time: ${it.averageTime}ms")
    println("Best accuracy: ${it.averageAccuracy}%")
}

// Get recent sessions
val recentSessions = GameResultManager.getRecentSessions(limit = 10)
```

### 7. Get Best Scores

```kotlin
val sessionRepo = GameSessionRepository()

// Best score for a specific pattern
val bestSession = sessionRepo.getBestScoreForPattern("triangles")

// Best scores for all patterns
val allBestScores = sessionRepo.getBestScoresForAllPatterns("default_player")
```

## Configuring Patterns

### Pattern Sequence Format

Sequences are stored as comma-separated integers in the database:
- Database: `"5,7,4,5"`
- Domain model: `listOf(5, 7, 4, 5)`

The repository handles conversion automatically.

### Adding Bundled Patterns

Bundled patterns are seeded on first app launch from [TrainingPattern.getBundledPatterns()](shared/src/commonMain/kotlin/venturewave/one/gridgames/model/TrainingPattern.kt).

To add a new bundled pattern:

1. Add it to `TrainingPattern.getBundledPatterns()`:
```kotlin
TrainingPattern(
    id = "new_pattern",
    name = "New Pattern",
    description = "Description here",
    targetSequence = listOf(1, 2, 3, 6, 9),
    difficultyLevel = 2,
    estimatedDuration = 12
)
```

2. Map the thumbnail in [TrainingPatternRepository](shared/src/commonMain/kotlin/venturewave/one/gridgames/data/repository/TrainingPatternRepository.kt):
```kotlin
private fun getThumbnailResourceForPattern(patternId: String): String {
    return when (patternId) {
        "new_pattern" -> "pattern_thumb_new.png"
        // ...
    }
}
```

3. Map the accent color:
```kotlin
private fun getAccentColorForPattern(patternId: String): String {
    return when (patternId) {
        "new_pattern" -> "#27E6F5"  // Cyan
        // ...
    }
}
```

### Creating Custom Patterns at Runtime

Users can create custom patterns (this UI is not yet implemented):

```kotlin
val customPattern = TrainingPattern(
    id = "user_custom_${UUID.randomUUID()}",
    name = userInputName,
    description = userInputDescription,
    targetSequence = userSelectedSequence,  // e.g., [1, 5, 9, 5]
    difficultyLevel = userSelectedDifficulty,
    estimatedDuration = estimatedSeconds
)

repository.insertPattern(
    pattern = customPattern,
    isCustom = true
)
```

## Database Maintenance

### Reset Database

To clear all data and re-seed bundled patterns:

```kotlin
DatabaseInitializer.resetDatabase()
```

**WARNING:** This deletes all player data, scores, and custom patterns!

### Re-seed Patterns Only

To re-seed bundled patterns without clearing user data:

```kotlin
DatabaseInitializer.reseedPatterns()
```

### Query Database Directly

For debugging, you can query the database directly:

```kotlin
val db = Database.getInstance()
val patterns = db.trainingPatternEntityQueries.getAllPatterns().executeAsList()
```

## Performance Considerations

1. **Lazy Loading** - Patterns are loaded from database in `LaunchedEffect`, not blocking the UI
2. **Coroutines** - All database operations use `Dispatchers.IO`
3. **Indexed Queries** - Pattern and session queries are indexed for fast lookups
4. **Flow Support** - Repositories provide Flow-based APIs for reactive updates
5. **Caching** - The repository layer can be extended with in-memory caching if needed

## Migration Strategy

When schema changes are needed:

1. Create a new `.sq` file with `-- Migration` comments
2. Update `BallStarsDatabase.Schema` version
3. Add migration logic in `DatabaseDriverFactory`
4. Test thoroughly on both platforms

## Testing

To test database functionality:

```kotlin
// In a test or debug screen
suspend fun testDatabase() {
    val repo = TrainingPatternRepository()

    // Verify patterns loaded
    val patterns = repo.getAllPatterns()
    println("Loaded ${patterns.size} patterns")

    // Save test result
    val testResult = GameResult(...)
    GameResultManager.saveGameResult(testResult)

    // Query stats
    val stats = GameResultManager.getPatternStats("triangles")
    println("Stats: $stats")
}
```

## Troubleshooting

### "Database not initialized" Error

Ensure `DatabaseInitializer.initialize()` is called in your platform's entry point (MainActivity on Android, App.swift on iOS).

### Patterns Not Showing

1. Check database initialization logs
2. Verify seeding completed successfully
3. Try calling `DatabaseInitializer.reseedPatterns()`

### Slow Performance

1. Ensure queries use indexed columns (patternId, playerId)
2. Check that database operations are on `Dispatchers.IO`
3. Consider adding in-memory caching layer

## References

- SQLDelight Documentation: https://cashapp.github.io/sqldelight/
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html
- Compose Multiplatform: https://www.jetbrains.com/lp/compose-multiplatform/
