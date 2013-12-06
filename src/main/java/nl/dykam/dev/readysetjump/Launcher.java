package nl.dykam.dev.readysetjump;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

public class Launcher {
  private Vector mulVelocity = new Vector(1, 1, 1);
  private Vector setVelocity = new Vector(Double.NaN, Double.NaN, Double.NaN);
  private Vector addVelocity = new Vector(0, 0, 0);

  private int noFallDamageDelay = 20;
  private boolean sneakNoLaunch = false;
  private Block block;
  private ConfigurationSection config;

  public Launcher(Block block, ConfigurationSection config) {
    this.block = block;
    this.config = config;
    load();
  }

  public void save() {
    config.set("mul", mulVelocity);
    config.set("set", setVelocity);
    config.set("add", addVelocity);
    config.set("sneak-nolaunch", sneakNoLaunch);
    config.set("no-fall-damage-delay", noFallDamageDelay);
  }

  public void delete() {
    config.getParent().set(config.getName(), null);
  }

  public void load() {
    ConfigurationSection defaultConfig = ReadySetJumpPlugin.getInstance().getConfig().getConfigurationSection("default");
    if (defaultConfig == null)
      defaultConfig = ReadySetJumpPlugin.getInstance().getConfig().createSection("default");
    setMulVelocity(config.getVector("mul", defaultConfig.getVector("mul", getMulVelocity())));
    setSetVelocity(config.getVector("set", defaultConfig.getVector("set", getSetVelocity())));
    setAddVelocity(config.getVector("add", defaultConfig.getVector("add", getAddVelocity())));

    sneakNoLaunch = config.getBoolean("sneak-nolaunch", defaultConfig.getBoolean("sneak-nolaunch", sneakNoLaunch));

    noFallDamageDelay = config.getInt("no-fall-damage-delay", defaultConfig.getInt("no-fall-damage-delay", noFallDamageDelay));
  }

  public void apply(final Player player) {
    if (sneakNoLaunch && player instanceof Player && ((Player) player).isSneaking())
      return;
    PainlessFlight.Tracker tracker = (PainlessFlight.Tracker) ReadySetJumpPlugin.getInstance().getMetadata(player, "FlightTracker").value();
    final Vector velocity = player.getVelocity();
    MetadataValue playerLaunchTime = ReadySetJumpPlugin.getInstance().getMetadata(player, "LaunchTime");
    int current = player.getTicksLived();
    if (playerLaunchTime != null && playerLaunchTime.asInt() + 20 > current)
      return;
    ReadySetJumpPlugin.getInstance().setMetadata(player, "LaunchTime", current);

      player.getWorld().playSound(block.getLocation().add(.5, .5, .5), Sound.SLIME_ATTACK , 1, 0);

    velocity.multiply(mulVelocity);
    velocity.add(addVelocity);
    if (!Double.isNaN(setVelocity.getX()))
      velocity.setX(setVelocity.getX());
    if (!Double.isNaN(setVelocity.getY()))
      velocity.setY(setVelocity.getY());
    if (!Double.isNaN(setVelocity.getZ()))
      velocity.setZ(setVelocity.getZ());
    player.setVelocity(velocity);

    Bukkit.getScheduler().runTaskLater(ReadySetJumpPlugin.getInstance(), new Runnable() {
      @Override
      public void run() {
        PainlessFlight.Tracker tracker = (PainlessFlight.Tracker) ReadySetJumpPlugin.getInstance().getMetadata(player, "FlightTracker").value();
        tracker.cancelFall();
      }
    }, noFallDamageDelay);
  }

  public Vector getMulVelocity() {
    return mulVelocity.clone();
  }

  public void setMulVelocity(Vector mulVelocity) {
    this.mulVelocity = mulVelocity;

    // Default NaN to Identify values.
    if (Double.isNaN(mulVelocity.getX()))
      mulVelocity.setX(1);
    if (Double.isNaN(mulVelocity.getY()))
      mulVelocity.setY(1);
    if (Double.isNaN(mulVelocity.getZ()))
      mulVelocity.setZ(1);
  }

  public Vector getSetVelocity() {
    return setVelocity.clone();
  }

  public void setSetVelocity(Vector setVelocity) {
    this.setVelocity = setVelocity;
  }

  public Vector getAddVelocity() {
    return addVelocity.clone();
  }

  public void setAddVelocity(Vector addVelocity) {
    this.addVelocity = addVelocity;

    if (Double.isNaN(addVelocity.getX()))
      addVelocity.setX(0);
    if (Double.isNaN(addVelocity.getY()))
      addVelocity.setY(0);
    if (Double.isNaN(addVelocity.getZ()))
      addVelocity.setZ(0);
  }

  public int getNoFallDamageDelay() {
    return noFallDamageDelay;
  }

  public void setNoFallDamageDelay(int noFallDamageDelay) {
    this.noFallDamageDelay = noFallDamageDelay;
  }

  public boolean isSneakNoLaunch() {
    return sneakNoLaunch;
  }

  public void setSneakNoLaunch(boolean sneakNoLaunch) {
    this.sneakNoLaunch = sneakNoLaunch;
  }

  public Block getBlock() {
    return block;
  }

  public ConfigurationSection getConfig() {
    return config;
  }

  public Launcher clone(Block block, ConfigurationSection config) {
    Launcher launcher = new Launcher(block, config);
    launcher.setAddVelocity(getAddVelocity());
    launcher.setMulVelocity(getMulVelocity());
    launcher.setSetVelocity(getSetVelocity());
    launcher.setSneakNoLaunch(isSneakNoLaunch());
    return launcher;
  }

  /**
   * Sets the parameters so the player arrives at chosen location.
   *
   * @param location The location to arrive
   */
  public void aim(Location location) {
    aim(location, 3);
  }

  /**
   * Sets the parameters so the player arrives at chosen location.
   *
   * @param location The location to arrive
   * @param clearing Clearing. More clearing means a bigger arch.
   */
  public void aim(Location location, float clearing) {
    throw new NotImplementedException(Launcher.class);
  }
}
