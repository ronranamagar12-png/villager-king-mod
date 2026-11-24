package com.villagerking.voice;

public class VoiceCommandParser {
    /** Returns the /king sub-command for what the player said, or null if not understood. */
    public static String parse(String said) {
        String s = said.toLowerCase();
        if (s.contains("castle"))                          return "castle";
        if (s.contains("house"))                           return "house";
        if (s.contains("netherite"))                       return "bring netherite";
        if (s.contains("diamond"))                         return "bring diamond";
        if (s.contains("gold"))                            return "bring gold";
        if (s.contains("iron"))                            return "bring iron";
        if (s.contains("follow") || s.contains("come"))    return "follow";
        if (s.contains("stay") || s.contains("stop"))      return "stay";
        if (s.contains("guard") || s.contains("protect"))  return "guard";
        if (s.contains("farm") || s.contains("harvest"))   return "farm";
        if (s.contains("wood") || s.contains("tree"))      return "wood";
        if (s.contains("mine") || s.contains("dig"))       return "mine";
        if (s.contains("work") || s.contains("relax") || s.contains("normal")) return "work";
        return null;
    }
}
