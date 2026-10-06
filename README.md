# Impostor

[![Build](https://github.com/francocorelli23/impostor-game/actions/workflows/build.yml/badge.svg)](https://github.com/francocorelli23/impostor-game/actions/workflows/build.yml)

**One word. One secret. Find the impostor.**

An offline Android party game for a group sharing a single phone. Everyone gets the
same secret word — except the impostors, who have to blend in and work it out from
what the others say.

The app has **no internet permission at all**. Every word, hint, sound and setting
ships inside the APK. It works identically in Airplane Mode.

---

## The game

1. **Setup** — pick the number of players, how many impostors (or let the game draw
   a secret number each round), which categories are in play, a difficulty, whether
   impostors get a hint, and optional timers.
2. **Secret reveal** — the phone goes around in whatever order suits the table. Each
   player taps *their own name*, sees their role alone, then taps *Hide & Pass Phone*.
   A name that has been opened greys out, so nobody sees a role twice.
3. **Discussion** — the app names a random player to give the first clue, then
   you go around the circle.
4. **Voting** — the phone goes around again and everyone votes in secret.
5. **Reveal** — the word, the impostors, the tally, and who won.
6. **Next round** — Play Again, New Categories, Change Settings, or Home.

The crew wins only by voting out exactly the impostors. A tie at the cut-off means
nobody is ejected and the impostors get away with it.

## What's inside

| | |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3 |
| Min / target SDK | 26 / 36 — Android 8.0 and up, targeting Android 16 |
| Languages | English and Croatian — interface *and* word list, switchable from the home screen |
| Words | 760 across 25 categories per language, bundled as `assets/words/<lang>.json` |
| Hints | Two per word — one clear, one cryptic — plus an option to hide the category entirely |
| My Words | Players can add their own words to any category or make new categories. Hints are optional; a word without one gives the impostor no hint in any mode. Saved per language in `files/custom_words.json` |
| Permissions | **none** |
| Dependencies | AndroidX core, lifecycle, activity, Compose. No backend, no analytics, no ads |
| Fonts | Space Grotesk + Inter, bundled under the SIL Open Font License (see `licenses/`) |
| Audio | Nine short clips, synthesised and bundled as OGG in `res/raw` (see `tools/mksfx.py`) |

## Project layout

```
app/src/main/
  assets/words/en.json           the English word database
  assets/words/hr.json           the Croatian word database
  java/com/impostor/party/
    MainActivity.kt              single activity, splash handover, music lifecycle
    ImpostorApplication.kt       three-object container, no DI framework
    data/
      WordRepository.kt          asset parsing, no-repeat history, merges custom words
      CustomWordStore.kt         the players' own words and categories (JSON in app storage)
      SettingsRepository.kt      SharedPreferences + StateFlow
      model/Models.kt            Difficulty, HintMode, GameConfig, Round, ...
    game/
      GameRules.kt               pure logic: impostor draw, win condition
      GameViewModel.kt           phase machine, timers, votes
    ui/
      ImpostorApp.kt             routes, back handling, theme + feedback wiring
      theme/                     colour, type, shapes
      components/                buttons, cards, stepper, segmented control, logo
      screens/                   Home, Setup, Reveal, Discussion, Voting, Results,
                                 HowToPlay, Settings, CategoryPicker, NameEditor,
                                 CustomWords
  res/values/strings.xml         base language
  res/values-hr/strings.xml      Croatian
  res/font, res/raw, res/drawable
app/src/test/                    unit tests for the game rules
store/                           Play Store icon + feature graphic
licenses/                        OFL licences for the bundled fonts
```

## Design notes

**No accidental exposure.** The reveal card is only ever composed after an explicit
tap, and the animation reads the player index from the *animated* state rather than
the current one — otherwise the outgoing card would re-render with the next player's
word while it faded away. That subtlety is the whole reason the game is trustworthy.

**Fair draw.** Impostors come from a full `shuffled(random).take(n)` of the seats, so
every seat is equally likely and a seat can never be drawn twice. There is a unit
test that runs 12,000 rounds and checks the distribution.

**The impostor count is the group's call.** Anything from one up to `players - 1` is
allowed — the only rule the code enforces is that somebody has to know the word. Past
half the table you get a warning, not a block. Switch on *Random each round* and the
game secretly draws 1..N every round, and the reveal card then stops saying how many
impostors there are, because that is now hidden information.

**Nobody always goes first.** The opening speaker is drawn fresh every round from
every seat, impostors included. Speaking first is a real disadvantage — you have the
least information — so it should not keep landing on whoever happens to be player one.

**Names outlive the round.** The same group usually plays several games in a row, so
typed names are written to preferences the moment they are entered and are only ever
removed by the explicit *Clear all names* button. They are stored at full length too,
so dropping from eight players to four and back up again does not lose the last four.

**Words don't repeat.** The last 80 words used are remembered on the device and
skipped while there is anything else to pick. Category selection is drawn first and
the word second, so picking twelve categories does not favour the biggest one.

**Language is a context, not a restart.** Choosing a language hands the whole Compose
tree a `Context` configured for that locale, so `stringResource` resolves against
`values-hr/` immediately — no appcompat dependency, no activity recreation. The word
list follows the same tag.

**Hints never leak.** Every hint in both languages is checked against its own word at
build time — not just for the word itself but for shared stems, which is what catches
inflected Croatian ("Putovnica" / "za putovanje"). Seventeen real leaks were found and
rewritten that way.

## Building

See [BUILD.md](BUILD.md) — including how to produce `app-release.aab` for Play.

## Privacy

See [PRIVACY.md](PRIVACY.md). Short version: the app collects nothing, sends nothing,
and cannot — it has no network permission.

## Licence

MIT — see [LICENSE](LICENSE). The bundled fonts keep their own SIL Open Font
License, in `licenses/`.
