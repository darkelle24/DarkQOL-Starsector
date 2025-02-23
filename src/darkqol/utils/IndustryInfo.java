package darkqol.utils;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public class IndustryInfo {
    private int tier;
    private BaseIndustry industry;

    public IndustryInfo(int tier, BaseIndustry industry) {
        this.tier = tier;
        this.industry = industry;
    }

    public int getTier() {
        return tier;
    }

    public BaseIndustry getIndustry() {
        return industry;
    }
}