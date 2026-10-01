# BallStars gameplay HUD — state and scoring wiring

Use the existing game/domain logic as the source of truth. The UI must observe state; it must not invent a second scoring engine.

## Pattern loop
The selected pattern repeats indefinitely while active:

```kotlin
val actualTargetIndex = currentTargetIndex % pattern.targetSequence.size
val currentTargetNumber = pattern.targetSequence[actualTargetIndex]
```

On a valid hit, advance `currentTargetIndex`, increment `hitCount`, update `lastHitTargetNumber`, and keep the existing per-iteration deduplication.

## Misses
Preserve the current miss rule. When the ball leaves the target before a correct hit, increment `missCount` and reset `currentStreak`.

## Streak
- successful hit: `currentStreak++`
- `bestStreak = max(bestStreak, currentStreak)`
- miss: `currentStreak = 0`

## Combo
Use the existing thresholds:
- streak 0–4: **1x**
- streak 5–9: **2x**
- streak 10+: **3x**

Visual treatment:
- 1x: white
- 2x: cyan
- 3x: magenta

## Stars
The existing rule is:
`earnedStars = hitCount / 5`

Award/animate a new star every fifth successful hit.

## Power-up
If the current implementation retains the existing power-up mechanic, it activates after one complete pattern iteration and lasts 5 seconds. Show the existing state rather than creating a new mechanic.

## Final score
Where the existing result flow uses the documented score calculation, preserve it:

```kotlin
val baseScore = 1000
val timeBonus = maxOf(0, (30000 - totalTimeMs) / 100).toInt()
val accuracyBonus = (accuracy * 10).roundToInt()
val score = baseScore + timeBonus + accuracyBonus
```

Accuracy:
```kotlin
val totalTargets = hitCount + missCount
val accuracy =
    if (totalTargets > 0) hitCount.toFloat() / totalTargets.toFloat() * 100f
    else 0f
```

Do not silently change this formula in the UI.

## Live `SCORE` and `LAST HIT`
The mock-up displays `SCORE 1,240` and `LAST HIT +10` as visual examples.

Wire `SCORE` to the existing live score/state if the game engine already exposes one.

For `LAST HIT`, display the actual points supplied by the existing scoring event. If the current game model does **not** award per-hit points, do **not** hardcode `+10` merely because it appears in the mock-up; show hit feedback without a fabricated points value until the domain model supplies it.

## Timed rounds
UI offers only:
- 30 seconds
- 60 seconds

Once selected:
1. show the 10 → 1 start countdown,
2. show `GO!`,
3. enable scoring,
4. count the selected round timer down to zero,
5. end through the existing game-completion/result path.

The countdown itself must not register scoring.
