package me.iron.stronghold.mod.AlienStation;

import api.ModPlayground;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;

public class AlienArea extends StellarControllableArea {
    public static boolean debugVisibleArea = true;
    private final static String noStation = ":(";
    private String centerStationUID = noStation;
    private int detectionRadius;
    public static float lootFrequencyMinutes = 0.1f;
    private long nextLootFillUnix = 0;
    private long lastLootFillUnix = System.currentTimeMillis();
    private int amountLootPerUpdate = 1;
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
    public void onUpdate(AbstractControllableArea area) {
        super.onUpdate(area);
        if (this != area)
            return;

        ModPlayground.broadcastMessage("hobbitses lurking in my swamp!");
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis > nextLootFillUnix) {
            SpaceStation station = getStation();
            if (station == null)
                return;

            setLoot(getLootForTimeframe(currentTimeMillis-lastLootFillUnix), station);
            nextLootFillUnix = getNextLootFillUnix();
            lastLootFillUnix = currentTimeMillis;
        }

    }


    private static long getNextLootFillUnix() {
        return System.currentTimeMillis() + (long)(Math.random() * 1000 * 60 * lootFrequencyMinutes) ;
    }

    private SpaceStation getStation() {
        Object o = GameServerState.instance.getSegmentControllersByName().get(centerStationUID);
        if (!(o instanceof SpaceStation))
            return null;
        SpaceStation station = (SpaceStation) o;
        return station;
    }

    private void setLoot(int amount, SpaceStation station) {
        //check if station is loaded


        ObjectCollection<Inventory> cargoCollectionManagers = station.getManagerContainer().getInventories().values();
        for (Inventory cm : cargoCollectionManagers) {
            ElementInformation info = ElementKeyMap.getInfoArray()[ElementKeyMap.CORE_ID];
            if (info == null)
                continue;
            cm.incExistingOrNextFreeSlotWithoutException(info.id, amount);
            cm.sendAll();
        }
    }

    protected int getLootForTimeframe(long millisSinceRefill) {
        //TODO write a unit test for this, im to tired to be smart
        return (int)Math.ceil(amountLootPerUpdate * millisSinceRefill  / (float)(lootFrequencyMinutes* 60 * 1000));
    }

    @Override
    public void synch(SendableUpdateable a) {
        super.synch(a);
        assert a instanceof AlienArea;
        this.centerStationUID = ((AlienArea) a).centerStationUID;
        this.detectionRadius = ((AlienArea) a).detectionRadius;
        this.nextLootFillUnix = ((AlienArea) a).nextLootFillUnix;
        this.lastLootFillUnix = ((AlienArea) a).lastLootFillUnix;
        this.amountLootPerUpdate =  ((AlienArea) a).amountLootPerUpdate;
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
