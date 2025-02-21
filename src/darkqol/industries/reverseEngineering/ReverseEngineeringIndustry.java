package darkqol.industries.reverseEngineering;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import darkqol.ids.Ids;
import darkqol.utils.AbstractSubmarketIndustry;
import darkqol.utils.DailyCycleTracker;
import darkqol.utils.SaveOneData;

public class ReverseEngineeringIndustry extends AbstractSubmarketIndustry {
    public static final Logger log = Global.getLogger(ReverseEngineeringIndustry.class);
    private static final int NUMBER_REVERSE_TO_GET_BP = 5; // Seuil pour débloquer un blueprint
    private static final int NUMBER_GET_BY_REVERSE = 1;
    private static final int DAY_REQUIRED = 30;
    private static final boolean DEBUG_MODE = true;

    private static final int AI_ALPHA_DAYREQUIRED_SUB = 15;
    private static final int AI_BETA_DAYREQUIRED_SUB = 10;
    private static final int AI_GAMMA_DAYREQUIRED_SUB = 5;
    private static final int IMPROVE_DAYREQUIRED_SUB = 10;

    private static final int AI_ALPHA_RESEARCH_ADVANCE_ADD = 2;
    private static final int AI_BETA_RESEARCH_ADVANCE_ADD = 1;
    private static final int IMPROVE_RESEARCH_ADVANCE_ADD = 2;

    private ShipVariantAPI currentShipVariant = null;
    private int daysRequired = DAY_REQUIRED;
    private int daysPassed = 0;
    private SaveOneData<Map<String, Float>> reverseEngShipProgressList;
    private SaveOneData<Map<String, Float>> reverseEngWeaponProgressList;
    private SaveOneData<Map<String, Float>> reverseEngWingProgressList;

    private DailyCycleTracker dailyCycleTracker;

    public ReverseEngineeringIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkEngHubStorageColour");
        this.dailyCycleTracker = new DailyCycleTracker();
        this.reverseEngShipProgressList = new SaveOneData<Map<String, Float>>(Ids.REVERSE_ENG_SHIP_MEMORY,
                new HashMap<String, Float>());
        this.reverseEngWeaponProgressList = new SaveOneData<Map<String, Float>>(Ids.REVERSE_ENG_WEAPON_MEMORY,
                new HashMap<String, Float>());
        this.reverseEngWingProgressList = new SaveOneData<Map<String, Float>>(Ids.REVERSE_ENG_WING_MEMORY,
                new HashMap<String, Float>());
    }

    @Override
    public void advance(float amount) {
        if (isFunctional() && dailyCycleTracker.newDay()) {
            onNewDay();
        }
    }

    private void onNewDay() {
        debugLog("New day event triggered.");

        if (currentShipVariant == null) {
            startNewDeconstruction();
        } else if (daysRequired <= daysPassed) {
            completeDeconstruction();
        } else {
            continueDeconstruction();
        }
    }

    private void startNewDeconstruction() {
        if (initDeconstruction()) {
            notifyDeconstructionStart();
        } else {
            debugLog("No ship available for reverse engineering.");
        }
    }

    private boolean initDeconstruction() {
        CargoAPI storage = market.getSubmarket(Ids.REVERSE_ENG_SUB).getCargo();
        if (storage == null || storage.getMothballedShips() == null) {
            debugLog("Error: CargoAPI or Mothballed ships list is null.");
            return false;
        }

        List<FleetMemberAPI> ships = storage.getMothballedShips().getMembersListCopy();
        if (ships.isEmpty()) {
            debugLog("No mothballed ships available for deconstruction.");
            return false;
        }

        for (FleetMemberAPI ship : ships) {
            if (ship != null) {
                currentShipVariant = ship.getVariant();
                transferWeaponsAndWingsToStorage(ship);
                storage.getMothballedShips().removeFleetMember(ship);
                refreshRequiredDays();
                debugLog("Deconstruction initialized for " + currentShipVariant.getHullSpec().getHullId());
                return true;
            }
        }
        return false;
    }

    public void transferWeaponsAndWingsToStorage(FleetMemberAPI fleetMember) {
        if (fleetMember == null || fleetMember.getVariant() == null) {
            return;
        }

        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            return;
        }

        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        CargoAPI storageCargo = storage.getCargo();

        ShipVariantAPI variant = fleetMember.getVariant();
        ShipHullSpecAPI hullSpec = variant.getHullSpec();

        HashMap<String, String> builtInWeapons = hullSpec.getBuiltInWeapons();
        List<String> builtInWings = hullSpec.getBuiltInWings();

        for (String weaponSlot : variant.getFittedWeaponSlots()) {
            WeaponSpecAPI weaponSpec = variant.getWeaponSpec(weaponSlot);
            if (weaponSpec != null && !builtInWeapons.containsKey(weaponSlot)) {
                storageCargo.addWeapons(weaponSpec.getWeaponId(), 1);
            }
        }

        for (String wingId : variant.getFittedWings()) {
            if (!builtInWings.contains(wingId)) {
                storageCargo.addFighters(wingId, 1);
            }
        }

        storage.getCargo().sort();
    }

    private void notifyDeconstructionStart() {
        String name = currentShipVariant.getHullSpec().getNameWithDesignationWithDashClass();
        Global.getSector().getCampaignUI().addMessage("Reverse engineering has begun for a %s at %s.",
                Global.getSettings().getColor("standardTextColor"), name, market.getName(),
                Misc.getHighlightColor(), market.getFaction().getBrightUIColor());
    }

    private void continueDeconstruction() {
        debugLog("Deconstruction in progress. " + daysPassed + " days passed out of " + daysRequired);
        daysPassed++;
    }

    private void completeDeconstruction() {
        String id = currentShipVariant.getHullSpec().getHullId();
        debugLog("Reverse engineering of " + currentShipVariant.getHullSpec().getNameWithDesignationWithDashClass()
                + " completed.");

        float progress = addProgress(id);
        notifyDeconstructionCompletion(progress);
        checkBlueprintUnlock(id);
        resetDeconstructionVariables();
    }

    private void notifyDeconstructionCompletion(float progress) {
        MessageIntel intel = new MessageIntel("Reverse engineering of the %s has finished.",
                Misc.getTextColor(),
                new String[] { currentShipVariant.getHullSpec().getNameWithDesignationWithDashClass() },
                Misc.getHighlightColor());

        float progressShow = Math.min(progress / NUMBER_REVERSE_TO_GET_BP, 1);
        intel.addLine(BaseIntelPlugin.BULLET + "The current progress is: %s",
                Misc.getHighlightColor(),
                new String[] { Math.round(progressShow * 100) + "%" });

        intel.setIcon(Global.getSettings().getSpriteName("DarkQOL", "revBP"));
        Global.getSector().getCampaignUI().addMessage(intel);
        intel.setSound(BaseIntelPlugin.getSoundMinorMessage());
    }

    private void resetDeconstructionVariables() {
        daysRequired = DAY_REQUIRED;
        daysPassed = 0;
        currentShipVariant = null;
    }

    private float addProgress(String hullId) {
        Map<String, Float> progressList = reverseEngShipProgressList.getData();
        int researchAdvance = calculateResearchAdvance();

        float progress = progressList.getOrDefault(hullId, 0f) + researchAdvance;
        progressList.put(hullId, progress);
        reverseEngShipProgressList.setData(progressList);
        return progress;
    }

    private int calculateResearchAdvance() {
        int researchAdvance = NUMBER_GET_BY_REVERSE;

        switch (getAiCoreIdNotNull()) {
            case Commodities.BETA_CORE:
                researchAdvance += AI_BETA_RESEARCH_ADVANCE_ADD;
                break;
            case Commodities.ALPHA_CORE:
                researchAdvance += AI_ALPHA_RESEARCH_ADVANCE_ADD;
                break;
        }

        if (isImproved()) {
            researchAdvance += IMPROVE_RESEARCH_ADVANCE_ADD;
        }

        return researchAdvance;
    }

    private void checkBlueprintUnlock(String hullId) {
        Map<String, Float> progressList = reverseEngShipProgressList.getData();
        debugLog("Checking blueprint unlock for " + hullId + ". Progress: " + progressList.getOrDefault(hullId, 0f));

        if (progressList.getOrDefault(hullId, 0f) >= NUMBER_REVERSE_TO_GET_BP) {
            generateBlueprint(hullId);
            progressList.remove(hullId);
            reverseEngShipProgressList.setData(progressList);
        }
    }

    private void generateBlueprint(String hullId) {
        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            debugLog("Error: No storage submarket found.");
            return;
        }

        CargoAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
        SpecialItemData blueprint = new SpecialItemData("ship_bp", hullId);
        storage.addSpecial(blueprint, 1);
    }

    private void refreshRequiredDays() {
        if (currentShipVariant != null) {
            daysRequired = DAY_REQUIRED;
            switch (getAiCoreIdNotNull()) {
                case Commodities.GAMMA_CORE:
                    daysRequired -= AI_GAMMA_DAYREQUIRED_SUB;
                    break;
                case Commodities.BETA_CORE:
                    daysRequired -= AI_BETA_DAYREQUIRED_SUB;
                    break;
                case Commodities.ALPHA_CORE:
                    daysRequired -= AI_ALPHA_DAYREQUIRED_SUB;
                    break;
            }
            if (isImproved()) {
                daysRequired -= IMPROVE_DAYREQUIRED_SUB;
            }
        }
    }

    private String getAiCoreIdNotNull() {
        String save = getAICoreId();
        return save != null ? save : "none";
    }

    private void debugLog(String message) {
        if (DEBUG_MODE) {
            log.info(message);
        }
    }

    @Override
    public boolean isAvailableToBuild() {
        return market.isPlayerOwned();
    }

    @Override
    public boolean canBeDisrupted() {
        return false;
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        addCurrentProjectTooltip(tooltip, mode);
    }

    public void addCurrentProjectTooltip(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        if (!isBuilding() && isFunctional() && mode.equals(Industry.IndustryTooltipMode.NORMAL)) {
            FactionAPI faction = market.getFaction();
            tooltip.addSectionHeading("Current Project", faction.getBaseUIColor(), faction.getDarkUIColor(),
                    Alignment.MID, 10f);

            if (currentShipVariant != null) {
                TooltipMakerAPI text = tooltip.beginImageWithText(currentShipVariant.getHullSpec().getSpriteName(), 48);
                text.addPara("Reverse engineering: %s. Time remaining: %s days.", 5f, Misc.getHighlightColor(),
                        currentShipVariant.getHullSpec().getNameWithDesignationWithDashClass(),
                        String.valueOf(daysRequired - daysPassed));
                tooltip.addImageWithText(5f);
            } else {
                tooltip.addPara("No ship is currently being reverse engineered.", 5f);
            }
        }
    }

    protected void addCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode, String coreLevel,
            int dayReduction, int researchAdvance) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();
        String pre = coreLevel + "-level AI core currently assigned. ";
        if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            pre = coreLevel + "-level AI core. ";
        }

        if (mode != AICoreDescriptionMode.INDUSTRY_TOOLTIP && mode != AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            tooltip.addPara(
                    pre + "Reduces upkeep cost by %s. " +
                            "Reduces research time by %s days. " +
                            "Advances research progress by %s points.",
                    opad, highlight,
                    new String[] { (int) ((1.0F - UPKEEP_MULT) * 100.0F) + "%", "" + dayReduction,
                            "" + researchAdvance });
        } else {
            CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(this.aiCoreId);
            TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48.0F);
            text.addPara(
                    pre + "Reduces upkeep cost by %s. " +
                            "Reduces research time by %s days. " +
                            "Advances research progress by %s points.",
                    0.0F, highlight,
                    new String[] { (int) ((1.0F - UPKEEP_MULT) * 100.0F) + "%", "" + dayReduction,
                            "" + researchAdvance });
            tooltip.addImageWithText(opad);
        }
    }

    @Override
    protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Alpha", AI_ALPHA_DAYREQUIRED_SUB, AI_ALPHA_RESEARCH_ADVANCE_ADD);
    }

    @Override
    protected void addBetaCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Beta", AI_BETA_DAYREQUIRED_SUB, AI_BETA_RESEARCH_ADVANCE_ADD);
    }

    @Override
    protected void addGammaCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Gamma", AI_GAMMA_DAYREQUIRED_SUB, 0);
    }

    @Override
    public boolean canImprove() {
        return true;
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        float opad = 10f;
        Color highlight = Misc.getHighlightColor();

        if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
            info.addPara("Reduces research time by %s days. " +
                    "Advances research progress by %s points.",
                    0f, highlight, "" + IMPROVE_DAYREQUIRED_SUB, "" + IMPROVE_RESEARCH_ADVANCE_ADD);
        } else {
            info.addPara("Reduces research time by %s days. " +
                    "Advances research progress by %s points.",
                    0f, highlight, "" + IMPROVE_DAYREQUIRED_SUB, "" + IMPROVE_RESEARCH_ADVANCE_ADD);
        }

        info.addSpacer(opad);
        super.addImproveDesc(info, mode);
    }
}
