package darkqol.intels.factionSystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;

public class FactionSystemManager {
    public static Logger log = Global.getLogger(FactionSystemManager.class);

    protected Map<String, FactionSystemIntel> profiles = new HashMap<>();

    protected static final String MANAGER_MAP_KEY = "dark_factionSystemManager";

    public static FactionSystemManager getManager() {
        Map<String, Object> data = Global.getSector().getPersistentData();
        return (FactionSystemManager) data.get(MANAGER_MAP_KEY);
    }

    public static FactionSystemManager createManager() {
        Map<String, Object> data = Global.getSector().getPersistentData();
        FactionSystemManager manager = (FactionSystemManager) data.get(MANAGER_MAP_KEY);
        if (manager != null) {
            return manager;
        }

        manager = new FactionSystemManager();
        data.put(MANAGER_MAP_KEY, manager);
        return manager;
    }

    public FactionSystemIntel getFactionSystemIntel(String factionId) {
        return profiles.get(factionId);
    }

    public FactionSystemIntel createFactionSystemIntel(String factionId) {

        if (profiles.containsKey(factionId)) {
            return profiles.get(factionId);
        }

        FactionSystemIntel profile = FactionSystemIntel.createEvent(factionId);
        if (profile == null) {
            log.error("Failed to create FactionSystemIntel for faction " + factionId);
            return null;
        }
        log.info("Created FactionSystemIntel for faction " + factionId);
        profiles.put(factionId, profile);
        return profile;
    }

    public void removeFactionSystemIntel(String factionId) {
        if (profiles.containsKey(factionId)) {
            profiles.get(factionId).endImmediately();
            profiles.remove(factionId);
        }
    }

    public void refreshProfiles() {

        List<FactionAPI> allFactions = Global.getSector().getAllFactions();
        Set<String> existingFactionIds = new HashSet<>();

        for (FactionAPI faction : allFactions) {
            if (faction.isShowInIntelTab() || faction.isPlayerFaction()) {
                existingFactionIds.add(faction.getId());
            }
        }

        for (String factionId : existingFactionIds) {
            if (!profiles.containsKey(factionId)) {
                createFactionSystemIntel(factionId);
            }
        }

        Set<String> profileFactions = new HashSet<>(profiles.keySet());
        for (String factionId : profileFactions) {
            if (!existingFactionIds.contains(factionId)) {
                removeFactionSystemIntel(factionId);
            }
        }

    }
}
