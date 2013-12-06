package nl.dykam.dev.readysetjump.blockconfig;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the launchers in each chunk.
 */
public class BlockConfig implements Listener {
  Plugin plugin;
  Map<Chunk, FileConfiguration> configs;
  Map<Chunk, Map<Block, ConfigurationSection>> blockConfigs;
  public BlockConfig(Plugin plugin) {
    this.plugin = plugin;
    configs = new HashMap<Chunk, FileConfiguration>();
    blockConfigs = new HashMap<Chunk, Map<Block, ConfigurationSection>>();
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private void onChunkLoad(ChunkLoadEvent cle) {
    if(BlockConfigLoadEvent.getHandlerList().getRegisteredListeners().length == 0)
      return;
    Chunk chunk = cle.getChunk();
    for(Map.Entry<Block, ConfigurationSection> entry : getBlocksWithConfig(chunk).entrySet()) {
      BlockConfigLoadEvent event = new BlockConfigLoadEvent(plugin, entry.getValue(), entry.getKey());
      Bukkit.getPluginManager().callEvent(event);
    }
  }

  public Map<Block, ConfigurationSection> getBlocksWithConfig(Chunk chunk) {
    ChunkCoord key = new ChunkCoord(chunk);
    if(blockConfigs.containsKey(chunk))
      return blockConfigs.get(chunk);
    Map<Block, ConfigurationSection> result = new HashMap<Block, ConfigurationSection>();
    ConfigurationSection config = getBlocksConfig(chunk);
    String dbg = config.getKeys(false).toString();

    for(Map.Entry<String, Object> entry : config.getValues(false).entrySet()) {
      if(!(entry.getValue() instanceof ConfigurationSection)) {
        plugin.getLogger().warning("Skipped: Invalid configuration section '" + entry.getKey() + "' for chunk " + getChunkName(chunk));
        continue;
      }
      ConfigurationSection section = (ConfigurationSection) entry.getValue();
      String[] locationParts = entry.getKey().split(" ");
      if(locationParts.length != 3) {
        plugin.getLogger().warning("Skipped: Invalid configuration name '" + entry.getKey() + "' for chunk " + getChunkName(chunk));
        continue;
      }
      int[] locationValues = new int[3];
      try {
        for(int i = 0; i < 3; i++) {
          locationValues[i] = Integer.parseInt(locationParts[i]);
        }
      } catch(NumberFormatException nfe) {
        plugin.getLogger().warning("Skipped: Invalid configuration name '" + entry.getKey() + "' for chunk " + getChunkName(chunk));
        continue;
      }
      result.put(chunk.getBlock(locationValues[0], locationValues[1], locationValues[2]), section);
    }
    blockConfigs.put(chunk, result);
    return result;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private void onChunkUnload(ChunkUnloadEvent cue) {
    Chunk chunk = cue.getChunk();
    ConfigurationSection config = configs.get(cue.getChunk());
    if(config != null && config.isConfigurationSection("blocks")) {
      ConfigurationSection blocks = config.getConfigurationSection("blocks");
      for(Map.Entry<String, Object> entry : blocks.getValues(false).entrySet()) {
        ConfigurationSection section = (ConfigurationSection) entry.getValue();
        String[] locationParts = entry.getKey().split(" ");
        int[] locationValues = new int[3];
        for(int i = 0; i < 3; i++) {
          locationValues[i] = Integer.parseInt(locationParts[i]);
        }
        BlockConfigUnloadEvent event = new BlockConfigUnloadEvent(plugin, section, cue.getChunk().getBlock(locationValues[0], locationValues[2], locationValues[2]));
        Bukkit.getPluginManager().callEvent(event);
      }
    }
    saveConfig(cue.getChunk());
    configs.remove(chunk);
    blockConfigs.remove(chunk);
  }

  private ConfigurationSection getBlocksConfig(Chunk chunk) {
    return getBlocksConfig(chunk, false);
  }
  private ConfigurationSection getBlocksConfig(Chunk chunk, boolean reload) {
    FileConfiguration config = configs.get(chunk);
    if(reload || config == null) {
      File file = getChunkFile(chunk);
      if(!file.exists())
        config = new YamlConfiguration();
      config = YamlConfiguration.loadConfiguration(file);
      configs.put(chunk, config);
    }
    ConfigurationSection blocksConfig = config.getConfigurationSection("blocks");
    if(blocksConfig == null)
      blocksConfig = config.createSection("blocks");
    return blocksConfig;
  }
  public void saveConfig() {
    for(Chunk chunk : configs.keySet()) {
      saveConfig(chunk);
    }
  }
  public boolean saveConfig(Chunk chunk) {
    FileConfiguration config = configs.get(chunk);
    if(config != null)
      cleanConfig(config);
    File file = getChunkFile(chunk);
    boolean delete = false;
    if(config == null || config.getKeys(false).isEmpty()) {
      delete = true;
    } else {
      ConfigurationSection blocks = config.getConfigurationSection("blocks");
      if(blocks == null || blocks.getKeys(false).isEmpty())
        delete = true;
    }
    if(delete) {
      if(file.exists())
        file.delete();
      return true;
    }
    try {
      config.save(file);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;
    }
    return true;
  }

  private void cleanConfig(ConfigurationSection config) {
    config = config.getConfigurationSection("blocks");
    if(config != null) {
      for(Map.Entry<String, Object> entry : config.getValues(false).entrySet()) {
        if(!(entry.getValue() instanceof ConfigurationSection))
          config.set(entry.getKey(), null);
        ConfigurationSection section = (ConfigurationSection)entry.getValue();
        if(section.getKeys(false).isEmpty())
          config.set(entry.getKey(), null);
      }
    }
  }

  public ConfigurationSection getConfig(Block block) {
    ConfigurationSection config = getBlocksConfig(block.getChunk());
    String name = getBlockName(block);
    ConfigurationSection section = config.getConfigurationSection(name);
    if(section == null)
      section = config.createSection(name);
    return section;
  }
  public void saveConfig(Block block) {
    saveConfig(block.getChunk());
  }

  private String getBlockName(Block block) {
    return "" + block.getX() + " " + block.getY() + " " + block.getZ();
  }
  
  private File getChunkFile(Chunk chunk) {
    return Paths.get(plugin.getDataFolder().getPath(), "launchers", chunk.getWorld().getWorldFolder().getName(), chunk.getX() + "-" + chunk.getZ() + ".yml").toFile();
  }
  private String getChunkName(Chunk chunk) {
    return chunk.getWorld().getWorldFolder().getName() + "/" + chunk.getX() + "-" + chunk.getZ();
  }

  class ChunkCoord {

    public final int x;
    public final int z;

    public ChunkCoord(int x, int z) {
        this.x = x;
        this.z = z;
    }


    public ChunkCoord(Chunk chunk) {
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkCoord)) return false;
        ChunkCoord coord = (ChunkCoord) o;
        return x == coord.x && z == coord.z;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + z;
        return result;
    }
  }
}
