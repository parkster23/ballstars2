package venturewave.one.gridgames.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * Top navigation bar component
 *
 * Displays home and profile icons overlaid on the top left of screens.
 * Should appear on all screens except the Splash screen.
 *
 * @param onHomeClick Callback when home icon is clicked
 * @param onProfileClick Callback when profile icon is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
fun TopNavBar(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home icon button
        IconButton(
            onClick = onHomeClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = BallStarsColor.Surface.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = BallStarsColor.TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Profile icon button
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = BallStarsColor.Surface.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = BallStarsColor.TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
