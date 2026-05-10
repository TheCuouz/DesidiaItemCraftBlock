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
