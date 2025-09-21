package org.haemin.advancement.api;

import org.bukkit.entity.Player;
import org.haemin.advancement.model.GoalDef;

import java.util.UUID;

public interface AdvancementAPI {
    void increment(UUID player, String goalKey, long delta);
    default void increment(Player p, String goalKey, long delta) { increment(p.getUniqueId(), goalKey, delta); }
}
