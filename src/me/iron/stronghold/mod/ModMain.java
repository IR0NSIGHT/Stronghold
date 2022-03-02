package me.iron.stronghold.mod;

import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import me.iron.stronghold.mod.effects.map.RadarMapDrawer;
import me.iron.stronghold.mod.framework.AreaManager;
import me.iron.stronghold.mod.framework.GenericNewsCollector;
import me.iron.stronghold.mod.framework.UpdatePacket;
import me.iron.stronghold.mod.utility.DebugUI;
import org.schema.schine.resource.ResourceLoader;

public class ModMain extends StarMod {
    public static AreaManager areaManager;
    public static ModMain instance;
    @Override
    public void onEnable() {
        super.onEnable();
        PacketUtil.registerPacket(UpdatePacket.class);
        areaManager = new AreaManager();
        areaManager.addListener(new GenericNewsCollector("[STRONGHOLD]"));
        instance = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        areaManager.onShutdown();
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onServerCreated(ServerInitializeEvent serverInitializeEvent) {
        super.onServerCreated(serverInitializeEvent);
        areaManager.setServer(this);
        StarLoader.registerCommand(new DebugUI());
    }
    public static RadarMapDrawer radarMapDrawer;
    @Override
    public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
        super.onClientCreated(clientInitializeEvent);
        areaManager.setClient();
    }

    @Override
    public void onResourceLoad(ResourceLoader resourceLoader) {
        super.onResourceLoad(resourceLoader);
        radarMapDrawer = new RadarMapDrawer(this);

        radarMapDrawer.loadSprites(this);
    }
}
