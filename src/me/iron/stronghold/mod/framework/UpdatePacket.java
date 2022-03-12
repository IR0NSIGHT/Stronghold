package me.iron.stronghold.mod.framework;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import me.iron.stronghold.mod.ModMain;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;
import java.util.Iterator;

public class UpdatePacket extends Packet {
    public UpdatePacket() {

    }

    AbstractAreaContainer container;
    public void addContainer(AbstractAreaContainer container) {
        this.container = container;
    }
    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        if (container == null)
            container = new AbstractAreaContainer();
        container.onDeserialize(packetReadBuffer);
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        //AreaManager.dlog("writing packet data");
        container.onSerialize(packetWriteBuffer); //FIXME server dies here.
        //AreaManager.dlog("done writing packet data");
        container = null;
    }

    @Override
    public void processPacketOnClient() {
        ModMain.areaManager.loadFromContainer(container);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }

    public void sendToAll() {
        //AreaManager.dlog("send update packet to all");
        for( PlayerState p: GameServerState.instance.getPlayerStatesByName().values()) {
            PacketUtil.sendPacket(p,this);
        }
        //AreaManager.dlog("done sent update packet to all");

    }
}
