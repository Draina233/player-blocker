# Player Blocker

A player standing in front of you is easy to ignore. A player who is simply **not there** is easier.

Player Blocker removes one chosen player from your own client — their model, name tag, tab-list entry,
chat lines, whispers, footstep sounds, minimap dot — without touching the server, the network
protocol, or the way anyone else sees the world.

* **Client-side only.** No server plugin, no packet rewriting, no data leaves your game.
* **Nobody notices.** The other player and the server have no way to tell it is running.
* **Reversible.** Everything is a switch; `/pb reveal` brings the world back instantly.

## What it hides

Every channel is independent and can be toggled per player.

| Channel | Default | What it covers |
|---|---|---|
| Model | on | Body, armour, held items, cape |
| Name tag | on | Floating name above the head |
| Outline | on | Glowing effect and drop shadow |
| Tab list | on | Vanilla player list and BetterTab (both read the same source) |
| Chat | on | Lines authored by that player |
| System messages | on | Join/leave, whispers, advancement broadcasts, server text naming them |
| Suggestions | on | Tab completion, so typing their name does not surface them |
| Sound | on | Footsteps, hurt, attack, item pickup |
| Voice | on | Simple Voice Chat proximity audio |
| Minimap | on | Xaero's minimap / world-map radar dot |
| Tooltip | on | Jade crosshair entity panel |
| Particles | off | Potion swirl, totem pop — matched by position, so it is a heuristic |
| Sidebar | off | Scoreboard `/list`-style text — the highest false-positive channel |

The two channels that default to **off** are the heuristic ones: they match on position or on plain
text rather than on a UUID. Turn them on only if you want that extra coverage, and use
`/pb dryrun` first if you are unsure.

## Requirements

| | |
|---|---|
| Minecraft | 26.2 |
| Loader | Fabric 0.19.3 or newer |
| Dependency | Fabric API |
| Java | 25 |

Optional soft integrations, picked up automatically when installed: **Mod Menu** (config entry),
**Xaero's Minimap**, **Jade**, **Simple Voice Chat**. None of them are required — their mixins are
skipped when the mod is absent.

## Installation

1. Install Fabric Loader for Minecraft 26.2.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) into `mods/`.
3. Drop `playerblocker-<version>.jar` into `mods/`.
4. Start the game. Optionally install [Mod Menu](https://modrinth.com/mod/modmenu) to reach the
   config screen from the mod list.

## Usage

Join a server, then either use `/pb add` and pick from the full online list, or type a name directly.

```
/pb add <player> [global|here]   block someone (opens the picker when run bare)
/pb remove <player>              unblock
/pb scope <player> global|here   change where an entry applies
/pb list                         show the active blocklist
/pb gui                          open the config screen
/pb status                       master switch, scope, dry-run, debug state
/pb reveal                       toggle "show everything" without deleting entries
/pb dryrun                       log what would be hidden, hide nothing
/pb debug                        log every chat line and suggestion the filters inspect
/pb reload                       re-read the config from disk
```

Everything is also available in the GUI: **Mod Menu → Player Blocker → Config**, or `/pb gui`.
The GUI is where per-player channel switches live.

Tab completion for `/pb add` lists **every player the server has reported**, not just the ones
nearby — a player across the map is still a valid target.

### Scope

Entries are **global** by default: once blocked, blocked everywhere. Use `here` to restrict an entry
to the server you are currently on, which is only useful when two servers happen to use the same
name for different people.

## Configuration

`config/playerblocker/blocklist.json`, written atomically with a rolling `.bak`.

| Setting | Meaning |
|---|---|
| `enabled` | Master switch |
| `dryRun` | Record matches without hiding anything |
| `logHiddenChat` | Mirror suppressed chat into `logs/playerblocker-hidden.log` |
| `debug` | Verbose logging of every inspected line (`[pb/debug]` in `latest.log`) |

The hidden-chat log is local only, never uploaded, and is the honest answer to "did I miss
something?" when a heuristic channel is enabled. It is on by default.

## How it works

Each channel hooks the single place the client decides to act, rather than patching rendering
output:

* `EntityRenderDispatcher#submit` for models and outlines — the only injection point that also
  catches players, whose renderers override the per-entity one.
* `ClientPacketListener`'s listed/online player collections for the tab list, the social screen and
  vanilla command completion; `ClientSuggestionProvider` for completion the server computes itself.
* `ChatComponent#addMessage`, the one private funnel all three public chat entry points delegate to,
  so plugin-server plain text is covered alongside signed player chat.
* `ClientLevel#playSeededSound` / `playLocalSound` for audio and `#addParticle` for particles.
* Small per-mod hooks for the optional integrations.

Matches are UUID-based wherever a UUID exists. Where only text exists, matching is whole-token and
case-insensitive, so `Alex` never matches `Alex2` or `Alexander`.

## Compatibility

Built against **26.2**, which ships unobfuscated — no mappings, no refmap.

Known-good alongside: Sodium, Iris, EntityCulling, ImmediatelyFast, BetterTab, ChatHeads, Xaero's
Minimap and World Map, Jade, Simple Voice Chat, Mod Menu.

Other mods that hide players independently are not a conflict; Player Blocker only removes things
from your own client, and two clientside filters simply hide twice.

## Building

```bash
# JDK 25 required
./gradlew build        # or: gradle build
```

Output: `build/libs/playerblocker-<version>.jar`.

Soft-dependency types are compiled against `libs/voicechat.jar` and `libs/modmenu.jar`, which are
**not** distributed with this repository. Put the matching mod jars there before building, or delete
the two `compileOnly` lines in `build.gradle` if you only need the core channels.

## Disclaimer

This is a presentation filter for your own client. It does not change what anyone else sees, it does
not remove you from anyone's view, and it cannot be used to evade a server's rules or a punishment.
Some servers restrict third-party client mods — check their rules before using it there.

## License

MIT — see [LICENSE](LICENSE).
