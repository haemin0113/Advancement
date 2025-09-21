package org.haemin.advancement.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Text {
    private static final Pattern HEX = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    public static String legacy(String s) {
        if (s == null || s.isEmpty()) return "";
        Matcher m = HEX.matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1).toLowerCase();
            StringBuilder code = new StringBuilder("§x");
            for (int i=0;i<6;i++) code.append('§').append(hex.charAt(i));
            m.appendReplacement(out, code.toString());
        }
        m.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    public static void msg(Player p, String s) {
        if (p == null || s == null || s.isEmpty()) return;
        p.sendMessage(legacy(s));
    }
}
