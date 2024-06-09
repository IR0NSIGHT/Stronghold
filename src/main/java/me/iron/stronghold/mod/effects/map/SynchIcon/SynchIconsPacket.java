package me.iron.stronghold.mod.effects.map.SynchIcon;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

public class SynchIconsPacket extends Packet {
    SynchMapIcon[] icons = new SynchMapIcon[0];
    String[][] deleteCategories = new String[0][];
    public SynchIconsPacket() {
    }

    public SynchIconsPacket(SynchMapIcon[] icons, String[][] deleteCategories) {
        this.icons = icons;
        this.deleteCategories = deleteCategories;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        this.icons = packetReadBuffer.readObject(SynchMapIcon[].class);
        this.deleteCategories = packetReadBuffer.readObject(String[][].class);
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeObject(icons);
        packetWriteBuffer.writeObject(deleteCategories);
    }

    @Override
    public void processPacketOnClient() {
        SynchIconManager.instance.AddIconsLocal(this.icons, this.deleteCategories);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        SynchIconManager.instance.remoteUpdateFor(playerState);
    }
}
