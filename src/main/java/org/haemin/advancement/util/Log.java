package org.haemin.advancement.util;

import java.util.logging.Logger;

public class Log {
    private static Logger L;
    public static void bind(Logger logger) { L = logger; }
    public static void info(String s) { if (L != null) L.info(s); }
    public static void warn(String s) { if (L != null) L.warning(s); }
    public static void severe(String s) { if (L != null) L.severe(s); }
}
