package playerweaponshop;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Set;

public class PlayerWeaponShopSubmarketPlugin extends BaseSubmarketPlugin {

    private boolean isActive = false;

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
        this.submarket.setFaction(Global.getSector().getFaction("playerWeaponShopColor"));
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        if (!market.hasIndustry("PlayerWeaponShopIndustry")) {
            isActive = false;
            getCargo().clear();
            return;
        }

        if (!isActive) {
            isActive = true;
            refreshMarketStock();
        }
    }

    private void refreshMarketStock() {
        getCargo().clear();
        addUnlockedWeapons();
        addUnlockedFighters();
        getCargo().sort();
    }

    private void addUnlockedWeapons() {
        Set<String> unlockedWeapons = Global.getSector().getPlayerFaction().getKnownWeapons();
        for (String weaponId : unlockedWeapons) {
            WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(weaponId);
            if (weaponSpec != null) {
                getCargo().addWeapons(weaponId, 50);
            }
        }
    }

    private void addUnlockedFighters() {
        Set<String> unlockedFighters = Global.getSector().getPlayerFaction().getKnownFighters();
        for (String fighterId : unlockedFighters) {
            FighterWingSpecAPI fighterSpec = Global.getSettings().getFighterWingSpec(fighterId);
            if (fighterSpec != null) {
                getCargo().addFighters(fighterId, 50);
            }
        }
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false; // Ce marché ne fait pas partie de l'économie globale
    }

    @Override
    public boolean isIllegalOnSubmarket(com.fs.starfarer.api.campaign.CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL; // Interdit la vente d'objets
    }

    @Override
    public String getIllegalTransferText(com.fs.starfarer.api.campaign.CargoStackAPI stack, TransferAction action) {
        return "You cannot sell items here.";
    }

    @Override
    public boolean isTooltipExpandable() { // TODO: 18/06/2020 tooltip expand stuff
        return super.isTooltipExpandable();
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);
        tooltip.addPara("The Player Weapon Shop Market allows the player to purchase all unlocked weapons and fighters. Selling items is not allowed.", 10f);
    }

    @Override
    public float getTariff() {
        return 1.0f;
    }
}
