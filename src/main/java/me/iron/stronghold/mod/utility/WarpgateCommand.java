package me.iron.stronghold.mod.utility;

import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import me.iron.stronghold.mod.ModMain;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.controller.database.tables.FTLTable;
import org.schema.game.common.controller.elements.StationaryManagerContainer;
import org.schema.game.common.controller.elements.warpgate.WarpgateCollectionManager;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.objects.Sendable;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static me.iron.stronghold.mod.utility.DebugUI.echo;
import static me.iron.stronghold.mod.utility.SimpleTools.moveObjectToInSectorPosition;

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
        return "Modify warpgates: " +
                "/warpgate target # sets warpgate free target destination to your navigation-selection sector and activates gate.\n" +
                "/warpgate shift <center,east,west,north,south> # will move station to this position in sector, 2km from center.\n" +
                "/warpgate name myName # renames station to myName.\n" +
                "/warpgate info # display information about gate.\n" +
                "/warpgate protect # sets users faction mode to PROTECTED => can not be hurt.";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    public boolean targetAndActivate(PlayerState playerState, String[] strings) {
        try {
            int entityId = playerState.getSelectedEntityId();
            long fromIndex = 68722294787L;   //read from live debugger, is that a constant somewhere?
            Vector3i target = playerState.getNetworkObject().waypoint.getVector();
            Sendable sendable = playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);

            if (sendable != null && sendable instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) sendable).getManagerContainer() instanceof StationaryManagerContainer<?>) {
                StationaryManagerContainer<?> m = (StationaryManagerContainer<?>) ((ManagedSegmentController<?>) sendable).getManagerContainer();
                //TODO what is fromIndex?
                List<WarpgateCollectionManager> managers = m.getWarpgate().getCollectionManagers();
                WarpgateCollectionManager c = managers.get(0);// .getCollectionManagersMap().get(fromIndex);

                if (c != null) {
                    String directUID = FTLTable.DIRECT_PREFIX + target.x + "_" + target.y + "_" + target.z + "_" + DatabaseEntry.removePrefixWOException(m.getSegmentController().getUniqueIdentifier());

                    Vector3f targetDirection = target.toVector3f();
                    targetDirection.sub(playerState.getCurrentSector().toVector3f());
                    float targetDistance = targetDirection.length();
                    float wantedDistance = Math.min(targetDistance, c.getMaxDistance() * 0.95f);
                    targetDirection.normalize();
                    targetDirection.scale(wantedDistance);
                    float actualDistance = targetDirection.length();
                    targetDirection.add(playerState.getCurrentSector().toVector3f());
                    target = new Vector3i(targetDirection);

                    c.setDestination(
                            directUID, target);
                    c.setActive(true);

                    echo("Successfully set warp gate target to %s!" + c.getLocalDestination() + ", " + actualDistance + " sectors away, max distance = " + c.getMaxDistance(), playerState);
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


    public boolean shiftStation(PlayerState playerState, String[] strings) {
        String dir = strings[1];
        Vector3f target = new Vector3f();
        switch (dir) {
            case "center":
                target.set(0, 0, 0);
                break;
            case "west":
                target.set(0, 0, -2000);
                break;
            case "east":
                target.set(0, 0, 2000);
                break;
            case "north":
                target.set(2000, 0, 0);
                break;
            case "south":
                target.set(-2000, 0, 0);
                break;
        }

        int entityId = playerState.getSelectedEntityId();
        Sendable sendable = playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);

        if (sendable instanceof ManagedSegmentController) {
            ManagedSegmentController msc = ((ManagedSegmentController) sendable);
            echo("shift station " + msc.getSegmentController().getRealName() + " to " + target.toString(), playerState);
            SegmentController sc = msc.getSegmentController();
            moveObjectToInSectorPosition(sc, target);
            return true;
        }
        echo("unable to shift object", playerState);
        return false;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] strings) {
        try {
            switch (strings[0]) {
                case "target":
                    return targetAndActivate(playerState, strings);
                case "name":
                    return nameStation(playerState, strings);
                case "shift":
                    return shiftStation(playerState, strings);
                case "info":
                    int entityId = playerState.getSelectedEntityId();
                    SegmentController sendable = (SegmentController) playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
                    StationaryManagerContainer<?> m = (StationaryManagerContainer<?>) ((ManagedSegmentController<?>) sendable).getManagerContainer();
                    List<WarpgateCollectionManager> managers = m.getWarpgate().getCollectionManagers();
                    WarpgateCollectionManager c = managers.get(0);
                    echo("Warpgate" + sendable.getRealName() + ":\n" + (c.isActive() ? "active" : "inactive") + "\n" +
                                    " target: " + c.getLocalDestination() + "\n" +
                                    " max distance: " + c.getMaxDistance() + "\n" +
                                    " valid:" + c.isValid() + "\n" +
                                    " powered:" + c.getPowered() + "\n" +
                                    c.toString()
                            , playerState);
                    return true;
                case "protect":
                    boolean active = (strings.length == 1 || !Objects.equals(strings[1], "off"));

                    Faction f = GameServerState.instance.getFactionManager().getFaction(playerState.getFactionId());
                    f.setFactionMode(Faction.MODE_SPECTATORS, active);
                    echo("faction " + f.getName() + " " + f.getIdFaction() + " is now faction mode " +  f.getFactionMode() + "(0 = default, 4 = spectator)", playerState);
                    return true;
                default:
                    echo("could not match subcommand " + strings[0], playerState);
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
