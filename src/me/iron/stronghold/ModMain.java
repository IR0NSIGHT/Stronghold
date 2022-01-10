package me.iron.stronghold;

import api.DebugFile;
import api.ModPlayground;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import org.schema.game.server.data.GameServerState;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 17:49
 */
public class ModMain extends StarMod {
    public static ModMain instance;
    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();
        StarLoader.registerCommand(new DebugUI());
        PacketUtil.registerPacket(StrongholdPacket.class);
        ModMain.log("registered stronghold packet.");
        new DamageHandler();
    }

    @Override
    public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
        PacketUtil.registerPacket(StrongholdPacket.class);

        new SystemController().init(); //clientside controller, 98% passive, just receives synchs from server
    }

    @Override
    public void onServerCreated(ServerInitializeEvent serverInitializeEvent) {

        super.onServerCreated(serverInitializeEvent);
        SystemController.loadOrNew(this.getSkeleton()).init();
    }
    public static void log(String msg) {
        System.out.println(msg);
        DebugFile.log(msg);
        if (GameServerState.instance!=null)
            ModPlayground.broadcastMessage(msg);
    }
}
