package me.iron.stronghold.mod.utility;

import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import me.iron.stronghold.mod.ModMain;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.controller.database.tables.FTLTable;
import org.schema.game.common.controller.elements.StationaryManagerContainer;
import org.schema.game.common.controller.elements.warpgate.WarpgateCollectionManager;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.network.objects.Sendable;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

import static me.iron.stronghold.mod.utility.DebugUI.echo;

public class WarpgateCommand implements CommandInterface {
    @Override
    public String getCommand() {
        return "warpgate";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"warpgate"};
    }

    @Override
    public String getDescription() {
        return "Modify warpgates: /warpgate x y z";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    public boolean targetAndActivate(PlayerState playerState, String[] strings) {
        try {
            int entityId = playerState.getSelectedEntityId();
            long fromIndex = 68722294787l;   //read from live debugger, is that a constant somewhere?
            int toX = Integer.parseInt(strings[1]);
            int toY = Integer.parseInt(strings[2]);
            int toZ = Integer.parseInt(strings[3]);

            Sendable sendable = playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);

            if (sendable != null && sendable instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) sendable).getManagerContainer() instanceof StationaryManagerContainer<?>) {
                StationaryManagerContainer<?> m = (StationaryManagerContainer<?>) ((ManagedSegmentController<?>) sendable).getManagerContainer();
                //TODO what is fromIndex?
                WarpgateCollectionManager c = m.getWarpgate().getCollectionManagersMap().get(fromIndex);

                if (c != null) {
                    String directUID = FTLTable.DIRECT_PREFIX + toX + "_" + toY + "_" + toZ + "_" + DatabaseEntry.removePrefixWOException(m.getSegmentController().getUniqueIdentifier());
                    c.setDestination(
                            directUID, new Vector3i(toX, toY, toZ));
                    c.setActive(true);
                    echo("Successfully set warp gate target to %s!" + toX + ", " + toY + ", " + toZ, playerState);
                } else {
                    echo("Warp Gate not found!", playerState);
                }
            } else {
                echo("Entity not found!", playerState);
            }
            return true;
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            echo("invalid parameters", playerState);
        } catch (Exception ex) {
            echo("Unexpected exception", playerState);
            ModMain.LogError("Warpgate command:" + " params =" + Arrays.toString(strings), ex);
        }
        return false;
    }

    public boolean nameStation(PlayerState playerState, String[] strings) {
        String name = strings[1];
        int entityId = playerState.getSelectedEntityId();
        Sendable sendable = playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);

        if (sendable != null && sendable instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) sendable).getManagerContainer() instanceof StationaryManagerContainer<?>) {
            echo("Set name " + name + "for selected station ", playerState);
            ((ManagedSegmentController) sendable).getSegmentController().setRealName(name);
            return true;
        }

        return false;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] strings) {
        try {
            if (Objects.equals(strings[0], "target")) {
                return targetAndActivate(playerState, strings);
            } else if (Objects.equals(strings[0], "name")) {
                return nameStation(playerState, strings);
            } else {
                return false;
            }
        } catch (Exception ex) {
            echo("unable to execute command, likely syntax error", playerState);
            return false;
        }
    }

    @Override
    public void serverAction(@Nullable PlayerState playerState, String[] strings) {

    }

    @Override
    public StarMod getMod() {
        return null;
    }
}
