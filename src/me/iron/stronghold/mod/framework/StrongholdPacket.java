package me.iron.stronghold.mod.framework;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import me.iron.stronghold.mod.ModMain;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 21:41
 */
public class StrongholdPacket extends Packet {
    private LinkedList<Stronghold> systems = new LinkedList<>();
    public StrongholdPacket(Collection<Stronghold> systems) {
        this.systems.addAll(systems);
    }

    public StrongholdPacket() {

    }

    @Override
    public void readPacketData(PacketReadBuffer b) throws IOException {
        int size = b.readInt();
        for (int i = 0; i < size; i++) {
            Vector3i sys = b.readVector();
            ModMain.log("updating Stronghold " + sys.toString() + " from buffer.");
            StrongholdController.getInstance().updateStrongholdFromBuffer(sys, b);
            String s = b.readString();
            if (!s.equals("stop"))
                throw new IOException("malformed input.");
        }
    }

    @Override
    public void writePacketData(PacketWriteBuffer b) throws IOException {
        System.out.println("writing systems to packet.");
        b.writeInt(systems.size());
        for (Stronghold s: systems) {
            b.writeVector(s.getStellarPos());
            s.onSerialize(b);
            b.writeString("stop");
        }
    }

    @Override
    public void processPacketOnClient() {
    //    StrongholdController.getInstance().synchFromServer(systems);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        //this client requested to receive a synch for all stations.
        StrongholdController.getInstance().synchClientFull(playerState);
    }

    public void sendToAll() {
        for (PlayerState p: GameServerState.instance.getPlayerStatesByName().values()) {
            PacketUtil.sendPacket(p,this);
        }
    }
}
