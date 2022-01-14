package me.iron.stronghold.mod.framework;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 21:41
 */
class StrongholdPacket extends Packet {
    private LinkedList<Stronghold> systems = new LinkedList<>();
    public StrongholdPacket(LinkedList<Stronghold> systems) {
        this.systems.addAll(systems);
    }

    public StrongholdPacket() {

    }

    @Override
    public void readPacketData(PacketReadBuffer b) throws IOException {
        int size = b.readInt();
        for (int i = 0; i < size; i++) {
            UUID uuid = b.readObject(UUID.class);
            StrongholdController.getInstance().updateStronghold(uuid, b);
        }
    }

    @Override
    public void writePacketData(PacketWriteBuffer b) throws IOException {
        b.writeInt(systems.size());
        for (Stronghold s: systems) {
            s.onSerialize(b);
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
