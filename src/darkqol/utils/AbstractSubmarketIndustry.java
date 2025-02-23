package darkqol.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public abstract class AbstractSubmarketIndustry extends BaseIndustry {
    protected transient SubmarketAPI saved = null;
    protected String submarketId;
    protected String factionSubMarketID;

    public AbstractSubmarketIndustry(String submarketId, String factionSubMarketID) {
        this.submarketId = submarketId;
        this.factionSubMarketID = factionSubMarketID;
    }

    public void apply() {
        super.apply(true);

        if (isFunctional() && market.isPlayerOwned()) {
            SubmarketAPI open = market.getSubmarket(submarketId);
            if (open == null) {
                if (saved != null) {
                    market.addSubmarket(saved);
                } else {
                    market.addSubmarket(submarketId);
                    SubmarketAPI sub = market.getSubmarket(submarketId);
                    sub.setFaction(Global.getSector().getFaction(factionSubMarketID));
                    Global.getSector().getEconomy().forceStockpileUpdate(market);
                }
            }
        } else if (market.isPlayerOwned()) {
            market.removeSubmarket(submarketId);
        }

        if (!isFunctional()) {
            unapply();
        }
    }

    public void unapply() {
        super.unapply();

        if (market.isPlayerOwned()) {
            SubmarketAPI open = market.getSubmarket(submarketId);
            saved = open;
            market.removeSubmarket(submarketId);
        }
    }
}
