package me.iron.stronghold.mod.framework;

import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import com.sun.istack.internal.Nullable;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.playerUI.ScanHandler;
import org.schema.common.util.linAlg.Vector;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 18:52
 */
public class DebugUI implements CommandInterface {
    private String cmd = "debug";
    private String dsc = "debug";
    @Override
    public String getCommand() {
        return cmd;
    }

    @Override
    public String[] getAliases() {
        return new String[]{cmd};
    }

    @Override
    public String getDescription() {
        return dsc;
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] strings) {
        if (strings.length>0&&strings[0].toLowerCase().equals("takesys")) {
            try {
                Vector3i sys = playerState.getCurrentSystem();
                int faction = playerState.getFactionId();
                GameServerState.instance.getUniverse().getStellarSystemFromStellarPosIfLoaded(sys).setOwnerFaction(-1);
                ModMain.log(String.format("took control of system %s for faction %s => %s",sys, faction, GameServerState.instance.getUniverse().getStellarSystemFromStellarPosIfLoaded(sys).getOwnerFaction()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        if (strings.length>0&&strings[0].toLowerCase().equals("log")) {
            ModMain.instance.logAll = !ModMain.instance.logAll;
            PlayerUtils.sendMessage(playerState,"set logging for stronghold to: "+ModMain.instance.logAll);
        }
        if (strings.length>0&&strings[0].toLowerCase().equals("print")) {
            String out = StrongholdController.getInstance().toStringPretty();
            PlayerUtils.sendMessage(playerState,out);
            return true;
        }
        if (strings.length>0&&strings[0].toLowerCase().equals("save")) {
            PlayerUtils.sendMessage(playerState,"saving controller "+ StrongholdController.getInstance().toStringPretty());
            StrongholdController.getInstance().save();

        }

        if (strings.length>0&&strings[0].toLowerCase().equals("load")) {
            PlayerUtils.sendMessage(playerState,"loading from file");
            StrongholdController.getInstance().load();
            PlayerUtils.sendMessage(playerState,"loaded controller.");
            ModMain.log(StrongholdController.getInstance().toStringPretty());
        }

        if (strings.length>0&&strings[0].toLowerCase().equals("clear")) {
            StrongholdController.getInstance().getStrongholdHashMap().clear();
        }

        if (strings.length>0&&strings[0].toLowerCase().equals("generate")) {
            for (int i = 0; i < 5; i++) {
                Vector3i sys = new Vector3i(-10,-10,-i);
                Stronghold h = new Stronghold(StrongholdController.getInstance(), new Vector3i(-10,-10,i),i);
                h.setDefensePoints(i*100);
                StrongholdController.getInstance().getStrongholdHashMap().put(
                        sys, h
                );

            }
        }

        if (strings.length>0&&strings[0].toLowerCase().equals("sethp")) {
            if (StrongholdController.getInstance()==null)
                return true;
            int points = 100;
            if (strings.length>1) {
                try {
                    points = Integer.parseInt(strings[1]);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
            Vector3i system = playerState.getCurrentSystem();
            StrongholdController.getInstance().getStronghold(system).setDefensePoints(points);
            ModMain.log(String.format("set system %s to %s hp (now %s)",system,points, StrongholdController.getInstance().getStronghold(system).getDefensePoints()));
            return true;
        }

        if (strings.length>0&&strings[0].toLowerCase().equals("reset")) {
            StrongholdController.getInstance().reset();
        }
        return false;
    }

    @Override
    public void serverAction(@Nullable PlayerState playerState, String[] strings) {

    }

    @Override
    public StarMod getMod() {
        return ModMain.instance;
    }
}
