package venturewave.one.gridgames.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import grid_games_mobile.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing

/**
 * Profile Screen - Player profile and stats
 *
 * Layout:
 * - Background image (urban/graffiti theme)
 * - Top bar with back button
 * - Avatar selection (4 colorful avatars)
 * - Player name
 * - Stats cards (games played, favorite pattern, best score)
 * - Badges/achievements section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedAvatar by remember { mutableStateOf(0) }
    var playerName by remember { mutableStateOf("Player") }
    var isEditingName by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(Res.drawable.mainmenu_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        // Dark overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07131F).copy(alpha = 0.3f))
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with back button
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        color = BallStarsColor.TextPrimary,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
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

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = BallStarsSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(BallStarsSpacing.md))

                // Avatar selection section
                AvatarSelectionSection(
                    selectedAvatar = selectedAvatar,
                    onAvatarSelected = { selectedAvatar = it }
                )

                Spacer(modifier = Modifier.height(BallStarsSpacing.lg))

                // Player name
                PlayerNameSection(
                    playerName = playerName,
                    isEditing = isEditingName,
                    onEditClick = { isEditingName = true },
                    onNameChanged = { playerName = it },
                    onEditComplete = { isEditingName = false }
                )

                Spacer(modifier = Modifier.height(BallStarsSpacing.xl))

                // Stats cards
                StatsSection()

                Spacer(modifier = Modifier.height(BallStarsSpacing.xl))

                // Badges section
                BadgesSection()

                Spacer(modifier = Modifier.height(BallStarsSpacing.xxl))
            }
        }
    }
}

/**
 * Avatar selection with 4 colorful avatars
 */
@Composable
private fun AvatarSelectionSection(
    selectedAvatar: Int,
    onAvatarSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF08263A).copy(alpha = 0.5f),
        border = BorderStroke(2.dp, BallStarsColor.GlowCyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(BallStarsSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose Your Avatar",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = BallStarsColor.TextPrimary
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Avatar options in a row
            val avatars = listOf(
                Res.drawable.avatar_green,
                Res.drawable.avatar_orange,
                Res.drawable.avatar_purple,
                Res.drawable.avatar_white
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                avatars.forEachIndexed { index, avatar ->
                    AvatarOption(
                        avatar = avatar,
                        isSelected = selectedAvatar == index,
                        onClick = { onAvatarSelected(index) }
                    )
                }
            }
        }
    }
}

/**
 * Individual avatar option
 */
@Composable
private fun AvatarOption(
    avatar: org.jetbrains.compose.resources.DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(if (isSelected) 90.dp else 70.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) BallStarsColor.GlowCyan.copy(alpha = 0.3f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Image(
            painter = painterResource(avatar),
            contentDescription = "Avatar",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(BallStarsColor.GlowCyan.copy(alpha = 0.2f))
            )
        }
    }
}

/**
 * Player name section with edit capability
 */
@Composable
private fun PlayerNameSection(
    playerName: String,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onNameChanged: (String) -> Unit,
    onEditComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF08263A),
        border = BorderStroke(2.dp, BallStarsColor.GlowCyan)
    ) {
        Column(
            modifier = Modifier.padding(BallStarsSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isEditing) {
                TextField(
                    value = playerName,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BallStarsColor.Surface,
                        unfocusedContainerColor = BallStarsColor.Surface,
                        focusedTextColor = BallStarsColor.TextPrimary,
                        unfocusedTextColor = BallStarsColor.TextPrimary,
                        cursorColor = BallStarsColor.GlowCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onEditComplete) {
                    Text("Done", color = BallStarsColor.GlowCyan)
                }
            } else {
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = BallStarsColor.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onEditClick) {
                    Text("Edit Name", color = BallStarsColor.GlowCyan)
                }
            }
        }
    }
}

/**
 * Stats section with game statistics
 */
@Composable
private fun StatsSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.md)
    ) {
        Text(
            text = "Stats",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = BallStarsColor.TextPrimary
        )

        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BallStarsSpacing.sm)
        ) {
            StatCard(
                title = "Games\nPlayed",
                value = "12",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Best\nScore",
                value = "95%",
                modifier = Modifier.weight(1f)
            )
        }

        StatCard(
            title = "Favorite Pattern",
            value = "Cruyff Turn",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Individual stat card
 */
@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF08263A),
        border = BorderStroke(2.dp, BallStarsColor.GlowCyan.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(BallStarsSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = BallStarsColor.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = BallStarsColor.GlowCyan,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Badges/achievements section
 */
@Composable
private fun BadgesSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.md)
    ) {
        Text(
            text = "Badges",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = BallStarsColor.TextPrimary
        )

        // Badge grid
        val badges = listOf(
            Res.drawable.badge_cruyff to "Cruyff Master",
            Res.drawable.badge_heel_flick to "Heel Flick Pro",
            Res.drawable.badge_messi_triangles to "Triangle King",
            Res.drawable.badge_penguin_feet to "Penguin Expert"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.sm)
        ) {
            badges.chunked(2).forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BallStarsSpacing.sm)
                ) {
                    rowBadges.forEach { (badgeIcon, badgeName) ->
                        BadgeCard(
                            icon = badgeIcon,
                            name = badgeName,
                            isUnlocked = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if odd number
                    if (rowBadges.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Individual badge card
 */
@Composable
private fun BadgeCard(
    icon: org.jetbrains.compose.resources.DrawableResource,
    name: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF08263A),
        border = BorderStroke(
            2.dp,
            if (isUnlocked) BallStarsColor.GlowCyan.copy(alpha = 0.5f) else BallStarsColor.Outline
        )
    ) {
        Column(
            modifier = Modifier.padding(BallStarsSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = name,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
                alpha = if (isUnlocked) 1f else 0.3f
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isUnlocked) BallStarsColor.TextSecondary else BallStarsColor.TextSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
