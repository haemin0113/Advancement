package org.haemin.advancement.model;

public class Progress {
    public long value;
    public long target;
    public boolean completed;

    // 기간 키(예: daily:2025-09-21). 기간이 바뀌면 자동 초기화
    public String period;

    // checklist
    public long checklistBits;
    // streak
    public int  streak;
    public long lastTs;
    public int  best;
    // timetrial
    public long ttStart;
    public long ttAccum;
    public long ttCooldownUntil;
    public long ttBest;

    public Progress() {}
    public Progress(long value, long target) { this.value = value; this.target = target; }

    public int percent() {
        if (target <= 0) return 0;
        long p = Math.round((value * 100.0) / target);
        return (int) Math.max(0, Math.min(100, p));
    }
}
