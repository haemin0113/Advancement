package org.haemin.advancement.util;

import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.GoalType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GoalLore {
    private static final Map<String, String> PRESET_DESCRIPTIONS = Map.ofEntries(
            Map.entry("harvest", "&7- 성숙 작물 수확"),
            Map.entry("shear", "&7- 동물 털깎기"),
            Map.entry("breed", "&7- 동물 번식"),
            Map.entry("tame", "&7- 몹 길들이기"),
            Map.entry("trade", "&7- 주민 거래 결과 수령"),
            Map.entry("enchant", "&7- 아이템 마법 부여"),
            Map.entry("anvil", "&7- 모루 수리/합성"),
            Map.entry("smithing", "&7- 대장간 작업"),
            Map.entry("brew", "&7- 물약 양조"),
            Map.entry("consume", "&7- 음식/물약 소비"),
            Map.entry("distance", "&7- 이동 거리 측정"),
            Map.entry("advancement", "&7- 마인크래프트 업적 달성")
    );

    public static List<String> describe(GoalDef d) {
        if (d.lore != null && !d.lore.isEmpty()) return d.lore;
        List<String> out = new ArrayList<>();
        if (d.preset != null) {
            String desc = PRESET_DESCRIPTIONS.get(d.preset);
            if (desc != null) out.add(desc);
        }
        if (d.type == GoalType.COUNTER || d.type == GoalType.UNIQUE) {
            if (d.track != null) {
                for (Map<String,Object> e : d.track) {
                    Object src = e.get("source");
                    if (src != null) out.add("&7- 소스: &f" + src);
                }
            }
            out.add("&7- 목표: &f" + d.target);
        } else if (d.type == GoalType.CHECKLIST) {
            int need = d.checklistRequire > 0 ? d.checklistRequire : (d.checklistItems == null ? 0 : d.checklistItems.size());
            out.add("&7- 체크 항목 " + need + "개 달성");
        } else if (d.type == GoalType.STREAK) {
            out.add("&7- 목표 콤보: &f" + d.target);
        } else if (d.type == GoalType.TIMETRIAL) {
            out.add("&7- 제한 시간 목표 달성");
            out.add("&7- 목표량: &f" + d.target);
        } else {
            out.add("&7- 목표: &f" + d.target);
        }
        if (d.reset != null) {
            String r = d.reset.toLowerCase().startsWith("repeat") ? "반복형" : d.reset;
            out.add("&7- 리셋: &f" + r);
        }
        return out;
    }
}
