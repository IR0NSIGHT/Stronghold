package me.iron.stronghold.mod.AlienStation;

import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;

public class AlienArea extends StellarControllableArea {
    public static boolean debugVisibleArea = true;
    private final static String noStation = ":(";
    private String centerStationUID = noStation;
    private int detectionRadius;
    public static AlienArea aroundSpaceStation(SpaceStation station, int radius) {
        AlienArea area = new AlienArea();
        Vector3i center = station.getSector(new Vector3i());
        area.detectionRadius = radius;
        area.setDimensions(
                new Vector3i(center.x - radius, center.y - radius, center.z - radius),
                new Vector3i(center.x + radius, center.y + radius, center.z + radius));
        area.centerStationUID = station.getUniqueIdentifier();
        return area;
    }

    @Override
    public void onAreaEntered(StellarControllableArea area, Vector3i enteredSector, Ship object) {
        super.onAreaEntered(area, enteredSector, object);
        object.sendControllingPlayersServerMessage(Lng.astr("you have entered an alien area"), ServerMessage.MESSAGE_TYPE_WARNING);
    }

    @Override
    public void onAreaInnerMovement(StellarControllableArea area, Vector3i leftSector, Vector3i enteredSector, Ship object) {
        super.onAreaInnerMovement(area, leftSector, enteredSector, object);
        object.sendControllingPlayersServerMessage(Lng.astr("the alien can sense you moving"), ServerMessage.MESSAGE_TYPE_INFO);
    }

    @Override
    public void onAreaLeft(StellarControllableArea area, Vector3i leftSector, Ship object) {
        super.onAreaLeft(area, leftSector, object);
        object.sendControllingPlayersServerMessage(Lng.astr("you have left an alien area"), ServerMessage.MESSAGE_TYPE_INFO);
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }

    @Override
    public boolean isVisibleOnMap() {
        return debugVisibleArea;    //DEBUG: FOR NOW
    }
}
