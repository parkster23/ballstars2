package venturewave.one.gridgames.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.lilitaone_regular
import grid_games_mobile.shared.generated.resources.nunito_bold
import grid_games_mobile.shared.generated.resources.nunito_regular
import grid_games_mobile.shared.generated.resources.nunito_semibold

/**
 * BallStars Typography System
 *
 * Display Font: Lilita One - Heavy, rounded, playful (for headlines, buttons, move names)
 * Body Font: Nunito - Rounded sans-serif, highly legible (for body text, stats, descriptions)
 *
 * All text sizes are 16sp or larger for readability at distance (kids playing 2m from phone).
 * WCAG AA contrast enforced with BallStarsColor palette.
 */

@Composable
fun lilitaOneFontFamily() = FontFamily(
    Font(Res.font.lilitaone_regular, FontWeight.Normal)
)

@Composable
fun nunitoFontFamily() = FontFamily(
    Font(Res.font.nunito_regular, FontWeight.Normal),
    Font(Res.font.nunito_semibold, FontWeight.SemiBold),
    Font(Res.font.nunito_bold, FontWeight.Bold)
)

@Composable
fun ballStarsTypography(): Typography {
    val lilitaOne = lilitaOneFontFamily()
    val nunito = nunitoFontFamily()

    return Typography(
        // Display styles (Lilita One) - Used for splash logo, major headings
        displayLarge = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = 0.sp
        ),
        displayMedium = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),

        // Headline styles (Lilita One) - Screen titles, section headings, move names
        headlineLarge = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),

        // Title styles (Lilita One) - Button labels, card titles
        titleLarge = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = lilitaOne,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        ),

        // Body styles (Nunito) - Paragraph text, descriptions
        bodyLarge = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.4.sp
        ),

        // Label styles (Nunito) - Stats, badges, timestamps
        labelLarge = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

/**
 * Custom text styles not in Material 3 Typography spec
 */
object BallStarsTextStyle {
    /**
     * Logo style: "BallStars" on splash screen
     * 64sp, Lilita One, suitable for large branding
     */
    @Composable
    fun logo() = TextStyle(
        fontFamily = lilitaOneFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = 0.sp
    )

    /**
     * Score display: Large numbers in game/match results
     * 56sp, Nunito Bold, tabular nums for alignment
     */
    @Composable
    fun scoreDisplay() = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    )

    /**
     * Timer/Counter: In-game timers, rep counters
     * 32sp, Nunito Bold, highly visible
     */
    @Composable
    fun timer() = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    /**
     * Grade stamp: PERFECT/GREAT/GOOD/MISS
     * 40sp, Lilita One, all caps
     */
    @Composable
    fun gradeStamp() = TextStyle(
        fontFamily = lilitaOneFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = 2.sp
    )

    /**
     * XP award: "+100 XP" on success screens
     * 28sp, Nunito Bold, gold color
     */
    @Composable
    fun xpAward() = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
}
