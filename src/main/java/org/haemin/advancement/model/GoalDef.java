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
    public Map<String, List<TrackSpec>> trackSpecs = Collections.emptyMap();

    public static final class TrackMatcher {
        private final boolean matchesAny;
        private final Set<String> values;
        private final List<String> wildcards;

        public TrackMatcher(boolean matchesAny, Set<String> values, List<String> wildcards) {
            this.matchesAny = matchesAny;
            this.values = (values == null || values.isEmpty()) ? Collections.emptySet() : Collections.unmodifiableSet(values);
            this.wildcards = (wildcards == null || wildcards.isEmpty()) ? Collections.emptyList() : List.copyOf(wildcards);
        }

        public boolean matches(String id) {
            if (matchesAny) return true;
            if (id == null || id.isEmpty()) return false;
            if (values.contains(id)) return true;
            if (wildcards.isEmpty()) return false;
            for (String pattern : wildcards) {
                if (Wildcard.match(pattern, id)) return true;
            }
            return false;
        }

        public boolean matchesAny() { return matchesAny; }

        public Set<String> values() { return values; }

        public List<String> wildcards() { return wildcards; }

        @Override
        public String toString() {
            return "TrackMatcher{" +
                    "matchesAny=" + matchesAny +
                    ", values=" + values +
                    ", wildcards=" + wildcards +
                    '}';
        }

        @Override
        public int hashCode() { return Objects.hash(matchesAny, values, wildcards); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TrackMatcher that)) return false;
            return matchesAny == that.matchesAny && Objects.equals(values, that.values) && Objects.equals(wildcards, that.wildcards);
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

    public static final class TrackSpec {
        private final int index;
        private final String kind;
        private final boolean any;
        private final Set<String> values;
        private final List<String> wildcards;
        private final String preset;
        private final int levelMin;
        private final int levelMax;
        private final String merchantProfession;
        private final int distanceSampleMs;
        private final double distanceMinMeters;
        private final String modeFilter;

        public TrackSpec(int index,
                         String kind,
                         boolean any,
                         Set<String> values,
                         List<String> wildcards,
                         String preset,
                         int levelMin,
                         int levelMax,
                         String merchantProfession,
                         int distanceSampleMs,
                         double distanceMinMeters,
                         String modeFilter) {
            this.index = index;
            this.kind = kind;
            this.any = any;
            this.values = (values == null || values.isEmpty()) ? Collections.emptySet() : Collections.unmodifiableSet(values);
            this.wildcards = (wildcards == null || wildcards.isEmpty()) ? Collections.emptyList() : List.copyOf(wildcards);
            this.preset = preset;
            this.levelMin = levelMin;
            this.levelMax = levelMax;
            this.merchantProfession = merchantProfession;
            this.distanceSampleMs = distanceSampleMs;
            this.distanceMinMeters = distanceMinMeters;
            this.modeFilter = modeFilter;
        }

        public int index() { return index; }

        public String kind() { return kind; }

        public boolean matchesAny() { return any; }

        public boolean matchesId(String id) {
            if (any) return true;
            if (id == null || id.isEmpty()) return false;
            if (values.contains(id)) return true;
            if (wildcards.isEmpty()) return false;
            for (String pattern : wildcards) {
                if (Wildcard.match(pattern, id)) return true;
            }
            return false;
        }

        public Set<String> values() { return values; }

        public List<String> wildcards() { return wildcards; }

        public String preset() { return preset; }

        public int levelMin() { return levelMin; }

        public int levelMax() { return levelMax; }

        public String merchantProfession() { return merchantProfession; }

        public int distanceSampleMs() { return distanceSampleMs; }

        public double distanceMinMeters() { return distanceMinMeters; }

        public String modeFilter() { return modeFilter; }
    }

    public static final class Wildcard {
        public static boolean match(String pattern, String value) {
            if (pattern == null || value == null) return false;
            if (pattern.isEmpty()) return value.isEmpty();
            if (!pattern.contains("*")) return pattern.equals(value);
            String[] parts = pattern.split("\\*", -1);
            int index = 0;
            boolean first = true;
            for (String part : parts) {
                if (part.isEmpty()) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    continue;
                }
                int pos = value.indexOf(part, index);
                if (pos < 0) return false;
                if (first && !pattern.startsWith("*")) {
                    if (pos != 0) return false;
                }
                index = pos + part.length();
                first = false;
            }
            if (!pattern.endsWith("*")) {
                String last = parts[parts.length - 1];
                return value.endsWith(last);
            }
            return true;
        }
    }
}
