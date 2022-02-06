package me.iron.stronghold.mod.framework;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;
import java.util.Iterator;

public class UpdatePacket extends Packet {

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
        container.onSerialize(packetWriteBuffer);
    }

    @Override
    public void processPacketOnClient() {
        AreaManager client = testMain.client;
        //instantiate tree structure of empty object
        if (container.getTree() != null)
            client.instantiateArea(container.getTree(),null);

        //update objects with values
        Iterator<SendableUpdateable> it = container.getSynchObjectIterator();
        while (it.hasNext()) {
            SendableUpdateable o2 = it.next();
            client.updateObject(o2);
        }

        Iterator<Long> delete = container.getDeleteUIDIterator();
        while (delete.hasNext()) {
            client.removeObject(delete.next());
        }
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }

    public void sendToAll() {
        for( PlayerState p: GameServerState.instance.getPlayerStatesByName().values()) {
            PacketUtil.sendPacket(p,this);
        }
    }
}