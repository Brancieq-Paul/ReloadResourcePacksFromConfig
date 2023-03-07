package net.raconteur.rpupdater;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.raconteur.rpupdater.config.ModConfigs;

import de.keksuccino.fancymenu.menu.variables.VariableHandler;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VerifyRPVersionsAction extends ButtonActionContainer {
    public VerifyRPVersionsAction() {
        super("paul_verify_rp_versions");
    }

    @Override
    public String getAction() { return "verify_rp_versions";}

    @Override
    public boolean hasValue() { return false; }

    private String getLocalRP() {
        Minecraft minecraft = Minecraft.getInstance();
        PackRepository pack_repo = minecraft.getResourcePackRepository();
        Collection<Pack> selected_packs = pack_repo.getSelectedPacks();
        Iterator<Pack> iterator = selected_packs.iterator();

        // Find RP in selected RPs
        Pattern pattern = Pattern.compile(ModConfigs.RP_NAME_REGEX);
        while (iterator.hasNext()) {
            Pack pack = iterator.next();
            Matcher matcher = pattern.matcher(pack.getId());
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    private static String getLatestZipName(String latestReleaseUrl) {
        try {
            URL url = new URL(latestReleaseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            JsonObject release = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();

            JsonArray assets = release.getAsJsonArray("assets");
            JsonObject asset = assets.get(0).getAsJsonObject();
            return asset.get("name").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getRPLatestRelease() {
        String latestReleaseUrl = String.format("https://api.github.com/repos/%s/%s/releases/latest", ModConfigs.REPO_OWNER, ModConfigs.REPO_NAME);
        String latestZipName = getLatestZipName(latestReleaseUrl);

        if (latestZipName == null) {
            UpdateRPMod.LOGGER.error("Could not retrieve latest zip name");
            return null;
        }
        Pattern pattern = Pattern.compile(ModConfigs.RP_NAME_REGEX);
        Matcher matcher = pattern.matcher(latestZipName);
        if (!matcher.matches()) {
            UpdateRPMod.LOGGER.error("Zip name does not match expected pattern");
            return null;
        }
        return latestZipName;
    }

    @Override
    public void execute(String value) {
        String local_version = getLocalRP();
        String latest_version = getRPLatestRelease();

        if (local_version != null) {
            VariableHandler.setVariable("rp_actual_version", local_version);
        } else {
            VariableHandler.setVariable("rp_actual_version", "Null");
        }
        if (latest_version != null) {
            VariableHandler.setVariable("rp_latest_available_version", latest_version);
        } else {
            return;
        }
        if (local_version == null || !local_version.equals(latest_version)) {
            VariableHandler.setVariable("rp_need_update", "true");
        } else {
            VariableHandler.setVariable("rp_need_update", "false");
        }
    }

    @Override
    public String getActionDescription() {
        return "Verify versions and set corresponding variables: rp_latest_available_version, rp_actual_version, rp_need_update";
    }

    //This action has no value, so I just return NULL here
    @Override
    public String getValueDescription() {
        return null;
    }

    //Same thing. No value. Return NULL.
    @Override
    public String getValueExample() {
        return null;
    }
}
