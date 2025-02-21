package darkqol.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

public class SpawnGate implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        StarSystemAPI system = Global.getSector().getPlayerFleet().getStarSystem();
        if (system == null) {
            Console.showMessage("You must be in a star system to use this command.");
            return CommandResult.ERROR;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            Console.showMessage("Unable to locate the player's fleet.");
            return CommandResult.ERROR;
        }

        PlanetAPI nearestPlanet = findNearestPlanetWithGravity(system, playerFleet.getLocation());
        SectorEntityToken orbitTarget = system.getStar();

        float distanceToStar = Misc.getDistance(system.getStar().getLocation(), playerFleet.getLocation());
        float distanceToPlanet = (nearestPlanet != null)
                ? Misc.getDistance(nearestPlanet.getLocation(), playerFleet.getLocation())
                : Float.MAX_VALUE;

        if (nearestPlanet != null && distanceToPlanet < distanceToStar && distanceToPlanet < 2000f) {
            orbitTarget = nearestPlanet;
            Console.showMessage("The gate will orbit the planet: " + nearestPlanet.getName());
        } else {
            Console.showMessage("The gate will orbit the star.");
        }

        float angle = Misc.getAngleInDegrees(orbitTarget.getLocation(), playerFleet.getLocation());
        float distance = Misc.getDistance(orbitTarget.getLocation(), playerFleet.getLocation());

        CustomCampaignEntityAPI gate = system.addCustomEntity(null, null, "inactive_gate", "neutral");
        gate.setCircularOrbitPointingDown(orbitTarget, angle, distance, 180f);

        Console.showMessage("Gate successfully created in the star system.");
        return CommandResult.SUCCESS;
    }

    private PlanetAPI findNearestPlanetWithGravity(StarSystemAPI system, Vector2f location) {
        PlanetAPI nearestPlanet = null;
        float minDistance = Float.MAX_VALUE;

        for (PlanetAPI planet : system.getPlanets()) {
            float distance = Misc.getDistance(planet.getLocation(), location);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPlanet = planet;
            }
        }

        return nearestPlanet;
    }
}