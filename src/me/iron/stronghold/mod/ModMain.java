package me.iron.stronghold.mod;

import api.DebugFile;
import api.ModPlayground;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import glossar.GlossarCategory;
import glossar.GlossarEntry;
import glossar.GlossarInit;
import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.framework.DebugUI;
import me.iron.stronghold.mod.framework.StrongholdPacket;
import me.iron.stronghold.mod.framework.StrongholdController;
import me.iron.stronghold.mod.playerUI.ScanHandler;
import me.iron.stronghold.mod.voidshield.VoidShield;
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

    }

    @Override
    public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
        PacketUtil.registerPacket(StrongholdPacket.class);
        if (StrongholdController.getInstance()==null) //on SP, this is taken up by the server one
            new StrongholdController().init(); //clientside controller, 98% passive, just receives synchs from server
        initGlossar();
        new SoundManager();
        VoidShield.initClientEHs();
        new ScanHandler(); //user interaction interface basically. scan and get infos about system.
    }

    @Override
    public void onServerCreated(ServerInitializeEvent serverInitializeEvent) {
        super.onServerCreated(serverInitializeEvent);

        new StrongholdController().init();
        VoidShield.initEHs();
        new ScanHandler(); //user interaction interface basically. scan and get infos about system.
    }
    public static void log(String msg) {
        System.out.println(msg);
        DebugFile.log(msg);
        if (GameServerState.instance!=null)
            ModPlayground.broadcastMessage(msg);
    }
    private void initGlossar() {
        GlossarInit.initGlossar(this);
        GlossarCategory cat = new GlossarCategory("Stronghold");
        cat.addEntry(new GlossarEntry("Introduction","The 'Stronghold' mod adds a capture-and-hold-bases system, which allows to make all stations inside a starsystem invulnerable to attacks. Use your ships scanner to receive details about the stronghold system you are inside of."));
        cat.addEntry(new GlossarEntry("Voidshield","The voidshield is a passive, starsystem wide defense mechanism. Any stations, that belong to the faction that owns the starsystem, will have unbreakable shields, as long as the voidshield is active. This makes all shielded stations invulnerable to any attacks. Stations in a strongpoint sector are not affected by the voidshield and can still be damaged.\n\n" +
                "The voidshield is active, as long as the starsystems shieldpoints are above "+ VoidShield.getRequiredPointsForShield()+". The shieldpoints can not go below "+ StrongholdController.hpRange[0]+" and not above "+ StrongholdController.hpRange[1]+". Capturing and holding strongpoints directly influences the amount of shieldpoints in a starsystem."));
        cat.addEntry(new GlossarEntry("Strongpoints","Every starsystem has 3 to 7 strongpoints, randomly distributed around the system center, at least 2 sectors away from the border. To capture a strongpoint, place a station of your faction in the strongpoint sector. The station can not be a homebase. If you own more strongpoints than all other factions combined in the system (neutral points are not counted), the shieldpoints increase. if you own less strongpoints than all other factions combined in the system, the shieldpoints decrease. Shieldpoints increase/decrease faster if you own/do not own more strongpoints. If all strongpoints are neutral, the shieldpoints slowly decrease."));
        GlossarInit.addCategory(cat);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (GameServerState.instance!=null)
            StrongholdController.getInstance().onShutdown();

    }
}
