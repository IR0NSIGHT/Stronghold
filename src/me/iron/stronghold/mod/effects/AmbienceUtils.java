package me.iron.stronghold.mod.effects;

import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionNewsPost;
import org.schema.game.server.data.GameServerState;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 27.01.2022
 * TIME: 16:04
 */
public class AmbienceUtils {
    /**
     * add this faction message to the factions messageboard. fully serverside.
     * @param faction
     * @param topic
     * @param mssg
     */
    public static void addToFactionBoard(int faction, String topic, String mssg) {
        assert GameServerState.instance!=null;
        if (faction <= 0)
            return; //not doing NPCs or neutral.
        FactionNewsPost fp = new FactionNewsPost();

        fp.set(faction, topic, System.currentTimeMillis(), topic, mssg, 0);

        GameServerState.instance.getFactionManager().addNewsPostServer(fp);
    }

    public static void clientShowMessage(String mssg) {
        GameClientState.instance.getController().popupGameTextMessage(mssg,0);
    }

    public static String tryGetFactionName(int faction) {
        FactionManager f = null;

        if (GameClientState.instance!=null) {
            f = GameClientState.instance.getFactionManager();
        } else if (GameServerState.instance!=null) {
            f = GameServerState.instance.getFactionManager();
        } else {
            return "Faction "+faction;
        }
        Faction owners = f.getFaction(faction);
        String name;
        if (owners!=null) {
            name ="'"+ owners.getName()+"'";
        } else
            name = "neutral";
        return name;
    }

    public static CPNameGen getControlPointNameGen() {
        return new CPNameGen();
    }
}

