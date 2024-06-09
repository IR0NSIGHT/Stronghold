package me.iron.stronghold.mod.effects.map.SynchIcon;

import org.schema.common.util.linAlg.Vector3i;

import javax.vecmath.Vector4f;
import java.io.Serializable;

public class SynchMapIcon implements Serializable {
    int iconIndex;
    Vector3i sector;
    float size;
    Vector4f color;
    int animationType;
    //list of categories in hirarchy: map->infrastrucutre->warpgates f.e.
    //used by player to hide/show categories on the map.
    String[] category;
    //stop displaying after this time
    long expirationUnixTime;
    // client should request an update when this icon expires
    boolean synchOnExpiration;

    public SynchMapIcon(int iconIndex, Vector3i sector, float size, Vector4f color, int animationType, String[] category, long expirationUnixTime, boolean synchOnExpiration) {
        this.iconIndex = iconIndex;
        this.sector = sector;
        this.size = size;
        this.color = color;
        this.animationType = animationType;
        this.category = category;
        this.expirationUnixTime = expirationUnixTime;
        this.synchOnExpiration = synchOnExpiration;
    }
}
