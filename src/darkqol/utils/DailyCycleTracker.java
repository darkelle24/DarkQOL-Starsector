package darkqol.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;

public class DailyCycleTracker {

    private boolean firstTick = true;
    private int lastDayChecked = 0;

    public boolean newDay() {
        CampaignClockAPI clock = Global.getSector().getClock();
        if (firstTick) {
            lastDayChecked = clock.getDay();
            firstTick = false;
            return false;
        } else if (clock.getDay() != lastDayChecked) {
            lastDayChecked = clock.getDay();
            return true;
        }
        return false;
    }

}
