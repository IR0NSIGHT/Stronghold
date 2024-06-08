package me.iron.stronghold.mod.framework;

import api.listener.Listener;
import api.listener.events.player.PlayerChangeSectorEvent;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;

import java.util.LinkedList;

public class ChunkPlayerSectorChangeListener extends Listener<PlayerChangeSectorEvent> {
    private ChunkManager manager;

    public ChunkPlayerSectorChangeListener(ChunkManager manager) {
        this.manager = manager;
    }

    @Override
    public void onEvent(PlayerChangeSectorEvent event) {
        if (GameServerState.instance == null || GameServerState.instance.getUniverse() == null)
            return;
        //generate info, get chunks
        Vector3i start = new Vector3i(), end = new Vector3i();
        Sector s1 = GameServerState.instance.getUniverse().getSector(event.getOldSectorId()), s2 = GameServerState.instance.getUniverse().getSector(event.getNewSectorId());
        if (s1 == null || s2 == null)
            return;
        start.set(s1.pos);
        end.set(s2.pos);
        AreaChunk startC = manager.getChunkFromSector(start), endC = manager.getChunkFromSector(end);

        //get intruding ship
        SimpleTransformableSendableObject s = event.getPlayerState().getFirstControlledTransformableWOExc();
        if (!(s instanceof Ship))
            return;
        Ship ship = (Ship) s;

        //notify areas in relevant chunks //TODO jumping to warp causes wrong chunk maybe bc %arraylength??
        LinkedList<StellarControllableArea> areas = new LinkedList<>();
        if (startC != null) {
            for (SendableUpdateable c : startC.children)
                areas.add((StellarControllableArea) c);
        }
        if (endC != null && (startC == null || !startC.equals(endC))) {
            for (SendableUpdateable c : endC.children)
                areas.add((StellarControllableArea) c);
        }
        for (StellarControllableArea c : areas)
            c.onShipChangeSector(start, end, ship);
    }
}

