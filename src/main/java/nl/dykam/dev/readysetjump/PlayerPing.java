// Useless class of the year. Might be fun for display and statistical purposes, but on short intervals as reliable as Math.random();
package nl.dykam.dev.readysetjump;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PlayerPing {
  static Method craftPlayerGetHandle;
  static Field entityHumanPing;
  static boolean initialized = false;

  public static int getPing(Player player, int fallback) {
    if(!init(player))
      return fallback;
    try {
      Object handle = craftPlayerGetHandle.invoke(player);
      return entityHumanPing.getInt(handle);
    } catch (IllegalAccessException iae) {
      return fallback;
    } catch (InvocationTargetException ite) {
      return fallback;
    }
  }

  private static boolean init(Player player) {
    if(initialized)
      return craftPlayerGetHandle != null;
    try {
      Class craftPlayerClass = player.getClass();
      if(!craftPlayerClass.getSimpleName().equals("CraftPlayer"))
        return false;
      craftPlayerGetHandle = craftPlayerClass.getMethod("getHandle");
      Object handle = craftPlayerGetHandle.invoke(player);
      Class entityHumanClass = handle.getClass();
      if(entityHumanClass.getSimpleName().equals("EntityHuman"))
        return false;
      entityHumanPing = entityHumanClass.getField("ping");
      return true;
    } catch (NoSuchMethodException nsme) {
      return false;
    }  catch (NoSuchFieldException nsfe) {
      return false;
    } catch (IllegalAccessException iae) {
      return false;
    } catch (InvocationTargetException ite) {
      return false;
    }
  }
}
