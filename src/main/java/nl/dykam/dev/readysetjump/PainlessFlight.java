package nl.dykam.dev.readysetjump;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

@SuppressWarnings("UnusedDeclaration")
public class PainlessFlight implements Listener {
  private final Plugin plugin;

  // Values for ground-checking
  static final double size = 0.3;
  static final Vector[] offsets = {
          new Vector(-size, -.001, -size),
          new Vector( size, -.001, -size),
          new Vector(-size, -.001,  size),
          new Vector( size, -.001,  size),
  };

  public PainlessFlight(Plugin plugin) {
    this.plugin = plugin;
    Bukkit.getPluginManager().registerEvents(this, plugin);
    for(Player player : Bukkit.getOnlinePlayers()) {
      ReadySetJumpPlugin.getInstance().setMetadata(player, "FlightTracker", new Tracker(player));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private void onPlayerJoin(PlayerJoinEvent pje) {
    ReadySetJumpPlugin.getInstance().setMetadata(pje.getPlayer(), "FlightTracker", new Tracker(pje.getPlayer()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private void onPlayerQuit(PlayerQuitEvent pqe) {
    pqe.getPlayer().removeMetadata("FlightTracker", plugin);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private void onPlayerMove(PlayerMoveEvent pme) {
    MetadataValue trackerData = ReadySetJumpPlugin.getInstance().getMetadata(pme.getPlayer(), "FlightTracker");
    if(trackerData == null || !(trackerData.value() instanceof Tracker))
      return;
    Tracker tracker = (Tracker)trackerData.value();
    tracker.track(pme);
  }

  @EventHandler
  private void onEntityDamage(EntityDamageEvent ede) {
    if(ede.getCause() != EntityDamageEvent.DamageCause.FALL)
      return;
    if(ede.getEntityType() != EntityType.PLAYER)
      return;
    Tracker tracker = (Tracker)ReadySetJumpPlugin.getInstance().getMetadata(ede.getEntity(), "FlightTracker").value();
    tracker.handleFallDamage(ede);
  }

  public class Tracker {
    private final Player player;
    private boolean wasFlying;
    private boolean isJumping;
    private boolean cancelFall;
    private double oldY;
    private static final float threshold = 0.01f;

    public int getFlight() {
      return flight;
    }

    public void setFlight(int flight) {
      this.flight = flight;
    }

    private int flight;
    public Tracker(Player player) {
      this.player = player;
    }

    public void cancelFall() {
      cancelFall = true;
    }

    private void track(PlayerMoveEvent pme) {
      isJumping = Math.abs(pme.getTo().getY() - oldY) > threshold;
      isJumping |= isFlying();

      oldY = pme.getTo().getY();
      if(isJumping()) {
        if(!wasFlying) {
          flight++;
          wasFlying = true;
        }
      } else if(wasFlying) {
        wasFlying = false;
        cancelFall = false;
      }
    }

    private void handleFallDamage(EntityDamageEvent ede) {
      if(cancelFall) {
        ede.setCancelled(true);
        cancelFall = false;
        Location loc = ede.getEntity().getLocation();
        loc.getWorld().playEffect(loc, Effect.SMOKE, 4);
        loc.getWorld().playSound(loc, Sound.BAT_TAKEOFF, 0.2f, 0);
      }
    }

    public boolean isJumping() {
      return isJumping;
    }

      /**
       * Flawed method to check whether the tracked player is flying.
       * @return Whether the tracked player is flying
       */
    public boolean isFlying() {
      for(Vector offset : offsets) {
          Location location = player.getLocation().add(offset);
          if(!location.getBlock().getType().isTransparent())
              return false;
      }
      return true;
    }
  }
}
