package darkqol.submarkets;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import darkqol.ids.Ids;
import darkqol.utils.DarkBaseSubmarketPlugin;

public class ReverseEngineeringSubmarketPlugin extends DarkBaseSubmarketPlugin {
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }

    @Override
    public String getName() {
        String toReturn = "Reverse\n" +
                "Engineering\n" +
                "Storage ";
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
            toReturn += "I";
        } else if (market.hasIndustry(Ids.REVERSE_ENG_2_IND)) {
            toReturn += "II";
        } else if (market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
            toReturn += "III";
        }
        return toReturn;
    }

    @Override
    public String getDesc() {
        String toReturn = "Deposit ships / weapons / wing for analysis here, to gain progress on the reverse engineering of the item.";
        toReturn += "\n\n";
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
            toReturn += "Tier 1: Only weapons are allowed.";
        } else if (market.hasIndustry(Ids.REVERSE_ENG_2_IND)) {
            toReturn += "Tier 2: Only weapons and fighter wings are allowed.";
        } else if (market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
            toReturn += "Tier 3: Weapons, fighter wings, and ships are allowed.";
        }
        return toReturn;
    }

    @Override
    public boolean showInCargoScreen() {
        return true;
    }

    @Override
    public String getBuyVerb() {
        return "Take";
    }

    @Override
    public String getSellVerb() {
        return "Leave";
    }

    @Override
    public float getTariff() {
        return 0f;
    }

    @Override
    public boolean isFreeTransfer() {
        return true;
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }

    @Override
    public SubmarketPlugin.PlayerEconomyImpactMode getPlayerEconomyImpactMode() {
        return PlayerEconomyImpactMode.NONE;
    }

    @Override
    public boolean showInFleetScreen() {
        return market.isPlayerOwned();
    }

    // FIX: To fix this
    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        // ((ReverseEngineeringIndustry)
        // market.getIndustry(Ids.REVERSE_ENG_IND)).addCurrentProjectTooltip(tooltip,
        // Industry.IndustryTooltipMode.NORMAL);
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND) || market.hasIndustry(Ids.REVERSE_ENG_2_IND)
                || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {

            if (!market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
                return "Can not be Reverse Engineered - Ships are allowed in Tier 3.";
            } else {
                return "";
            }
        }

        return "something broke.";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND) || market.hasIndustry(Ids.REVERSE_ENG_2_IND)
                || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {

            if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
                if (!stack.isWeaponStack()) {
                    return "Can not be Reverse Engineered - Only weapons are allowed in Tier 1.";
                }
            } else if (market.hasIndustry(Ids.REVERSE_ENG_2_IND) || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
                if (!stack.isFighterWingStack() && !stack.isWeaponStack()) {
                    return "Can not be Reverse Engineered - Only weapons and fighter wings are allowed in Tier 2.";
                }
            }
        }

        return "something broke.";
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND) || market.hasIndustry(Ids.REVERSE_ENG_2_IND)
                || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {

            if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
                if (!stack.isWeaponStack()) {
                    return true;
                }
            } else if (market.hasIndustry(Ids.REVERSE_ENG_2_IND) || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
                if (!stack.isWeaponStack() && !stack.isFighterWingStack()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
        if (market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, SubmarketPlugin.TransferAction action) {
        return true;
    }
}
