# DesidiaItemCraftBlock

A Spigot/Paper plugin for Minecraft 1.13+ that lets server administrators block crafting recipes per world, per region (WorldGuard), or globally — with a GUI, statistics, PlaceholderAPI support, and a clean public API.

---

## Features

- **Per-world blocking** — different blocked/allowed lists for each world
- **Per-region blocking** — native WorldGuard flags (`deny-craft`, `deny-craft-items`, `allow-craft-items`)
- **Blacklist & Whitelist modes** — choose whether to block specific items or allow only specific ones
- **Wildcard matching** — patterns like `*_SWORD` or `NETHERITE_*`
- **Visual GUI** — paginated inventory to manage blocked items without commands
- **Statistics** — track blocked craft attempts per player and globally
- **PlaceholderAPI** — expose data to scoreboards, chat plugins, etc.
- **bStats** — anonymous usage metrics
- **Update checker** — notifies on server startup if a new version is available
- **Public API** — other plugins can query blocking decisions via `DIBApi`
- **Localization** — Spanish and English included; easily extensible

---

## Requirements

| Dependency | Version | Required |
|---|---|---|
| Paper / Spigot | 1.13+ | Yes |
| WorldGuard | 7.x | No (optional) |
| PlaceholderAPI | 2.11+ | No (optional) |

---

## Installation

1. Download `DesidiaItemCraftBlock-2.0.0.jar`
2. Drop it into your server's `plugins/` folder
3. Restart the server
4. Edit `plugins/DesidiaItemCraftBlock/config.yml` to your liking
5. Run `/dib reload` to apply changes without restarting

---

## Commands

All commands require the corresponding permission (default: `op`).

| Command | Description | Permission |
|---|---|---|
| `/dib help` | Show all available commands | `dib.help` |
| `/dib reload` | Reload config and locale files | `dib.reload` |
| `/dib toggle [world]` | Enable or disable blocking globally or per world | `dib.toggle` |
| `/dib status` | Show plugin status (version, mode, integrations) | `dib.status` |
| `/dib list [world]` | List blocked/allowed items for a world | `dib.list` |
| `/dib block <item> [world]` | Add an item to the blocked list | `dib.block` |
| `/dib unblock <item> [world]` | Remove an item from the blocked list | `dib.unblock` |
| `/dib check <item>` | Check if an item is blocked at your current location | `dib.check` |
| `/dib stats [player\|reset [player]]` | View or reset craft-block statistics | `dib.stats` |
| `/dib gui` | Open the visual management inventory | `dib.gui` |

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `dib.help` | op | Use `/dib help` |
| `dib.reload` | op | Use `/dib reload` |
| `dib.toggle` | op | Use `/dib toggle` |
| `dib.status` | op | Use `/dib status` |
| `dib.list` | op | Use `/dib list` |
| `dib.block` | op | Use `/dib block` |
| `dib.unblock` | op | Use `/dib unblock` |
| `dib.check` | op | Use `/dib check` |
| `dib.stats` | op | Use `/dib stats` |
| `dib.stats.reset` | op | Reset statistics |
| `dib.gui` | op | Open the GUI |
| `dib.notify` | op | Receive admin notifications on blocked crafts |
| `dib.bypass` | false | Bypass all blocking (includes children below) |
| `dib.bypass.world.<world>` | false | Bypass blocking in a specific world |
| `dib.bypass.region.<region>` | false | Bypass blocking in a specific region |
| `dib.bypass.item.<material>` | false | Bypass blocking for a specific item |

---

## Configuration

```yaml
# plugins/DesidiaItemCraftBlock/config.yml

settings:
  mode: blacklist          # blacklist or whitelist (global default)
  enabled: true            # global on/off switch
  locale: es               # es or en
  notify-admins: false     # notify players with dib.notify on blocked crafts
  log-attempts: false      # log blocked attempts to console
  update-checker: true     # check for new versions on startup
  stats-autosave-interval: 5  # minutes between automatic stats saves

message:
  enabled: true
  text: "&c!No puedes craftear &e{item}&c aqui!"
  type: CHAT               # CHAT, ACTIONBAR, or TITLE
  sound: ENTITY_VILLAGER_NO
  cooldown: 3              # seconds between repeated messages per player

worlds:
  default:
    enabled: true
    mode: blacklist
    blocked:
      - DIAMOND_SWORD
      - "*_SWORD"          # wildcard: all swords
    allowed: []
    custom-messages:
      DIAMOND_SWORD: "&cLas espadas de diamante estan prohibidas aqui."

regions: {}               # config-based regions (without WorldGuard)
```

### Per-region config (without WorldGuard)

```yaml
regions:
  spawn:
    mode: whitelist
    blocked: []
    allowed:
      - CRAFTING_TABLE
```

---

## WorldGuard Integration

When WorldGuard is present, three custom flags are registered:

| Flag | Values | Effect |
|---|---|---|
| `deny-craft` | `allow` / `deny` | Deny or allow all crafting in the region |
| `deny-craft-items` | `ITEM1,ITEM2,*_SWORD` | Deny crafting specific items (comma-separated, wildcards OK) |
| `allow-craft-items` | `ITEM1,ITEM2` | Explicitly allow items even if `deny-craft` is set |

### Example

```
/rg flag <region> deny-craft deny
/rg flag <region> deny-craft-items DIAMOND_SWORD,*_AXE
/rg flag <region> allow-craft-items WOODEN_SWORD
```

### Priority order (highest to lowest)

1. `dib.bypass` permission → always allow
2. `dib.bypass.item.<material>` permission → allow that item
3. Plugin disabled globally → allow
4. WorldGuard `allow-craft-items` flag → allow
5. WorldGuard `deny-craft-items` flag → block
6. WorldGuard `deny-craft` state flag → block or allow
7. Config-based region rules
8. World config
9. Global default mode

---

## PlaceholderAPI

Available placeholders (requires PlaceholderAPI installed):

| Placeholder | Returns |
|---|---|
| `%dib_enabled%` | `true` / `false` — plugin enabled state |
| `%dib_mode%` | Global mode (`blacklist` / `whitelist`) |
| `%dib_total_blocked%` | Total blocked attempts for the player |
| `%dib_top_item%` | Item with most blocked attempts for the player |
| `%dib_top_item_count%` | Count for the top item |
| `%dib_last_world%` | World of the player's last blocked attempt |
| `%dib_last_region%` | Region of the player's last blocked attempt |
| `%dib_time_since_last%` | Time since the player's last blocked attempt |
| `%dib_wg_available%` | `true` / `false` — WorldGuard detected |
| `%dib_global_total%` | Total blocked attempts across all players |

---

## Developer API

Add DesidiaItemCraftBlock as a dependency and query blocking decisions from your plugin:

```java
// Check if a material is blocked for a player at their current location
boolean blocked = DIBApi.get().isBlocked(player, Material.DIAMOND_SWORD);

// Check global enabled state
boolean enabled = DIBApi.get().isEnabled();

// Listen to the cancellable event
@EventHandler
public void onCraftBlocked(CraftBlockedEvent event) {
    Player player = event.getPlayer();
    Material material = event.getMaterial();
    BlockReason reason = event.getReason(); // REGION, WORLD, or GLOBAL
    event.setCancelled(true); // cancel DIB's block if you want to override
}
```

`CraftBlockedEvent` is fired before the craft is cancelled, so it can be cancelled by other plugins to allow the craft through.

---

## Statistics

Statistics persist across restarts in `plugins/DesidiaItemCraftBlock/stats.yml`.

- `/dib stats` — global top items and top players
- `/dib stats <player>` — per-player breakdown (total, top item, last attempt)
- `/dib stats reset` — reset global stats (requires `dib.stats.reset`)
- `/dib stats reset <player>` — reset one player's stats

---

## Localization

The plugin ships with `messages_es.yml` (Spanish) and `messages_en.yml` (English). Set your locale in `config.yml`:

```yaml
settings:
  locale: en
```

To add a custom language, create `messages_<locale>.yml` in the plugin folder. All keys from the built-in files must be present.

---

## License

This project is open source. See [LICENSE](LICENSE) for details.
