# Game Mechanics Proposal — Sticky/Addictive Scoring for Training & Freestyle Modes

This proposes a replacement for the current scoring system (documented in
`SCORING_MECHANISM.md`) that implements your two new requirements —
wrong-target cancels the pattern bonus, and a freestyle Gameplay Mode with
combo-linked patterns — while also fixing several of the dead/inert
mechanics that doc flagged along the way. Nothing here is implemented yet;
this is the design to review before I touch code.

The 13 patterns (already live in `TrainingPattern.kt`, confirmed identical
to your table) are the shared "move library" both modes below are built on:

| Pattern | Sequence | Difficulty |
|---|---|---|
| Right Peak | 5,7,4,5 | 1 |
| Left Peak | 5,9,6,5 | 1 |
| Right Cruyff | 5,4,1,3,5 | 2 |
| Left Cruyff | 5,6,3,1,5 | 2 |
| Right Drift | 1,3,9,1 | 1 |
| Left Drift | 3,1,7,3 | 1 |
| Right Box Blast | 1,7,9,3,1 | 2 |
| Left Box Blast | 3,9,7,1,3 | 2 |
| V's | 5,9,5,7,5 | 2 |
| Dap Up | 1,5,3,5,1 | 2 |
| Penguin Feet | 5,4,5,6,5 | 2 |
| Right Crossover | 1,9,7,3,1 | 2 |
| Left Crossover | 3,7,9,1,3 | 2 |

---

## 1. Training Mode — points, 3x completion bonus, wrong-target penalty

**Implemented** (as of this revision — simplified from the original "Clean
Lap" proposal below to three direct rules):

- **+10 points** for every correct target hit, awarded immediately as it
  lands — score visibly climbs live during play, not just at the end.
- **3x multiplier** on completing a full pattern (one loop through
  `pattern.targetSequence`) **without any wrong-target hit** during that
  loop. Since the 1x base is already paid out per-hit, this pays out the
  remaining 2x as a bonus the moment the loop closes clean.
- **-10 points** the instant the ball is detected in any target *other*
  than the current one (debounced so holding the ball in a wrong box only
  costs once per entry, not once per frame) — and that wrong hit also
  disqualifies the in-progress loop's 3x bonus (it still banks its 1x
  per-hit points, just no multiplier). This finally gives `missCount` a
  real meaning, fixing the old bug where `accuracy` could only ever be 0%
  or 100%.
- `ScoreScreen` shows the running totals: `Bonus (3x)` (sum of completion
  bonuses earned) and `Points Lost` (sum of wrong-target penalties), on top
  of the existing Score/Hits/Misses/Accuracy/Patterns Completed cards.

This keeps the forgiving, low-friction tone that works for kids: a mistake
costs that loop's bonus and 10 points, not your overall progress or the
game itself — you're always one clean loop away from a big 3x payout.

<details>
<summary>Original "Clean Lap" proposal (superseded)</summary>

The first pass at this built a more layered system — a difficulty-scaled
lap bonus, an escalating clean-streak multiplier, and a per-lap speed
bonus — before being simplified down to the three flat rules above per
direct follow-up feedback. Kept here for history, not current behavior:

| Component | Value | Notes |
|---|---|---|
| Lap Bonus | `150 × difficultyLevel` | Only if the lap was clean |
| Clean Streak multiplier | 1 clean lap = ×1, 2 in a row = ×1.5, 3+ in a row = ×2 (cap) | Applied to the Lap Bonus only |
| Speed Bonus | up to `+100` per lap, scaled by pace vs. `estimatedDuration` | Replaced the old inert global `timeBonus` |

</details>

---

## 2. Gameplay Mode (freestyle) — "Trick-Chain Combo" system

This is new — there's no pattern-recognition-during-freeplay logic in the
codebase today. Modeled on the trick-combo system genre games like Tony
Hawk's Pro Skater popularized: link *different* moves together for an
escalating multiplier; repeat the same move and the multiplier collapses.
That's a direct match for "evolving sequences score most, duplicating the
same pattern scores least."

**Recognition:** keep a rolling buffer of the last ~8 confirmed target
hits. After every new hit, check whether the buffer's tail exactly matches
any of the 13 known sequences. A match = a "trick" landed; clear the
matched hits from the buffer and emit it into the current chain.

**Chain rules:**
- A chain is live as long as hits keep landing within a ~3s window of each other and none of them is a wrong-target hit (same "wrong target" check as Training Mode — any wrong-target hit ends the chain immediately, it's the one hard penalty in freestyle).
- Each landed trick scores: `base = 100 × difficultyLevel`.
- **Chain multiplier** (applies to that trick's base points):

| Position in chain | Multiplier |
|---|---|
| 1st trick | ×1 |
| 2nd trick, different from the last | ×1.5 |
| 3rd trick, different from the last | ×2 |
| 4th+ trick, different from the last | ×3 (cap) |
| Any trick that repeats the immediately preceding one | multiplier drops back to **×1** regardless of chain length |

- **Flow Bonus:** if the last 3+ tricks in the chain are all distinct (no repeats anywhere in that window), add a flat `+250` on top — rewards genuine variety, not just raw chain length.
- When the chain ends (timeout, wrong-target, or session timer), the chain's total is banked and a recap banner shows ("5-trick chain — Right Peak → V's → Dap Up → Left Drift → Right Cruyff — +1,850").

**Combo highlights (your explicit ask):** fire an escalating celebration at
chain milestones — these are cheap to build and are the single highest-
leverage "addictive" lever here, since variable, escalating positive
feedback is what actually drives repeat play. Sound effects only, no
haptics — a vibration buzz would physically move the phone mid-shot and
throw off ball tracking. Think Block Blast's stacked, escalating combo
chimes: each step up the ladder gets a brighter/higher-pitched sound layered
on the last, building to a distinct fanfare at the top tier:

| Chain length | Moment |
|---|---|
| 3 | "Nice Combo!" — small flash + rising chime |
| 5 | "ON FIRE!" — screen glow + bigger stacked chime |
| 8+ | "UNSTOPPABLE!" — particles/confetti + full fanfare sting |
| Flow Bonus triggers | "FLOW STATE!" — distinct highlight sound, separate from the length-based ones |

---

## 3. Shared "sticky" layer (cross-cutting, both modes)

These aren't things you explicitly asked for, but they're the standard
toolkit for turning a scoring system into something kids come back to daily
— flagging them now since several directly repair dead/placeholder code
`SCORING_MECHANISM.md` already found:

- **Daily streak counter** (separate from in-game combo streak) shown on the Home screen, Duolingo-style — "3 days in a row!" This *requires* persistence, which today doesn't exist (`PatternRepository.updatePlayerProgress` is dead code, never called).
- **Personal-best chasing** — "Beat your best: 2,400" before a session, "NEW RECORD!" banner after. Directly repairs the Best Scores screen, which is currently a hard-coded "Coming Soon" placeholder, and `ProfileScreen`'s hard-coded "12"/"95%" stat cards.
- **Tiered end-of-session grade** (Bronze/Silver/Gold/Star) on `ScoreScreen`, driven by clean-lap rate and longest chain, instead of just a raw integer — kids respond much better to a badge than a number.
- **Near-miss feedback** — if a detection lands just outside a target box, a quiet "So close!" cue rather than silence, to keep retry motivation high without rewarding the miss itself.
- **Light unlockables** — new ball trail colors / grid skins unlocked by total laps or longest-ever chain. No pay-to-win, purely cosmetic collection loop.

---

## 4. Implementation notes

- **Consolidate scoring into commonMain.** `SCORING_MECHANISM.md` flagged that Android and iOS already have two different, drifted scoring formulas inlined per-platform. Building this fresh is the natural moment to put it in one shared `ScoringEngine`/`PatternMatcher` both `GameScreen.android.kt` and `GameScreen.ios.kt` call into, instead of writing a third divergent copy.
- **Wrong-target detection reuses the existing per-frame pipeline** — today's code already transforms each detection and checks it against the *current* target's box; this just extends that same check to the other 8 boxes.
- **Freestyle pattern matching is entirely new logic** — the buffer/suffix-match approach above is the core new piece of work, and the main open technical question is what happens when a buffer tail could match more than one pattern at once (none currently overlap, but worth a defined tie-break rule — longest match wins — before implementation).
- **Persistence has to get wired up for real** for any of the streak/personal-best/grade mechanics in section 3 to work — that's a prerequisite piece of work, not optional polish.

## Suggested next step

This is a big enough change I'd want to implement it in two passes rather
than one large one: **(1) Training Mode's clean-lap bonus** — smaller,
touches code that already exists — **then (2) Freestyle Gameplay Mode**,
which is new end-to-end. Let me know if the point values above feel right
(they're a reasonable starting point, easy to retune later) and which pass
you want to start with, and I'll turn it into a proper implementation plan.
