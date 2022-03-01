package me.iron.stronghold.mod.implementation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;

import java.io.Serializable;

public class RadarContact implements Serializable {
    String uid;
    String name;
    Vector3i sector;
    SimpleTransformableSendableObject.EntityType type;
    long timestamp;
    int factionid;

    public RadarContact(String uid, String name, SimpleTransformableSendableObject.EntityType type, int factionid, Vector3i sector, long timestamp) {
        this.uid = uid;
        this.name = name;
        this.sector = sector;
        this.type = type;
        this.timestamp = timestamp;
        this.factionid = factionid;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public Vector3i getSector() {
        return sector;
    }

    public SimpleTransformableSendableObject.EntityType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getFactionid() {
        return factionid;
    }

    @Override
    public String toString() {
        return "RadarContact{" +
                "uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                ", sector=" + sector +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", factionid=" + factionid +
                '}';
    }
}
