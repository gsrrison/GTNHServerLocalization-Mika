package site.gtnhserverlocalization;

import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * Resolves GregTech ore display names from OreInfo instead of raw block metadata.
 */
public final class ProspectionNameHelper {

    private static Method getOreInfoMethod;
    private static Method getLocalizedNameMethod;
    private static Method getStackMethod;
    private static Method closeMethod;
    private static boolean reflectionInitialized;
    private static boolean reflectionUnavailable;
    private static boolean warned;

    private ProspectionNameHelper() {}

    public static String getDisplayName(final Block block, final int meta) {
        if (block == null) {
            return "";
        }

        final String gregTechName = getGregTechOreDisplayName(block, meta);
        if (isUsableName(gregTechName)) {
            return gregTechName;
        }

        return new ItemStack(block, 1, meta).getDisplayName();
    }

    private static String getGregTechOreDisplayName(final Block block, final int meta) {
        if (reflectionUnavailable) {
            return null;
        }

        Object oreInfo = null;
        try {
            initReflection();
            if (reflectionUnavailable) {
                return null;
            }

            oreInfo = getOreInfoMethod.invoke(null, block, meta);
            if (oreInfo == null) {
                return null;
            }

            final Object localizedName = getLocalizedNameMethod.invoke(null, oreInfo);
            if (localizedName instanceof String && isUsableName((String) localizedName)) {
                return (String) localizedName;
            }

            final Object stack = getStackMethod.invoke(null, oreInfo, 1);
            if (stack instanceof ItemStack) {
                return ((ItemStack) stack).getDisplayName();
            }
        } catch (final ThreadDeath e) {
            throw e;
        } catch (final Throwable e) {
            warnOnce(e);
        } finally {
            closeOreInfo(oreInfo);
        }

        return null;
    }

    private static synchronized void initReflection() {
        if (reflectionInitialized || reflectionUnavailable) {
            return;
        }

        try {
            final Class<?> oreManagerClass = Class.forName("gregtech.common.ores.OreManager");
            final Class<?> oreInfoClass = Class.forName("gregtech.common.ores.OreInfo");

            getOreInfoMethod = oreManagerClass.getMethod("getOreInfo", Block.class, int.class);
            getLocalizedNameMethod = oreManagerClass.getMethod("getLocalizedName", oreInfoClass);
            getStackMethod = oreManagerClass.getMethod("getStack", oreInfoClass, int.class);
            closeMethod = oreInfoClass.getMethod("close");
            reflectionInitialized = true;
        } catch (final ThreadDeath e) {
            throw e;
        } catch (final Throwable e) {
            reflectionUnavailable = true;
            warnOnce(e);
        }
    }

    private static void closeOreInfo(final Object oreInfo) {
        if (oreInfo == null || closeMethod == null) {
            return;
        }

        try {
            closeMethod.invoke(oreInfo);
        } catch (final ThreadDeath e) {
            throw e;
        } catch (final Throwable ignored) {
            // Best-effort cleanup for GregTech's pooled OreInfo objects.
        }
    }

    private static boolean isUsableName(final String value) {
        return value != null && !value.isEmpty() && !"<illegal ore>".equals(value);
    }

    private static void warnOnce(final Throwable e) {
        if (warned) {
            return;
        }

        warned = true;
        System.err.println("[GTNHServerLocalization] Failed to resolve GregTech ore display name, using vanilla name.");
        e.printStackTrace();
    }
}
