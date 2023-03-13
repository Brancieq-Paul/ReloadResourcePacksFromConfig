package net.raconteur.rpupdater;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.raconteur.rpupdater.config.ModConfigs;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UpdateRPAction extends ButtonActionContainer {
    public UpdateRPAction() {
        //The action identifier needs to be unique, so just use your username or something similar as prefix
        super("paul_update_rp");
    }

    //The name of your action. Should be lowercase and without any spaces.
    @Override
    public String getAction() {
        return "update_rp";
    }

    //If the custom action has a value or not
    @Override
    public boolean hasValue() {
        return false;
    }

    // Get latest release name
    private static String getLatestZipUrl(String latestReleaseUrl) {
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
            return asset.get("browser_download_url").getAsString();
        } catch (IOException e) {
            UpdateRPMod.LOGGER.error("Failed to get ZipUrl! Exception raised.");
            e.printStackTrace();
            return null;
        }
    }

    private static boolean downloadZip(String zipUrl, String outputPath) {
        try {
            URL url = new URL(zipUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                UpdateRPMod.LOGGER.error("Failed to download zip: " + responseCode);
                return false;
            }

            String fileName = zipUrl.substring(zipUrl.lastIndexOf("/") + 1);
            File outputFile = new File(outputPath, fileName);

            InputStream inputStream = connection.getInputStream();
            OutputStream outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            return true;
        } catch (IOException e) {
            UpdateRPMod.LOGGER.error("Failed to download zip ! Exception raised.");
            e.printStackTrace();
            return false;
        }
    }

    //Download latest resource pack
    private String download_pack() {
        String owner = ModConfigs.REPO_OWNER;
        String repo = ModConfigs.REPO_NAME;
        String latestReleaseUrl = String.format("https://api.github.com/repos/%s/%s/releases/latest", owner, repo);
        String latestZipUrl = getLatestZipUrl(latestReleaseUrl);
        if (latestZipUrl == null) {
            UpdateRPMod.LOGGER.error("Could not retrieve latest zip URL");
            return null;
        }

        Pattern pattern = Pattern.compile(ModConfigs.RP_NAME_REGEX);
        Matcher matcher = pattern.matcher(latestZipUrl);
        if (!matcher.find()) {
            UpdateRPMod.LOGGER.error("Zip URL does not match expected pattern");
            return null;
        }
        String resourcepacksPath = "./resourcepacks";
        if (downloadZip(latestZipUrl, resourcepacksPath)) {
            UpdateRPMod.LOGGER.info(String.format("Successfully downloaded %s to %s", latestZipUrl, resourcepacksPath));
        } else {
            UpdateRPMod.LOGGER.error(String.format("Failed to download %s to %s", latestZipUrl, resourcepacksPath));
        }
        return matcher.group();
    }

    //Set selected resource pack
    private void set_selected(String updated_pack_name) {
        Minecraft minecraft = Minecraft.getInstance();
        PackRepository pack_repo = minecraft.getResourcePackRepository();
        Collection<Pack> selected_packs = pack_repo.getSelectedPacks();
        boolean selected = false;
        boolean added = false;
        Pack updated_pack = null;

        // Reload available resource packs
        pack_repo.reload();

        // Mutable pack collection
        Collection<Pack> selected_packs_m = new ArrayList<>(selected_packs);
        Iterator<Pack> iterator = selected_packs_m.iterator();

        // Select updated resource pack
        UpdateRPMod.LOGGER.info("Adding updated pack... !");
        Collection<Pack> availablePacks = pack_repo.getAvailablePacks();
        // Choose updated pack
        for (Pack pack : availablePacks) {
            if (pack.getId().equals("file/"+updated_pack_name)) {
                updated_pack = pack;
                selected = true;
                UpdateRPMod.LOGGER.info("New pack added to pack selection !");
            }
        }
        if (!selected) {
            UpdateRPMod.LOGGER.error("New pack \""+ updated_pack_name +"\" could not be added to pack selection !");
            return;
        }

        List<Pack> temp = new ArrayList<>(selected_packs_m);
        // Set updated pack
        while (iterator.hasNext()) {
            Pack pack = iterator.next();
            if (pack.getId().matches("file/" + ModConfigs.RP_NAME_REGEX)) {
                int index = minecraft.options.resourcePacks.indexOf(pack.getId());
                minecraft.options.resourcePacks.set(index, updated_pack.getId());
                index = temp.indexOf(pack);
                temp.set(index, updated_pack);
                added = true;
                UpdateRPMod.LOGGER.info("Old pack removed from pack selection !");
                break;
            }
        }
        if (!added) {
            temp.add(updated_pack);
        }
        selected_packs_m = temp;

        Collection<String> id_list = new ArrayList<>();
        for (Pack pack : selected_packs_m) {
            id_list.add(pack.getId());
        }

        // Reload resource packs
        pack_repo.setSelected(id_list);
        pack_repo.reload();
        minecraft.options.save();
        UpdateRPMod.LOGGER.info("Selected resource packs list updated !");
        minecraft.reloadResourcePacks();
        UpdateRPMod.LOGGER.info("Resource packs reloaded!");
    }

    //Gets called when a button with this custom action is getting clicked
    @Override
    public void execute(String value) {
        String pack_name = download_pack();

        if (pack_name != null) {
            set_selected(pack_name);
        }
    }

    //The description of the action
    @Override
    public String getActionDescription() {
        return "Update the resource pack.";
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
