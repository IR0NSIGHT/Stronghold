package me.iron.stronghold.mod.utility;

import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.map.SynchIcon.SynchIconManager;
import me.iron.stronghold.mod.effects.map.SynchIcon.SynchMapIcon;
import me.iron.stronghold.mod.warpgate.WarpgateContainer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.controller.database.tables.FTLTable;
import org.schema.game.common.controller.elements.StationaryManagerContainer;
import org.schema.game.common.controller.elements.warpgate.WarpgateCollectionManager;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.controller.SectorSwitch;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.objects.Sendable;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static me.iron.stronghold.mod.utility.DebugUI.echo;
import static me.iron.stronghold.mod.utility.SimpleTools.moveObjectToInSectorPosition;

/**
 * a command line tool to help with setting up invulnerable warpgate-highways as public infrastructure
 */
public class WarpgateCommand implements CommandInterface {
    private WarpgateContainer container;

    public WarpgateCommand() {
        this.container = WarpgateContainer.load();
        updateGateIconsAndBroadcast(false);
    }

    public void onShutdown() {
        this.container.save();
    }

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
                "/warpgate protect [off] # sets faction of selected object to mode spectator => can not be hurt or hurt others.\n" +
                "/warpgate <name> <east|west> # applies target, shift and name to selected station.\n" +
                "/warpgate follow # jump to warpgates destination.\n" +
                "/warpgate list <add|remove|show> # add/remove selected object to internal list of stations or show list.\n";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    Sendable getSelectedObject(PlayerState playerState) {
        int entityId = playerState.getSelectedEntityId();
        return playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
    }

    public WarpgateCollectionManager getSelectedGate(PlayerState playerState) throws IllegalArgumentException {
        Sendable sendable = getSelectedObject(playerState);

        if (sendable != null && sendable instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) sendable).getManagerContainer() instanceof StationaryManagerContainer<?>) {
            StationaryManagerContainer<?> m = (StationaryManagerContainer<?>) ((ManagedSegmentController<?>) sendable).getManagerContainer();
            List<WarpgateCollectionManager> managers = m.getWarpgate().getCollectionManagers();
            if (managers.size() != 1)
                throw new IllegalArgumentException("selected entity has incorrect amount of warpgates");
            WarpgateCollectionManager c = managers.get(0);
            return c;
        }
        throw new IllegalArgumentException("selected entity is incorrect type");
    }

    public boolean targetAndActivate(PlayerState playerState) {
        try {
            WarpgateCollectionManager c = getSelectedGate(playerState);
            Vector3i target = playerState.getNetworkObject().waypoint.getVector();

            if (c != null) {
                String directUID = FTLTable.DIRECT_PREFIX + target.x + "_" + target.y + "_" + target.z + "_" + DatabaseEntry.removePrefixWOException(c.getContainer().getSegmentController().getUniqueIdentifier());

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
            return true;
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            echo("invalid parameters", playerState);
        } catch (Exception ex) {
            echo("Unexpected exception", playerState);
            ModMain.LogError("Warpgate command:", ex);
        }
        return false;
    }

    public boolean nameStation(PlayerState playerState, String name) {
        int entityId = playerState.getSelectedEntityId();
        Sendable sendable = playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);

        if (sendable != null && sendable instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) sendable).getManagerContainer() instanceof StationaryManagerContainer<?>) {
            echo("Set name " + name + "for selected station ", playerState);
            ((ManagedSegmentController) sendable).getSegmentController().setRealName(name);
            return true;
        }

        return false;
    }

    public boolean shiftStation(PlayerState playerState, String dir) {
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

    public boolean listStation(PlayerState playerState, String action) {
        switch (action) {
            case "add": {
                SegmentController sendable = (SegmentController) getSelectedObject(playerState);
                container.add(new WarpgateContainer.SaveableGate((SpaceStation) sendable, getSelectedGate(playerState).getLocalDestination()));
                echo("add this station to tracker list:" + sendable.getRealName(), playerState);

                break;
            }
            case "remove": {
                SegmentController sendable = (SegmentController) getSelectedObject(playerState);
                container.remove(sendable.getUniqueIdentifier());
                echo("remove this station to tracker list", playerState);
                break;
            }
            case "show": {
                StringBuilder b = new StringBuilder("all tracked gate stations:\n");
                for (WarpgateContainer.SaveableGate gate : container.gates) {
                    b.append(gate.toString()).append("\n");
                }
                echo(b.toString(), playerState);
                break;
            }
            default:
                return false;

        }


        updateGateIconsAndBroadcast(true);
        return true;
    }

    private LinkedList<SynchMapIcon> getIconsFromGateList(WarpgateContainer gates) {
        LinkedList<SynchMapIcon> gateIcons = new LinkedList<>();
        for (WarpgateContainer.SaveableGate gate : container.gates) {
            gateIcons.add(new SynchMapIcon(
                    SynchIconManager.DEFAULT_ICON,
                    gate.position,
                    .02f,
                    new Vector4f(0, 1, 0, .5f),
                    SynchIconManager.ANIMATION_NONE,
                    new String[]{"root", "public", "warpgates"},
                    -1,
                    false,
                    gate.realName));
        }
        return gateIcons;
    }

    private void updateGateIconsAndBroadcast(boolean broadcast) {
        SynchIconManager.instance.setPublicIcons(getIconsFromGateList(container));
        if (broadcast)
            SynchIconManager.instance.remoteUpdateEveryone();
    }

    public boolean printInfo(PlayerState playerState) {
        int entityId = playerState.getSelectedEntityId();
        SegmentController sendable = (SegmentController) playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
        StationaryManagerContainer<?> m = (StationaryManagerContainer<?>) ((ManagedSegmentController<?>) sendable).getManagerContainer();
        List<WarpgateCollectionManager> managers = m.getWarpgate().getCollectionManagers();
        WarpgateCollectionManager c = managers.get(0);
        echo("Warpgate" + sendable.getRealName() + ":\n" + (c.isActive() ? "active" : "inactive") + "\n" + " target: " + c.getLocalDestination() + "\n" + " max distance: " + c.getMaxDistance() + "\n" + " valid:" + c.isValid() + "\n" + " powered:" + c.getPowered() + "\n" + c.toString(), playerState);
        return true;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] strings) {
        try {
            switch (strings[0]) {
                case "target":
                    return targetAndActivate(playerState);
                case "name":
                    return nameStation(playerState, strings[1]);
                case "shift":
                    return shiftStation(playerState, strings[1]);
                case "info": {
                    return printInfo(playerState);
                }
                case "protect": {
                    boolean active = (strings.length == 1 || !Objects.equals(strings[1], "off"));
                    int entityId = playerState.getSelectedEntityId();
                    SegmentController sendable = (SegmentController) playerState.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
                    Faction f = GameServerState.instance.getFactionManager().getFaction(sendable.getFactionId());
                    f.setFactionMode(Faction.MODE_SPECTATORS, active);
                    //ATTENTION this can go into desync with clients. use very sparingly.
                    echo("faction " + f.getName() + " " + f.getIdFaction() + " is now faction mode " + f.getFactionMode() + "(0 = default, 4 = spectator)", playerState);
                    return true;
                }
                case "follow": {
                    WarpgateCollectionManager c = getSelectedGate(playerState);

                    SectorSwitch s = GameServerState.instance.getController().queueSectorSwitch(
                            playerState.getFirstControlledTransformableWOExc(),
                            c.getLocalDestination(),
                            SectorSwitch.TRANS_JUMP,
                            false,
                            true,
                            true);
                    echo("jumpigg to gate destination:" + c.getLocalDestination(), playerState);
                    return true;
                }
                case "list": {
                    return listStation(playerState, strings[1]);
                }
                default: {         //    /warpgate <name> <east|west>
                    if (strings.length != 2)
                        throw new IllegalArgumentException("default command requires 2 argumetns, was given " + strings.length);
                    targetAndActivate(playerState);
                    nameStation(playerState, strings[0]);
                    shiftStation(playerState, Objects.equals(strings[1], "away") ? "west" : "east");
                    listStation(playerState, "add");
                    printInfo(playerState);
                    return true;
                }
            }
        } catch (Exception ex) {
            echo("unable to execute command, error message: \n" + ex.getMessage(), playerState);
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
