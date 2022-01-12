package me.iron.stronghold;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import me.iron.stronghold.events.StrongpointOwnerChangedEvent;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 12.01.2022
 * TIME: 18:12
 */
public class Strongpoint extends SimpleSerializerWrapper {
    private Vector3i sector;
    private int owner;

    public Strongpoint(Vector3i sector) {
        this.sector = sector;
    }

    public void update() {
        if (GameServerState.instance.getUniverse().isSectorLoaded(sector)) {
            try {
                Sector s = GameServerState.instance.getUniverse().getSector(sector);
                int mass = 0, stationOwner = 0;
                for (SimpleTransformableSendableObject obj: s.getEntities()) {
                    if (!obj.getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION))
                        continue;
                    if (obj.getMass()>mass) { //heaviest station will dominate the sector and become the new owner.
                        mass = (int) obj.getMass();
                        stationOwner = obj.getFactionId();
                    }
                }
                setOwner(stationOwner);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }




    @Override
    public void onDeserialize(PacketReadBuffer buffer) {
        try {
            owner = buffer.readInt();
            sector = buffer.readVector();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialize(PacketWriteBuffer buffer) {
        try {
            buffer.writeInt(owner);
            buffer.writeVector(sector);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Vector3i getSector() {
        return sector;
    }

    public void setSector(Vector3i sector) {
        this.sector = sector;
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        if (owner!=this.owner) {
            new StrongpointOwnerChangedEvent(this, owner);
        }
        this.owner = owner;
    }
}
