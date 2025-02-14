package playerweaponshop;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.List;
import java.util.Set;

public class PlayerWeaponShopSubmarketPlugin extends BaseSubmarketPlugin {

    private boolean isActive = false;

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
        this.submarket.setFaction(Global.getSector().getFaction("player"));
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
                getCargo().addWeapons(weaponId, 20);
            }
        }
    }

    private void addUnlockedFighters() {
        Set<String> unlockedFighters = Global.getSector().getPlayerFaction().getKnownFighters();
        for (String fighterId : unlockedFighters) {
            FighterWingSpecAPI fighterSpec = Global.getSettings().getFighterWingSpec(fighterId);
            if (fighterSpec != null) {
                getCargo().addFighters(fighterId, 20);
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
}
