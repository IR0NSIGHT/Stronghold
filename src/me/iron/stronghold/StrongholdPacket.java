package me.iron.stronghold;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 21:41
 */
public class StrongholdPacket extends Packet {
    private LinkedList<StrongholdSystem> systems = new LinkedList<>();
    public StrongholdPacket(LinkedList<StrongholdSystem> systems) {
        this.systems.addAll(systems);
    }

    public StrongholdPacket() {

    }

    @Override
    public void readPacketData(PacketReadBuffer b) throws IOException {
        int size = b.readInt();
        for (int i = 0; i < size; i++) {
            systems.add(new StrongholdSystem(b));
        }
    }

    @Override
    public void writePacketData(PacketWriteBuffer b) throws IOException {
        b.writeInt(systems.size());
        for (StrongholdSystem s: systems) {
            s.onSerialize(b);
        }
    }

    @Override
    public void processPacketOnClient() {

        SystemController.getInstance().synchFromServer(systems);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }

    public void sendToAll() {
        for (PlayerState p: GameServerState.instance.getPlayerStatesByName().values()) {
            PacketUtil.sendPacket(p,this);
        }
    }
}
