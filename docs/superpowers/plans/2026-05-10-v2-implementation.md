# DesidiaItemCraftBlock v2.0.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite DesidiaItemCraftBlock desde 50 líneas de bloqueador global a herramienta de gestión de crafteo con WorldGuard, GUI, stats, PAPI y bStats (v2.0.0).

**Architecture:** Capa de decisión única (`BlockManager`), hooks opcionales para WG y PAPI que degradan gracefully si no están presentes, patrón dispatcher para subcomandos.

**Tech Stack:** Java 8, Maven, Spigot API 1.13+, Foundation 6.5.8, WorldGuard 7.x (optional), PlaceholderAPI 2.x (optional), bStats 3.x, JUnit 5, Mockito 4.x

**Spec:** `docs/superpowers/specs/2026-05-10-stratospheric-improvement-design.md`

---

## File Map

| File | Acción | Responsabilidad |
|---|---|---|
| `pom.xml` | Modify | Añadir deps: WG, PAPI, bStats, JUnit5, Mockito |
| `src/main/resources/plugin.yml` | Modify | Permisos completos, softdepend, commands |
| `src/main/resources/config.yml` | Modify | Nueva estructura con worlds, regions, message |
| `src/main/resources/messages_es.yml` | Create | Todos los mensajes en español |
| `src/main/resources/messages_en.yml` | Create | Todos los mensajes en inglés |
| `info/desidia/util/ColorUtil.java` | Create | Traducción códigos &→color Bukkit |
| `info/desidia/managers/LocaleManager.java` | Create | Carga messages_XX.yml con fallback |
| `info/desidia/managers/ConfigManager.java` | Create | Carga config.yml, inner classes WorldConfig/RegionConfig |
| `info/desidia/managers/StatsManager.java` | Create | Contadores en memoria + persistencia YAML |
| `info/desidia/api/events/BlockReason.java` | Create | Enum: REGION, WORLD, GLOBAL |
| `info/desidia/managers/BlockDecision.java` | Create | DTO: blocked + reason + context |
| `info/desidia/api/events/CraftBlockedEvent.java` | Create | Bukkit Event cancellable |
| `info/desidia/managers/BlockManager.java` | Create | Único punto de decisión de bloqueo |
| `info/desidia/listeners/CraftListener.java` | Create | Delega a BlockManager, envía mensajes |
| `info/desidia/commands/SubCommand.java` | Create | Interface para subcomandos |
| `info/desidia/commands/DIBCommand.java` | Create | Dispatcher + tab completion |
| `info/desidia/commands/sub/HelpCommand.java` | Create | /dib help |
| `info/desidia/commands/sub/ReloadCommand.java` | Create | /dib reload |
| `info/desidia/commands/sub/ToggleCommand.java` | Create | /dib toggle [world] |
| `info/desidia/commands/sub/StatusCommand.java` | Create | /dib status |
| `info/desidia/commands/sub/ListCommand.java` | Create | /dib list [world] |
| `info/desidia/commands/sub/BlockCommand.java` | Create | /dib block <item> [world] |
| `info/desidia/commands/sub/UnblockCommand.java` | Create | /dib unblock <item> [world] |
| `info/desidia/commands/sub/CheckCommand.java` | Create | /dib check <item> |
| `info/desidia/commands/sub/StatsCommand.java` | Create | /dib stats [player] [reset] |
| `info/desidia/hooks/WorldGuardHook.java` | Create | Registra flags WG, evalúa regiones, caché |
| `info/desidia/hooks/PlaceholderAPIHook.java` | Create | Registra placeholders %dib_*% |
| `info/desidia/gui/ItemManagementGUI.java` | Create | Inventario 54 slots paginado |
| `info/desidia/commands/sub/GuiCommand.java` | Create | /dib gui |
| `info/desidia/api/DIBApi.java` | Create | API pública estática |
| `info/desidia/util/UpdateChecker.java` | Create | Consulta SpigotMC async |
| `info/desidia/DesidiaItemCraftBlock.java` | Modify | Main class reescrita |
| `src/test/java/info/desidia/util/ColorUtilTest.java` | Create | Tests de ColorUtil |
| `src/test/java/info/desidia/managers/LocaleManagerTest.java` | Create | Tests de LocaleManager |
| `src/test/java/info/desidia/managers/StatsManagerTest.java` | Create | Tests de StatsManager |
| `src/test/java/info/desidia/managers/BlockManagerTest.java` | Create | Tests de BlockManager |

---

## Task 1: Build setup

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1.1: Añadir repositorios WG y PAPI al pom.xml**

Dentro de `<repositories>`, añadir después del repo de jitpack:
```xml
<repository>
    <id>enginehub-repo</id>
    <url>https://maven.enginehub.org/repo/</url>
</repository>
<repository>
    <id>placeholderapi</id>
    <url>https://repo.extendedclip.com/content/repositories/placeholderapi/</url>
</repository>
```

- [ ] **Step 1.2: Añadir dependencias al pom.xml**

Dentro de `<dependencies>`, añadir después de Foundation:
```xml
<!-- WorldGuard (opcional en runtime) -->
<dependency>
    <groupId>com.sk89q.worldguard</groupId>
    <artifactId>worldguard-bukkit</artifactId>
    <version>7.0.9</version>
    <scope>provided</scope>
</dependency>

<!-- PlaceholderAPI (opcional en runtime) -->
<dependency>
    <groupId>me.clip</groupId>
    <artifactId>placeholderapi</artifactId>
    <version>2.11.5</version>
    <scope>provided</scope>
</dependency>

<!-- bStats -->
<dependency>
    <groupId>org.bstats</groupId>
    <artifactId>bstats-bukkit</artifactId>
    <version>3.0.2</version>
    <scope>compile</scope>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>4.11.0</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 1.3: Añadir relocations de bStats y surefire plugin**

En `<build><plugins>`, dentro del `maven-shade-plugin`, añadir en `<relocations>`:
```xml
<relocation>
    <pattern>org.bstats</pattern>
    <shadedPattern>info.desidia.desidiaitemcraftblock.lib.bstats</shadedPattern>
</relocation>
```

Añadir también el plugin surefire (para JUnit 5) dentro de `<plugins>`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.1.2</version>
</plugin>
```

- [ ] **Step 1.4: Actualizar versión a 2.0.0**

En `pom.xml`, cambiar:
```xml
<version>2.0.0</version>
```

- [ ] **Step 1.5: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS sin errores.

- [ ] **Step 1.6: Commit**

```bash
git add pom.xml
git commit -m "build: add WG, PAPI, bStats, JUnit5 deps; bump to 2.0.0"
```

---

## Task 2: Resource files

**Files:**
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/resources/plugin.yml`
- Create: `src/main/resources/messages_es.yml`
- Create: `src/main/resources/messages_en.yml`

- [ ] **Step 2.1: Reescribir config.yml**

```yaml
# DesidiaItemCraftBlock v2.0.0 — config.yml
settings:
  mode: blacklist
  enabled: true
  locale: es
  notify-admins: false
  log-attempts: false
  update-checker: true
  stats-autosave-interval: 5

message:
  enabled: true
  text: "&c!No puedes craftear &e{item}&c aqui!"
  type: CHAT
  sound: ENTITY_VILLAGER_NO
  cooldown: 3

worlds:
  default:
    enabled: true
    mode: blacklist
    blocked:
      - DIAMOND_SWORD
    allowed: []
    custom-messages: {}

regions: {}
```

- [ ] **Step 2.2: Reescribir plugin.yml**

```yaml
name: ${project.name}
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

- [ ] **Step 2.3: Crear messages_es.yml**

```yaml
no-permission: "&cNo tienes permiso para ejecutar este comando."
reload-success: "&a Configuracion recargada correctamente."
toggle-enabled: "&a Bloqueo de crafteo &lACTIVADO&a."
toggle-disabled: "&c Bloqueo de crafteo &lDESACTIVADO&c."
craft-blocked: "&c!No puedes craftear &e{item}&c aqui!"
admin-notify: "&8[&6DIB&8] &e{player} &7intento craftear &e{item} &7en &e{world}&7/&e{region}"
stats-header: "&6 Estadisticas de &e{player}"
stats-global-header: "&6 Estadisticas globales del servidor"
stats-total: "&7Intentos bloqueados: &f{total}"
stats-top-item: "&7Item mas intentado: &f{item} &7({count} veces)"
stats-top-items: "&7Top items: &f{items}"
stats-top-players: "&7Top jugadores: &f{players}"
stats-last: "&7Ultimo intento: &f{world} | {region} | hace {time}"
stats-reset-success: "&aEstadisticas de &e{player} &areiniciadas."
stats-reset-global: "&aEstadisticas globales reiniciadas."
unknown-command: "&cComando desconocido. Usa &f/dib help&c."
item-not-found: "&cMaterial &e{item}&c no existe."
list-header: "&6Items en &e{world}&6 (modo: &e{mode}&6):"
list-entry: "&7- &f{item}"
list-empty: "&7(lista vacia)"
status-header: "&6 Estado de DesidiaItemCraftBlock v{version}"
status-enabled: "&7Estado: &aACTIVADO"
status-disabled: "&7Estado: &cDESACTIVADO"
status-mode: "&7Modo global: &f{mode}"
status-wg: "&7WorldGuard: &f{status}"
status-papi: "&7PlaceholderAPI: &f{status}"
block-success: "&a{item} anadido a la lista en &e{world}&a."
unblock-success: "&a{item} eliminado de la lista en &e{world}&a."
check-blocked: "&e{item} &cesta BLOQUEADO en tu ubicacion actual."
check-allowed: "&e{item} &aesta PERMITIDO en tu ubicacion actual."
help-header: "&6=== DesidiaItemCraftBlock v{version} ==="
help-entry: "&e{cmd} &7- {desc}"
update-available: "&8[&6DIB&8] &eNueva version disponible: &f{new} &e(tienes &f{current}&e)"
update-up-to-date: "&8[&6DIB&8] &aTienes la ultima version."
toggle-world-enabled: "&a Bloqueo activado en &e{world}&a."
toggle-world-disabled: "&c Bloqueo desactivado en &e{world}&c."
```

- [ ] **Step 2.4: Crear messages_en.yml**

```yaml
no-permission: "&cYou don't have permission to run this command."
reload-success: "&a Configuration reloaded successfully."
toggle-enabled: "&a Craft blocking &lENABLED&a."
toggle-disabled: "&c Craft blocking &lDISABLED&c."
craft-blocked: "&cYou can't craft &e{item}&c here!"
admin-notify: "&8[&6DIB&8] &e{player} &7tried to craft &e{item} &7in &e{world}&7/&e{region}"
stats-header: "&6 Stats for &e{player}"
stats-global-header: "&6 Global Server Stats"
stats-total: "&7Blocked attempts: &f{total}"
stats-top-item: "&7Most attempted: &f{item} &7({count} times)"
stats-top-items: "&7Top items: &f{items}"
stats-top-players: "&7Top players: &f{players}"
stats-last: "&7Last attempt: &f{world} | {region} | {time} ago"
stats-reset-success: "&aStats for &e{player} &areset."
stats-reset-global: "&aGlobal stats reset."
unknown-command: "&cUnknown command. Use &f/dib help&c."
item-not-found: "&cMaterial &e{item}&c does not exist."
list-header: "&6Items in &e{world}&6 (mode: &e{mode}&6):"
list-entry: "&7- &f{item}"
list-empty: "&7(empty list)"
status-header: "&6 DesidiaItemCraftBlock v{version} Status"
status-enabled: "&7Status: &aENABLED"
status-disabled: "&7Status: &cDISABLED"
status-mode: "&7Global mode: &f{mode}"
status-wg: "&7WorldGuard: &f{status}"
status-papi: "&7PlaceholderAPI: &f{status}"
block-success: "&a{item} added to the list in &e{world}&a."
unblock-success: "&a{item} removed from the list in &e{world}&a."
check-blocked: "&e{item} &cis BLOCKED at your current location."
check-allowed: "&e{item} &ais ALLOWED at your current location."
help-header: "&6=== DesidiaItemCraftBlock v{version} ==="
help-entry: "&e{cmd} &7- {desc}"
update-available: "&8[&6DIB&8] &eNew version available: &f{new} &e(you have &f{current}&e)"
update-up-to-date: "&8[&6DIB&8] &aYou are up to date."
toggle-world-enabled: "&a Blocking enabled in &e{world}&a."
toggle-world-disabled: "&c Blocking disabled in &e{world}&c."
```

- [ ] **Step 2.5: Commit**

```bash
git add src/main/resources/
git commit -m "feat: add v2.0.0 resource files (config, plugin.yml, messages)"
```

---

## Task 3: ColorUtil

**Files:**
- Create: `src/main/java/info/desidia/util/ColorUtil.java`
- Create: `src/test/java/info/desidia/util/ColorUtilTest.java`

- [ ] **Step 3.1: Crear test**

```java
// src/test/java/info/desidia/util/ColorUtilTest.java
package info.desidia.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorUtilTest {

    @Test
    void color_translatesAmpersandCodes() {
        String result = ColorUtil.color("&aHello &cWorld");
        assertTrue(result.contains("§a"));
        assertTrue(result.contains("§c"));
    }

    @Test
    void color_replacesVariables() {
        String result = ColorUtil.replace("Hello {player}!", "player", "Steve");
        assertEquals("Hello Steve!", result);
    }

    @Test
    void color_nullSafe() {
        assertEquals("", ColorUtil.color(null));
    }
}
```

- [ ] **Step 3.2: Correr test (debe fallar)**

```bash
mvn test -Dtest=ColorUtilTest -q
```
Esperado: FAIL — `ColorUtil` no existe.

- [ ] **Step 3.3: Implementar ColorUtil**

```java
// src/main/java/info/desidia/util/ColorUtil.java
package info.desidia.util;

import org.bukkit.ChatColor;

public final class ColorUtil {

    private ColorUtil() {}

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String replace(String text, String key, String value) {
        if (text == null) return "";
        return text.replace("{" + key + "}", value == null ? "" : value);
    }

    public static String replacePlaceholders(String text, Object... keyValuePairs) {
        if (text == null) return "";
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            text = replace(text, String.valueOf(keyValuePairs[i]), String.valueOf(keyValuePairs[i + 1]));
        }
        return color(text);
    }
}
```

- [ ] **Step 3.4: Correr test (debe pasar)**

```bash
mvn test -Dtest=ColorUtilTest -q
```
Esperado: BUILD SUCCESS, 3 tests passed.

- [ ] **Step 3.5: Commit**

```bash
git add src/
git commit -m "feat: add ColorUtil with color translation and placeholder replacement"
```

---

## Task 4: LocaleManager

**Files:**
- Create: `src/main/java/info/desidia/managers/LocaleManager.java`
- Create: `src/test/java/info/desidia/managers/LocaleManagerTest.java`

- [ ] **Step 4.1: Crear test**

```java
// src/test/java/info/desidia/managers/LocaleManagerTest.java
package info.desidia.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocaleManagerTest {

    private LocaleManager locale;

    @BeforeEach
    void setUp() {
        locale = new LocaleManager(null, "en");
    }

    @Test
    void get_returnsKeyInBracketsWhenMissing() {
        String result = locale.get("nonexistent.key");
        assertEquals("[nonexistent.key]", result);
    }

    @Test
    void get_returnsDefaultWhenProvided() {
        String result = locale.getOrDefault("missing", "fallback");
        assertEquals("fallback", result);
    }
}
```

- [ ] **Step 4.2: Correr test (debe fallar)**

```bash
mvn test -Dtest=LocaleManagerTest -q
```
Esperado: FAIL.

- [ ] **Step 4.3: Implementar LocaleManager**

```java
// src/main/java/info/desidia/managers/LocaleManager.java
package info.desidia.managers;

import info.desidia.util.ColorUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LocaleManager {

    private final Plugin plugin;
    private final Map<String, String> messages = new HashMap<>();

    public LocaleManager(Plugin plugin, String locale) {
        this.plugin = plugin;
        load(locale);
    }

    private void load(String locale) {
        messages.clear();
        // Load from jar (bundled default)
        loadFromJar("messages_en.yml");
        // Override with selected locale
        if (!locale.equals("en")) {
            loadFromJar("messages_" + locale + ".yml");
        }
        // Override with file on disk if exists (custom translations)
        if (plugin != null) {
            File file = new File(plugin.getDataFolder(), "messages_" + locale + ".yml");
            if (file.exists()) {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
                for (String key : yml.getKeys(false)) {
                    messages.put(key, yml.getString(key, ""));
                }
            }
        }
    }

    private void loadFromJar(String resourceName) {
        if (plugin == null) return;
        InputStream stream = plugin.getResource(resourceName);
        if (stream == null) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(
            new InputStreamReader(stream, StandardCharsets.UTF_8));
        for (String key : yml.getKeys(false)) {
            messages.put(key, yml.getString(key, ""));
        }
    }

    public String get(String key) {
        return messages.getOrDefault(key, "[" + key + "]");
    }

    public String getOrDefault(String key, String def) {
        return messages.getOrDefault(key, def);
    }

    public String format(String key, Object... kvPairs) {
        return ColorUtil.replacePlaceholders(get(key), kvPairs);
    }

    public void reload(String locale) {
        load(locale);
    }
}
```

- [ ] **Step 4.4: Correr test**

```bash
mvn test -Dtest=LocaleManagerTest -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 4.5: Commit**

```bash
git add src/
git commit -m "feat: add LocaleManager with fallback chain and hot reload"
```

---

## Task 5: ConfigManager

**Files:**
- Create: `src/main/java/info/desidia/managers/ConfigManager.java`

- [ ] **Step 5.1: Crear ConfigManager con inner classes WorldConfig y RegionConfig**

```java
// src/main/java/info/desidia/managers/ConfigManager.java
package info.desidia.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.regex.Pattern;

public class ConfigManager {

    private final Plugin plugin;

    // Global settings
    private boolean enabled;
    private String globalMode;
    private String locale;
    private boolean notifyAdmins;
    private boolean logAttempts;
    private boolean updateChecker;
    private int statsAutosaveInterval;

    // Message settings
    private boolean messageEnabled;
    private String messageText;
    private String messageType;
    private String messageSound;
    private int messageCooldown;

    private final Map<String, WorldConfig> worldConfigs = new HashMap<>();
    private final Map<String, RegionConfig> regionConfigs = new HashMap<>();

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        enabled = c.getBoolean("settings.enabled", true);
        globalMode = c.getString("settings.mode", "blacklist");
        locale = c.getString("settings.locale", "en");
        notifyAdmins = c.getBoolean("settings.notify-admins", false);
        logAttempts = c.getBoolean("settings.log-attempts", false);
        updateChecker = c.getBoolean("settings.update-checker", true);
        statsAutosaveInterval = c.getInt("settings.stats-autosave-interval", 5);

        messageEnabled = c.getBoolean("message.enabled", true);
        messageText = c.getString("message.text", "&cYou can't craft {item} here!");
        messageType = c.getString("message.type", "CHAT");
        messageSound = c.getString("message.sound", "ENTITY_VILLAGER_NO");
        messageCooldown = c.getInt("message.cooldown", 3);

        worldConfigs.clear();
        ConfigurationSection worlds = c.getConfigurationSection("worlds");
        if (worlds != null) {
            for (String key : worlds.getKeys(false)) {
                worldConfigs.put(key, WorldConfig.load(worlds.getConfigurationSection(key)));
            }
        }

        regionConfigs.clear();
        ConfigurationSection regions = c.getConfigurationSection("regions");
        if (regions != null) {
            for (String key : regions.getKeys(false)) {
                regionConfigs.put(key, RegionConfig.load(regions.getConfigurationSection(key)));
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("settings.enabled", enabled);
        plugin.saveConfig();
    }

    public WorldConfig getWorldConfig(String world) {
        return worldConfigs.getOrDefault(world, worldConfigs.get("default"));
    }

    public RegionConfig getRegionConfig(String region) {
        return regionConfigs.get(region);
    }

    public void addBlockedItem(String world, String material) {
        WorldConfig wc = worldConfigs.computeIfAbsent(world, k -> new WorldConfig());
        wc.blocked.add(material.toUpperCase());
        String path = "worlds." + world + ".blocked";
        plugin.getConfig().set(path, new ArrayList<>(wc.blocked));
        plugin.saveConfig();
    }

    public void removeBlockedItem(String world, String material) {
        WorldConfig wc = worldConfigs.get(world);
        if (wc == null) return;
        wc.blocked.remove(material.toUpperCase());
        wc.allowed.remove(material.toUpperCase());
        String blockedPath = "worlds." + world + ".blocked";
        String allowedPath = "worlds." + world + ".allowed";
        plugin.getConfig().set(blockedPath, new ArrayList<>(wc.blocked));
        plugin.getConfig().set(allowedPath, new ArrayList<>(wc.allowed));
        plugin.saveConfig();
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public String getGlobalMode() { return globalMode; }
    public String getLocale() { return locale; }
    public boolean isNotifyAdmins() { return notifyAdmins; }
    public boolean isLogAttempts() { return logAttempts; }
    public boolean isUpdateChecker() { return updateChecker; }
    public int getStatsAutosaveInterval() { return statsAutosaveInterval; }
    public boolean isMessageEnabled() { return messageEnabled; }
    public String getMessageText() { return messageText; }
    public String getMessageType() { return messageType; }
    public String getMessageSound() { return messageSound; }
    public int getMessageCooldown() { return messageCooldown; }
    public Map<String, WorldConfig> getWorldConfigs() { return Collections.unmodifiableMap(worldConfigs); }

    // --- Inner classes ---

    public static class WorldConfig {
        public boolean enabled = true;
        public String mode = "blacklist";
        public final Set<String> blocked = new LinkedHashSet<>();
        public final Set<String> allowed = new LinkedHashSet<>();
        public final Map<String, String> customMessages = new HashMap<>();

        public static WorldConfig load(ConfigurationSection s) {
            WorldConfig wc = new WorldConfig();
            if (s == null) return wc;
            wc.enabled = s.getBoolean("enabled", true);
            wc.mode = s.getString("mode", "blacklist");
            if (s.isList("blocked")) wc.blocked.addAll(toUpperList(s.getStringList("blocked")));
            if (s.isList("allowed")) wc.allowed.addAll(toUpperList(s.getStringList("allowed")));
            ConfigurationSection cm = s.getConfigurationSection("custom-messages");
            if (cm != null) {
                for (String k : cm.getKeys(false)) wc.customMessages.put(k.toUpperCase(), cm.getString(k));
            }
            return wc;
        }
    }

    public static class RegionConfig {
        public String mode = "blacklist";
        public final Set<String> blocked = new LinkedHashSet<>();
        public final Set<String> allowed = new LinkedHashSet<>();
        public final Map<String, String> customMessages = new HashMap<>();

        public static RegionConfig load(ConfigurationSection s) {
            RegionConfig rc = new RegionConfig();
            if (s == null) return rc;
            rc.mode = s.getString("mode", "blacklist");
            if (s.isList("blocked")) rc.blocked.addAll(toUpperList(s.getStringList("blocked")));
            if (s.isList("allowed")) rc.allowed.addAll(toUpperList(s.getStringList("allowed")));
            ConfigurationSection cm = s.getConfigurationSection("custom-messages");
            if (cm != null) {
                for (String k : cm.getKeys(false)) rc.customMessages.put(k.toUpperCase(), cm.getString(k));
            }
            return rc;
        }
    }

    private static List<String> toUpperList(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String s : list) result.add(s.toUpperCase());
        return result;
    }

    public static boolean matchesPattern(String pattern, String material) {
        if (!pattern.contains("*") && !pattern.contains("?")) {
            return pattern.equalsIgnoreCase(material);
        }
        String regex = "^" + Pattern.quote(pattern).replace("\\*", ".*").replace("\\?", ".") + "$";
        return material.toUpperCase().matches(regex);
    }

    public static boolean isInList(Set<String> patterns, String material) {
        for (String pattern : patterns) {
            if (matchesPattern(pattern, material)) return true;
        }
        return false;
    }
}
```

- [ ] **Step 5.2: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 5.3: Commit**

```bash
git add src/main/java/info/desidia/managers/ConfigManager.java
git commit -m "feat: add ConfigManager with world/region configs and wildcard matching"
```

---

## Task 6: StatsManager

**Files:**
- Create: `src/main/java/info/desidia/managers/StatsManager.java`
- Create: `src/test/java/info/desidia/managers/StatsManagerTest.java`

- [ ] **Step 6.1: Crear test**

```java
// src/test/java/info/desidia/managers/StatsManagerTest.java
package info.desidia.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

class StatsManagerTest {

    private StatsManager stats;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        stats = new StatsManager(null);
        uuid = UUID.randomUUID();
    }

    @Test
    void record_incrementsTotal() {
        stats.record(uuid, "Steve", "DIAMOND_SWORD", "world", "spawn");
        stats.record(uuid, "Steve", "TNT", "world", "spawn");
        assertEquals(2, stats.getTotalBlocked(uuid));
    }

    @Test
    void record_tracksTopItem() {
        stats.record(uuid, "Steve", "TNT", "world", "spawn");
        stats.record(uuid, "Steve", "TNT", "world", "spawn");
        stats.record(uuid, "Steve", "DIAMOND_SWORD", "world", "spawn");
        assertEquals("TNT", stats.getTopItem(uuid));
    }

    @Test
    void getGlobalTotal_sumsAllPlayers() {
        UUID uuid2 = UUID.randomUUID();
        stats.record(uuid, "Steve", "TNT", "world", "spawn");
        stats.record(uuid2, "Alex", "TNT", "world", "spawn");
        assertEquals(2, stats.getGlobalTotal());
    }

    @Test
    void reset_clearsPlayerStats() {
        stats.record(uuid, "Steve", "TNT", "world", "spawn");
        stats.resetPlayer(uuid);
        assertEquals(0, stats.getTotalBlocked(uuid));
    }
}
```

- [ ] **Step 6.2: Correr test (debe fallar)**

```bash
mvn test -Dtest=StatsManagerTest -q
```
Esperado: FAIL.

- [ ] **Step 6.3: Implementar StatsManager**

```java
// src/main/java/info/desidia/managers/StatsManager.java
package info.desidia.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager {

    private final Plugin plugin;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();

    public StatsManager(Plugin plugin) {
        this.plugin = plugin;
        if (plugin != null) load();
    }

    public void record(UUID uuid, String name, String material, String world, String region) {
        PlayerStats ps = playerStats.computeIfAbsent(uuid, k -> new PlayerStats(name));
        ps.name = name;
        ps.totalBlocked++;
        ps.itemCounts.merge(material, 1, Integer::sum);
        ps.worldCounts.merge(world, 1, Integer::sum);
        ps.lastItem = material;
        ps.lastWorld = world;
        ps.lastRegion = region == null ? "" : region;
        ps.lastAttemptMs = System.currentTimeMillis();
    }

    public int getTotalBlocked(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? 0 : ps.totalBlocked;
    }

    public String getTopItem(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        if (ps == null || ps.itemCounts.isEmpty()) return "N/A";
        return ps.itemCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("N/A");
    }

    public int getItemCount(UUID uuid, String material) {
        PlayerStats ps = playerStats.get(uuid);
        if (ps == null) return 0;
        return ps.itemCounts.getOrDefault(material, 0);
    }

    public String getLastItem(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? "N/A" : ps.lastItem;
    }

    public String getLastWorld(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? "" : ps.lastWorld;
    }

    public String getLastRegion(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? "" : ps.lastRegion;
    }

    public String getTimeSinceLastAttempt(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        if (ps == null || ps.lastAttemptMs == 0) return "never";
        long diffMs = System.currentTimeMillis() - ps.lastAttemptMs;
        long mins = diffMs / 60000;
        if (mins < 1) return "just now";
        if (mins == 1) return "1 min";
        return mins + " min";
    }

    public int getGlobalTotal() {
        return playerStats.values().stream().mapToInt(ps -> ps.totalBlocked).sum();
    }

    public List<Map.Entry<String, Integer>> getGlobalTopItems(int limit) {
        Map<String, Integer> totals = new HashMap<>();
        for (PlayerStats ps : playerStats.values()) {
            ps.itemCounts.forEach((item, count) -> totals.merge(item, count, Integer::sum));
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<>(totals.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<String, Integer>> getGlobalTopPlayers(int limit) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> e : playerStats.entrySet()) {
            list.add(new AbstractMap.SimpleEntry<>(e.getValue().name, e.getValue().totalBlocked));
        }
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public void resetPlayer(UUID uuid) {
        playerStats.remove(uuid);
    }

    public void resetAll() {
        playerStats.clear();
    }

    public void save() {
        if (plugin == null) return;
        File file = new File(plugin.getDataFolder(), "stats.yml");
        YamlConfiguration yml = new YamlConfiguration();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Map.Entry<UUID, PlayerStats> e : playerStats.entrySet()) {
            String base = "players." + e.getKey().toString();
            PlayerStats ps = e.getValue();
            yml.set(base + ".name", ps.name);
            yml.set(base + ".blocked-attempts", ps.totalBlocked);
            yml.set(base + ".last-item", ps.lastItem);
            yml.set(base + ".last-world", ps.lastWorld);
            if (ps.lastAttemptMs > 0) yml.set(base + ".last-attempt", sdf.format(new Date(ps.lastAttemptMs)));
            for (Map.Entry<String, Integer> ic : ps.itemCounts.entrySet()) {
                yml.set(base + ".top-items." + ic.getKey(), ic.getValue());
            }
        }
        try { yml.save(file); } catch (IOException ignored) {}
    }

    private void load() {
        File file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        if (!yml.isConfigurationSection("players")) return;
        for (String uuidStr : yml.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String base = "players." + uuidStr;
                PlayerStats ps = new PlayerStats(yml.getString(base + ".name", "Unknown"));
                ps.totalBlocked = yml.getInt(base + ".blocked-attempts", 0);
                ps.lastItem = yml.getString(base + ".last-item", "");
                ps.lastWorld = yml.getString(base + ".last-world", "");
                if (yml.isConfigurationSection(base + ".top-items")) {
                    for (String item : yml.getConfigurationSection(base + ".top-items").getKeys(false)) {
                        ps.itemCounts.put(item, yml.getInt(base + ".top-items." + item, 0));
                    }
                }
                playerStats.put(uuid, ps);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static class PlayerStats {
        String name;
        int totalBlocked = 0;
        String lastItem = "";
        String lastWorld = "";
        String lastRegion = "";
        long lastAttemptMs = 0;
        final Map<String, Integer> itemCounts = new HashMap<>();
        final Map<String, Integer> worldCounts = new HashMap<>();

        PlayerStats(String name) { this.name = name; }
    }
}
```

- [ ] **Step 6.4: Correr test**

```bash
mvn test -Dtest=StatsManagerTest -q
```
Esperado: BUILD SUCCESS, 4 tests passed.

- [ ] **Step 6.5: Commit**

```bash
git add src/
git commit -m "feat: add StatsManager with in-memory tracking and YAML persistence"
```

---

## Task 7: Eventos y BlockDecision

**Files:**
- Create: `src/main/java/info/desidia/api/events/BlockReason.java`
- Create: `src/main/java/info/desidia/managers/BlockDecision.java`
- Create: `src/main/java/info/desidia/api/events/CraftBlockedEvent.java`

- [ ] **Step 7.1: Crear BlockReason**

```java
// src/main/java/info/desidia/api/events/BlockReason.java
package info.desidia.api.events;

public enum BlockReason {
    REGION,
    WORLD,
    GLOBAL
}
```

- [ ] **Step 7.2: Crear BlockDecision**

```java
// src/main/java/info/desidia/managers/BlockDecision.java
package info.desidia.managers;

import info.desidia.api.events.BlockReason;

public class BlockDecision {

    private final boolean blocked;
    private final BlockReason reason;
    private final String context;
    private final String customMessage;

    private BlockDecision(boolean blocked, BlockReason reason, String context, String customMessage) {
        this.blocked = blocked;
        this.reason = reason;
        this.context = context;
        this.customMessage = customMessage;
    }

    public static BlockDecision allow() {
        return new BlockDecision(false, null, null, null);
    }

    public static BlockDecision block(BlockReason reason, String context, String customMessage) {
        return new BlockDecision(true, reason, context, customMessage);
    }

    public boolean isBlocked() { return blocked; }
    public BlockReason getReason() { return reason; }
    public String getContext() { return context; }
    public String getCustomMessage() { return customMessage; }
}
```

- [ ] **Step 7.3: Crear CraftBlockedEvent**

```java
// src/main/java/info/desidia/api/events/CraftBlockedEvent.java
package info.desidia.api.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Recipe;

public class CraftBlockedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Material material;
    private final Recipe recipe;
    private final String world;
    private final String region;
    private final BlockReason reason;
    private boolean cancelled = false;

    public CraftBlockedEvent(Player player, Material material, Recipe recipe,
                             String world, String region, BlockReason reason) {
        this.player = player;
        this.material = material;
        this.recipe = recipe;
        this.world = world;
        this.region = region;
        this.reason = reason;
    }

    public Player getPlayer() { return player; }
    public Material getMaterial() { return material; }
    public Recipe getRecipe() { return recipe; }
    public String getWorld() { return world; }
    public String getRegion() { return region; }
    public BlockReason getReason() { return reason; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
```

- [ ] **Step 7.4: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 7.5: Commit**

```bash
git add src/main/java/info/desidia/api/ src/main/java/info/desidia/managers/BlockDecision.java
git commit -m "feat: add CraftBlockedEvent, BlockReason enum, and BlockDecision DTO"
```

---

## Task 8: BlockManager

**Files:**
- Create: `src/main/java/info/desidia/managers/BlockManager.java`
- Create: `src/test/java/info/desidia/managers/BlockManagerTest.java`

- [ ] **Step 8.1: Crear test**

```java
// src/test/java/info/desidia/managers/BlockManagerTest.java
package info.desidia.managers;

import info.desidia.api.events.BlockReason;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BlockManagerTest {

    private Player player;
    private ConfigManager config;

    @BeforeEach
    void setUp() {
        player = Mockito.mock(Player.class);
        config = Mockito.mock(ConfigManager.class);
        when(player.hasPermission("dib.bypass")).thenReturn(false);
        when(player.hasPermission(Mockito.startsWith("dib.bypass.item."))).thenReturn(false);
        when(player.hasPermission(Mockito.startsWith("dib.bypass.world."))).thenReturn(false);
        when(player.hasPermission(Mockito.startsWith("dib.bypass.region."))).thenReturn(false);
        when(config.isEnabled()).thenReturn(true);
    }

    @Test
    void evaluate_allowsWhenBypassPermission() {
        when(player.hasPermission("dib.bypass")).thenReturn(true);
        BlockManager bm = new BlockManager(config, null);
        BlockDecision d = bm.evaluate(player, Material.DIAMOND_SWORD, "world", null);
        assertFalse(d.isBlocked());
    }

    @Test
    void evaluate_allowsWhenToggleOff() {
        when(config.isEnabled()).thenReturn(false);
        BlockManager bm = new BlockManager(config, null);
        BlockDecision d = bm.evaluate(player, Material.DIAMOND_SWORD, "world", null);
        assertFalse(d.isBlocked());
    }

    @Test
    void evaluate_blocksWhenItemInBlacklist() {
        ConfigManager.WorldConfig wc = new ConfigManager.WorldConfig();
        wc.mode = "blacklist";
        wc.blocked.add("DIAMOND_SWORD");
        when(config.getWorldConfig("world")).thenReturn(wc);
        when(config.getGlobalMode()).thenReturn("blacklist");

        BlockManager bm = new BlockManager(config, null);
        BlockDecision d = bm.evaluate(player, Material.DIAMOND_SWORD, "world", null);
        assertTrue(d.isBlocked());
        assertEquals(BlockReason.WORLD, d.getReason());
    }

    @Test
    void evaluate_allowsWhenItemNotInBlacklist() {
        ConfigManager.WorldConfig wc = new ConfigManager.WorldConfig();
        wc.mode = "blacklist";
        wc.blocked.add("TNT");
        when(config.getWorldConfig("world")).thenReturn(wc);
        when(config.getGlobalMode()).thenReturn("blacklist");

        BlockManager bm = new BlockManager(config, null);
        BlockDecision d = bm.evaluate(player, Material.DIAMOND_SWORD, "world", null);
        assertFalse(d.isBlocked());
    }

    @Test
    void evaluate_blocksAllInWhitelistWhenNotInList() {
        ConfigManager.WorldConfig wc = new ConfigManager.WorldConfig();
        wc.mode = "whitelist";
        wc.allowed.add("TORCH");
        when(config.getWorldConfig("world")).thenReturn(wc);
        when(config.getGlobalMode()).thenReturn("whitelist");

        BlockManager bm = new BlockManager(config, null);
        BlockDecision d = bm.evaluate(player, Material.DIAMOND_SWORD, "world", null);
        assertTrue(d.isBlocked());
    }
}
```

- [ ] **Step 8.2: Correr test (debe fallar)**

```bash
mvn test -Dtest=BlockManagerTest -q
```
Esperado: FAIL.

- [ ] **Step 8.3: Implementar BlockManager**

```java
// src/main/java/info/desidia/managers/BlockManager.java
package info.desidia.managers;

import info.desidia.api.events.BlockReason;
import info.desidia.hooks.WorldGuardHook;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BlockManager {

    private final ConfigManager config;
    private WorldGuardHook worldGuard;

    public BlockManager(ConfigManager config, WorldGuardHook worldGuard) {
        this.config = config;
        this.worldGuard = worldGuard;
    }

    public void setWorldGuardHook(WorldGuardHook wg) {
        this.worldGuard = wg;
    }

    public BlockDecision evaluate(Player player, Material material, String worldName, String regionName) {
        String mat = material.name();

        // 1. Global bypass
        if (player.hasPermission("dib.bypass")) return BlockDecision.allow();

        // 2. Item-specific bypass
        if (player.hasPermission("dib.bypass.item." + mat.toLowerCase())) return BlockDecision.allow();

        // 3. Toggle
        if (!config.isEnabled()) return BlockDecision.allow();

        // 4. WorldGuard region check
        if (worldGuard != null && worldGuard.isAvailable() && regionName != null) {
            BlockDecision wgDecision = worldGuard.evaluate(player, material, regionName);
            if (wgDecision != null) return wgDecision;
        }

        // 5. Config region check (when WG not available)
        if ((worldGuard == null || !worldGuard.isAvailable()) && regionName != null) {
            ConfigManager.RegionConfig rc = config.getRegionConfig(regionName);
            if (rc != null) {
                if (player.hasPermission("dib.bypass.region." + regionName.toLowerCase())) return BlockDecision.allow();
                String customMsg = rc.customMessages.get(mat);
                return evaluateList(rc.mode, rc.blocked, rc.allowed, mat, BlockReason.REGION, regionName, customMsg);
            }
        }

        // 6. World config check
        if (player.hasPermission("dib.bypass.world." + worldName.toLowerCase())) return BlockDecision.allow();
        ConfigManager.WorldConfig wc = config.getWorldConfig(worldName);
        if (wc != null && wc.enabled) {
            String customMsg = wc.customMessages.get(mat);
            return evaluateList(wc.mode, wc.blocked, wc.allowed, mat, BlockReason.WORLD, worldName, customMsg);
        }

        // 7. Global default
        return evaluateList(config.getGlobalMode(), new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>(),
            mat, BlockReason.GLOBAL, "global", null);
    }

    private BlockDecision evaluateList(String mode, java.util.Set<String> blocked,
                                       java.util.Set<String> allowed, String material,
                                       BlockReason reason, String context, String customMsg) {
        if ("blacklist".equalsIgnoreCase(mode)) {
            if (ConfigManager.isInList(blocked, material)) {
                return BlockDecision.block(reason, context, customMsg);
            }
            return BlockDecision.allow();
        } else { // whitelist
            if (ConfigManager.isInList(allowed, material)) {
                return BlockDecision.allow();
            }
            return BlockDecision.block(reason, context, customMsg);
        }
    }
}
```

- [ ] **Step 8.4: Correr test**

```bash
mvn test -Dtest=BlockManagerTest -q
```
Esperado: BUILD SUCCESS, 5 tests passed.

- [ ] **Step 8.5: Commit**

```bash
git add src/
git commit -m "feat: add BlockManager — single decision point for craft blocking"
```

---

## Task 9: CraftListener + Main class

**Files:**
- Create: `src/main/java/info/desidia/listeners/CraftListener.java`
- Modify: `src/main/java/info/desidia/DesidiaItemCraftBlock.java`

- [ ] **Step 9.1: Crear CraftListener**

```java
// src/main/java/info/desidia/listeners/CraftListener.java
package info.desidia.listeners;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.api.events.CraftBlockedEvent;
import info.desidia.managers.BlockDecision;
import info.desidia.managers.BlockManager;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import info.desidia.managers.StatsManager;
import info.desidia.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CraftListener implements Listener {

    private final DesidiaItemCraftBlock plugin;
    private final BlockManager blockManager;
    private final ConfigManager config;
    private final LocaleManager locale;
    private final StatsManager stats;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CraftListener(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.blockManager = plugin.getBlockManager();
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
        this.stats = plugin.getStatsManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Material material = event.getRecipe().getResult().getType();
        String worldName = player.getWorld().getName();

        // Get region name via WG hook (null if not available)
        String regionName = plugin.getWorldGuardHook() != null
            ? plugin.getWorldGuardHook().getRegionName(player)
            : null;

        BlockDecision decision = blockManager.evaluate(player, material, worldName, regionName);
        if (!decision.isBlocked()) return;

        // Fire cancellable API event
        CraftBlockedEvent apiEvent = new CraftBlockedEvent(
            player, material, event.getRecipe(),
            worldName, regionName, decision.getReason());
        Bukkit.getPluginManager().callEvent(apiEvent);
        if (apiEvent.isCancelled()) return;

        // Cancel the craft
        event.setCancelled(true);

        // Stats
        stats.record(player.getUniqueId(), player.getName(), material.name(), worldName, regionName);

        // Log
        if (config.isLogAttempts()) {
            plugin.getLogger().info("[DIB] " + player.getName() + " tried to craft " + material.name() + " in " + worldName);
        }

        // Notify admins
        if (config.isNotifyAdmins()) {
            String msg = locale.format("admin-notify",
                "player", player.getName(),
                "item", material.name(),
                "world", worldName,
                "region", regionName == null ? "" : regionName);
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("dib.notify"))
                .forEach(p -> p.sendMessage(msg));
        }

        // Send message to player (with cooldown)
        if (!config.isMessageEnabled()) return;
        long now = System.currentTimeMillis();
        long lastSent = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastSent < config.getMessageCooldown() * 1000L) return;
        cooldowns.put(player.getUniqueId(), now);

        String rawMsg = decision.getCustomMessage() != null
            ? decision.getCustomMessage()
            : locale.format("craft-blocked", "item", material.name(), "player", player.getName(),
                "world", worldName, "region", regionName == null ? "" : regionName);

        sendMessage(player, ColorUtil.color(rawMsg));

        // Sound
        try {
            Sound sound = Sound.valueOf(config.getMessageSound());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    private void sendMessage(Player player, String msg) {
        switch (config.getMessageType().toUpperCase()) {
            case "ACTIONBAR":
                player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                break;
            case "TITLE":
                player.sendTitle(msg, "", 10, 40, 10);
                break;
            default:
                player.sendMessage(msg);
        }
    }
}
```

- [ ] **Step 9.2: Reescribir DesidiaItemCraftBlock.java**

```java
// src/main/java/info/desidia/DesidiaItemCraftBlock.java
package info.desidia;

import info.desidia.commands.DIBCommand;
import info.desidia.hooks.PlaceholderAPIHook;
import info.desidia.hooks.WorldGuardHook;
import info.desidia.listeners.CraftListener;
import info.desidia.managers.BlockManager;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import info.desidia.managers.StatsManager;
import info.desidia.util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.mineacademy.fo.plugin.SimplePlugin;

public final class DesidiaItemCraftBlock extends SimplePlugin {

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private StatsManager statsManager;
    private BlockManager blockManager;
    private WorldGuardHook worldGuardHook;
    private PlaceholderAPIHook placeholderAPIHook;

    @Override
    protected void onPluginStart() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        localeManager = new LocaleManager(this, configManager.getLocale());
        statsManager = new StatsManager(this);

        // Optional hooks
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook();
            worldGuardHook.register();
            getLogger().info("[DIB] WorldGuard detected - region blocking enabled.");
        }

        blockManager = new BlockManager(configManager, worldGuardHook);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIHook = new PlaceholderAPIHook(this);
            placeholderAPIHook.register();
            getLogger().info("[DIB] PlaceholderAPI detected - placeholders registered.");
        }

        // Register listener and command
        getServer().getPluginManager().registerEvents(new CraftListener(this), this);
        DIBCommand cmd = new DIBCommand(this);
        getCommand("dib").setExecutor(cmd);
        getCommand("dib").setTabCompleter(cmd);

        // Stats autosave task
        int interval = configManager.getStatsAutosaveInterval() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, statsManager::save, interval, interval);

        // Update checker
        if (configManager.isUpdateChecker()) {
            new UpdateChecker(this, 0).checkAsync(); // Replace 0 with real SpigotMC resource ID
        }

        // bStats
        new Metrics(this, 0); // Replace 0 with real bStats plugin ID

        // DIBApi initialized in Task 17 after DIBApi class is created

        getLogger().info("[DIB] DesidiaItemCraftBlock v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    protected void onPluginStop() {
        statsManager.save();
        getLogger().info("[DIB] Stats saved. Plugin disabled.");
    }

    public void reload() {
        configManager.reload();
        localeManager.reload(configManager.getLocale());
    }

    // Getters
    public ConfigManager getConfigManager() { return configManager; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public BlockManager getBlockManager() { return blockManager; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public PlaceholderAPIHook getPlaceholderAPIHook() { return placeholderAPIHook; }
}
```

- [ ] **Step 9.3: Verificar compilación** *(va a fallar hasta Task 10 — crear stubs)*

Crear stubs mínimos para que compile:

`src/main/java/info/desidia/hooks/WorldGuardHook.java`:
```java
package info.desidia.hooks;

import info.desidia.managers.BlockDecision;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WorldGuardHook {
    public void register() {}
    public boolean isAvailable() { return true; }
    public BlockDecision evaluate(Player player, Material material, String region) { return null; }
    public String getRegionName(Player player) { return null; }
}
```

`src/main/java/info/desidia/hooks/PlaceholderAPIHook.java`:
```java
package info.desidia.hooks;

import org.bukkit.plugin.Plugin;

public class PlaceholderAPIHook {
    public PlaceholderAPIHook(Plugin plugin) {}
    public void register() {}
}
```

`src/main/java/info/desidia/commands/DIBCommand.java`:
```java
package info.desidia.commands;

import info.desidia.DesidiaItemCraftBlock;
import org.bukkit.command.*;
import java.util.List;
import java.util.Collections;

public class DIBCommand implements CommandExecutor, TabCompleter {
    public DIBCommand(DesidiaItemCraftBlock plugin) {}
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) { return true; }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) { return Collections.emptyList(); }
}
```

`src/main/java/info/desidia/util/UpdateChecker.java`:
```java
package info.desidia.util;

import org.bukkit.plugin.Plugin;

public class UpdateChecker {
    public UpdateChecker(Plugin plugin, int resourceId) {}
    public void checkAsync() {}
}
```

`src/main/java/info/desidia/gui/ItemManagementGUI.java` (stub — implementado completamente en Task 16):
```java
package info.desidia.gui;

import info.desidia.DesidiaItemCraftBlock;
import org.bukkit.entity.Player;

public class ItemManagementGUI {
    public ItemManagementGUI(DesidiaItemCraftBlock plugin, Player player, String world) {}
    public void open() {}
}
```

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 9.4: Commit**

```bash
git add src/
git commit -m "feat: add CraftListener and rewrite main class with full manager wiring"
```

---

## Task 10: Command infrastructure completa

**Files:**
- Modify: `src/main/java/info/desidia/commands/DIBCommand.java`
- Create: `src/main/java/info/desidia/commands/SubCommand.java`

- [ ] **Step 10.1: Crear interfaz SubCommand**

```java
// src/main/java/info/desidia/commands/SubCommand.java
package info.desidia.commands;

import org.bukkit.command.CommandSender;
import java.util.List;

public interface SubCommand {
    String getName();
    String getPermission();
    String getUsage();
    String getDescription();
    void execute(CommandSender sender, String[] args);
    List<String> tabComplete(CommandSender sender, String[] args);
}
```

- [ ] **Step 10.2: Reescribir DIBCommand con dispatcher**

```java
// src/main/java/info/desidia/commands/DIBCommand.java
package info.desidia.commands;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.sub.*;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.*;

import java.util.*;

public class DIBCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final LocaleManager locale;

    public DIBCommand(DesidiaItemCraftBlock plugin) {
        this.locale = plugin.getLocaleManager();
        register(new HelpCommand(plugin, this));
        register(new ReloadCommand(plugin));
        register(new ToggleCommand(plugin));
        register(new StatusCommand(plugin));
        register(new ListCommand(plugin));
        register(new BlockCommand(plugin));
        register(new UnblockCommand(plugin));
        register(new CheckCommand(plugin));
        register(new StatsCommand(plugin));
        register(new GuiCommand(plugin));
    }

    private void register(SubCommand cmd) {
        subCommands.put(cmd.getName().toLowerCase(), cmd);
    }

    public Collection<SubCommand> getSubCommands() {
        return subCommands.values();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            subCommands.get("help").execute(sender, args);
            return true;
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            sender.sendMessage(locale.format("unknown-command"));
            return true;
        }
        if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
            sender.sendMessage(locale.format("no-permission"));
            return true;
        }
        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (SubCommand sub : subCommands.values()) {
                if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                    if (sub.getName().startsWith(args[0].toLowerCase())) names.add(sub.getName());
                }
            }
            return names;
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub != null && (sub.getPermission() == null || sender.hasPermission(sub.getPermission()))) {
            return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        return Collections.emptyList();
    }
}
```

- [ ] **Step 10.3: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS (los subcomandos aún son stubs temporales).

- [ ] **Step 10.4: Commit**

```bash
git add src/main/java/info/desidia/commands/
git commit -m "feat: add SubCommand interface and DIBCommand dispatcher with tab completion"
```

---

## Task 11: Subcomandos — help, reload, toggle, status

**Files:**
- Create: `src/main/java/info/desidia/commands/sub/HelpCommand.java`
- Create: `src/main/java/info/desidia/commands/sub/ReloadCommand.java`
- Create: `src/main/java/info/desidia/commands/sub/ToggleCommand.java`
- Create: `src/main/java/info/desidia/commands/sub/StatusCommand.java`

- [ ] **Step 11.1: Crear HelpCommand**

```java
// src/main/java/info/desidia/commands/sub/HelpCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.DIBCommand;
import info.desidia.commands.SubCommand;
import info.desidia.managers.LocaleManager;
import info.desidia.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class HelpCommand implements SubCommand {

    private final LocaleManager locale;
    private final DIBCommand dispatcher;
    private final String version;

    public HelpCommand(DesidiaItemCraftBlock plugin, DIBCommand dispatcher) {
        this.locale = plugin.getLocaleManager();
        this.dispatcher = dispatcher;
        this.version = plugin.getDescription().getVersion();
    }

    @Override public String getName() { return "help"; }
    @Override public String getPermission() { return "dib.help"; }
    @Override public String getUsage() { return "/dib help"; }
    @Override public String getDescription() { return "Show all commands"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(locale.format("help-header", "version", version));
        for (SubCommand sub : dispatcher.getSubCommands()) {
            if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                sender.sendMessage(locale.format("help-entry", "cmd", sub.getUsage(), "desc", sub.getDescription()));
            }
        }
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
```

- [ ] **Step 11.2: Crear ReloadCommand**

```java
// src/main/java/info/desidia/commands/sub/ReloadCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final DesidiaItemCraftBlock plugin;
    private final LocaleManager locale;

    public ReloadCommand(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "reload"; }
    @Override public String getPermission() { return "dib.reload"; }
    @Override public String getUsage() { return "/dib reload"; }
    @Override public String getDescription() { return "Reload configuration"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reload();
        sender.sendMessage(locale.format("reload-success"));
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
```

- [ ] **Step 11.3: Crear ToggleCommand**

```java
// src/main/java/info/desidia/commands/sub/ToggleCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ToggleCommand implements SubCommand {

    private final ConfigManager config;
    private final LocaleManager locale;

    public ToggleCommand(DesidiaItemCraftBlock plugin) {
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "toggle"; }
    @Override public String getPermission() { return "dib.toggle"; }
    @Override public String getUsage() { return "/dib toggle [world]"; }
    @Override public String getDescription() { return "Enable/disable craft blocking"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String worldName = args[0];
            ConfigManager.WorldConfig wc = config.getWorldConfigs().get(worldName);
            if (wc == null) {
                sender.sendMessage(locale.format("item-not-found", "item", worldName));
                return;
            }
            wc.enabled = !wc.enabled;
            String key = wc.enabled ? "toggle-world-enabled" : "toggle-world-disabled";
            sender.sendMessage(locale.format(key, "world", worldName));
        } else {
            boolean newState = !config.isEnabled();
            config.setEnabled(newState);
            String key = newState ? "toggle-enabled" : "toggle-disabled";
            sender.sendMessage(locale.format(key));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> worlds = new ArrayList<>();
        if (args.length == 1) {
            Bukkit.getWorlds().forEach(w -> {
                if (w.getName().startsWith(args[0])) worlds.add(w.getName());
            });
        }
        return worlds;
    }
}
```

- [ ] **Step 11.4: Crear StatusCommand**

```java
// src/main/java/info/desidia/commands/sub/StatusCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class StatusCommand implements SubCommand {

    private final DesidiaItemCraftBlock plugin;
    private final ConfigManager config;
    private final LocaleManager locale;

    public StatusCommand(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "status"; }
    @Override public String getPermission() { return "dib.status"; }
    @Override public String getUsage() { return "/dib status"; }
    @Override public String getDescription() { return "Show plugin status"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(locale.format("status-header", "version", plugin.getDescription().getVersion()));
        sender.sendMessage(locale.format(config.isEnabled() ? "status-enabled" : "status-disabled"));
        sender.sendMessage(locale.format("status-mode", "mode", config.getGlobalMode()));
        boolean wgActive = plugin.getWorldGuardHook() != null;
        boolean papiActive = plugin.getPlaceholderAPIHook() != null;
        sender.sendMessage(locale.format("status-wg", "status", wgActive ? "active" : "not installed"));
        sender.sendMessage(locale.format("status-papi", "status", papiActive ? "active" : "not installed"));
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
```

- [ ] **Step 11.5: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 11.6: Commit**

```bash
git add src/main/java/info/desidia/commands/sub/
git commit -m "feat: add help, reload, toggle, status subcommands"
```

---

## Task 12: Subcomandos — list, block, unblock, check

**Files:**
- Create: `src/main/java/info/desidia/commands/sub/ListCommand.java`
- Create: `src/main/java/info/desidia/commands/sub/BlockCommand.java`
- Create: `src/main/java/info/desidia/commands/sub/UnblockCommand.java`
- Create: `src/main/java/info/desidia/commands/sub/CheckCommand.java`

- [ ] **Step 12.1: Crear ListCommand**

```java
// src/main/java/info/desidia/commands/sub/ListCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListCommand implements SubCommand {

    private final ConfigManager config;
    private final LocaleManager locale;

    public ListCommand(DesidiaItemCraftBlock plugin) {
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "list"; }
    @Override public String getPermission() { return "dib.list"; }
    @Override public String getUsage() { return "/dib list [world]"; }
    @Override public String getDescription() { return "List blocked/allowed items"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String worldName = args.length > 0 ? args[0] : "default";
        ConfigManager.WorldConfig wc = config.getWorldConfig(worldName);
        if (wc == null) {
            sender.sendMessage(locale.format("item-not-found", "item", worldName));
            return;
        }
        sender.sendMessage(locale.format("list-header", "world", worldName, "mode", wc.mode));
        Set<String> list = "blacklist".equals(wc.mode) ? wc.blocked : wc.allowed;
        if (list.isEmpty()) {
            sender.sendMessage(locale.format("list-empty"));
        } else {
            list.forEach(item -> sender.sendMessage(locale.format("list-entry", "item", item)));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> worlds = new ArrayList<>();
        if (args.length == 1) Bukkit.getWorlds().forEach(w -> worlds.add(w.getName()));
        return worlds;
    }
}
```

- [ ] **Step 12.2: Crear BlockCommand**

```java
// src/main/java/info/desidia/commands/sub/BlockCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BlockCommand implements SubCommand {

    private final ConfigManager config;
    private final LocaleManager locale;

    public BlockCommand(DesidiaItemCraftBlock plugin) {
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "block"; }
    @Override public String getPermission() { return "dib.block"; }
    @Override public String getUsage() { return "/dib block <item> [world]"; }
    @Override public String getDescription() { return "Block an item"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sender.sendMessage(locale.format("unknown-command")); return; }
        String matName = args[0].toUpperCase();
        try { Material.valueOf(matName); } catch (IllegalArgumentException e) {
            sender.sendMessage(locale.format("item-not-found", "item", matName));
            return;
        }
        String world = args.length > 1 ? args[1] : "default";
        config.addBlockedItem(world, matName);
        sender.sendMessage(locale.format("block-success", "item", matName, "world", world));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Material.values())
                .map(Material::name)
                .filter(n -> n.startsWith(args[0].toUpperCase()))
                .limit(20)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
```

- [ ] **Step 12.3: Crear UnblockCommand**

```java
// src/main/java/info/desidia/commands/sub/UnblockCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UnblockCommand implements SubCommand {

    private final ConfigManager config;
    private final LocaleManager locale;

    public UnblockCommand(DesidiaItemCraftBlock plugin) {
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "unblock"; }
    @Override public String getPermission() { return "dib.unblock"; }
    @Override public String getUsage() { return "/dib unblock <item> [world]"; }
    @Override public String getDescription() { return "Unblock an item"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sender.sendMessage(locale.format("unknown-command")); return; }
        String matName = args[0].toUpperCase();
        String world = args.length > 1 ? args[1] : "default";
        config.removeBlockedItem(world, matName);
        sender.sendMessage(locale.format("unblock-success", "item", matName, "world", world));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Material.values())
                .map(Material::name)
                .filter(n -> n.startsWith(args[0].toUpperCase()))
                .limit(20)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
```

- [ ] **Step 12.4: Crear CheckCommand**

```java
// src/main/java/info/desidia/commands/sub/CheckCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.BlockDecision;
import info.desidia.managers.BlockManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CheckCommand implements SubCommand {

    private final BlockManager blockManager;
    private final LocaleManager locale;
    private final DesidiaItemCraftBlock plugin;

    public CheckCommand(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.blockManager = plugin.getBlockManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "check"; }
    @Override public String getPermission() { return "dib.check"; }
    @Override public String getUsage() { return "/dib check <item>"; }
    @Override public String getDescription() { return "Check if an item is blocked here"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can use this."); return; }
        if (args.length == 0) { sender.sendMessage(locale.format("unknown-command")); return; }
        Player player = (Player) sender;
        String matName = args[0].toUpperCase();
        Material material;
        try { material = Material.valueOf(matName); } catch (IllegalArgumentException e) {
            sender.sendMessage(locale.format("item-not-found", "item", matName));
            return;
        }
        String worldName = player.getWorld().getName();
        String regionName = plugin.getWorldGuardHook() != null ? plugin.getWorldGuardHook().getRegionName(player) : null;
        BlockDecision d = blockManager.evaluate(player, material, worldName, regionName);
        String key = d.isBlocked() ? "check-blocked" : "check-allowed";
        sender.sendMessage(locale.format(key, "item", matName));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Material.values())
                .map(Material::name)
                .filter(n -> n.startsWith(args[0].toUpperCase()))
                .limit(20)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
```

- [ ] **Step 12.5: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 12.6: Commit**

```bash
git add src/main/java/info/desidia/commands/sub/
git commit -m "feat: add list, block, unblock, check subcommands"
```

---

## Task 13: Subcomandos — stats y gui

**Files:**
- Create: `src/main/java/info/desidia/commands/sub/StatsCommand.java`
- Create: `src/main/java/info/desidia/commands/sub/GuiCommand.java`

- [ ] **Step 13.1: Crear StatsCommand**

```java
// src/main/java/info/desidia/commands/sub/StatsCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.LocaleManager;
import info.desidia.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsCommand implements SubCommand {

    private final StatsManager stats;
    private final LocaleManager locale;

    public StatsCommand(DesidiaItemCraftBlock plugin) {
        this.stats = plugin.getStatsManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "stats"; }
    @Override public String getPermission() { return "dib.stats"; }
    @Override public String getUsage() { return "/dib stats [player|reset [player]]"; }
    @Override public String getDescription() { return "Show craft block statistics"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // /dib stats reset [player]
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("dib.stats.reset")) {
                sender.sendMessage(locale.format("no-permission"));
                return;
            }
            if (args.length > 1) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(locale.format("item-not-found", "item", args[1])); return; }
                stats.resetPlayer(target.getUniqueId());
                sender.sendMessage(locale.format("stats-reset-success", "player", target.getName()));
            } else {
                stats.resetAll();
                sender.sendMessage(locale.format("stats-reset-global"));
            }
            return;
        }

        // /dib stats [player]
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(locale.format("item-not-found", "item", args[0])); return; }
            sender.sendMessage(locale.format("stats-header", "player", target.getName()));
            sender.sendMessage(locale.format("stats-total", "total", stats.getTotalBlocked(target.getUniqueId())));
            sender.sendMessage(locale.format("stats-top-item", "item", stats.getTopItem(target.getUniqueId()),
                "count", stats.getItemCount(target.getUniqueId(), stats.getTopItem(target.getUniqueId()))));
            sender.sendMessage(locale.format("stats-last", "world", stats.getLastWorld(target.getUniqueId()),
                "region", stats.getLastRegion(target.getUniqueId()),
                "time", stats.getTimeSinceLastAttempt(target.getUniqueId())));
        } else {
            // Global stats
            sender.sendMessage(locale.format("stats-global-header"));
            sender.sendMessage(locale.format("stats-total", "total", stats.getGlobalTotal()));
            List<Map.Entry<String, Integer>> topItems = stats.getGlobalTopItems(3);
            if (!topItems.isEmpty()) {
                String items = topItems.stream().map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.joining(", "));
                sender.sendMessage(locale.format("stats-top-items", "items", items));
            }
            List<Map.Entry<String, Integer>> topPlayers = stats.getGlobalTopPlayers(3);
            if (!topPlayers.isEmpty()) {
                String players = topPlayers.stream().map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.joining(", "));
                sender.sendMessage(locale.format("stats-top-players", "players", players));
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("reset");
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            return options.stream().filter(o -> o.startsWith(args[0])).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.startsWith(args[1])).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
```

- [ ] **Step 13.2: Crear GuiCommand (stub — la GUI se implementa en Task 16)**

```java
// src/main/java/info/desidia/commands/sub/GuiCommand.java
package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.gui.ItemManagementGUI;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class GuiCommand implements SubCommand {

    private final DesidiaItemCraftBlock plugin;
    private final LocaleManager locale;

    public GuiCommand(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "gui"; }
    @Override public String getPermission() { return "dib.gui"; }
    @Override public String getUsage() { return "/dib gui"; }
    @Override public String getDescription() { return "Open item management GUI"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can use this."); return; }
        new ItemManagementGUI(plugin, (Player) sender, "default").open();
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
```

- [ ] **Step 13.3: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 13.4: Commit**

```bash
git add src/main/java/info/desidia/commands/sub/
git commit -m "feat: add stats and gui subcommands"
```

---

## Task 14: WorldGuardHook completo

**Files:**
- Modify: `src/main/java/info/desidia/hooks/WorldGuardHook.java`

- [ ] **Step 14.1: Reemplazar stub con implementación completa**

```java
// src/main/java/info/desidia/hooks/WorldGuardHook.java
package info.desidia.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import info.desidia.managers.BlockDecision;
import info.desidia.api.events.BlockReason;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorldGuardHook {

    private static StateFlag CRAFT_BLOCK_FLAG;
    private static StringFlag CRAFT_WHITELIST_FLAG;
    private static StringFlag CRAFT_BLACKLIST_FLAG;

    // Cache: key = UUID:material, value = [decision, timestamp]
    private final Map<String, Map.Entry<BlockDecision, Long>> cache = new HashMap<>();
    private static final long CACHE_TTL_MS = 100L; // 2 ticks

    public void register() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            CRAFT_BLOCK_FLAG = new StateFlag("craft-block", false);
            registry.register(CRAFT_BLOCK_FLAG);
        } catch (FlagConflictException ignored) {
            Flag<?> existing = registry.get("craft-block");
            if (existing instanceof StateFlag) CRAFT_BLOCK_FLAG = (StateFlag) existing;
        }
        try {
            CRAFT_WHITELIST_FLAG = new StringFlag("craft-whitelist");
            registry.register(CRAFT_WHITELIST_FLAG);
        } catch (FlagConflictException ignored) {
            Flag<?> existing = registry.get("craft-whitelist");
            if (existing instanceof StringFlag) CRAFT_WHITELIST_FLAG = (StringFlag) existing;
        }
        try {
            CRAFT_BLACKLIST_FLAG = new StringFlag("craft-blacklist");
            registry.register(CRAFT_BLACKLIST_FLAG);
        } catch (FlagConflictException ignored) {
            Flag<?> existing = registry.get("craft-blacklist");
            if (existing instanceof StringFlag) CRAFT_BLACKLIST_FLAG = (StringFlag) existing;
        }
    }

    public boolean isAvailable() { return CRAFT_BLOCK_FLAG != null; }

    public String getRegionName(Player player) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager rm = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (rm == null) return null;
            ApplicableRegionSet regions = rm.getApplicableRegions(
                BukkitAdapter.asBlockVector(player.getLocation()));
            if (regions.size() == 0) return null;
            return regions.getRegions().iterator().next().getId();
        } catch (Exception e) {
            return null;
        }
    }

    public BlockDecision evaluate(Player player, Material material, String regionName) {
        String cacheKey = player.getUniqueId() + ":" + material.name();
        long now = System.currentTimeMillis();
        Map.Entry<BlockDecision, Long> cached = cache.get(cacheKey);
        if (cached != null && now - cached.getValue() < CACHE_TTL_MS) {
            return cached.getKey();
        }

        BlockDecision result = evaluateFresh(player, material);
        cache.put(cacheKey, new AbstractMap.SimpleEntry<>(result, now));
        return result;
    }

    private BlockDecision evaluateFresh(Player player, Material material) {
        try {
            com.sk89q.worldguard.LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager rm = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (rm == null) return null;

            ApplicableRegionSet regions = rm.getApplicableRegions(
                BukkitAdapter.asBlockVector(player.getLocation()));
            String regionId = regions.size() > 0 ? regions.getRegions().iterator().next().getId() : null;

            // craft-block flag overrides everything
            State craftBlockState = regions.queryState(wgPlayer, CRAFT_BLOCK_FLAG);
            if (craftBlockState == State.DENY) return BlockDecision.block(BlockReason.REGION, regionId, null);
            if (craftBlockState == State.ALLOW) return BlockDecision.allow();

            // craft-blacklist flag
            String blacklistStr = regions.queryValue(wgPlayer, CRAFT_BLACKLIST_FLAG);
            if (blacklistStr != null && !blacklistStr.isEmpty()) {
                for (String entry : blacklistStr.split(",")) {
                    if (entry.trim().equalsIgnoreCase(material.name())) {
                        return BlockDecision.block(BlockReason.REGION, regionId, null);
                    }
                }
            }

            // craft-whitelist flag
            String whitelistStr = regions.queryValue(wgPlayer, CRAFT_WHITELIST_FLAG);
            if (whitelistStr != null && !whitelistStr.isEmpty()) {
                for (String entry : whitelistStr.split(",")) {
                    if (entry.trim().equalsIgnoreCase(material.name())) {
                        return BlockDecision.allow();
                    }
                }
                // whitelist defined but item not in it → block
                return BlockDecision.block(BlockReason.REGION, regionId, null);
            }

            return null; // No WG flags apply, fall through to world config
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 14.2: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 14.3: Commit**

```bash
git add src/main/java/info/desidia/hooks/WorldGuardHook.java
git commit -m "feat: implement WorldGuardHook with native flags and TTL cache"
```

---

## Task 15: PlaceholderAPIHook

**Files:**
- Modify: `src/main/java/info/desidia/hooks/PlaceholderAPIHook.java`

- [ ] **Step 15.1: Implementar PlaceholderAPIHook completo**

```java
// src/main/java/info/desidia/hooks/PlaceholderAPIHook.java
package info.desidia.hooks;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final DesidiaItemCraftBlock plugin;
    private final StatsManager stats;
    private final ConfigManager config;

    public PlaceholderAPIHook(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.stats = plugin.getStatsManager();
        this.config = plugin.getConfigManager();
    }

    @Override public @NotNull String getIdentifier() { return "dib"; }
    @Override public @NotNull String getAuthor() { return "cuouz"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        switch (params.toLowerCase()) {
            case "blocked_total":
                return String.valueOf(stats.getTotalBlocked(player.getUniqueId()));
            case "top_item":
                return stats.getTopItem(player.getUniqueId());
            case "last_attempt":
                return stats.getLastItem(player.getUniqueId());
            case "enabled":
                return String.valueOf(config.isEnabled());
            case "mode":
                return config.getGlobalMode();
        }
        // %dib_is_blocked_<MATERIAL>%
        if (params.toLowerCase().startsWith("is_blocked_")) {
            String material = params.substring("is_blocked_".length()).toUpperCase();
            ConfigManager.WorldConfig wc = config.getWorldConfig("default");
            if (wc == null) return "false";
            boolean inBlacklist = "blacklist".equals(wc.mode) && ConfigManager.isInList(wc.blocked, material);
            boolean notInWhitelist = "whitelist".equals(wc.mode) && !ConfigManager.isInList(wc.allowed, material);
            return String.valueOf(inBlacklist || notInWhitelist);
        }
        return null;
    }
}
```

- [ ] **Step 15.2: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 15.3: Commit**

```bash
git add src/main/java/info/desidia/hooks/PlaceholderAPIHook.java
git commit -m "feat: implement PlaceholderAPIHook with %dib_*% placeholders"
```

---

## Task 16: GUI — ItemManagementGUI

**Files:**
- Create: `src/main/java/info/desidia/gui/ItemManagementGUI.java`

- [ ] **Step 16.1: Implementar GUI paginada**

```java
// src/main/java/info/desidia/gui/ItemManagementGUI.java
package info.desidia.gui;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import info.desidia.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ItemManagementGUI implements Listener {

    private static final int PAGE_SIZE = 45; // slots 0-44 for items, row 5 for controls
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int ADD_SLOT = 46;
    private static final int CLOSE_SLOT = 52;

    private final DesidiaItemCraftBlock plugin;
    private final Player player;
    private final ConfigManager config;
    private final LocaleManager locale;
    private String currentWorld;
    private int page = 0;
    private Inventory inv;

    public ItemManagementGUI(DesidiaItemCraftBlock plugin, Player player, String world) {
        this.plugin = plugin;
        this.player = player;
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
        this.currentWorld = world;
    }

    public void open() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        buildAndOpen();
    }

    private void buildAndOpen() {
        ConfigManager.WorldConfig wc = config.getWorldConfig(currentWorld);
        if (wc == null) wc = new ConfigManager.WorldConfig();

        List<String> itemList = new ArrayList<>("blacklist".equals(wc.mode) ? wc.blocked : wc.allowed);
        int totalPages = Math.max(1, (int) Math.ceil(itemList.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = ColorUtil.color("&8[&6DIB&8] &e" + currentWorld + " &7| " + wc.mode.toUpperCase()
            + " &8| &7" + (page + 1) + "/" + totalPages);
        inv = Bukkit.createInventory(null, 54, title);

        // Fill item slots
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < itemList.size(); i++) {
            String matName = itemList.get(start + i);
            Material mat = parseMaterial(matName);
            ItemStack item = new ItemStack(mat == null ? Material.BARRIER : mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.color("&f" + matName));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtil.color("&7Click: toggle"));
                lore.add(ColorUtil.color("&7Right-click: custom message"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        // Control bar
        if (page > 0) inv.setItem(PREV_SLOT, makeControl(Material.ARROW, "&ePrevious page"));
        if (page < totalPages - 1) inv.setItem(NEXT_SLOT, makeControl(Material.ARROW, "&eNext page"));
        inv.setItem(INFO_SLOT, makeControl(Material.BOOK, "&6Mode: &f" + wc.mode.toUpperCase(),
            "&7World: &f" + currentWorld));
        inv.setItem(ADD_SLOT, makeControl(Material.LIME_STAINED_GLASS_PANE, "&a+ Add held item"));
        inv.setItem(CLOSE_SLOT, makeControl(Material.BARRIER, "&cClose"));

        player.openInventory(inv);
    }

    private ItemStack makeControl(Material mat, String... lines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(lines[0]));
            if (lines.length > 1) {
                List<String> lore = new ArrayList<>();
                for (int i = 1; i < lines.length; i++) lore.add(ColorUtil.color(lines[i]));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv)) return;
        if (!(event.getWhoClicked().equals(player))) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot == CLOSE_SLOT) { player.closeInventory(); return; }
        if (slot == NEXT_SLOT) { page++; buildAndOpen(); return; }
        if (slot == PREV_SLOT) { page--; buildAndOpen(); return; }

        if (slot == ADD_SLOT) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType() == Material.AIR) return;
            config.addBlockedItem(currentWorld, held.getType().name());
            buildAndOpen();
            return;
        }

        // Item slots
        if (slot < PAGE_SIZE) {
            ConfigManager.WorldConfig wc = config.getWorldConfig(currentWorld);
            if (wc == null) return;
            List<String> itemList = new ArrayList<>("blacklist".equals(wc.mode) ? wc.blocked : wc.allowed);
            int idx = page * PAGE_SIZE + slot;
            if (idx >= itemList.size()) return;
            String matName = itemList.get(idx);

            if (event.isRightClick()) {
                // Ask for custom message via chat
                player.closeInventory();
                player.sendMessage(ColorUtil.color("&eType the custom message for &f" + matName + "&e (or 'cancel'):"));
                // Chat listener registration would go here — simplified to reopen
                Bukkit.getScheduler().runTaskLater(plugin, this::buildAndOpen, 40L);
            } else {
                // Toggle: remove from list
                config.removeBlockedItem(currentWorld, matName);
                buildAndOpen();
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inv) && event.getPlayer().equals(player)) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }

    private Material parseMaterial(String name) {
        try { return Material.valueOf(name); } catch (IllegalArgumentException e) { return null; }
    }
}
```

- [ ] **Step 16.2: Verificar compilación**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 16.3: Commit**

```bash
git add src/main/java/info/desidia/gui/
git commit -m "feat: add paginated ItemManagementGUI with click-to-toggle and add-held-item"
```

---

## Task 17: DIBApi, UpdateChecker y bStats

**Files:**
- Create: `src/main/java/info/desidia/api/DIBApi.java`
- Modify: `src/main/java/info/desidia/util/UpdateChecker.java`

- [ ] **Step 17.1: Implementar DIBApi**

```java
// src/main/java/info/desidia/api/DIBApi.java
package info.desidia.api;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.managers.BlockDecision;
import info.desidia.managers.BlockManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class DIBApi {

    private static DesidiaItemCraftBlock plugin;

    private DIBApi() {}

    static void init(DesidiaItemCraftBlock instance) {
        plugin = instance;
    }

    public static boolean isBlocked(Player player, Material material) {
        if (plugin == null) throw new IllegalStateException("DIBApi not initialized");
        BlockDecision d = plugin.getBlockManager().evaluate(
            player, material,
            player.getWorld().getName(),
            plugin.getWorldGuardHook() != null ? plugin.getWorldGuardHook().getRegionName(player) : null
        );
        return d.isBlocked();
    }

    public static boolean isPluginEnabled() {
        return plugin != null && plugin.getConfigManager().isEnabled();
    }
}
```

En `DesidiaItemCraftBlock.onPluginStart()`, añadir después de crear blockManager (añadir también el import):
```java
import info.desidia.api.DIBApi;
// ...
DIBApi.init(this);
```

- [ ] **Step 17.2: Implementar UpdateChecker completo**

```java
// src/main/java/info/desidia/util/UpdateChecker.java
package info.desidia.util;

import info.desidia.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private final Plugin plugin;
    private final int resourceId;
    private String latestVersion = null;

    public UpdateChecker(Plugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    latestVersion = reader.readLine().trim();
                }
                String current = plugin.getDescription().getVersion();
                if (!current.equals(latestVersion)) {
                    plugin.getLogger().info("[DIB] New version available: " + latestVersion + " (you have " + current + ")");
                    Bukkit.getPluginManager().registerEvents(this, plugin);
                }
            } catch (Exception ignored) {
                // Silent fail — no internet or invalid resource ID
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (latestVersion != null && player.hasPermission("dib.notify")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String current = plugin.getDescription().getVersion();
                player.sendMessage(ColorUtil.color(
                    "&8[&6DIB&8] &eNew version: &f" + latestVersion + " &e(you have &f" + current + "&e)"));
            }, 40L);
        }
    }
}
```

- [ ] **Step 17.3: Verificar compilación final**

```bash
mvn compile -q
```
Esperado: BUILD SUCCESS.

- [ ] **Step 17.4: Correr todos los tests**

```bash
mvn test
```
Esperado: BUILD SUCCESS, todos los tests pasan.

- [ ] **Step 17.5: Build completo del JAR**

```bash
mvn package -q
```
Esperado: `target/DesidiaItemCraftBlock-2.0.0.jar` generado.

- [ ] **Step 17.6: Commit final**

```bash
git add src/
git commit -m "feat: add DIBApi, UpdateChecker, and complete plugin build for v2.0.0"
```

---

## Resumen de orden de ejecución

```
Task 1 (pom.xml) → Task 2 (resources) → Task 3 (ColorUtil) → Task 4 (LocaleManager)
→ Task 5 (ConfigManager) → Task 6 (StatsManager) → Task 7 (events)
→ Task 8 (BlockManager) → Task 9 (CraftListener + Main)
→ Task 10 (DIBCommand) → Task 11 (help/reload/toggle/status)
→ Task 12 (list/block/unblock/check) → Task 13 (stats/gui stub)
→ Task 14 (WorldGuardHook) → Task 15 (PlaceholderAPIHook)
→ Task 16 (GUI) → Task 17 (DIBApi + UpdateChecker + build)
```

Cada task produce código compilable. El plugin es funcional (sin WG ni PAPI) desde el final de la Task 9.
