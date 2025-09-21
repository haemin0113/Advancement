package org.haemin.advancement.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.Locale;

public class ItemSpec {
    public static boolean has(Player p, String spec) {
        if (p == null || spec == null || spec.isEmpty()) return false;
        String s = spec.trim();
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("ia:")) return hasIA(p, s.substring(3));
        if (lower.startsWith("mmoitems:")) return hasMMO(p, s.substring(9));
        if (lower.startsWith("mc:")) return hasVanilla(p, s.substring(3));
        return hasVanilla(p, s);
    }

    private static boolean hasVanilla(Player p, String id) {
        String raw = id.contains(":") ? id.substring(id.indexOf(':')+1) : id;
        Material m = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
        if (m == null) return false;
        for (ItemStack it : allItems(p)) if (it != null && it.getType() == m) return true;
        return false;
    }

    private static boolean hasIA(Player p, String nsOrId) {
        for (ItemStack it : allItems(p)) {
            if (it == null) continue;
            String ia = iaId(it);
            if (ia == null) continue;
            if (equalsId(ia, nsOrId)) return true;
        }
        return false;
    }

    private static boolean hasMMO(Player p, String typeColonId) {
        String want = typeColonId.replace("::", ":");
        for (ItemStack it : allItems(p)) {
            if (it == null) continue;
            String mi = mmoId(it);
            if (mi == null) continue;
            if (equalsId(mi, want)) return true;
        }
        return false;
    }

    private static ItemStack[] allItems(Player p) {
        ItemStack[] inv = p.getInventory().getContents();
        ItemStack off = p.getInventory().getItem(EquipmentSlot.OFF_HAND);
        ItemStack head = p.getInventory().getHelmet();
        ItemStack chest = p.getInventory().getChestplate();
        ItemStack legs = p.getInventory().getLeggings();
        ItemStack feet = p.getInventory().getBoots();
        int n = inv.length + 5;
        ItemStack[] all = new ItemStack[n];
        System.arraycopy(inv, 0, all, 0, inv.length);
        all[inv.length] = off;
        all[inv.length+1] = head;
        all[inv.length+2] = chest;
        all[inv.length+3] = legs;
        all[inv.length+4] = feet;
        return all;
    }

    private static boolean equalsId(String a, String b) {
        return a.equalsIgnoreCase(b) || a.equalsIgnoreCase("minecraft:"+b) || ("minecraft:"+a).equalsIgnoreCase(b);
    }

    private static String iaId(ItemStack it) {
        try {
            Class<?> cs = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method by = cs.getMethod("byItemStack", ItemStack.class);
            Object o = by.invoke(null, it);
            if (o == null) return null;
            try {
                Method m = o.getClass().getMethod("getNamespacedID");
                Object r = m.invoke(o);
                if (r != null) return String.valueOf(r);
            } catch (NoSuchMethodException ignore) {
                Method m2 = o.getClass().getMethod("getNamespace");
                Method m3 = o.getClass().getMethod("getId");
                Object ns = m2.invoke(o);
                Object id = m3.invoke(o);
                if (ns != null && id != null) return ns + ":" + id;
            }
        } catch (Throwable ignored) {}
        try {
            PersistentDataContainer pdc = it.getItemMeta()==null? null : it.getItemMeta().getPersistentDataContainer();
            if (pdc != null) {
                for (NamespacedKey k : pdc.getKeys()) {
                    if (k.getKey().toLowerCase(Locale.ROOT).contains("itemsadder")) {
                        String v = pdc.get(k, PersistentDataType.STRING);
                        if (v != null && v.contains(":")) return v;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String mmoId(ItemStack it) {
        try {
            Class<?> nbtCls = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
            Method get = nbtCls.getMethod("get", ItemStack.class);
            Object nbt = get.invoke(null, it);
            if (nbt != null) {
                Method gs = nbt.getClass().getMethod("getString", String.class);
                String id = String.valueOf(gs.invoke(nbt, "MMOITEMS_ITEM_ID"));
                String type = String.valueOf(gs.invoke(nbt, "MMOITEMS_ITEM_TYPE"));
                if (id != null && type != null && !"null".equalsIgnoreCase(id) && !"null".equalsIgnoreCase(type))
                    return type + ":" + id;
            }
        } catch (Throwable ignored) {}
        try {
            PersistentDataContainer pdc = it.getItemMeta()==null? null : it.getItemMeta().getPersistentDataContainer();
            if (pdc != null) {
                String id = tryPdc(pdc, "MMOITEMS_ITEM_ID");
                String type = tryPdc(pdc, "MMOITEMS_ITEM_TYPE");
                if (id != null && type != null) return type + ":" + id;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String tryPdc(PersistentDataContainer pdc, String key) {
        for (NamespacedKey k : pdc.getKeys()) {
            if (k.getKey().equalsIgnoreCase(key)) {
                String v = pdc.get(k, PersistentDataType.STRING);
                if (v != null) return v;
            }
        }
        return null;
    }
}
