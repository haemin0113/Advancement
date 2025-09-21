package org.haemin.advancement.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.haemin.advancement.AdvancementPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class RewardsService {
    private final AdvancementPlugin plugin;
    private YamlConfiguration yml;

    public RewardsService(AdvancementPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File dir = new File(plugin.getDataFolder(), "config");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, "reward.yml");
        if (!f.exists()) {
            YamlConfiguration y = new YamlConfiguration();
            y.set("items.빵세트", new ItemStack(Material.BREAD, 1));
            try { y.save(f); } catch (IOException ignored) {}
        }
        yml = YamlConfiguration.loadConfiguration(f);
    }

    public boolean saveItem(String name, ItemStack stack) {
        if (name == null || name.isEmpty() || stack == null) return false;
        yml.set("items."+name, stack);
        try { yml.save(new File(new File(plugin.getDataFolder(),"config"), "reward.yml")); return true; }
        catch (IOException e) { return false; }
    }

    public ItemStack resolveSample(String spec) {
        if (spec == null || spec.isEmpty()) return new ItemStack(Material.PAPER, 1);
        String[] sp = spec.split("\\s+");
        String id = sp[0];
        int amt = 1;
        if (sp.length > 1) try { amt = Math.max(1, Integer.parseInt(sp[1])); } catch (Exception ignored) {}
        if (id.startsWith("@")) {
            String key = id.substring(1);
            ItemStack saved = yml.getItemStack("items."+key);
            if (saved != null) {
                ItemStack c = saved.clone(); c.setAmount(amt); return c;
            }
        }
        Material m = Material.matchMaterial(id.toUpperCase(Locale.ROOT));
        if (m != null) return new ItemStack(m, amt);
        return new ItemStack(Material.PAPER, amt);
    }

    public void give(Player p, String spec) {
        ItemStack it = resolveSample(spec);
        Map<Integer, ItemStack> remain = p.getInventory().addItem(it);
        if (!remain.isEmpty()) remain.values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
    }
}
