package playerweaponshop;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public class PlayerWeaponShopIndustry extends BaseIndustry {

    @Override
    public void apply() {
        super.apply(true);

        if (market.isPlayerOwned() && !market.hasSubmarket("PlayerWeaponShopMarket")) {
            market.addSubmarket("PlayerWeaponShopMarket");
        }
    }

    @Override
    public void unapply() {
        super.unapply();

        if (market.hasSubmarket("PlayerWeaponShopMarket")) {
            market.removeSubmarket("PlayerWeaponShopMarket");
        }
    }

    @Override
    public boolean isAvailableToBuild() {
        return market.isPlayerOwned();  // Seulement le joueur peut le construire
    }
}
