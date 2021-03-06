package nl.dykam.dev.readysetjump.blockconfig;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

/**
 * Called after loading block configuration.
 */
public class BlockConfigLoadEvent extends BlockConfigEvent {
  public BlockConfigLoadEvent(Plugin plugin, ConfigurationSection block, Block config) {
    super(plugin, block, config);
  }

  private static final HandlerList handlers = new HandlerList();
  public HandlerList getHandlers() {
      return handlers;
  }

  public static HandlerList getHandlerList() {
      return handlers;
  }
}
