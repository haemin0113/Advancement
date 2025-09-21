package org.haemin.advancement.util;

import org.bukkit.inventory.ItemStack;

public class ItemDetect {

    /** ItemsAdder: dev.lone.itemsadder.api.CustomStack#byItemStack → namespaced ID */
    public static String iaId(ItemStack it) {
        if (it == null) return null;
        try {
            Class<?> cs = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object custom = cs.getMethod("byItemStack", org.bukkit.inventory.ItemStack.class).invoke(null, it);
            if (custom != null) {
                return String.valueOf(custom.getClass().getMethod("getNamespacedID").invoke(custom)); // e.g. "myset:bread"
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /** MMOItems: NBTItem#get(item).getString("MMOITEMS_ITEM_ID") / getType() */
    public static String[] mmo(ItemStack it) {
        if (it == null) return null;
        try {
            Class<?> nbtClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
            Object nbt = nbtClass.getMethod("get", org.bukkit.inventory.ItemStack.class).invoke(null, it);
            if (nbt == null) return null;
            String id = String.valueOf(nbtClass.getMethod("getString", String.class).invoke(nbt, "MMOITEMS_ITEM_ID"));
            Object typeObj = nbtClass.getMethod("getType").invoke(nbt);
            String type = (typeObj == null) ? null : String.valueOf(typeObj);
            if (id != null && !id.isEmpty()) return new String[]{type, id};
        } catch (Throwable ignored) {}
        return null;
    }
}
