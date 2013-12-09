package nl.dykam.dev.readysetjump;

import nl.dykam.dev.readysetjump.blockconfig.BlockConfig;
import nl.dykam.dev.readysetjump.blockconfig.BlockConfigLoadEvent;
import nl.dykam.dev.readysetjump.blockconfig.BlockConfigUnloadEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

public class ReadySetJumpPlugin extends JavaPlugin implements Listener {
  BlockConfig loader;
  PainlessFlight painlessFlight;

  static ReadySetJumpPlugin instance;
  public static ReadySetJumpPlugin getInstance() {
    return instance;
  }

  @Override
  public void onDisable() {
    loader.saveConfig();
  }

  @Override
  public void onEnable() {
    instance = this;
    loader = new BlockConfig(this);
    getServer().getPluginManager().registerEvents(this, this);
    for(World world : Bukkit.getWorlds()) {
      for(Chunk chunk : world.getLoadedChunks()) {
        for(Map.Entry<Block, ConfigurationSection> blockConfig : loader.getBlocksWithConfig(chunk).entrySet()) {
          ConfigurationSection config = blockConfig.getValue().getConfigurationSection("launcher");
          if(config == null)
            continue;
          final Launcher launcher = new Launcher(blockConfig.getKey(), config);
          launcher.save();
          blockConfig.getKey().setMetadata("launcher", new FixedMetadataValue(this, launcher));
        }
      }
    }
    painlessFlight = new PainlessFlight(this);
  }

  @EventHandler
  private void onPlayerQuit(PlayerQuitEvent pqe) {
    pqe.getPlayer().removeMetadata("LaunchTime", this);
  }

  @EventHandler
  private void onPlayerMove(PlayerMoveEvent pme) {
    Location loc = pme.getTo().clone();
    Vector diff = loc.toVector().subtract(pme.getFrom().toVector());
    diff.multiply(2); // Magic value, should be playerPing * 20, but playerPing is as reliable as Math.random()
    if(diff.lengthSquared() < 1) {
      loc.subtract(0, .01, 0);
      if(loc.getY() < 0 || loc.getY() >= 256)
        return;
      launch(loc.getBlock(), pme.getPlayer());
    } else {
      if(loc.getY() < 0 || loc.getY() >= 256)
        return;
      BlockIterator iterator = new BlockIterator(loc.getWorld(), loc.toVector(), diff.clone().normalize(), -0.1, (int)Math.ceil(diff.length()));
      while (iterator.hasNext()) {
        if(launch(iterator.next(), pme.getPlayer()))
          return;
      }
    }
  }

  private boolean launch(Block block, final Player player) {
    if(block == null)
        return false;
      MetadataValue value = getMetadata(block, "launcher");
      if(value == null)
        return false;
      final Launcher launcher;
      if(value.value() instanceof Launcher) {
        launcher = (Launcher) value.value();
      } else {
        ConfigurationSection config = loader.getConfig(block).getConfigurationSection("launcher");
        if(config == null)
          return false;
        launcher = new Launcher(block, config);
        block.setMetadata("launcher", new FixedMetadataValue(this, launcher));
      }
      Bukkit.getScheduler().runTask(ReadySetJumpPlugin.getInstance(), new Runnable() {
        @Override
        public void run() {
          launcher.apply(player);
        }
      });
      return true;
  }

  @EventHandler
  private void onBlockConfigLoad(BlockConfigLoadEvent bcle) {
    if(!bcle.getPlugin().equals(this))
      return;
      ConfigurationSection config = bcle.getConfig().getConfigurationSection("launcher");
      if(config == null)
        return;
      Launcher launcher = new Launcher(bcle.getBlock(), config);
      bcle.getBlock().setMetadata("launcher", new FixedMetadataValue(this, launcher));
  }
  @EventHandler
  private void onBlockConfigUnload(BlockConfigUnloadEvent bcue) {
    MetadataValue value = getMetadata(bcue.getBlock(), "launcher");
    if(value == null)
      return;
    bcue.getBlock().removeMetadata("launcher", this);
    Launcher launcher = (Launcher)value.value();
    launcher.save();
    loader.saveConfig(bcue.getBlock());
  }

  @EventHandler
  private void onPlayerInteract(PlayerInteractEvent pie) {
    if(pie.getAction() != Action.LEFT_CLICK_BLOCK)
      return;
    MetadataValue selectingValue = getMetadata(pie.getPlayer(), "RSJMode");
    Mode mode = selectingValue == null || !(selectingValue.value() instanceof Mode) ? Mode.NONE : (Mode)selectingValue.value();
    switch (mode) {
      case SELECT:
        pie.setCancelled(true);
        setMetadata(pie.getPlayer(), "RSJMode", Mode.NONE);
        setMetadata(pie.getPlayer(), "RSJSelected", pie.getClickedBlock());
        pie.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Block selected. Select mode disabled.");
        break;
      case CLONE:
        pie.setCancelled(true);
        MetadataValue selectedValue = getMetadata(pie.getPlayer(), "RSJSelected");
        Block selected = selectedValue == null ? null : (Block)selectedValue.value();
        MetadataValue value = getMetadata(selected, "launcher");
        Launcher launcher;
        if(value == null) {
          pie.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "Selected block to clone does not container a launcher.");
          return;
        } else {
          Launcher selectedLauncher = (Launcher)value.value();
          ConfigurationSection config = loader.getConfig(pie.getClickedBlock());
          config = config.isConfigurationSection("launcher") ? config.getConfigurationSection("launcher") : config.createSection("launcher");
          launcher = selectedLauncher.clone(pie.getClickedBlock(), config);
          setMetadata(pie.getClickedBlock(), "launcher", launcher);
          pie.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Launcher cloned. Clone mode still enabled.");
        }
        break;
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if(args.length == 0)
      return false;
    if(!(sender instanceof Player))
      return false;
    Player player = (Player)sender;

    MetadataValue selectingValue = getMetadata(player, "RSJMode");
    Mode mode = selectingValue == null  || !(selectingValue.value() instanceof Mode) ? Mode.NONE : (Mode)selectingValue.value();
    MetadataValue selectedValue = getMetadata(player, "RSJSelected");
    Block selected = selectedValue == null ? null : (Block)selectedValue.value();

    if(args[0].equals("select")) {
      if(mode == Mode.SELECT) {
        mode = Mode.NONE;
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "Selecting cancelled.");
      } else {
        mode = Mode.SELECT;
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Punch a block to select it.");
      }
    } else if(args[0].equals("clone")) {
      if(mode == Mode.CLONE) {
        mode = Mode.NONE;
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "Cloning cancelled.");
      } else {
        mode = Mode.CLONE;
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Punch a block to clone the selected block to it.");
      }
    } else if(args[0].equals("add")) {
      if(selected == null) {
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "You don't have a block selected. To select a block, use /rsj select");
        return false;
      }
      ConfigurationSection config = loader.getConfig(selected);
      config = config.isConfigurationSection("launcher") ? config.getConfigurationSection("launcher") : config.createSection("launcher");
      setMetadata(selected, "launcher", new Launcher(selected, config));
      player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Created the launcher.");
    } else if(args[0].equals("remove")) {
      if(selected == null) {
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "You don't have a block selected. To select a block, use /rsj select");
        return false;
      }
      MetadataValue value = getMetadata(selected, "launcher");
      if(value == null) {
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "Found no launcher to remove");
        return false;
      }
      Launcher launcher = (Launcher)value.value();
      launcher.delete();
      selected.removeMetadata("launcher", this);
      player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Removed the launcher.");
    }  else if(args[0].equals("info")) {
      if(selected == null) {
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "You don't have a block selected. To select a block, use /rsj select");
        return false;
      }
      MetadataValue value = getMetadata(selected, "launcher");
      if(value == null) {
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "Found no launcher to print info of");
        return false;
      }
      Launcher launcher = (Launcher)value.value();
      player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "block: " + launcher.getBlock().getLocation().toVector());
      player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "add: " + launcher.getAddVelocity());
      player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "set: " + launcher.getSetVelocity());
      player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "mul: " + launcher.getMulVelocity());
      player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "sneak-nolaunch: " + launcher.isSneakNoLaunch());
    } else if(args[0].equals("set") && args.length >= 2) {
      if(selected == null) {
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "You don't have a block selected. To select a block, use /rsj select");
        return false;
      }
      MetadataValue value = getMetadata(selected, "launcher");
      if(value == null || !(value.value() instanceof Launcher)) {
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.RED + "No launcher found in the selected block. Make one with /rsj add");
        return false;
      }
      Launcher launcher = (Launcher)value.value();
      if(args[1].equals("add")) {
        Vector vec = parseVector(args, 2);
        if(vec == null)
          vec = new Vector(0, 0, 0);
        launcher.setAddVelocity(vec);
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Set the add vector.");
      } else if(args[1].equals("set")) {
        Vector vec = parseVector(args, 2);
        if(vec == null)
          vec = new Vector(Double.NaN, Double.NaN, Double.NaN);
        launcher.setSetVelocity(vec);
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Set the set vector.");
      } else if(args[1].equals("mul")) {
        Vector vec = parseVector(args, 2);
        if(vec == null)
          vec = new Vector(1, 1, 1);
        launcher.setMulVelocity(vec);
        player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Set the multiply vector.");
      } else if(args[1].equals("sneak-nolaunch") && args.length == 3) {
        sender.sendMessage("TEST");
        if(args[2].toLowerCase().equals("true")) {
          launcher.setSneakNoLaunch(true);
          player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Enabled sneak-nolaunch.");
        } else if(args[2].toLowerCase().equals("false")) {
          launcher.setSneakNoLaunch(false);
          player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Disabled sneak-nolaunch.");
        } else {
          return false;
        }
      }
      launcher.save();
    } else if(args[0].equals("save")) {
      loader.saveConfig();
      player.sendMessage(ChatColor.DARK_PURPLE + "[RSJ] " + ChatColor.GREEN + "Saved.");
    }
    setMetadata(player, "RSJMode", mode);
    setMetadata(player, "RSJSelected", selected);
    return true;
  }

  Vector parseVector(String[] args, int start) {
    if(args.length < start + 3)
      return null;
    double[] parts = new double[3];
    for(int i = 0; i < 3; i++) {
      parts[i] = Double.parseDouble(args[i + start]);
    }
    return new Vector(parts[0], parts[1], parts[2]);
  }

  MetadataValue getMetadata(Metadatable metadatable, String key) {
    List<MetadataValue> values = metadatable.getMetadata(key);
    if(values == null)
      return null;
    for(MetadataValue value : values) {
      if(this.equals(value.getOwningPlugin()))
        return value;
    }
    return null;
  }
  void setMetadata(Metadatable metadatable, String key, Object value) {
    metadatable.setMetadata(key, new FixedMetadataValue(this, value));
  }

  enum Mode {
    NONE,
    SELECT,
    CLONE,
  }
}
