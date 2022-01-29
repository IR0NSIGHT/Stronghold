package me.iron.stronghold.mod.voidshield;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.Strongpoint;
import org.schema.common.util.linAlg.Vector3i;

import java.util.HashMap;
import java.util.Vector;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 29.01.2022
 * TIME: 19:53
 */
public class VoidShieldContainer extends SimpleSerializerWrapper {
    private HashMap<Strongpoint, Long> cpConquered_atTime = new HashMap<>();

    protected void setCPConquered(Strongpoint p, long millis) {
        ModMain.log("controlpoint " + p.getSector() + " was conquered and is locked now.");
        cpConquered_atTime.put(p,millis);
    }
    protected long getCPConqueredAt(Strongpoint p) {
        Long out = cpConquered_atTime.get(p);
        if (out == null)
            return -1;
        return out;
    }

    //serializing shit
    @Override
    public void onDeserialize(PacketReadBuffer packetReadBuffer) {

    }

    @Override
    public void onSerialize(PacketWriteBuffer packetWriteBuffer) {

    }
}
