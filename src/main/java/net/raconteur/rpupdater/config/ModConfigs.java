package net.raconteur.rpupdater.config;

import com.mojang.datafixers.util.Pair;
import net.raconteur.rpupdater.UpdateRPMod;

public class ModConfigs {
    public static SimpleConfig CONFIG;
    private static ModConfigProvider configs;
    public static String RP_NAME_REGEX;
    public static String REPO_OWNER;
    public static String REPO_NAME;

    public static void registerConfigs() {
        configs = new ModConfigProvider();
        createConfigs();

        CONFIG = SimpleConfig.of(UpdateRPMod.MOD_ID + "_config").provider(configs).request();

        assignConfigs();
    }

    private static void createConfigs() {
        configs.addKeyValuePair(new Pair<>("rp.name.regex", "<pack_name_regex>"), "String");
        configs.addKeyValuePair(new Pair<>("rp.repo.owner", "<repo_owner>"), "String");
        configs.addKeyValuePair(new Pair<>("rp.repo.name", "<repo_name>"), "String");
    }

    private static void assignConfigs() {
        RP_NAME_REGEX = CONFIG.getOrDefault("rp.name.regex", "<pack_name_regex>");
        REPO_OWNER = CONFIG.getOrDefault("rp.repo.owner", "<repo_owner>");
        REPO_NAME = CONFIG.getOrDefault("rp.repo.name", "<repo_name>");


        System.out.println("All " + configs.getConfigsList().size() + " configs have been set properly");
    }
}