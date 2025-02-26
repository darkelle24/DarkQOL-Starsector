package darkqol;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;

import darkqol.intels.factionSystem.FactionSystemIntel;
import darkqol.intels.factionSystem.FactionSystemManager;

public class DarkQOLModPlugin extends BaseModPlugin {
    public static Logger log = Global.getLogger(DarkQOLModPlugin.class);
    protected static boolean isNewGame = false;

    @Override
    public void onApplicationLoad() throws Exception {
    }

    @Override
    public void configureXStream(XStream x) {
        x.alias("FactionSystemIntel", FactionSystemIntel.class);
    }

    @Override
    public void onNewGame() {
        isNewGame = true;
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore) {
        super.onEnabled(wasEnabledBefore);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        isNewGame = newGame;

        FactionSystemManager manager = FactionSystemManager.createManager();
        manager.refreshProfiles();
    }
}
