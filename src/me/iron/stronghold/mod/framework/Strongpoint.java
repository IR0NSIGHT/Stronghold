package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SpaceStation;
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
    transient private Vector3i sector;
    transient private int owner;
    transient private Stronghold stronghold;

    protected Strongpoint(Vector3i sector, Stronghold stronghold) {
        this.sector = sector;
        this.stronghold = stronghold;
    }

    protected Strongpoint() {
        sector = new Vector3i();
    } //used for serialize stuff and dummies

    protected boolean update() {
        boolean changed = false;
        if (sector==null)
            return false;

        //test if the strongpoint sector is loaded, if so check if the required station is still in there.
        if (GameServerState.instance.getUniverse().isSectorLoaded(sector)) {
            try {
                Sector s = GameServerState.instance.getUniverse().getSector(sector);
                int mass = 0, stationOwner = 0;
                for (SimpleTransformableSendableObject obj: s.getEntities()) {
                    if (!obj.getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION))
                        continue;

                    float objMass = ((SpaceStation)obj).getMassWithoutDockIncludingStation();
                    if (objMass>mass) { //heaviest station will dominate the sector and become the new owner.
                        mass =(int) objMass;
                        stationOwner = obj.getFactionId();
                    }
                }
                if (stationOwner != owner) {
                    changed = true;
                    setOwner(stationOwner);
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return changed;
    }

    /**
     * can safely be deleted
     * @return
     */
    protected boolean isRedundant() {
        return owner == 0;
    }

    protected void setSector(Vector3i sector) {
        this.sector = sector;
    }

    protected void setOwner(int owner) {
        if (owner!=this.owner) {
            stronghold.onStrongpointCaptured(this,owner);
            this.owner = owner;
        }
    }

    @Override
    public void onDeserialize(PacketReadBuffer buffer) {
        try {
            setOwner(buffer.readInt());
            setSector(buffer.readVector());
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

    public int getOwner() {
        return owner;
    }


}
