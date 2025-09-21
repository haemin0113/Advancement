package org.haemin.advancement.util;

import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.GoalType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GoalLore {
    public static List<String> describe(GoalDef d) {
        if (d.lore != null && !d.lore.isEmpty()) return d.lore;
        List<String> out = new ArrayList<>();
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
