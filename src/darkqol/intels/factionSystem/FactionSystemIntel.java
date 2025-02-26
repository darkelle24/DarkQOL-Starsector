package darkqol.intels.factionSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class FactionSystemIntel extends BaseIntelPlugin {

    protected FactionAPI faction;

    private List<StarSystemAPI> factionSystems = new ArrayList<>();

    FactionSystemIntel(String factionId) {
        this.faction = Global.getSector().getFaction(factionId);
        refreshFactionSystemData();
    }

    @Override
    public void notifyPlayerAboutToOpenIntelScreen() {
        refreshFactionSystemData();
    }

    private double getDistance(Vector2f a, Vector2f b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void refreshFactionSystemData() {
        factionSystems = new ArrayList<>();

        final Vector2f playerLocation = Global.getSector().getPlayerFleet().getLocation();

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
                if (market.getFaction() == faction && !factionSystems.contains(system)) {
                    factionSystems.add(system);
                    break;
                }
            }
        }

        Collections.sort(factionSystems, new Comparator<StarSystemAPI>() {
            @Override
            public int compare(StarSystemAPI system1, StarSystemAPI system2) {
                double distance1 = getDistance(system1.getLocation(), playerLocation);
                double distance2 = getDistance(system2.getLocation(), playerLocation);
                return Double.compare(distance1, distance2);
            }
        });
    }

    public FactionAPI getFaction() {
        return faction;
    }

    public static FactionSystemIntel createEvent(String factionId) {
        FactionSystemIntel profile = new FactionSystemIntel(factionId);
        Global.getSector().getIntelManager().addIntel(profile, true);
        profile.setNew(false);
        return profile;
    }

    @Override
    public String getName() {
        return Misc.ucFirst(faction.getDisplayName()) + " Systems";
    }

    @Override
    public String getSortString() {
        return faction.getDisplayName();
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addPara("Systems controlled by " + faction.getDisplayName() + ":", faction.getBaseUIColor(), 10f);
        float heightButton = 40f;
        float padButton = 0f;

        for (StarSystemAPI system : factionSystems) {
            ButtonAPI button = info.addButton(system.getName(), system.getId(), faction.getBaseUIColor(),
                    faction.getDarkUIColor(), Alignment.MID, CutStyle.NONE, (int) width, heightButton, padButton);
            // button.setShortcutKey(30, false);
        }
    }

    /*
     * public boolean buttonPressed(Object buttonId) {
     * if (buttonId instanceof String) {
     * StarSystemAPI system = Global.getSector().getStarSystem((String) buttonId);
     * if (system != null) {
     * Global.getSector().getCampaignUI().centerOnStarSystem(system);
     * return true;
     * }
     * }
     * return false;
     * }
     */

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("faction_systems");
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return null; // Pas de localisation spécifique sur la carte
    }

    @Override
    public String getIcon() {
        return faction.getCrest();
    }
}