package org.haemin.advancement.service;

import org.haemin.advancement.AdvancementPlugin;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ResetService {
    private final ZoneId zone;

    public ResetService(AdvancementPlugin plugin) {
        String tz = plugin.getConfig().getString("general.timezone", "Asia/Seoul");
        ZoneId z;
        try { z = ZoneId.of(tz); } catch (Exception e) { z = ZoneId.systemDefault(); }
        this.zone = z;
    }

    public String periodIdFor(String reset) {
        if (reset == null || reset.isEmpty()) return "none";
        String r = reset.toLowerCase(Locale.ROOT);
        if (r.startsWith("season:")) return r;
        if (r.startsWith("repeat"))  return "repeat";
        ZonedDateTime now = ZonedDateTime.now(zone);
        if (r.startsWith("daily"))   return "daily:"   + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        if (r.startsWith("monthly")) return "monthly:" + now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        if (r.startsWith("weekly")) {
            java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
            int week = now.get(wf.weekOfWeekBasedYear());
            int year = now.get(wf.weekBasedYear());
            return "weekly:%d-W%02d".formatted(year, week);
        }
        if (r.startsWith("rrule"))   return "rrule:" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return "none";
    }

    public boolean isRepeat(String reset) {
        return reset != null && reset.toLowerCase(Locale.ROOT).startsWith("repeat");
    }
}
