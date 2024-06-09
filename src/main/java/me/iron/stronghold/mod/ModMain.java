package me.iron.stronghold.mod;

import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import glossar.GlossarCategory;
import glossar.GlossarEntry;
import glossar.GlossarInit;
import me.iron.stronghold.mod.effects.map.AreaMapDrawer;
import me.iron.stronghold.mod.effects.map.RadarMapDrawer;
import me.iron.stronghold.mod.framework.AreaManager;
import me.iron.stronghold.mod.framework.GenericNewsCollector;
import me.iron.stronghold.mod.framework.UpdatePacket;
import me.iron.stronghold.mod.utility.DebugUI;
import me.iron.stronghold.mod.utility.WarpgateCommand;
import org.schema.schine.resource.ResourceLoader;

public class ModMain extends StarMod {
    public static AreaManager areaManager;
    public static ModMain instance;
    public static RadarMapDrawer radarMapDrawer;
    private WarpgateCommand warpgateCommand;
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
        warpgateCommand.onShutdown();
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
        warpgateCommand = new WarpgateCommand();
        StarLoader.registerCommand(warpgateCommand);
    }

    @Override
    public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
        super.onClientCreated(clientInitializeEvent);
        areaManager.setClient();
        addGlossar();
    }

    @Override
    public void onResourceLoad(ResourceLoader resourceLoader) {
        super.onResourceLoad(resourceLoader);
        new AreaMapDrawer(this);

        radarMapDrawer = new RadarMapDrawer(this);

        radarMapDrawer.loadSprites(this);
    }

    public static void LogError(String error, Exception exception) {
        System.err.println("ERROR " + ModMain.instance.getName() + ":" + error + " exc: " + exception);
        exception.printStackTrace(System.err);
    }

    private void addGlossar() {
        GlossarInit.initGlossar(this);
        GlossarCategory cat = new GlossarCategory("Stronghold");
        cat.addEntry(new GlossarEntry("PVE Areas", "The PVE Area is an admin-created zone where player-to-player combat is forbidden.\n\nOnly fighting with NPCs and pirates is allowed, any pvp-shielddamage is automatically negated."));
        cat.addEntry(new GlossarEntry("Stronghold", "A stronghold is a box shaped zone that can be conquered by players. Owning a stronghold allows access to powerful effects inside the stronghold, such as the voidshield and Radar.\n\n" + "To conquer a stronghold, a faction has to conquer all Controlzones (CZ). The CZs are marked on the map and can be conquered by placing a factioned station inside of it. CZs that are owned by allies, count into the owners CZs too. If the owning faction looses all own and allied CZs, the stronghold is lost and becomes neutral.\n\n" + "Own and allied forces experience effects inside the stronghold:\n\nRADAR\n\nThe long range radar marks any objects as pulsing dots on the map. It breaks through any cloak and jam, but can not tell the type, or faction of an object, just its presence. Allied and own objects automatically share their allegiance, and are marked in blue and green. The size of a dot represents the amount of single contacts in that sector. The transparance indicates how long ago the update was. Once a minute, the radar sweeps the complete zone and updates the map. Contacts inside the asteroid belts are hidden because of interference with the numerous asteroids.\n\n" + "The voidshield is a defensive effect, that strengthens own and allied shields on spacestations. It will negate all damage to the stations shield, making the station invulnerable. The shield is always active. Controlzones are not shielded by default."));
        GlossarInit.addCategory(cat);
    }
}
