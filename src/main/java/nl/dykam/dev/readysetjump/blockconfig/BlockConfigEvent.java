package nl.dykam.dev.readysetjump.blockconfig;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

public abstract class BlockConfigEvent extends Event {
  private Plugin plugin;

  public Plugin getPlugin() {
    return plugin;
  }

  public ConfigurationSection getConfig() {
    return config;
  }

  public Block getBlock() {
    return block;
  }

  protected BlockConfigEvent(Plugin plugin, ConfigurationSection config, Block block) {

    this.plugin = plugin;
    this.config = config;
    this.block = block;
  }

  private ConfigurationSection config;
  private Block block;
}
