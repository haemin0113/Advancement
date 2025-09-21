package org.haemin.advancement;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.haemin.advancement.command.AdvCommand;
import org.haemin.advancement.gui.AdvGuiManager;
import org.haemin.advancement.gui.GuiConfig;
import org.haemin.advancement.papi.PapiHook;
import org.haemin.advancement.service.*;
import org.haemin.advancement.tracking.WGStayTracker;
import org.haemin.advancement.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class AdvancementPlugin extends JavaPlugin {
    private CfgService cfg;
    private GoalRegistry goals;
    private ProgressService progress;
    private ResetService resets;
    private GuiConfig guiCfg;
    private SoundsService sounds;
    private RewardsService rewards;
    private AdvGuiManager gui;
    private EventRouter router;
    private WGStayTracker wgStay;
    private Economy econ;
    private PapiHook papi;
    private org.bukkit.scheduler.BukkitTask autosaveTask;

    @Override public void onEnable() {
        Log.bind(getLogger());
        getDataFolder().mkdirs();

        ensureDefaults();

        cfg = new CfgService(this);
        resets = new ResetService(this);
        goals = new GoalRegistry(this);
        progress = new ProgressService(this);
        guiCfg = new GuiConfig(this);
        sounds = new SoundsService(this);
        rewards = new RewardsService(this);
        gui = new AdvGuiManager(this);
        router = new EventRouter(this);
        getServer().getPluginManager().registerEvents(router, this);
        if (getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            wgStay = new WGStayTracker(this);
            wgStay.start();
        }
        hookEconomy();
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papi = new PapiHook(this);
            papi.register();
        }
        PluginCommand cmd = getCommand("adv");
        if (cmd != null) {
            AdvCommand exec = new AdvCommand(this);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }
        autosaveTask = getServer().getScheduler().runTaskTimer(this, () -> {
            try { progress.flushAll(); } catch (Throwable ignored) {}
        }, 20L*60L, 20L*60L);
    }

    @Override public void onDisable() {
        progress.flushAll();
        if (autosaveTask != null) try { autosaveTask.cancel(); } catch (Throwable ignored) {}
        if (wgStay != null) try { wgStay.stop(); } catch (Throwable ignored) {}
        if (papi != null) try { papi.unregister(); } catch (Throwable ignored) {}
    }

    public void reloadAll() {
        cfg.reload();
        guiCfg.reload();
        sounds.reload();
        rewards.reload();
        goals.reload();
    }

    private void hookEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }

    private void ensureDefaults() {
        File cfgDir = new File(getDataFolder(), "config");
        if (!cfgDir.exists()) cfgDir.mkdirs();

        export("config/gui.yml", new File(cfgDir, "gui.yml"), guiDefault());
        export("config/sounds.yml", new File(cfgDir, "sounds.yml"), soundsDefault());
        export("readme.md", new File(getDataFolder(), "readme.md"), readmeDefault());
    }

    private void export(String resourcePath, File out, String fallback) {
        if (out.exists()) return;
        try {
            saveResource(resourcePath, false);
        } catch (Throwable t) {
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(fallback.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
        }
    }

    private String guiDefault() {
        StringBuilder sb = new StringBuilder();
        sb.append("title: \"&6도전과제 — %tab%\"\n");
        sb.append("nav:\n");
        sb.append("  all:    { slot: 0, material: BOOKSHELF,   name: \"&e전체\", custom-model-data: 0 }\n");
        sb.append("  daily:  { slot: 1, material: SUNFLOWER,   name: \"&6일일\", custom-model-data: 0 }\n");
        sb.append("  weekly: { slot: 2, material: CLOCK,       name: \"&b주간\", custom-model-data: 0 }\n");
        sb.append("  season: { slot: 3, material: NETHER_STAR, name: \"&d시즌\", custom-model-data: 0 }\n\n");
        sb.append("list_slots: [9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53]\n\n");
        sb.append("item:\n");
        sb.append("  in_progress: { material: WHITE_STAINED_GLASS_PANE, custom-model-data: 0 }\n");
        sb.append("  completed:   { material: LIME_STAINED_GLASS_PANE,  custom-model-data: 0 }\n");
        sb.append("  locked:      { material: RED_STAINED_GLASS_PANE,   custom-model-data: 0, name: \"&c잠금: 선행 필요\" }\n");
        sb.append("  name: \"%title%\"\n");
        sb.append("  lore:\n");
        sb.append("    - \"&7진행: &a%value%&7/&f%target% &8(%percent%%)\"\n");
        sb.append("    - \"&7보상:\"\n");
        sb.append("    - \"%rewards%\"\n");
        sb.append("    - \"%goal_lore%\"\n");
        sb.append("    - \"{op}&8키: %key%\"\n");
        return sb.toString();
    }

    private String soundsDefault() {
        StringBuilder sb = new StringBuilder();
        sb.append("events:\n");
        sb.append("  gui_open:     { sound: UI_BUTTON_CLICK, volume: 1.0, pitch: 1.2 }\n");
        sb.append("  gui_close:    { sound: UI_BUTTON_CLICK, volume: 1.0, pitch: 0.8 }\n");
        sb.append("  progress_add: { sound: ENTITY_EXPERIENCE_ORB_PICKUP, volume: 1.0, pitch: 1.0 }\n");
        sb.append("  completed:    { sound: UI_TOAST_CHALLENGE_COMPLETE, volume: 1.0, pitch: 1.0 }\n");
        sb.append("  claimed:      { sound: ENTITY_PLAYER_LEVELUP, volume: 1.0, pitch: 1.0 }\n");
        return sb.toString();
    }

    private String readmeDefault() {
        return "# Advancement\n\n- 설정 파일: `plugins/Advancement/config/` 폴더를 확인하세요.\n- 쉬운 작성 가이드는 `config/GOALS_GUIDE.md` 문서를 참고하세요.\n- GUI 템플릿은 `config/gui.yml`, 사운드는 `config/sounds.yml` 에서 수정합니다.\n";
    }

    public CfgService cfg() { return cfg; }
    public GoalRegistry goals() { return goals; }
    public ProgressService progress() { return progress; }
    public ResetService resets() { return resets; }
    public GuiConfig guiCfg() { return guiCfg; }
    public SoundsService sounds() { return sounds; }
    public RewardsService rewards() { return rewards; }
    public AdvGuiManager gui() { return gui; }
    public Economy economy() { return econ; }
    public EventRouter router() { return router; }
}
