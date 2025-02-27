package darkqol.intels.factionSystem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

public class FactionSystemIntel extends BaseIntelPlugin {
    public static Logger log = Global.getLogger(FactionSystemIntel.class);

    protected FactionAPI faction;

    private List<StarSystemAPI> factionSystems = new ArrayList<>();

    private StarSystemAPI centerOnSystem = null;

    FactionSystemIntel(String factionId) {
        this.faction = Global.getSector().getFaction(factionId);
        refreshFactionSystemData();
    }

    @Override
    public void notifyPlayerAboutToOpenIntelScreen() {
        refreshFactionSystemData();
    }

    public void refreshFactionSystemData() {
        centerOnSystem = null;
        factionSystems = new ArrayList<>();

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
                if (market.getFaction() == faction && !factionSystems.contains(system) && system.isEnteredByPlayer()) {
                    factionSystems.add(system);
                    break;
                }
            }
        }

        Collections.sort(factionSystems, new Comparator<StarSystemAPI>() {
            @Override
            public int compare(StarSystemAPI system1, StarSystemAPI system2) {
                double distance1 = Misc.getDistanceToPlayerLY(system1.getLocation());
                double distance2 = Misc.getDistanceToPlayerLY(system2.getLocation());
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
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float pad = 10f;
        float mapWidth = width - 350f;
        float listWidth = 350f;

        TooltipMakerAPI mapElement = createMapElement(panel, mapWidth, height, pad);
        TooltipMakerAPI listElement = createListElement(panel, listWidth, height, pad);

        panel.addUIElement(mapElement).inTL(0f, 0f);
        panel.addUIElement(listElement).inTL(mapWidth, 0f);
    }

    private TooltipMakerAPI createMapElement(CustomPanelAPI panel, float width, float height, float pad) {
        TooltipMakerAPI mapElement = panel.createUIElement(width, height, true);
        Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();

        MapParams params = new MapParams();
        params.showFilter = true;
        params.zoomLevel = 2.0F;
        if (centerOnSystem != null) {
            params.centerOn = centerOnSystem.getLocation();
        } else {
            params.centerOn = playerLoc;
        }
        for (StarSystemAPI system : factionSystems) {
            params.showSystem(system);
        }
        UIPanelAPI map = mapElement.createSectorMap(width - 10f, height - 15f, params, null);
        mapElement.addCustom(map, 5f);
        return mapElement;
    }

    private TooltipMakerAPI createListElement(CustomPanelAPI panel, float width, float height, float listPadding) {
        TooltipMakerAPI listElement = panel.createUIElement(width, height, true);
        listElement.addSectionHeading("List System", Alignment.MID, 0f);

        listElement.addButton("Center On Player", "darkCenterOnPlayerClick", width - 10f, 20f, 10f);
        listElement.addSpacer(10f);

        int rowIndex = 0;
        for (StarSystemAPI system : factionSystems) {
            addSystemEntry(listElement, system, width - 10f, 0, panel, rowIndex++);
        }
        return listElement;
    }

    private void addSystemEntry(TooltipMakerAPI listElement, StarSystemAPI system, float containerWidth, float padding,
            CustomPanelAPI parentPanel, int rowIndex) {
        final float TEXT_WIDTH = containerWidth - 100f;
        final float BUTTON_WIDTH = 80f;
        final float ROW_HEIGHT = 50f;

        String systemName = system.getName();
        float distanceLY = Misc.getDistanceToPlayerLY(system.getLocation());
        String distanceStr = String.format("%.1f", distanceLY);
        String displayText = getNumberPlanetOfFactionInSector(system) + " planets " + " (" + distanceStr + " LY)";

        Color backgroundColor = (rowIndex % 2 == 0) ? faction.getGridUIColor() : Color.black;

        CustomPanelAPI rowContainer = parentPanel.createCustomPanel(containerWidth, ROW_HEIGHT, null);

        float bgPadding = (ROW_HEIGHT / 2) + padding / 2;
        UIComponentAPI backgroundRect = listElement.createRect(backgroundColor, bgPadding);
        rowContainer.addComponent(backgroundRect).inTL(0, 0).setSize(containerWidth, ROW_HEIGHT + padding);

        StockResult contentResult = createPanelWithTextAndSubText(
                parentPanel,
                containerWidth,
                ROW_HEIGHT,
                systemName,
                displayText,
                TEXT_WIDTH,
                padding,
                padding,
                null);
        CustomPanelAPI contentPanel = contentResult.panel;

        TooltipMakerAPI buttonHolder = contentPanel.createUIElement(BUTTON_WIDTH, 0f, false);
        ButtonAPI showButton = buttonHolder.addButton(
                "Show",
                "darkSystemClickShow_" + system.getId(),
                faction.getBaseUIColor(),
                faction.getDarkUIColor(),
                BUTTON_WIDTH - 10f,
                24,
                3);

        contentPanel.addUIElement(buttonHolder).inTL(260,
                (ROW_HEIGHT - 30) / 2);

        rowContainer.addComponent(contentPanel).inTL(0, 0);

        listElement.addCustom(rowContainer, padding);
    }

    public static StockResult createPanelWithTextAndSubText(
            CustomPanelAPI parentPanel,
            float panelWidth, float panelHeight,
            String text, String subText, float textAreaWidth, float textPadding,
            float pad, Color textColor) {

        if (textColor == null) {
            textColor = Misc.getTextColor();
        }

        CustomPanelAPI panel = parentPanel.createCustomPanel(panelWidth, panelHeight, null);
        TooltipMakerAPI textElement = panel.createUIElement(textAreaWidth, panelHeight, false);

        if (text != null) {
            textElement.setParaFontDefault();
            textElement.addPara(text, textColor, textPadding);
            if (subText != null) {
                textElement.addPara(subText, textColor, textPadding);
            }
        }
        panel.addUIElement(textElement).inTL(0, (panelHeight - textElement.getHeightSoFar()) / 2);

        StockResult result = new StockResult(panel);
        result.elements.add(textElement);
        return result;
    }

    private int getNumberPlanetOfFactionInSector(StarSystemAPI system) {
        int count = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (market.getFaction() == faction) {
                count++;
            }
        }
        return count;
    }

    public static class StockResult {
        public CustomPanelAPI panel;
        public List<UIComponentAPI> elements = new ArrayList<>();

        public StockResult(CustomPanelAPI panel) {
            this.panel = panel;
        }
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        log.info("Button pressed: " + buttonId);
        String[] separate = ((String) buttonId).split("_");

        if (separate[0].equals("darkSystemClickShow")) {
            StarSystemAPI system = Global.getSector().getStarSystem(separate[1]);
            centerOnSystem = system;
        } else if (buttonId.equals("darkCenterOnPlayerClick")) {
            centerOnSystem = null;
        }
        ui.updateUIForItem(this);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("Faction Systems");
        return tags;
    }

    @Override
    public String getIcon() {
        return faction.getCrest();
    }
}
