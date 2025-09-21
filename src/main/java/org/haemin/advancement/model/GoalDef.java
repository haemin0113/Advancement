package org.haemin.advancement.model;

import java.util.List;
import java.util.Map;

public class GoalDef {
    public String key;
    public String title;
    public GoalType type = GoalType.COUNTER;

    public List<Map<String, Object>> track;
    public String filter;
    public long target = 1;
    public String reset = "daily";
    public String uniqueBy;

    public List<Map<String, Object>> rewards;

    public List<Map<String, Object>> checklistItems;
    public int checklistRequire;

    public Map<String,Object> streakConf;
    public Map<String,Object> timetrialConf;

    public List<String> lore;

    public List<String> requires;
    public List<String> activateIfHas;
    public List<Map<String,Object>> boosts;
}
