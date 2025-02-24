package darkqol.industries.reverseEngineering;

import java.util.HashMap;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import darkqol.ids.Ids;
import darkqol.utils.DailyCycleTracker;
import darkqol.utils.MultiTierIndustry;

public class ReverseEngineeringIndustry extends MultiTierIndustry {

    private DailyCycleTracker dailyCycleTracker;

    public ReverseEngineeringIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkEngHubStorageColour", false);
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
        String toReturn = "As the sector is decaying in knowledge, an institution specialized in the disassembly of functional spacecraft is a rare sight, and can only be afforded by the rich or the desperate.\n\nPlace items into the storage to reverse engineer them.\n\nOnce you have achieved 100% progress, you will receive a blueprint.";

        toReturn += "\n\n";
        if (isTier(1)) {
            toReturn += "Tier 1: Only weapons are allowed.";
        } else if (isTier(2)) {
            toReturn += "Tier 2: Only weapons and fighter wings are allowed.";
        } else if (isTier(3)) {
            toReturn += "Tier 3: Weapons, fighter wings, and ships are allowed.";
        }
        return toReturn;
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
}

class ReverseEngineeringShipIndustry extends AbstractReverseEngineeringIndustry<ShipVariantAPI> {
    public ReverseEngineeringShipIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkEngHubStorageColour", "ship", Ids.REVERSE_ENG_MEMORY);
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

        for (FleetMemberAPI ship : ships) {
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
        super(Ids.REVERSE_ENG_SUB, "darkEngHubStorageColour", "weapon", Ids.REVERSE_ENG_MEMORY);
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

        for (CargoAPI.CargoItemQuantity<String> weaponItem : weaponItems) {
            String weaponId = weaponItem.getItem();
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
        super(Ids.REVERSE_ENG_SUB, "darkEngHubStorageColour", "fighter wing", Ids.REVERSE_ENG_MEMORY);
    }

    @Override
    protected int getDayRequired() {
        return 2;
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

        for (CargoAPI.CargoItemQuantity<String> fighterWingItem : fighterWingItems) {
            String fighterWingId = fighterWingItem.getItem();
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
        return new SpecialItemData("wing_bp", id);
    }
}