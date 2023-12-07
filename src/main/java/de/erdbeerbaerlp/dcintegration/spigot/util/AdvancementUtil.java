package de.erdbeerbaerlp.dcintegration.spigot.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AdvancementUtil {
    public record Advancement(String name, String description) {

        public String getTitle() {
                return name;
            }
        }

    public static Advancement getAdvancement(PlayerAdvancementDoneEvent ev) {
        final Object adv = ev.getAdvancement();
        final Class<?> advClass = adv.getClass();
        try {
            final Field h = advClass.getDeclaredField("handle");
            h.setAccessible(true);
            Object handle = h.get(adv);
            Class<?> handleClass = handle.getClass();
            final Field d = handleClass.getDeclaredField("display");
            d.setAccessible(true);
            Object display = d.get(handle);
            if (display == null) return null;  //Cannot be displayed
            Class<?> displayClass = display.getClass();

            final Field shouldAnnounceToChat = displayClass.getDeclaredField("g");
            shouldAnnounceToChat.setAccessible(true);
            if (!(boolean) shouldAnnounceToChat.get(display)) return null;

            final Field titleTxt = displayClass.getDeclaredField("a");
            titleTxt.setAccessible(true);
            Object titleTextComp = titleTxt.get(display);
            Class<?> titleTextCompClass = titleTextComp.getClass();

            final Method getStrTitle = titleTextCompClass.getMethod("getString");
            getStrTitle.setAccessible(true);
            String title = (String) getStrTitle.invoke(titleTextComp, new Object[0]);

            final Field descTxt = displayClass.getDeclaredField("b");
            descTxt.setAccessible(true);
            Object descTextComp = descTxt.get(display);
            Class<?> descTextCompClass = descTextComp.getClass();

            final Method getStrDesc = descTextCompClass.getMethod("getString");
            getStrDesc.setAccessible(true);
            String description = (String) getStrDesc.invoke(descTextComp, new Object[0]);
            return new Advancement(title, description);
        } catch (Exception ignored) {
        }
        try {
            final Method h = advClass.getMethod("getHandle");
            Object handle = h.invoke(adv);
            Class<?> handleClass = handle.getClass();
            final Method d = handleClass.getMethod("c");
            d.setAccessible(true);
            Object display = d.invoke(handle);
            if (display == null) return null;  //Cannot be displayed
            Class<?> displayClass = display.getClass();
            final Method shouldAnnounceToChat = displayClass.getMethod("i");
            if (!(boolean) shouldAnnounceToChat.invoke(display)) return null;
            final Field titleTxt = displayClass.getDeclaredField("a");
            titleTxt.setAccessible(true);
            Object titleTextComp = titleTxt.get(display);
            Class<?> titleTextCompClass = titleTextComp.getClass();

            final Method getStrTitle = titleTextCompClass.getMethod("getString");
            getStrTitle.setAccessible(true);
            String title = (String) getStrTitle.invoke(titleTextComp, new Object[0]);

            final Field descTxt = displayClass.getDeclaredField("b");
            descTxt.setAccessible(true);
            Object descTextComp = descTxt.get(display);
            Class<?> descTextCompClass = descTextComp.getClass();

            final Method getStrDesc = descTextCompClass.getMethod("getString");
            getStrDesc.setAccessible(true);
            String description = (String) getStrDesc.invoke(descTextComp, new Object[0]);
            return new Advancement(title, description);
        } catch (Exception ignored) {
        }
        System.err.println("Unable to find advancement...\n");
        return null; //Cannot find advancement fields
    }

    /**
     * Used for finding the required fields and methods
     */
    private static void printDebugMessage(Class<?> c) {
        DiscordIntegration.LOGGER.info("Declared fields: ");
        for (Field s : c.getDeclaredFields()) {
            s.setAccessible(true);
            try {
                DiscordIntegration.LOGGER.info(s + " : " + s.get(c));
            } catch (Exception ignored) {
            }
        }
        DiscordIntegration.LOGGER.info("Fields: ");
        for (Field s : c.getFields()) {
            s.setAccessible(true);
            try {
                DiscordIntegration.LOGGER.info(s + " : " + s.get(c));
            } catch (Exception ignored) {
            }
        }
        DiscordIntegration.LOGGER.info("Methods: ");
        for (Method m : c.getMethods()) DiscordIntegration.LOGGER.info(m.toString());
    }
}
