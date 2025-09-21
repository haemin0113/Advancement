package org.haemin.advancement.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    public Map<String, TrackMatcher> trackMatchers = Collections.emptyMap();
    public Map<String, List<ChecklistMatcher>> checklistMatchers = Collections.emptyMap();

    public static final class TrackMatcher {
        private final boolean matchesAny;
        private final Set<String> values;

        public TrackMatcher(boolean matchesAny, Set<String> values) {
            this.matchesAny = matchesAny;
            this.values = (values == null || values.isEmpty()) ? Collections.emptySet() : Collections.unmodifiableSet(values);
        }

        public boolean matches(String id) {
            if (matchesAny) return true;
            if (id == null || id.isEmpty()) return false;
            return values.contains(id);
        }

        public boolean matchesAny() { return matchesAny; }

        public Set<String> values() { return values; }

        @Override
        public String toString() {
            return "TrackMatcher{" +
                    "matchesAny=" + matchesAny +
                    ", values=" + values +
                    '}';
        }

        @Override
        public int hashCode() { return Objects.hash(matchesAny, values); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TrackMatcher that)) return false;
            return matchesAny == that.matchesAny && Objects.equals(values, that.values);
        }
    }

    public static final class ChecklistMatcher {
        private final int index;
        private final boolean matchesAny;
        private final Set<String> values;
        private final long bit;

        public ChecklistMatcher(int index, boolean matchesAny, Set<String> values) {
            this.index = index;
            this.matchesAny = matchesAny;
            this.values = (values == null || values.isEmpty()) ? Collections.emptySet() : Collections.unmodifiableSet(values);
            this.bit = 1L << index;
        }

        public boolean matches(String id) {
            if (matchesAny) return true;
            if (id == null || id.isEmpty()) return false;
            return values.contains(id);
        }

        public int index() { return index; }

        public long bit() { return bit; }

        @Override
        public String toString() {
            return "ChecklistMatcher{" +
                    "index=" + index +
                    ", matchesAny=" + matchesAny +
                    ", values=" + values +
                    '}';
        }

        @Override
        public int hashCode() { return Objects.hash(index, matchesAny, values); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChecklistMatcher that)) return false;
            return index == that.index && matchesAny == that.matchesAny && Objects.equals(values, that.values);
        }
    }
}
