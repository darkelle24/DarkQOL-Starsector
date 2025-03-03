package darkqol.industries.reverseEngineering;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import darkqol.ids.Ids;
import darkqol.utils.DailyCycleTracker;
import darkqol.utils.MultiTierIndustry;

public class ReverseEngineeringIndustry extends MultiTierIndustry {

    private DailyCycleTracker dailyCycleTracker;

    public ReverseEngineeringIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkReverseEngHubStorageColour", false);
        this.dailyCycleTracker = new DailyCycleTracker();
        initializeIndustries();
    }

    public boolean conditionToAdvance(float amount) {
        return dailyCycleTracker.newDay();
    }

    private void initializeIndustries() {
        // Ajout des industries avec des IDs uniques
        addIndustry(Ids.REVERSE_ENG_1_IND, 1, new ReverseEngineeringWeaponIndustry());
        addIndustry(Ids.REVERSE_ENG_2_IND, 2, new ReverseEngineeringFighterWingIndustry());
        addIndustry(Ids.REVERSE_ENG_3_IND, 3, new ReverseEngineeringShipIndustry());
    }

    @Override
    public String getDescriptionOverride() {
        String toReturn = "A high-tech facility dedicated to analyzing and reproducing advanced technology through reverse engineering. By deconstructing recovered items, it allows for the creation of blueprints to expand your faction's capabilities.\n\nPlace items into the storage to reverse engineer them.\n\nOnce you have achieved 100% progress, you will receive a blueprint.";

        toReturn += "\n\n";
        if (isTier(1)) {
            toReturn += "Tier 1: Only weapons are allowed.";
        } else if (isTier(2)) {
            toReturn += "Tier 2: Only weapons and fighter wings are allowed.";
        } else if (isTier(3)) {
            toReturn += "Tier 3: Weapons, fighter wings, and ships are allowed.";
        }
        toReturn += "\n\n";
        toReturn += "Choose in priority the item you don t have discovered yet.";
        return toReturn;
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
        ReverseEngineeringShipIndustry shipIndustry = (ReverseEngineeringShipIndustry) getIndustryById(
                Ids.REVERSE_ENG_3_IND);
        addCoreDescription(tooltip, mode, "Alpha", shipIndustry.AI_ALPHA_DAYREQUIRED_REDUCTION,
                shipIndustry.AI_ALPHA_RESEARCH_ADVANCE_ADD);
    }

    @Override
    protected void addBetaCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode) {
        ReverseEngineeringShipIndustry shipIndustry = (ReverseEngineeringShipIndustry) getIndustryById(
                Ids.REVERSE_ENG_3_IND);
        addCoreDescription(tooltip, mode, "Beta", shipIndustry.AI_BETA_DAYREQUIRED_REDUCTION,
                shipIndustry.AI_BETA_RESEARCH_ADVANCE_ADD);
    }

    @Override
    protected void addGammaCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode) {
        ReverseEngineeringShipIndustry shipIndustry = (ReverseEngineeringShipIndustry) getIndustryById(
                Ids.REVERSE_ENG_3_IND);
        addCoreDescription(tooltip, mode, "Gamma", shipIndustry.AI_GAMMA_DAYREQUIRED_REDUCTION, 0);
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        float opad = 10f;
        Color highlight = Misc.getHighlightColor();

        ReverseEngineeringShipIndustry shipIndustry = (ReverseEngineeringShipIndustry) getIndustryById(
                Ids.REVERSE_ENG_3_IND);

        if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
            info.addPara("Reduces research time by %s%%. " +
                    "Advances research progress by %s points.",
                    0f, highlight, "" + (int) (0.10f * 100), "" + shipIndustry.IMPROVE_RESEARCH_ADVANCE_ADD);
        } else {
            info.addPara("Reduces research time by %s%%. " +
                    "Advances research progress by %s points.",
                    0f, highlight, "" + (int) (0.10f * 100), "" + shipIndustry.IMPROVE_RESEARCH_ADVANCE_ADD);
        }

        info.addSpacer(opad);
        super.addImproveDesc(info, mode);
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        if (isTier(3)) {
            ReverseEngineeringShipIndustry shipIndustry = (ReverseEngineeringShipIndustry) getIndustryById(
                    Ids.REVERSE_ENG_3_IND);
            if (shipIndustry != null) {
                shipIndustry.addRightAfterDescriptionSection(tooltip, mode);
            }
        }
        if (isTier(2) || isTier(3)) {
            ReverseEngineeringFighterWingIndustry fighterWingIndustry = (ReverseEngineeringFighterWingIndustry) getIndustryById(
                    Ids.REVERSE_ENG_2_IND);
            if (fighterWingIndustry != null) {
                fighterWingIndustry.addRightAfterDescriptionSection(tooltip, mode);
            }
        }
        ReverseEngineeringWeaponIndustry weaponIndustry = (ReverseEngineeringWeaponIndustry) getIndustryById(
                Ids.REVERSE_ENG_1_IND);
        if (weaponIndustry != null) {
            weaponIndustry.addRightAfterDescriptionSection(tooltip, mode);
        }
    }

    public void addCurrentProjectTooltip(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        ReverseEngineeringShipIndustry shipIndustry = (ReverseEngineeringShipIndustry) getIndustryById(
                Ids.REVERSE_ENG_3_IND);
        if (shipIndustry != null) {
            shipIndustry.addCurrentProjectTooltip(tooltip, mode);
        }
    }

    @Override
    public boolean canImprove() {
        return true;
    }

    @Override
    public boolean canBeDisrupted() {
        return false;
    }

    @Override
    public boolean isAvailableToBuild() {
        return market.isPlayerOwned();
    }
}

class ReverseEngineeringShipIndustry extends AbstractReverseEngineeringIndustry<ShipVariantAPI> {
    public ReverseEngineeringShipIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkReverseEngHubStorageColour", "ship", Ids.REVERSE_ENG_MEMORY);
    }

    @Override
    protected String getSprite() {
        if (currentReverseEng == null) {
            return "";
        }
        return currentReverseEng.getHullSpec().getSpriteName();
    }

    protected boolean initDeconstruction() {
        SubmarketAPI sub = market.getSubmarket(Ids.REVERSE_ENG_SUB);

        if (sub == null) {
            debugLog("Error: SubmarketAPI is null.");
            return false;
        }

        CargoAPI storage = sub.getCargo();
        if (storage == null || storage.getMothballedShips() == null) {
            debugLog("Error: CargoAPI or Mothballed ships list is null.");
            return false;
        }

        List<FleetMemberAPI> ships = storage.getMothballedShips().getMembersListCopy();
        if (ships.isEmpty()) {
            debugLog("No mothballed ships available for deconstruction.");
            return false;
        }

        Set<String> unlockedShips = Global.getSector().getPlayerFaction().getKnownShips();
        List<FleetMemberAPI> availableShips = new ArrayList<>();
        List<FleetMemberAPI> allInCargoShips = new ArrayList<>();

        for (FleetMemberAPI ship : ships) {
            String shipHullId = ship.getHullId();
            if (!unlockedShips.contains(shipHullId)) {
                availableShips.add(ship);
            }
            allInCargoShips.add(ship);
        }

        if (availableShips.isEmpty()) {
            availableShips.addAll(allInCargoShips);
        }

        for (FleetMemberAPI ship : availableShips) {
            if (ship != null) {
                currentReverseEng = ship.getVariant();
                transferWeaponsAndWingsToStorage(ship);
                storage.getMothballedShips().removeFleetMember(ship);
                return true;
            }
        }

        return false;
    }

    @Override
    protected float getReductionFactor() {
        if (currentReverseEng == null) {
            return 1.0f;
        }

        String hullSize = currentReverseEng.getHullSize().toString();

        switch (hullSize) {
            case "FRIGATE":
                return 1.0f;
            case "DESTROYER":
                return 1.25f;
            case "CRUISER":
                return 1.5f;
            case "CAPITAL_SHIP":
                return 2f;
            default:
                return 1.0f;
        }
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

    protected String getNameReverse() {
        if (currentReverseEng == null) {
            return "No ship";
        }
        return currentReverseEng.getHullSpec().getNameWithDesignationWithDashClass();
    }

    protected String getIdReverse() {
        if (currentReverseEng == null) {
            return "No ship";
        }
        return currentReverseEng.getHullSpec().getHullId();
    }

    protected SpecialItemData getSpecialItem(String id) {
        return new SpecialItemData("ship_bp", id);
    }
}

class ReverseEngineeringWeaponIndustry extends AbstractReverseEngineeringIndustry<WeaponSpecAPI> {

    public ReverseEngineeringWeaponIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkReverseEngHubStorageColour", "weapon", Ids.REVERSE_ENG_MEMORY);
    }

    @Override
    protected int getDayRequired() {
        return 10;
    }

    @Override
    protected String getSprite() {
        if (currentReverseEng == null) {
            return "";
        }
        return currentReverseEng.getTurretSpriteName();
    }

    @Override
    protected boolean initDeconstruction() {
        SubmarketAPI sub = market.getSubmarket(Ids.REVERSE_ENG_SUB);

        if (sub == null) {
            debugLog("Error: SubmarketAPI is null.");
            return false;
        }

        CargoAPI storage = sub.getCargo();
        if (storage == null || storage.getWeapons() == null) {
            debugLog("Error: CargoAPI or Weapons list is null.");
            return false;
        }

        List<CargoAPI.CargoItemQuantity<String>> weaponItems = storage.getWeapons();
        if (weaponItems.isEmpty()) {
            debugLog("No weapons available for deconstruction.");
            return false;
        }

        Set<String> unlockedWeapons = Global.getSector().getPlayerFaction().getKnownWeapons();
        List<String> availableWeapons = new ArrayList<>();
        List<String> allInCargoWeapons = new ArrayList<>();

        for (CargoAPI.CargoItemQuantity<String> weaponItem : weaponItems) {
            String weaponId = weaponItem.getItem();
            if (!unlockedWeapons.contains(weaponId)) {
                availableWeapons.add(weaponId);
            }
            allInCargoWeapons.add(weaponId);
        }

        if (availableWeapons.isEmpty()) {
            availableWeapons.addAll(allInCargoWeapons);
        }

        for (String weaponId : availableWeapons) {
            WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(weaponId);

            if (weaponSpec != null) {
                currentReverseEng = weaponSpec;
                storage.removeWeapons(weaponId, 1);
                return true;
            }
        }
        return false;
    }

    protected String getNameReverse() {
        if (currentReverseEng == null) {
            return "No weapon";
        }
        return currentReverseEng.getWeaponName();
    }

    protected String getIdReverse() {
        if (currentReverseEng == null) {
            return "No weapon";
        }
        return currentReverseEng.getWeaponId();
    }

    protected SpecialItemData getSpecialItem(String id) {
        return new SpecialItemData("weapon_bp", id);
    }
}

class ReverseEngineeringFighterWingIndustry extends AbstractReverseEngineeringIndustry<FighterWingSpecAPI> {

    public ReverseEngineeringFighterWingIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkReverseEngHubStorageColour", "fighter wing", Ids.REVERSE_ENG_MEMORY);
    }

    @Override
    protected int getDayRequired() {
        return 20;
    }

    @Override
    protected String getSprite() {
        if (currentReverseEng == null) {
            return "";
        }
        return currentReverseEng.getVariant().getHullSpec().getSpriteName();
    }

    @Override
    protected boolean initDeconstruction() {
        SubmarketAPI sub = market.getSubmarket(Ids.REVERSE_ENG_SUB);

        if (sub == null) {
            debugLog("Error: SubmarketAPI is null.");
            return false;
        }

        CargoAPI storage = sub.getCargo();
        if (storage == null || storage.getFighters() == null) {
            debugLog("Error: CargoAPI or FighterWing list is null.");
            return false;
        }

        List<CargoAPI.CargoItemQuantity<String>> fighterWingItems = storage.getFighters();
        if (fighterWingItems.isEmpty()) {
            debugLog("No FighterWings available for deconstruction.");
            return false;
        }

        Set<String> unlockedFighter = Global.getSector().getPlayerFaction().getKnownFighters();
        List<String> availableFighter = new ArrayList<>();
        List<String> allInCargoFighter = new ArrayList<>();

        for (CargoAPI.CargoItemQuantity<String> fighterWingItem : fighterWingItems) {
            String fighterWingId = fighterWingItem.getItem();
            if (!unlockedFighter.contains(fighterWingId)) {
                availableFighter.add(fighterWingId);
            }
            allInCargoFighter.add(fighterWingId);
        }

        if (availableFighter.isEmpty()) {
            availableFighter.addAll(allInCargoFighter);
        }

        for (String fighterWingId : availableFighter) {

            FighterWingSpecAPI fighterWingSpec = Global.getSettings().getFighterWingSpec(fighterWingId);

            if (fighterWingSpec != null) {
                currentReverseEng = fighterWingSpec;
                storage.removeFighters(fighterWingId, 1);
                return true;
            }
        }
        return false;
    }

    protected String getNameReverse() {
        if (currentReverseEng == null) {
            return "No FighterWing";
        }
        return currentReverseEng.getWingName();
    }

    protected String getIdReverse() {
        if (currentReverseEng == null) {
            return "No FighterWing";
        }
        return currentReverseEng.getId();
    }

    protected SpecialItemData getSpecialItem(String id) {
        return new SpecialItemData("fighter_bp", id);
    }
}