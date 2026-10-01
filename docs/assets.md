# BallStars Assets Inventory & Specifications

**Version:** 1.0
**Date:** 2026-09-25
**Project:** Grid Games Mobile → BallStars UI Reskin

---

## Asset Directory Structure

```
shared/src/commonMain/composeResources/
├── drawable/
│   ├── onboarding_1.png
│   ├── onboarding_2.png
│   ├── onboarding_3.png
│   ├── onboarding_4.png
│   ├── hero_tracking.png
│   ├── empty_state_ball.png
│   ├── splash_background.png
│   ├── avatar_1.png
│   ├── avatar_2.png
│   ├── avatar_3.png
│   ├── avatar_4.png
│   ├── avatar_5.png
│   ├── avatar_6.png
│   ├── avatar_7.png
│   ├── avatar_8.png
│   ├── badge_first_move.png
│   ├── badge_10_streak.png
│   ├── badge_combo_master.png
│   ├── badge_speed_demon.png
│   ├── badge_all_moves.png
│   └── badge_level_10.png
├── drawable-xxxhdpi/
│   └── app_icon.png
├── drawable-xxhdpi/
│   └── app_icon.png
├── drawable-xhdpi/
│   └── app_icon.png
├── drawable-hdpi/
│   └── app_icon.png
├── drawable-mdpi/
│   └── app_icon.png
└── font/
    ├── lilitaone_regular.ttf
    ├── nunito_regular.ttf
    ├── nunito_semibold.ttf
    └── nunito_bold.ttf
```

---

## 1. Onboarding Illustrations

### Onboarding Page 1
- **Filename:** `onboarding_1.png`
- **Size:** 1080 × 2400 px (full screen, 9:20 aspect)
- **Format:** PNG with transparency
- **Content:** Street football scene with kids on cage pitch, graffiti walls
- **Key Elements:**
  - BallStars logo (top third)
  - "Real moves. Tracked. Rewarded." tagline
  - Cartoon kid dribbling ball with neon grid floor effect
  - Urban background (cage fence, graffiti, buildings)
  - Clear space at bottom 400px for green "Get Started" button
- **Color Palette:** Match design reference (navy bg, cyan grid, vibrant characters)
- **Style:** Cartoon/illustrated, not photographic, kid-friendly
- **Reference:** Screenshot 2 from `ballstars-reference.png`

### Onboarding Page 2
- **Filename:** `onboarding_2.png`
- **Size:** 1080 × 2400 px
- **Content:** "Move your way" - Kid doing street football trick
- **Key Elements:**
  - Headline: "Move your way" (cyan color, Lilita One font)
  - Subtext: "Do real street-football moves, and get them tracked by the camera."
  - Illustration: Kid mid-trick with ball, multiple kids in background
  - Neon grid on ground
  - Urban setting (apartment buildings, street art)
  - Bottom button space: 400px
- **Reference:** Screenshot 3 from design reference

### Onboarding Page 3
- **Filename:** `onboarding_3.png`
- **Size:** 1080 × 2400 px
- **Content:** "Level up" - Kid with stars and targets
- **Key Elements:**
  - Headline: "Level up" (cyan)
  - Subtext: "Hit targets, earn stars, and unlock new moves."
  - Illustration: Kid celebrating with golden stars floating around
  - Neon grid with visible target zones
  - Graffiti wall background
  - Bottom button space: 400px
- **Reference:** Screenshot 4 from design reference

### Onboarding Page 4
- **Filename:** `onboarding_4.png`
- **Size:** 1080 × 2400 px
- **Content:** "Better together" - Group of kids
- **Key Elements:**
  - Headline: "Better together" (cyan)
  - Subtext: "Compete, share, and show off your skills."
  - Illustration: 3-4 diverse kids together with phone, fist bumps, smiles
  - Casual setting (street, cage pitch visible in background)
  - Ball in foreground
  - Bottom button space: 400px
- **Reference:** Screenshot 5 from design reference

**Placeholder Strategy (Phase 2):**
- Gradient background (radial, navy to deep blue)
- Neon grid lines (cyan, 3×3) in center
- Simple football icon (white circle with pentagon pattern)
- Text overlays with theme colors
- Generate programmatically with Canvas API

---

## 2. App Icon

### Icon Variants (Android)
- **Base Design:** Football with orange flame trail on neon grid background, circular
- **Formats Required:**
  - `xxxhdpi`: 192 × 192 px
  - `xxhdpi`: 144 × 144 px
  - `xhdpi`: 96 × 96 px
  - `hdpi`: 72 × 72 px
  - `mdpi`: 48 × 48 px
- **Adaptive Icon (Android 8+):**
  - Foreground: Football with flame trail (transparent background)
  - Background: Neon grid on navy gradient
  - Safe zone: Center 66% must contain key visual
- **Export:** PNG-24 with transparency for foreground, solid for background
- **Reference:** Screenshot 1 (splash icon) from design reference

**Placeholder (Phase 2):**
- Green circle with white football icon (Material Icon: `sports_soccer`)
- Navy background
- No animation

---

## 3. Avatars

### Avatar Grid (8 Options)
- **Filenames:** `avatar_1.png` to `avatar_8.png`
- **Size:** 200 × 200 px (circular crop, transparent background)
- **Format:** PNG with alpha channel
- **Content:** Cartoon kid faces/busts, diverse representation
- **Diversity Requirements:**
  - 4 male-presenting, 4 female-presenting
  - Range of skin tones (4+ represented)
  - Various hairstyles, hair colors
  - Different expressions (smiles, focus, playful)
- **Style:** Match onboarding illustration style
- **No:** Real photos, licensed IP characters, text labels
- **Reference:** Profile avatar from screenshot 8 in design reference (Jamie example)

**Avatar Details:**
1. **Avatar 1:** Light skin, short brown hair, big smile, green shirt
2. **Avatar 2:** Medium-dark skin, curly black hair, focused expression, blue shirt
3. **Avatar 3:** Light-medium skin, long blonde ponytail, playful smile, orange shirt
4. **Avatar 4:** Dark skin, short black hair, determined look, white shirt
5. **Avatar 5:** Medium skin, short black hair with headband, excited, yellow shirt
6. **Avatar 6:** Light skin, medium brown hair, calm smile, purple shirt
7. **Avatar 7:** Dark skin, braids, happy expression, cyan shirt
8. **Avatar 8:** Medium-light skin, short red hair, grin, green shirt

**Placeholder (Phase 2):**
- Material Icons `account_circle` in different colors
- Circular background with theme colors
- No facial features, just colored circles

---

## 4. Badges

### Badge Icons (6 Core Badges)
- **Size:** 120 × 120 px
- **Format:** PNG with transparency
- **Style:** Flat icon with subtle gradient, gold/cyan accents
- **Background:** Transparent or subtle glow

#### Badge 1: First Move
- **Filename:** `badge_first_move.png`
- **Icon:** Football with single star above
- **Color:** Gold primary, white star
- **Unlock:** Complete first move drill

#### Badge 2: 10 Streak
- **Filename:** `badge_10_streak.png`
- **Icon:** Flame with "10" number
- **Color:** Orange/flame gradient
- **Unlock:** Achieve 10-hit streak in any mode

#### Badge 3: Combo Master
- **Filename:** `badge_combo_master.png`
- **Icon:** Three glyphs in chain with multiplier "×3"
- **Color:** Cyan and gold
- **Unlock:** Land a 3-move combo in match mode

#### Badge 4: Speed Demon
- **Filename:** `badge_speed_demon.png`
- **Icon:** Stopwatch with lightning bolt
- **Color:** Yellow and white
- **Unlock:** Complete Messi Triangles in under 3 seconds

#### Badge 5: All Moves Learned
- **Filename:** `badge_all_moves.png`
- **Icon:** Trophy with football inside
- **Color:** Gold with cyan accents
- **Unlock:** Complete drill for all 9 bundled moves

#### Badge 6: Level 10
- **Filename:** `badge_level_10.png`
- **Icon:** Star with "10" in center
- **Color:** Gold with white glow
- **Unlock:** Reach Level 10

**Placeholder (Phase 2):**
- Material Icons: `emoji_events` (trophy), `whatshot` (flame), `star`, `timer`, `sports_soccer`, `military_tech`
- Circular background with gradient (gold or cyan)
- Simple, no detail

---

## 5. Hero & Empty States

### Hero Tracking Image
- **Filename:** `hero_tracking.png`
- **Size:** 1080 × 1080 px (square)
- **Format:** PNG with transparency
- **Content:** Camera view with neon grid, kid about to kick ball
- **Usage:** Background for live tracking screen, mode select cards
- **Key Elements:**
  - Ground-level camera perspective
  - Neon cyan 3×3 grid on floor
  - Ball in view, mid-motion blur
  - Kid's legs/feet visible (not full body to avoid focus on person)
  - Outdoor setting (pavement, cage fence edge visible)
- **Reference:** Screenshot 7 (live tracking) from design reference

**Placeholder (Phase 2):**
- Radial gradient (dark navy center to deep blue edge)
- Neon grid overlay (3×3, cyan lines)
- White circle (ball) with subtle glow

### Empty State Ball
- **Filename:** `empty_state_ball.png`
- **Size:** 400 × 400 px
- **Format:** PNG with transparency
- **Content:** Cartoon football with question mark or shrug gesture
- **Usage:** "No moves yet" empty state screen
- **Style:** Playful, not sad - encouraging tone
- **Reference:** Screenshot 9 (empty state) from design reference

**Placeholder (Phase 2):**
- White circle with pentagon pattern (football icon)
- Cyan glow
- No expression, simple icon

### Splash Background
- **Filename:** `splash_background.png`
- **Size:** 1080 × 2400 px
- **Format:** PNG (can be JPEG if no transparency)
- **Content:** Burst background with neon grid and ball
- **Key Elements:**
  - Dark navy radial gradient
  - Gold/cyan burst rays from center
  - Neon grid square (300×300px) in center
  - Football with glow in grid center
  - Space for logo overlay at top third
- **Reference:** Screenshot 1 (splash) from design reference

**Placeholder (Phase 2):**
- Programmatic Canvas with radial gradient
- Gold burst lines (8 rays, 60° spacing)
- Cyan grid square
- White circle (ball)

---

## 6. Fonts

### Lilita One (Display Font)
- **Source:** Google Fonts (Open Font License)
- **Filename:** `lilitaone_regular.ttf`
- **URL:** https://fonts.google.com/specimen/Lilita+One
- **Weights:** Regular only (400)
- **File Size:** ~120 KB
- **Usage:** Logo, headings, button labels, move names
- **Fallback:** System bold (Roboto Black on Android)

**Integration:**
```kotlin
// In Typography.kt
val LilitaOne = FontFamily(
    Font(Res.font.lilitaone_regular, FontWeight.Normal)
)
```

### Nunito (Body Font)
- **Source:** Google Fonts (Open Font License)
- **Filenames:**
  - `nunito_regular.ttf` (400 weight)
  - `nunito_semibold.ttf` (600 weight)
  - `nunito_bold.ttf` (700 weight)
- **URL:** https://fonts.google.com/specimen/Nunito
- **File Sizes:** ~80 KB each
- **Usage:** Body text, stats, descriptions, timestamps
- **Fallback:** System regular (Roboto on Android)

**Integration:**
```kotlin
val Nunito = FontFamily(
    Font(Res.font.nunito_regular, FontWeight.Normal),
    Font(Res.font.nunito_semibold, FontWeight.SemiBold),
    Font(Res.font.nunito_bold, FontWeight.Bold)
)
```

---

## 7. Asset Delivery Checklist

### Phase 2 (Theme & Components)
- [ ] Download Lilita One Regular from Google Fonts
- [ ] Download Nunito Regular, SemiBold, Bold from Google Fonts
- [ ] Place fonts in `composeResources/font/`
- [ ] Generate placeholder onboarding images (programmatic)
- [ ] Generate placeholder avatars (Material Icons)
- [ ] Generate placeholder badges (Material Icons)
- [ ] Generate splash background (Canvas)
- [ ] Create basic app icon (green circle + football)

### Phase 3 (Navigation Shell)
- [ ] Finalize onboarding illustration style guide
- [ ] Commission/create onboarding illustrations 1-4
- [ ] Create avatar illustrations 1-8
- [ ] Design badge icons 1-6
- [ ] Replace placeholders with final assets

### Phase 4+ (Camera Screens)
- [ ] Create hero tracking image
- [ ] Create empty state ball illustration
- [ ] Update splash background with final burst design

### Polish Phase
- [ ] Create adaptive app icon (foreground + background layers)
- [ ] Export app icon in all densities (mdpi to xxxhdpi)
- [ ] Add app icon to `androidApp/src/main/res/mipmap-*/`

---

## 8. Asset Optimization

**PNG Compression:**
- Use `pngquant` or `TinyPNG` to reduce file size
- Target: <500 KB per onboarding image
- Preserve transparency for avatars and badges

**Font Subsetting (optional):**
- If bundle size is a concern, subset fonts to Latin + numbers
- Remove unused glyphs to reduce TTF file size by ~40%
- Use `pyftsubset` from fontTools

**Total Asset Budget:**
- Onboarding images: 4 × 500 KB = 2 MB
- Avatars: 8 × 50 KB = 400 KB
- Badges: 6 × 30 KB = 180 KB
- Hero images: 2 × 300 KB = 600 KB
- App icon: 6 variants × 20 KB = 120 KB
- Fonts: 4 × 100 KB = 400 KB
- **Total:** ~3.7 MB (acceptable for modern app)

---

## 9. Reference for Asset Creators

**Design Reference File:**
`docs/design/ballstars-reference.png`

**Key Screenshots:**
1. Splash screen (icon + burst)
2. Onboarding page 1 (BallStars logo)
3. Onboarding page 2 (Move your way)
4. Onboarding page 3 (Level up)
5. Onboarding page 4 (Better together)
6. Move glyphs (5 examples: Messi Triangles, The Cruyff, Penguin Feet, Step Overs, Elastico)
7. Live tracking (camera view with grid)
8. Profile card (Jamie avatar, stats)
9. Empty state (No moves yet)

**Color Palette:**
- Background: `#0A1929` (dark navy)
- Surface: `#132A42` (navy blue)
- Primary: `#1ED36A` (street green)
- Glow Cyan: `#00D9FF`
- Gold: `#FFC21A`
- Flame: `#FF7A1A`
- Text Primary: `#FFFFFF`
- Text Secondary: `#B7C7D6`

**Typography:**
- Display: Lilita One (heavy, rounded, playful)
- Body: Nunito (rounded sans-serif, legible)

**Illustration Style:**
- Cartoon/illustrated (not realistic)
- Kid-friendly, vibrant colors
- Street football aesthetic (cage pitches, graffiti, urban)
- No licensed IP characters
- Diverse representation (age, gender, ethnicity)
- Neon grid effect on ground (cyan glow)

---

## 10. Asset Loading in Code

**Drawable Resources:**
```kotlin
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.generated.resources.Res
import venturewave.one.gridgames.generated.resources.onboarding_1

@Composable
fun OnboardingPage1() {
    Image(
        painter = painterResource(Res.drawable.onboarding_1),
        contentDescription = "Kids playing street football",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}
```

**Font Resources:**
```kotlin
import org.jetbrains.compose.resources.Font
import venturewave.one.gridgames.generated.resources.lilitaone_regular

val LilitaOne = FontFamily(Font(Res.font.lilitaone_regular))
```

**Avatar Selection:**
```kotlin
@Composable
fun AvatarPicker(selectedId: String, onSelect: (String) -> Unit) {
    val avatarIds = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4",
                           "avatar_5", "avatar_6", "avatar_7", "avatar_8")
    LazyRow {
        items(avatarIds) { id ->
            val painter = painterResource(Res.drawable.getDrawableResource(id))
            Image(
                painter = painter,
                contentDescription = "Avatar $id",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(id) }
                    .border(
                        width = if (id == selectedId) 4.dp else 0.dp,
                        color = BallStarsColors.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}
```

---

**End of Assets Inventory**

**Next Steps:**
1. Await approval of ui-plan.md
2. Download fonts from Google Fonts
3. Generate programmatic placeholders for Phase 2
4. Commission/create final illustrations for Phase 3
