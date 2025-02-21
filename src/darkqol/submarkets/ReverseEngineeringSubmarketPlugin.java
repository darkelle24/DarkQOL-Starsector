package darkqol.submarkets;

import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import darkqol.ids.Ids;
import darkqol.industries.ReverseEngineeringIndustry;

public class ReverseEngineeringSubmarketPlugin extends BaseSubmarketPlugin {
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }

    @Override
    public boolean showInCargoScreen() {
        return false;
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

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        ((ReverseEngineeringIndustry) market.getIndustry(Ids.REVERSE_ENG_IND)).addCurrentProjectTooltip(tooltip,
                Industry.IndustryTooltipMode.NORMAL);
    }
}
