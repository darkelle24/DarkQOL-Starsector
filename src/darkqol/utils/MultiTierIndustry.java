package darkqol.utils;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public abstract class MultiTierIndustry extends AbstractSubmarketIndustry {

    private Map<String, IndustryInfo> industriesMap;
    private boolean activateOnlyCurrentTier;

    public MultiTierIndustry(String id, String storageColour, boolean activateOnlyCurrentTier) {
        super(id, storageColour);
        this.activateOnlyCurrentTier = activateOnlyCurrentTier;
        industriesMap = new HashMap<>();
    }

    protected void addIndustry(String id, int tier, BaseIndustry industry) {
        industriesMap.put(id, new IndustryInfo(tier, industry));
    }

    public boolean isTier(int tierNumber) {
        if (industriesMap.containsKey(getId())) {
            IndustryInfo info = industriesMap.get(getId());
            return info.getTier() == tierNumber;
        }
        return false;
    }

    public BaseIndustry getIndustryById(String id) {
        IndustryInfo info = industriesMap.get(id);
        return info != null ? info.getIndustry() : null;
    }

    public boolean conditionToAdvance(float amount) {
        return true;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        if (isFunctional() && conditionToAdvance(amount)) {
            int currentTier = getCurrentTier();

            for (IndustryInfo info : industriesMap.values()) {
                BaseIndustry industry = info.getIndustry();
                if (industry.getMarket() == null) {
                    industry.init(id, market);
                }

                if (activateOnlyCurrentTier && info.getTier() == currentTier) {
                    industry.advance(amount);
                } else if (!activateOnlyCurrentTier && info.getTier() <= currentTier) {
                    industry.advance(amount);
                }
            }
        }
    }

    private int getCurrentTier() {
        int maxTier = 0;
        for (IndustryInfo info : industriesMap.values()) {
            if (info.getTier() > maxTier) {
                maxTier = info.getTier();
            }
        }
        return maxTier;
    }

    public void setAICoreId(String aiCoreId) {
        super.setAICoreId(aiCoreId);
        for (IndustryInfo info : industriesMap.values()) {
            info.getIndustry().setAICoreId(aiCoreId);
        }
    }

    @Override
    public void setImproved(boolean improved) {
        super.setImproved(improved);
        for (IndustryInfo info : industriesMap.values()) {
            info.getIndustry().setImproved(improved);
        }
    }

    public void setDisrupted(float days, boolean useMax) {
        super.setDisrupted(days, useMax);
        for (IndustryInfo info : industriesMap.values()) {
            info.getIndustry().setDisrupted(days, useMax);
        }
    }

    public void setHidden(boolean hidden) {
        super.setHidden(hidden);
        for (IndustryInfo info : industriesMap.values()) {
            info.getIndustry().setHidden(hidden);
        }
    }

    public void setSpecialItem(SpecialItemData special) {
        super.setSpecialItem(special);
        for (IndustryInfo info : industriesMap.values()) {
            info.getIndustry().setSpecialItem(special);
        }
    }

    public void setBuildProgress(float buildProgress) {
        super.setBuildProgress(buildProgress);
        for (IndustryInfo info : industriesMap.values()) {
            info.getIndustry().setBuildProgress(buildProgress);
        }
    }
}