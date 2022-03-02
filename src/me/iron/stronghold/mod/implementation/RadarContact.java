package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;

import java.io.Serializable;

public class RadarContact implements Serializable {
    Vector3i sector;
    int amount; //of contacts in sector
    long timestamp;
    int factionid;

    public RadarContact(int factionid, int amount, Vector3i sector,long timestamp) {
        this.sector = sector;
        this.timestamp = timestamp;
        this.factionid = factionid;
        this.amount = amount;
    }
    public Vector3i getSector() {
        return sector;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getFactionid() {
        return factionid;
    }

    public int getAmount() {
        return amount;
    }
}
