package darkqol.industries;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

import darkqol.ids.Ids;

public class PrivateArsenalIndustry extends BaseIndustry {

    @Override
    public void apply() {
        super.apply(true);

        if (market.isPlayerOwned() && !market.hasSubmarket(Ids.PRIVATE_ARSENAL_SUB)) {
            market.addSubmarket(Ids.PRIVATE_ARSENAL_SUB);
        }
    }

    @Override
    public void unapply() {
        super.unapply();

        if (market.hasSubmarket(Ids.PRIVATE_ARSENAL_SUB)) {
            market.removeSubmarket(Ids.PRIVATE_ARSENAL_SUB);
        }
    }

    @Override
    public boolean isAvailableToBuild() {
        return market.isPlayerOwned(); // Seulement le joueur peut le construire
    }
}
