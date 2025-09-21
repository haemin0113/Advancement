package org.haemin.advancement.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
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
    public String preset;
    public String presetWhen;
    public Map<String, Object> presetOptions = Collections.emptyMap();

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
        private final Set<String> rawValues;
        private final Set<String> exactValues;
        private final List<String> wildcardValues;

        public TrackMatcher(boolean matchesAny, Set<String> values) {
            this.matchesAny = matchesAny;
            if (values == null || values.isEmpty()) {
                this.rawValues = Collections.emptySet();
                this.exactValues = Collections.emptySet();
                this.wildcardValues = List.of();
            } else {
                LinkedHashSet<String> raw = new LinkedHashSet<>();
                LinkedHashSet<String> exact = new LinkedHashSet<>();
                List<String> wildcard = new ArrayList<>();
                for (String value : values) {
                    if (value == null) continue;
                    String normalized = value.trim();
                    if (normalized.isEmpty()) continue;
                    raw.add(normalized);
                    if (normalized.indexOf('*') >= 0) wildcard.add(normalized);
                    else exact.add(normalized);
                }
                this.rawValues = Collections.unmodifiableSet(raw);
                this.exactValues = Collections.unmodifiableSet(exact);
                this.wildcardValues = wildcard.isEmpty() ? List.of() : List.copyOf(wildcard);
            }
        }

        public boolean matches(String id) {
            if (matchesAny) return true;
            if (id == null || id.isEmpty()) return false;
            if (exactValues.contains(id)) return true;
            if (wildcardValues.isEmpty()) return false;
            for (String pattern : wildcardValues) {
                if (wildcardMatch(pattern, id)) return true;
            }
            return false;
        }

        public boolean matchesAny() { return matchesAny; }

        public Set<String> values() { return rawValues; }

        private boolean wildcardMatch(String pattern, String value) {
            if (pattern.equals("*")) return true;
            int start = 0;
            int idx = pattern.indexOf('*');
            if (idx < 0) return pattern.equals(value);
            String remaining = pattern;
            boolean first = true;
            while (idx >= 0) {
                String segment = remaining.substring(0, idx);
                if (!segment.isEmpty()) {
                    int found = value.indexOf(segment, start);
                    if (found < 0 || (first && found != 0)) return false;
                    start = found + segment.length();
                }
                remaining = remaining.substring(idx + 1);
                first = false;
                idx = remaining.indexOf('*');
            }
            if (remaining.isEmpty()) return true;
            return value.endsWith(remaining);
        }

        @Override
        public String toString() {
            return "TrackMatcher{" +
                    "matchesAny=" + matchesAny +
                    ", values=" + rawValues +
                    '}';
        }

        @Override
        public int hashCode() { return Objects.hash(matchesAny, rawValues); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TrackMatcher that)) return false;
            return matchesAny == that.matchesAny && Objects.equals(rawValues, that.rawValues);
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
