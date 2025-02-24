package darkqol.industries.reverseEngineering;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import darkqol.utils.AbstractSubmarketIndustry;
import darkqol.utils.SaveOneData;

public abstract class AbstractReverseEngineeringIndustry<T> extends AbstractSubmarketIndustry {
    public static final Logger log = Global.getLogger(AbstractReverseEngineeringIndustry.class);

    protected final int NUMBER_REVERSE_TO_GET_BP = 5;
    protected final int NUMBER_GET_BY_REVERSE = 1;
    protected final int DAY_REQUIRED = 30;
    protected boolean DEBUG_MODE = true;

    protected final float AI_ALPHA_DAYREQUIRED_REDUCTION = 0.40f;
    protected final float AI_BETA_DAYREQUIRED_REDUCTION = 0.20f;
    protected final float AI_GAMMA_DAYREQUIRED_REDUCTION = 0.10f;
    protected final float IMPROVE_DAYREQUIRED_REDUCTION = 0.10f;

    protected final int AI_ALPHA_RESEARCH_ADVANCE_ADD = 2;
    protected final int AI_BETA_RESEARCH_ADVANCE_ADD = 1;
    protected final int IMPROVE_RESEARCH_ADVANCE_ADD = 2;

    protected int daysRequired = getDayRequired();
    protected int daysPassed = 0;
    protected SaveOneData<Map<String, Float>> reverseEngProgressList;

    protected T currentReverseEng = null;

    protected String typeReverse;
    protected String idMemory;

    public AbstractReverseEngineeringIndustry(String submarketId, String storageColour, String typeReverse,
            String idMemory) {
        super(submarketId, storageColour);
        this.reverseEngProgressList = new SaveOneData<Map<String, Float>>(idMemory, new HashMap<String, Float>());
        this.typeReverse = typeReverse;
        this.idMemory = idMemory;
    }

    @Override
    public void advance(float amount) {
        if (isFunctional()) {
            onNewDay();
        }
    }

    protected int getDayRequired() {
        return DAY_REQUIRED;
    }

    protected abstract boolean initDeconstruction();

    protected abstract String getNameReverse();

    protected abstract String getIdReverse();

    protected abstract String getSprite();

    protected abstract SpecialItemData getSpecialItem(String id); // blueprint = new SpecialItemData("ship_bp", hullId);
                                                                  // // weapon_bp wing_bp

    protected void onNewDay() {
        debugLog("New day event triggered.");

        if (currentReverseEng == null) {
            startNewDeconstruction();
        } else if (daysRequired <= daysPassed) {
            completeDeconstruction();
        } else {
            continueDeconstruction();
        }
    }

    private void startNewDeconstruction() {
        if (initDeconstruction()) {
            debugLog("Deconstruction initialized for " + getNameReverse());
            refreshRequiredDays();
            notifyDeconstructionStart();
        } else {
            debugLog("No " + typeReverse + " available for reverse engineering.");
        }
    }

    protected float getReductionFactor() {
        return 1.0f;
    }

    private void refreshRequiredDays() {
        if (currentReverseEng != null) {
            daysRequired = (int) (getDayRequired() * getReductionFactor());

            float reductionPercentage = 0f;

            switch (getAiCoreIdNotNull()) {
                case Commodities.GAMMA_CORE:
                    reductionPercentage += AI_GAMMA_DAYREQUIRED_REDUCTION;
                    break;
                case Commodities.BETA_CORE:
                    reductionPercentage += AI_BETA_DAYREQUIRED_REDUCTION;
                    break;
                case Commodities.ALPHA_CORE:
                    reductionPercentage += AI_ALPHA_DAYREQUIRED_REDUCTION;
                    break;
            }
            if (isImproved()) {
                reductionPercentage += IMPROVE_DAYREQUIRED_REDUCTION;
            }

            daysRequired = (int) (daysRequired * (1 - reductionPercentage));
        }
    }

    private String getAiCoreIdNotNull() {
        String save = getAICoreId();
        return save != null ? save : "none";
    }

    private void notifyDeconstructionStart() {
        String name = getNameReverse();
        String marketName = market.getName();
        int requiredDays = daysRequired;

        Global.getSector().getCampaignUI().addMessage(
                String.format("Reverse engineering has begun for a %s at %s. Required days: %s", name, marketName,
                        requiredDays),
                Global.getSettings().getColor("standardTextColor"),
                name,
                marketName,
                Misc.getHighlightColor(),
                market.getFaction().getBrightUIColor());
    }

    private void continueDeconstruction() {
        refreshRequiredDays();
        debugLog("Deconstruction of " + getNameReverse() + " in progress. " + daysPassed + " days passed out of "
                + daysRequired);
        daysPassed++;
    }

    private void completeDeconstruction() {
        String id = getIdReverse();
        debugLog("Reverse engineering of " + getNameReverse()
                + " completed.");

        float progress = addProgress(id);
        notifyDeconstructionCompletion(progress);
        checkBlueprintUnlock(id);
        resetDeconstructionVariables();
    }

    private void resetDeconstructionVariables() {
        daysRequired = getDayRequired();
        daysPassed = 0;
        currentReverseEng = null;
    }

    protected void notifyDeconstructionCompletion(float progress) {
        MessageIntel intel = new MessageIntel("Reverse engineering of the %s has finished.",
                Misc.getTextColor(),
                new String[] { getNameReverse() },
                Misc.getHighlightColor());

        float progressShow = Math.min(progress / NUMBER_REVERSE_TO_GET_BP, 1);
        intel.addLine(BaseIntelPlugin.BULLET + "The current progress is: %s",
                Misc.getHighlightColor(),
                new String[] { Math.round(progressShow * 100) + "%" });

        intel.setIcon(Global.getSettings().getSpriteName("DarkQOL", "revBP"));
        Global.getSector().getCampaignUI().addMessage(intel);
        intel.setSound(BaseIntelPlugin.getSoundMinorMessage());
    }

    private void checkBlueprintUnlock(String id) {
        Map<String, Float> progressList = reverseEngProgressList.getData();
        debugLog("Checking blueprint unlock for " + id + ". Progress: " + progressList.getOrDefault(id, 0f));

        if (progressList.getOrDefault(id, 0f) >= NUMBER_REVERSE_TO_GET_BP) {

            if (generateBlueprint(id)) {
                progressList.remove(id);
                reverseEngProgressList.setData(progressList);
            }
        }
    }

    private boolean generateBlueprint(String id) {
        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            debugLog("Error: No storage submarket found.");
            return false;
        }

        CargoAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
        SpecialItemData blueprint = getSpecialItem(id);
        storage.addSpecial(blueprint, 1);
        return true;
    }

    protected float addProgress(String itemId) {
        Map<String, Float> progressList = reverseEngProgressList.getData();
        int researchAdvance = calculateResearchAdvance();

        float progress = progressList.getOrDefault(itemId, 0f) + researchAdvance;
        progressList.put(itemId, progress);
        reverseEngProgressList.setData(progressList);
        return progress;
    }

    protected int calculateResearchAdvance() {
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

    protected void checkBlueprintUnlock(String itemId, String blueprintType) {
        Map<String, Float> progressList = reverseEngProgressList.getData();
        debugLog("Checking blueprint unlock for " + itemId + ". Progress: " + progressList.getOrDefault(itemId, 0f));

        if (progressList.getOrDefault(itemId, 0f) >= NUMBER_REVERSE_TO_GET_BP) {
            generateBlueprint(itemId, blueprintType);
            progressList.remove(itemId);
            reverseEngProgressList.setData(progressList);
        }
    }

    protected void generateBlueprint(String itemId, String blueprintType) {
        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            debugLog("Error: No storage submarket found.");
            return;
        }

        CargoAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
        SpecialItemData blueprint = new SpecialItemData(blueprintType, itemId);
        storage.addSpecial(blueprint, 1);
    }

    protected void debugLog(String message) {
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
        if (getMarket() != null && !isBuilding() && isFunctional()
                && mode.equals(Industry.IndustryTooltipMode.NORMAL)) {
            FactionAPI faction = market.getFaction();
            tooltip.addSectionHeading("Current Project " + typeReverse, faction.getBaseUIColor(),
                    faction.getDarkUIColor(),
                    Alignment.MID, 10f);

            if (currentReverseEng != null) {
                TooltipMakerAPI text = tooltip.beginImageWithText(getSprite(), 48);
                text.addPara("Reverse engineering: %s. Time remaining: %s days.", 5f, Misc.getHighlightColor(),
                        getNameReverse(),
                        String.valueOf(daysRequired - daysPassed));
                tooltip.addImageWithText(5f);
            } else {
                tooltip.addPara("No " + typeReverse + " is currently being reverse engineered.", 5f);
            }
        }
    }

    protected void addCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode, String coreLevel,
            float dayReductionPercentage, int researchAdvance) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();
        String pre = coreLevel + "-level AI core currently assigned. ";
        if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            pre = coreLevel + "-level AI core. ";
        }

        if (mode != AICoreDescriptionMode.INDUSTRY_TOOLTIP && mode != AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            tooltip.addPara(
                    pre + "Reduces upkeep cost by %s. " +
                            "Reduces research time by %s%%. " +
                            "Advances research progress by %s points.",
                    opad, highlight,
                    new String[] { (int) ((1.0F - UPKEEP_MULT) * 100.0F) + "%",
                            "" + (int) (dayReductionPercentage * 100),
                            "" + researchAdvance });
        } else {
            CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(this.aiCoreId);
            TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48.0F);
            text.addPara(
                    pre + "Reduces upkeep cost by %s. " +
                            "Reduces research time by %s%%. " +
                            "Advances research progress by %s points.",
                    0.0F, highlight,
                    new String[] { (int) ((1.0F - UPKEEP_MULT) * 100.0F) + "%",
                            "" + (int) (dayReductionPercentage * 100),
                            "" + researchAdvance });
            tooltip.addImageWithText(opad);
        }
    }

    @Override
    protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Alpha", AI_ALPHA_DAYREQUIRED_REDUCTION, AI_ALPHA_RESEARCH_ADVANCE_ADD);
    }

    @Override
    protected void addBetaCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Beta", AI_BETA_DAYREQUIRED_REDUCTION, AI_BETA_RESEARCH_ADVANCE_ADD);
    }

    @Override
    protected void addGammaCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Gamma", AI_GAMMA_DAYREQUIRED_REDUCTION, 0);
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
            info.addPara("Reduces research time by %s%%. " +
                    "Advances research progress by %s points.",
                    0f, highlight, "" + (int) (0.10f * 100), "" + IMPROVE_RESEARCH_ADVANCE_ADD);
        } else {
            info.addPara("Reduces research time by %s%%. " +
                    "Advances research progress by %s points.",
                    0f, highlight, "" + (int) (0.10f * 100), "" + IMPROVE_RESEARCH_ADVANCE_ADD);
        }

        info.addSpacer(opad);
        super.addImproveDesc(info, mode);
    }
}
