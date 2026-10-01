package venturewave.one.gridgames.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.ui.theme.BallStarsColor

// These screens render under the system status bar (enableEdgeToEdge), so
// floating icons need their own status-bar inset padding. Without it, the
// top portion of the touch target sits under the system status bar, which
// intercepts the touch before it reaches the app - producing a much smaller
// (or entirely non-functional) press area depending on status bar height,
// which varies by device and by the user's own Display size/Font size
// settings. Mirrors the private extension already used the same way in
// BallStarsMainMenuScreen.kt.
@Composable
private fun Modifier.statusBarsPadding(): Modifier = this.then(
    WindowInsets.statusBars.asPaddingValues().let { padding ->
        Modifier.padding(padding)
    }
)

/**
 * Floating navigation icons (Home, Profile, and Exit) displayed in the top-right corner
 *
 * Used across all screens except home/menu and splash
 */
@Composable
fun FloatingNavIcons(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onExitClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home Icon
        IconButton(
            onClick = {
                println("FloatingNavIcons: Home clicked!")
                onHomeClick()
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BallStarsColor.Surface.copy(alpha = 0.9f))
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = BallStarsColor.GlowCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        // Profile Icon
        IconButton(
            onClick = {
                println("FloatingNavIcons: Profile clicked!")
                onProfileClick()
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BallStarsColor.Surface.copy(alpha = 0.9f))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = BallStarsColor.GlowCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        // Exit Icon
        IconButton(
            onClick = {
                println("FloatingNavIcons: Exit clicked!")
                onExitClick()
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BallStarsColor.Surface.copy(alpha = 0.9f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit",
                tint = Color(0xFFFF4444), // Red tint for exit
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Floating back button, styled to match FloatingNavIcons (same 48.dp circle,
 * same surface/tint), for screens that render no other back affordance of
 * their own (e.g. no TopAppBar with a back arrow). Deliberately a separate,
 * top-LEFT-aligned composable rather than folded into FloatingNavIcons'
 * top-right row, so screens that already have their own dedicated back
 * button (e.g. a TopAppBar) never end up with two.
 *
 * Every screen except Home and Splash should offer a way back; this is the
 * lightweight option for screens that only had Home/Profile/Exit floating
 * icons and nothing else.
 */
@Composable
fun FloatingBackIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BallStarsColor.Surface.copy(alpha = 0.9f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BallStarsColor.GlowCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
