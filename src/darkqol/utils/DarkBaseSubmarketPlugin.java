package darkqol.utils;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Highlights;

public class DarkBaseSubmarketPlugin extends BaseSubmarketPlugin {

    public String getDesc() {
        return this.submarket.getSpec().getDesc();
    }

    @Override
    public void createTooltip(CoreUIAPI ui, TooltipMakerAPI tooltip, boolean expanded) {
        float opad = 10.0F;
        tooltip.addTitle(this.submarket.getNameOneLine());
        String desc = getDesc();
        desc = Global.getSector().getRules().performTokenReplacement((String) null, desc,
                this.market.getPrimaryEntity(), (Map) null);
        String appendix = this.submarket.getPlugin().getTooltipAppendix(ui);
        if (appendix != null) {
            desc = desc + "\n\n" + appendix;
        }

        if (desc != null && !desc.isEmpty()) {
            LabelAPI body = tooltip.addPara(desc, opad);
            if (this.getTooltipAppendixHighlights(ui) != null) {
                Highlights h = this.submarket.getPlugin().getTooltipAppendixHighlights(ui);
                if (h != null) {
                    body.setHighlightColors(h.getColors());
                    body.setHighlight(h.getText());
                }
            }
        }

        this.createTooltipAfterDescription(tooltip, expanded);
    }
}