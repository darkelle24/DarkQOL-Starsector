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
import darkqol.utils.AbstractSubmarketIndustry;
import darkqol.utils.DailyCycleTracker;

public class ReverseEngineeringIndustry extends AbstractSubmarketIndustry {

    protected ReverseEngineeringShipIndustry reverseEngineeringShipIndustry = new ReverseEngineeringShipIndustry();
    protected ReverseEngineeringWeaponIndustry reverseEngineeringWeaponIndustry = new ReverseEngineeringWeaponIndustry();
    protected ReverseEngineeringFighterWingIndustry ReverseEngineeringFighterWingIndustry = new ReverseEngineeringFighterWingIndustry();

    private DailyCycleTracker dailyCycleTracker;

    public ReverseEngineeringIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkEngHubStorageColour");
        this.dailyCycleTracker = new DailyCycleTracker();
    }

    @Override
    public void advance(float amount) {
        if (isFunctional() && dailyCycleTracker.newDay()) {
            if (isPhase3()) {
                if (reverseEngineeringShipIndustry.getMarket() == null) {
                    reverseEngineeringShipIndustry.init(id, market);
                }
                reverseEngineeringShipIndustry.advance(amount);
            }
            if (isPhase2() || isPhase3()) {
                if (ReverseEngineeringFighterWingIndustry.getMarket() == null) {
                    ReverseEngineeringFighterWingIndustry.init(id, market);
                }
                ReverseEngineeringFighterWingIndustry.advance(amount);
            }
            if (reverseEngineeringWeaponIndustry.getMarket() == null) {
                reverseEngineeringWeaponIndustry.init(id, market);
            }
            reverseEngineeringWeaponIndustry.advance(amount);
        }
    }

    public void addCurrentProjectTooltip(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        reverseEngineeringShipIndustry.addCurrentProjectTooltip(tooltip, mode);
    }

    public boolean isPhase1() {
        if (Ids.REVERSE_ENG_1_IND.equals(getId())) {
            return true;
        }
        return false;
    }

    public boolean isPhase2() {
        if (Ids.REVERSE_ENG_2_IND.equals(getId())) {
            return true;
        }
        return false;
    }

    public boolean isPhase3() {
        if (Ids.REVERSE_ENG_3_IND.equals(getId())) {
            return true;
        }
        return false;
    }

    @Override
    public String getDescriptionOverride() {
        String toReturn = "As the sector is decaying in knowledge an institution specialized in the disassembly of functional spacecraft is a rare sight, and can only be afforded by the rich or the desperate.\n\nPlace items into the storage to reverse engineer them.\n\nOnce you have achieved 100% progress, you will receive a blueprint.";

        toReturn += "\n\n";
        if (isPhase1()) {
            toReturn += "Tier 1: Only weapons are allowed.";
        } else if (isPhase2()) {
            toReturn += "Tier 2: Only weapons and fighter wings are allowed.";
        } else if (isPhase3()) {
            toReturn += "Tier 3: Weapons, fighter wings, and ships are allowed.";
        }
        return toReturn;
    }

    @Override
    public boolean canImprove() {
        return true;
    }

    public void setAICoreId(String aiCoreId) {
        super.setAICoreId(aiCoreId);
        reverseEngineeringShipIndustry.setAICoreId(aiCoreId);
        reverseEngineeringWeaponIndustry.setAICoreId(aiCoreId);
        ReverseEngineeringFighterWingIndustry.setAICoreId(aiCoreId);
    }

    @Override
    public void setImproved(boolean improved) {
        super.setImproved(improved);
        reverseEngineeringShipIndustry.setImproved(improved);
        reverseEngineeringWeaponIndustry.setImproved(improved);
        ReverseEngineeringFighterWingIndustry.setImproved(improved);
    }

    public void setDisrupted(float days, boolean useMax) {
        super.setDisrupted(days, useMax);
        reverseEngineeringShipIndustry.setDisrupted(days, useMax);
        reverseEngineeringWeaponIndustry.setDisrupted(days, useMax);
        ReverseEngineeringFighterWingIndustry.setDisrupted(days, useMax);
    }
}

class ReverseEngineeringShipIndustry extends AbstractReverseEngineeringIndustry<ShipVariantAPI> {
    public ReverseEngineeringShipIndustry() {
        super(Ids.REVERSE_ENG_SUB, "darkEngHubStorageColour", "ship", Ids.REVERSE_ENG_MEMORY);
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
        super(Ids.REVERSE_ENG_SUB, "darkEngHubStorageColour", "fighter_wing", Ids.REVERSE_ENG_MEMORY);
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