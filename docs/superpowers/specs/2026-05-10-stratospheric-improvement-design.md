# DesidiaItemCraftBlock — Diseño de Mejora Estratosférica (v2.0.0)

**Fecha:** 2026-05-10  
**Autor:** cuouz  
**Versión objetivo:** 2.0.0 (reescritura mayor desde 1.0.0)

---

## Resumen

Reescritura completa del plugin DesidiaItemCraftBlock para convertirlo de un bloqueador global de crafteo en una herramienta de referencia para administradores de servidores Minecraft. La v2.0.0 añade control granular por item, soporte por mundo, integración nativa con WorldGuard mediante flags propios, GUI in-game, estadísticas por jugador, localización multiidioma, PlaceholderAPI, bStats y un Update Checker.

---

## 1. Arquitectura y estructura de paquetes

```
info.desidia/
├── DesidiaItemCraftBlock.java        ← main, orquesta todo
├── commands/
│   ├── DIBCommand.java               ← dispatcher de subcomandos con tab completion
│   └── sub/
│       ├── ReloadCommand.java
│       ├── StatusCommand.java
│       ├── ToggleCommand.java
│       ├── ListCommand.java
│       ├── BlockCommand.java
│       ├── UnblockCommand.java
│       ├── CheckCommand.java
│       ├── StatsCommand.java         ← incluye subcomando reset
│       └── HelpCommand.java
├── listeners/
│   └── CraftListener.java            ← delega toda decisión a BlockManager
├── managers/
│   ├── BlockManager.java             ← único punto de decisión de bloqueo
│   ├── ConfigManager.java            ← carga/recarga de config y estados persistidos
│   ├── StatsManager.java             ← contadores en memoria + persistencia cada 5 min
│   └── LocaleManager.java            ← carga mensajes según locale con fallback a EN
├── hooks/
│   ├── WorldGuardHook.java           ← registra flags y consulta regiones (opcional)
│   └── PlaceholderAPIHook.java       ← registra placeholders %dib_*% (opcional)
├── api/
│   ├── DIBApi.java                   ← API pública estática para otros plugins
│   └── events/
│       └── CraftBlockedEvent.java    ← extends Event implements Cancellable
├── gui/
│   └── ItemManagementGUI.java        ← inventario paginado de 54 slots
└── util/
    ├── ColorUtil.java                ← traducción de códigos & a colores Bukkit
    └── UpdateChecker.java            ← consulta SpigotMC API async al iniciar
```

**Principio de diseño:** cada clase tiene una sola responsabilidad. `BlockManager` es el único punto de decisión — no sabe nada de mensajes ni de GUI. `CraftListener` no sabe nada de WorldGuard. Todo se comunica via interfaces bien definidas.

---

## 2. Sistema de configuración

### `config.yml`

```yaml
settings:
  mode: blacklist          # blacklist | whitelist (modo global por defecto)
  enabled: true            # estado del toggle — persiste entre reinicios
  locale: es               # idioma: es | en | cualquier messages_XX.yml
  notify-admins: false     # avisar a jugadores con dib.notify en cada intento
  log-attempts: false      # registrar intentos en consola
  update-checker: true
  stats-autosave-interval: 5  # minutos entre guardados de stats.yml

message:
  enabled: true
  text: "&c¡No puedes craftear &e{item}&c aquí!"
  type: CHAT               # CHAT | ACTIONBAR | TITLE | BOSSBAR
  sound: ENTITY_VILLAGER_NO
  cooldown: 3              # segundos entre mensajes repetidos por jugador

worlds:
  default:                 # aplica a todos los mundos no definidos explícitamente
    enabled: true
    mode: blacklist
    blocked:
      - DIAMOND_SWORD
      - NETHERITE_INGOT
      - "*_SWORD"          # wildcards soportados
    allowed: []
    custom-messages:
      DIAMOND_SWORD: "&c¡Las espadas de diamante están prohibidas aquí!"

  world_nether:
    enabled: true
    mode: whitelist
    allowed:
      - TORCH
      - CHEST

regions:                   # alternativa a flags WG para servidores sin WorldGuard
  spawn:
    mode: whitelist
    allowed:
      - TORCH
      - CHEST
      - CRAFTING_TABLE
    custom-messages:
      DIAMOND_SWORD: "&c¡Zona de spawn protegida!"
  pvp_arena:
    mode: blacklist
    blocked:
      - TNT
      - FIREWORK_ROCKET
```

### `messages_es.yml` / `messages_en.yml`

```yaml
no-permission: "&cNo tienes permiso para ejecutar este comando."
reload-success: "&a✔ Configuración recargada correctamente."
toggle-enabled: "&a✔ Bloqueo de crafteo &lACTIVADO&a."
toggle-disabled: "&c✘ Bloqueo de crafteo &lDESACTIVADO&c."
craft-blocked: "&c¡No puedes craftear &e{item}&c aquí!"
admin-notify: "&8[&6DIB&8] &e{player} &7intentó craftear &e{item} &7en &e{world}&7/&e{region}"
stats-header: "&6▶ Estadísticas de &e{player}"
stats-total: "&7Intentos bloqueados: &f{total}"
stats-top-item: "&7Item más intentado: &f{item} &7({count} veces)"
stats-reset-success: "&aEstadísticas de &e{player} &areiniciadas."
unknown-command: "&cComando desconocido. Usa &f/dib help&c."
item-not-found: "&cMaterial &e{item}&c no existe."
list-header: "&6Items bloqueados en &e{world}&6:"
status-header: "&6▶ Estado de DesidiaItemCraftBlock"
```

Variables disponibles en mensajes: `{item}`, `{player}`, `{world}`, `{region}`, `{attempts}`, `{count}`.

Fallback: si un mensaje no existe en el locale elegido → inglés → key entre corchetes `[craft-blocked]`.

### `stats.yml`

```yaml
players:
  <UUID>:
    name: Steve
    blocked-attempts: 42
    last-attempt: "2026-05-10"
    last-item: DIAMOND_SWORD
    last-world: world
    top-items:
      DIAMOND_SWORD: 15
      TNT: 27
```

Guardado automático cada `stats-autosave-interval` minutos y al apagar el servidor (`onPluginStop`).

---

## 3. Comandos y permisos

### Tabla de comandos

| Comando | Descripción | Permiso |
|---|---|---|
| `/dib help` | Lista todos los comandos disponibles | `dib.help` |
| `/dib reload` | Recarga config.yml, messages y stats | `dib.reload` |
| `/dib toggle [mundo]` | Activa/desactiva el bloqueo (persiste) | `dib.toggle` |
| `/dib status` | Muestra config activa: modo, mundos, regiones | `dib.status` |
| `/dib list [mundo]` | Lista items bloqueados/permitidos en un mundo | `dib.list` |
| `/dib block <item> [mundo]` | Bloquea un item en tiempo real y guarda en config | `dib.block` |
| `/dib unblock <item> [mundo]` | Desbloquea un item en tiempo real | `dib.unblock` |
| `/dib check <item>` | Comprueba si un item está bloqueado donde estás | `dib.check` |
| `/dib stats [jugador]` | Muestra estadísticas (global si sin args) | `dib.stats` |
| `/dib stats reset [jugador]` | Limpia estadísticas de un jugador o globales | `dib.stats.reset` |
| `/dib gui` | Abre el inventario de gestión visual | `dib.gui` |

Tab completion activo en todos los comandos: materiales de Minecraft, mundos cargados, regiones WG del mundo del jugador.

### Tabla de permisos

| Permiso | Default | Descripción |
|---|---|---|
| `dib.help` | op | Ver ayuda |
| `dib.reload` | op | Recargar config |
| `dib.toggle` | op | Activar/desactivar |
| `dib.status` | op | Ver estado |
| `dib.list` | op | Listar items |
| `dib.block` | op | Bloquear item |
| `dib.unblock` | op | Desbloquear item |
| `dib.check` | op | Verificar item |
| `dib.stats` | op | Ver estadísticas |
| `dib.stats.reset` | op | Resetear estadísticas |
| `dib.gui` | op | Abrir GUI |
| `dib.notify` | op | Recibir alertas de intentos |
| `dib.bypass` | false | Salta todos los bloqueos (concede todos los bypass hijos) |
| `dib.bypass.world.<mundo>` | false | Bypass en mundo específico |
| `dib.bypass.region.<región>` | false | Bypass en región WG específica |
| `dib.bypass.item.<material>` | false | Bypass para un item específico |

---

## 4. Lógica de decisión — BlockManager

### Flujo completo (orden de evaluación)

```
CraftItemEvent disparado
        │
        ▼
¿Jugador tiene dib.bypass?                    → PERMITIR
        │ no
        ▼
¿Jugador tiene dib.bypass.item.<material>?    → PERMITIR
        │ no
        ▼
¿Plugin toggle desactivado (enabled: false)?  → PERMITIR
        │ no
        ▼
[Consultar caché de resultados WG — TTL: 2 ticks]
        │
        ▼
¿WorldGuard disponible?
    ├── SÍ → ¿Región tiene flag craft-block=DENY?      → BLOQUEAR (ir a *)
    │         ¿Región tiene flag craft-block=ALLOW?     → PERMITIR
    │         ¿Región tiene craft-whitelist o craft-blacklist?
    │              ├── SÍ → dib.bypass.region.<r>?     → PERMITIR
    │              │         └── evaluar lista región   → BLOQUEAR o PERMITIR
    │              └── NO → bajar a nivel mundo
    └── NO → ¿Servidor tiene config regions: definidas?
              ├── SÍ → dib.bypass.region.<r>?             → PERMITIR
              │         └── evaluar lista región config    → BLOQUEAR o PERMITIR
              └── NO → bajar a nivel mundo
        │
        ▼
¿Mundo tiene config propia en config.yml?
    ├── SÍ → dib.bypass.world.<mundo>?                 → PERMITIR
    │         └── evaluar modo mundo (blacklist/whitelist) → BLOQUEAR o PERMITIR
    └── NO → usar configuración "default"
        │
        ▼
Evaluar modo global (blacklist/whitelist)
        │
        ▼
* BLOQUEAR:
    → Disparar CraftBlockedEvent (cancellable)
    → Si cancelado por otro plugin → PERMITIR
    → Si no cancelado:
        → Cancelar CraftItemEvent
        → ¿Jugador tiene mensaje en cooldown?
            ├── SÍ → no enviar mensaje
            └── NO → enviar mensaje (CHAT/ACTIONBAR/TITLE/BOSSBAR) + sonido
                      reiniciar cooldown del jugador
        → ¿notify-admins: true? → notificar a jugadores con dib.notify
        → ¿log-attempts: true? → log en consola
        → StatsManager.record(jugador, material, mundo, región)
```

### Caché de WorldGuard

`Map<String, CachedResult>` donde la clave es `UUID + ":" + regionName` con TTL de 2 ticks (100ms). La clave compuesta garantiza que cambiar de región invalida el caché correctamente. Se invalida automáticamente al expirar el TTL o al detectar cambio de mundo via `PlayerChangedWorldEvent`.

### CraftBlockedEvent

```java
public class CraftBlockedEvent extends Event implements Cancellable {
    private final Player player;
    private final Material item;
    private final Recipe recipe;       // receta completa con ingredientes
    private final String world;
    private final String region;       // null si no hay WG activo
    private final BlockReason reason;  // REGION, WORLD, GLOBAL
    private boolean cancelled;
    // getters + isCancelled() + setCancelled()
}

public enum BlockReason { REGION, WORLD, GLOBAL }
```

---

## 5. WorldGuard — Flags nativos

Registrados en `WorldGuardHook.onEnable()`:

| Flag | Tipo | Descripción |
|---|---|---|
| `craft-block` | StateFlag | ALLOW/DENY — sobreescribe toda otra regla DIB |
| `craft-whitelist` | StringListFlag | Lista de materiales permitidos en la región |
| `craft-blacklist` | StringListFlag | Lista de materiales bloqueados en la región |

Uso por admins vía comandos WG estándar:
```
/rg flag spawn craft-block deny
/rg flag spawn craft-whitelist TORCH,CHEST,CRAFTING_TABLE
/rg flag pvp_arena craft-blacklist TNT,FIREWORK_ROCKET
```

Cuando `craft-whitelist` y `craft-blacklist` están ambos definidos en la misma región, `craft-blacklist` tiene precedencia. Si el material no aparece en ninguna lista, se evalúa el siguiente nivel (mundo → global).

---

## 6. GUI — ItemManagementGUI

Inventario de 54 slots con paginación. Abierto con `/dib gui`.

```
┌──────────────────────────────────────────────────┐
│  [◄ Ant]   Página 1/3  [MODO: BLACKLIST]  [Sig ►] │
├──────────────────────────────────────────────────┤
│  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  │
│  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  │
│  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  │
│  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  ITEM  │
├──────────────────────────────────────────────────┤
│  [+ Añadir item en mano]  [🌍 Mundo]  [✖ Cerrar] │
└──────────────────────────────────────────────────┘
```

- Items bloqueados → fondo cristal rojo
- Items permitidos (modo whitelist) → fondo cristal verde
- **Click izquierdo:** toggle bloqueo/permiso inmediato, guarda en `config.yml`
- **Click derecho:** cierra GUI, solicita mensaje personalizado por chat, reabre GUI
- **"Añadir item en mano":** añade el `Material` del item que el admin sostiene — sin escritura
- **Selector de mundo:** cambia el contexto del GUI sin cerrar
- Cambios del GUI se escriben a `config.yml` en tiempo real

---

## 7. Estadísticas — StatsManager

Guardado en memoria durante la sesión, persistido en `stats.yml` cada N minutos y al apagar.

### `/dib stats` (global)
```
▶ Estadísticas globales del servidor
  Total intentos bloqueados: 1.247
  Top items: TNT (312), DIAMOND_SWORD (198), NETHERITE_AXE (88)
  Top jugadores: Steve (89), Alex (67), Notch (54)
```

### `/dib stats <jugador>`
```
▶ Estadísticas de Steve
  Intentos bloqueados: 89
  Item más intentado:  TNT (27 veces)
  Último intento:      world | spawn | hace 3 min
  Mundos más activos:  world (75), world_nether (14)
```

---

## 8. PlaceholderAPI

### Placeholders para otros plugins

| Placeholder | Valor |
|---|---|
| `%dib_blocked_total%` | Total intentos bloqueados del jugador |
| `%dib_top_item%` | Item que más intentó craftear |
| `%dib_last_attempt%` | Último item que intentó craftear |
| `%dib_enabled%` | `true`/`false` si el plugin está activo globalmente |
| `%dib_mode%` | `blacklist` o `whitelist` (modo activo) |
| `%dib_is_blocked_<MATERIAL>%` | `true`/`false` si ese material está bloqueado |

### Variables en mensajes del propio plugin

`{item}`, `{player}`, `{world}`, `{region}`, `{attempts}`, `{count}`

---

## 9. bStats

Dependencia en `pom.xml`:
```xml
<dependency>
    <groupId>org.bstats</groupId>
    <artifactId>bstats-bukkit</artifactId>
    <version>3.0.2</version>
    <scope>compile</scope>
</dependency>
```

Relocation en maven-shade-plugin:
```xml
<relocation>
    <pattern>org.bstats</pattern>
    <shadedPattern>info.desidia.desidiaitemcraftblock.lib.bstats</shadedPattern>
</relocation>
```

Métricas reportadas (anónimas):
- Versión del plugin
- Modo activo (blacklist/whitelist)
- WorldGuard activo: sí/no
- PlaceholderAPI activo: sí/no
- Rango de items bloqueados (0 / 1-10 / 11-50 / 50+)

---

## 10. Update Checker

Ejecutado una vez al iniciar, en hilo async, consultando la API de SpigotMC con el Resource ID del plugin. Nunca bloquea el startup. Si falla (sin internet, timeout), log silencioso y continúa.

- Versión nueva disponible → log en consola + mensaje a jugadores con `dib.notify` al entrar
- Al día → log silencioso
- Desactivable: `update-checker: false` en `config.yml`

---

## 11. plugin.yml final

```yaml
name: DesidiaItemCraftBlock
version: ${project.version}
main: ${main.class}
author: ${author}
api-version: 1.13

libraries:
  - org.openjdk.nashorn:nashorn-core:15.4

softdepend: [WorldGuard, PlaceholderAPI]

permissions:
  dib.help:        { default: op }
  dib.reload:      { default: op }
  dib.toggle:      { default: op }
  dib.status:      { default: op }
  dib.list:        { default: op }
  dib.block:       { default: op }
  dib.unblock:     { default: op }
  dib.check:       { default: op }
  dib.stats:       { default: op }
  dib.stats.reset: { default: op }
  dib.gui:         { default: op }
  dib.notify:      { default: op }
  dib.bypass:
    default: false
    children:
      dib.bypass.world.*: true
      dib.bypass.region.*: true
      dib.bypass.item.*: true

commands:
  dib:
    description: Comando principal de DesidiaItemCraftBlock
    usage: /dib help
    permission: dib.help
```

---

## 12. Versión y roadmap

| Versión | Contenido |
|---|---|
| **2.0.0** | Todo lo descrito en este documento |
| 2.1.0 | Soporte de recetas personalizadas (CustomRecipe blocking) |
| 2.2.0 | Persistencia en SQLite/MySQL (opcional) para stats |

---

## Precedencia de reglas (resumen)

```
dib.bypass > dib.bypass.item.X > toggle global
  > región WG (craft-block flag)
    > región WG (craft-whitelist/craft-blacklist)
      > config regions: (sin WG)
        > config worlds: (mundo específico)
          > config worlds.default (global)
```
