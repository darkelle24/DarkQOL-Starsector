package darkqol.industries;

import java.util.List;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

public class RiftmakerIndustry extends BaseIndustry {
    private SectorEntityToken gate;

    @Override
    public void apply() {
        super.apply(true);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        if (gate == null) {
            gate = spawnGate();
        }
    }

    @Override
    protected void buildingFinished() {
        super.buildingFinished();

        if (gate == null) {
            gate = spawnGate();
        }
    }

    @Override
    public void notifyBeingRemoved(MarketAPI.MarketInteractionMode mode, boolean forUpgrade) {
        super.notifyBeingRemoved(mode, forUpgrade);
        if (gate != null) {
            gate.getStarSystem().removeEntity(gate);
            gate = null;
        }
    }

    @Override
    public String getDescriptionOverride() {
        String toReturn = "A groundbreaking megastructure that generates a stable spatial gateway around the planet, enabling instant travel to distant star systems.";
        toReturn += "\n\n"
                + "The RiftMaker creates a gate that gravitates around the planet, allowing fleets and trade convoys to bypass hyperspace entirely.";
        return toReturn;
    }

    @Override
    public boolean canBeDisrupted() {
        return false;
    }

    private SectorEntityToken spawnGate() {
        SectorEntityToken planet = market.getPrimaryEntity();
        if (planet == null || planet.getStarSystem() == null)
            return null;

        String gateName = "Gate near " + planet.getName();
        SectorEntityToken newGate = planet.getStarSystem().addCustomEntity(
                null, gateName, Entities.INACTIVE_GATE, "neutral");

        float optimalDistance = planet.getRadius() + 250f;

        SectorEntityToken station = planet.getTags().contains(Tags.STATION) ? null : getOrbitalStationAtMarket();
        float angle = (station != null) ? station.getCircularOrbitAngle() + 90 : 90f;

        newGate.setCircularOrbitPointingDown(planet, angle, optimalDistance, (optimalDistance - 100f) / 10f);

        return newGate;
    }

    private SectorEntityToken getOrbitalStationAtMarket() {
        List<SectorEntityToken> orbitingEntities = market.getStarSystem().getEntitiesWithTag(Tags.STATION);
        for (SectorEntityToken entity : orbitingEntities) {
            if (entity.getOrbitFocus() == market.getPrimaryEntity()) {
                return entity;
            }
        }
        return null;
    }
}