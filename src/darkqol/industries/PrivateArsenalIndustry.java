package darkqol.industries;

import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

import darkqol.ids.Ids;
import darkqol.submarkets.PrivateArsenalSubmarketPlugin;
import darkqol.utils.AbstractSubmarketIndustry;

public class PrivateArsenalIndustry extends AbstractSubmarketIndustry {

    public PrivateArsenalIndustry() {
        super(Ids.PRIVATE_ARSENAL_SUB, "darkPlayerWeaponShopColor");
    }

    @Override
    public boolean isAvailableToBuild() {
        return market.isPlayerOwned(); // Seulement le joueur peut le construire
    }

    @Override
    public void apply() {
        super.apply();
        if (isFunctional() && market.isPlayerOwned()) {
            SubmarketAPI open = market.getSubmarket(submarketId);
            if (open != null) {
                PrivateArsenalSubmarketPlugin plugin = (PrivateArsenalSubmarketPlugin) open.getPlugin();
                plugin.refreshMarketStock();
            }
        }
    }
}
