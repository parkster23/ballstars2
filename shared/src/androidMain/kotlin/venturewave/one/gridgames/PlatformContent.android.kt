package venturewave.one.gridgames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import venturewave.one.gridgames.ui.theme.GridGamesColors

/**
 * Android-specific best scores screen (placeholder for now)
 * Will be implemented in Phase 6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformSpecificBestScores(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Best Scores",
                        color = GridGamesColors.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GridGamesColors.NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GridGamesColors.DeepBlue,
                    titleContentColor = GridGamesColors.White
                )
            )
        },
        containerColor = GridGamesColors.DarkNavy
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GridGamesColors.DarkNavy,
                            GridGamesColors.DeepBlue
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Leaderboard Coming Soon!",
                color = GridGamesColors.NeonCyan,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
