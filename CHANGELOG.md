# Changelog

## 0.2.3

### Added

- **Hitbox channel.** With F3+B on, a blocked player's white hitbox outline used to keep walking
  around after the body was gone, which gave their exact position away. `EntityHitboxDebugRenderer`
  now skips that one entity. Enabled by default, and it also answers to the `model` switch, so
  existing entries pick it up without any action.

## 0.2.2 — first public release

First version published to Modrinth.

### Channels

Thirteen independent, per-player switches: model, name tag, outline, tab list, chat, system
messages, command suggestions, sound, voice, minimap, Jade tooltip, particles, sidebar.

Eleven default to on. `particles` and `sidebar` default to off because they match on position or
plain text rather than on a UUID.

### Included

- Blocklist keyed on UUID, with a name snapshot for display, persisted atomically with a rolling
  backup.
- Global scope by default; per-server scope available via `here`.
- Full command set under `/pb`, all of it mirrored in a GUI reachable from Mod Menu.
- `/pb add` completion and the GUI picker list every player the server has reported, not only the
  ones currently nearby.
- Whole-token, case-insensitive text matching for the channels that carry no UUID — `Alex` does not
  match `Alex2` or `Alexander`.
- Soft integration with Xaero's Minimap, Jade and Simple Voice Chat; their hooks are skipped when
  the mod is absent, so none of them become a dependency.
- Dry-run mode and a local hidden-chat log, so a heuristic channel can be audited before it is
  trusted.
