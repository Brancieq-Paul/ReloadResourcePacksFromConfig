package net.raconteur.rpupdater;

import net.fabricmc.api.ModInitializer;
import net.raconteur.rpupdater.config.ModConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;

public class UpdateRPMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("rpupdater");
    public static final String MOD_ID = "rpupdater";

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing RP Updater !");

        // Mod configs
        ModConfigs.registerConfigs();

        // Register button
        ButtonActionRegistry.registerButtonAction(new UpdateRPAction());
        ButtonActionRegistry.registerButtonAction(new VerifyRPVersionsAction());
    }
}
