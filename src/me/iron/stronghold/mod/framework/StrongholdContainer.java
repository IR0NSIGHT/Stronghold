package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 22.01.2022
 * TIME: 14:29
 */
class StrongholdContainer extends SimpleSerializerWrapper {
    public boolean log;
    private static String code = "holds_container";
    private transient Collection<Stronghold> hs= new LinkedList<>();
    StrongholdContainer() {
    }

    protected void setStrongholds(Collection<Stronghold> hs) {
        rawData = new byte[1];
        this.hs.addAll(hs);
    }

    public Collection<Stronghold> getStrongHolds() {
        return hs;
    }

    @Override
    public void onDeserialize(PacketReadBuffer packetReadBuffer) {
        try {
            String s = packetReadBuffer.readString();
            if (s.equals(code)) {
               int size = packetReadBuffer.readInt();
               for (int i = 0; i < size; i++) {
                   hs.add(new Stronghold(packetReadBuffer));
                   if (!packetReadBuffer.readString().equals("stop"))
                       throw new IOException("malformed input.");
               }
            } else {
                System.out.println("stronghold container has malformed data. can not load.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSerialize(PacketWriteBuffer packetWriteBuffer) {
        try {
            packetWriteBuffer.writeString(code);
            packetWriteBuffer.writeInt(hs.size());
            for (Stronghold h: hs) {
                h.onSerialize(packetWriteBuffer);
                packetWriteBuffer.writeString("stop");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
