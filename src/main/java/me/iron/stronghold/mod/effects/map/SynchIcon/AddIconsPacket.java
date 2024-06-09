package me.iron.stronghold.mod.effects.map.SynchIcon;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

public class AddIconsPacket extends Packet {
    SynchMapIcon[] icons = new SynchMapIcon[0];

    public AddIconsPacket() {
    }

    public AddIconsPacket(SynchMapIcon[] icons) {
        this.icons = icons;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        this.icons = packetReadBuffer.readObject(SynchMapIcon[].class);
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeObject(icons);
    }

    @Override
    public void processPacketOnClient() {
        SynchIconManager.instance.AddIconsLocal(this.icons);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        SynchIconManager.instance.UpdateClient(playerState);
    }
}
