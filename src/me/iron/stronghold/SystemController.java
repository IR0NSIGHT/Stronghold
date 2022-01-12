package me.iron.stronghold;

import api.listener.Listener;
import api.listener.events.entity.SegmentControllerFullyLoadedEvent;
import api.listener.events.entity.SegmentControllerOverheatEvent;
import api.mod.ModSkeleton;
import api.mod.StarLoader;
import api.mod.config.PersistentObjectUtil;
import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.common.data.world.StellarSystem;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;
import java.util.*;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 17:52
 */
public class SystemController extends SimpleSerializerWrapper {
    private static SystemController instance;
    public static SystemController getInstance() {
        return instance;
    }
    public static int[] hpRange = new int[]{-500,0,1000}; //lower cap, point of failure, upper cap
    public static int changePerTimeUnit = 1;
    private HashMap<Vector3i,StrongholdSystem> system_hps = new HashMap<>();

    /**
     * loads a systemcontroller if it exists, otherwise instantiates one.
     * @param skeleton
     * @return
     */
    public static SystemController loadOrNew(ModSkeleton skeleton) {
       ArrayList<Object> objs = PersistentObjectUtil.getObjects(skeleton, SystemController.class);
       if (!objs.isEmpty()) {
           return (SystemController)objs.get(0);
       } else {
           SystemController c = new SystemController();
           PersistentObjectUtil.addObject(skeleton,c);
           return c;
       }
    }

    public SystemController() {
    }

    public void init() {
        instance = this;
        //load from save file
        if (GameServerState.instance!=null) {
            initServer();
        } else {
            initClient();
        }
    }

    protected void reset() {
        system_hps.clear();
    }

    private void initClient() {
        new StarRunnable(){
            @Override
            public void run() {
                try {
                    GameClientState.instance.getWorldDrawer().getGuiDrawer().getHud().getRadar().getLocation().setTextSimple(
                            new Object(){
                                @Override
                                public String toString() {
                                    Vector3i system = GameClientState.instance.getPlayer().getCurrentSystem();
                                    StrongholdSystem sys = system_hps.get(system);
                                    if (sys != null)
                                        return sys.toString();
                                    else
                                        return "null sys";
                                }
                            }
                    );
                    cancel();
                } catch (NullPointerException ignored) {

                }
            }
        }.runTimer(ModMain.instance,5);
        new StarRunnable(){
            @Override
            public void run() {
                try {
                    PacketUtil.sendPacketToServer(new StrongholdPacket()); //request update from server.
                    cancel();
                } catch (NullPointerException ignored){
                    //when the player joins, the clientprocessor doesnt directly have a connection yet, try till success.
                }
            }
        }.runTimer(ModMain.instance,5);
    }

    private void initServer() {
        new StarRunnable(){
            long last;
            @Override
            public void run() {
                try {
                    if (System.currentTimeMillis()>last+5000) {
                        update();
                       // ModMain.log("owo");
                        last = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTimer(ModMain.instance,100);

        StarLoader.registerListener(SegmentControllerFullyLoadedEvent.class, new Listener<SegmentControllerFullyLoadedEvent>() {
            @Override
            public void onEvent(SegmentControllerFullyLoadedEvent event) {
                if (!event.getController().getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION))
                    return; //dont even have to test for anything non-station.
                try {
                    Vector3i system = event.getController().getSystem(new Vector3i());
                    int owner = GameServerState.instance.getUniverse().getStellarSystemFromStellarPos(system).getOwnerFaction();
                    updateSystem(system,owner);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        },ModMain.instance);

        StarLoader.registerListener(SegmentControllerOverheatEvent.class, new Listener<SegmentControllerOverheatEvent>() {
            @Override
            public void onEvent(SegmentControllerOverheatEvent event) {
                if (!event.getEntity().getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION))
                    return; //dont even have to test for anything non-station.
                try {
                    Vector3i system = event.getEntity().getSystem(new Vector3i());
                    int owner = GameServerState.instance.getUniverse().getStellarSystemFromStellarPos(system).getOwnerFaction();
                    updateSystem(system,owner);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        },ModMain.instance);
    }

    /**
     * updates all logged systems that a player is currently in. does not create new stronghold systems by itself.
     */
    private void update() {
        LinkedList<StrongholdSystem> toRemove = new LinkedList<>();
        LinkedList<StrongholdSystem> synchList = new LinkedList<>();
        //update all systems that players are inside of rn
        for (PlayerState p: GameServerState.instance.getPlayerStatesByName().values()) {
            Vector3i system = p.getCurrentSystem();
            try {
                StellarSystem sys = GameServerState.instance.getUniverse().getStellarSystemFromStellarPos(system);
                StrongholdSystem ss = system_hps.get(system);
                if (ss != null) {
                    ss.update(System.currentTimeMillis()/1000);
                    if (ss.isFlagDelete()) {
                        //karteileiche
                        toRemove.add(ss);
                        ss.setFlagDelete(true);
                        continue;
                    }

                    if (ss.isSynchFlag()) { //synch to client required?
                        synchList.add(ss);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (StrongholdSystem s: toRemove) {
            system_hps.remove(s.getStellarPos());
        }

        synchList.addAll(toRemove);
        if (synchList.size()!= 0)
            new StrongholdPacket(synchList).sendToAll();
    }

    /**
     * update a system outside the update loop for event changes.
     * will instantiate a new system if required.
     * @param system
     * @param ownerFaction
     */
    public void updateSystem(Vector3i system, int ownerFaction) {
        StrongholdSystem sys = system_hps.get(system);
        if (sys==null) {
            sys = new StrongholdSystem(system, ownerFaction);
            sys.init();
        }
        sys.update(System.currentTimeMillis()/1000);
        if (!sys.isFlagDelete()) {
            system_hps.put(sys.getStellarPos(),sys);
        }
    }

    public void synchClientFull(final PlayerState p) {
        final LinkedList<StrongholdSystem> systems = new LinkedList<>();
        for (StrongholdSystem s: system_hps.values()) {
            systems.add(s);
        }
        PacketUtil.sendPacket(p, new StrongholdPacket(systems));
    }

    public boolean isObjectProtected(SimpleTransformableSendableObject obj, Vector3i system, int sysOwner) {
        boolean isStation = obj.getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION),
                factionMatchesSystem = obj.getFactionId()==sysOwner,
                systemIsOwned = sysOwner != 0,
                isVoidShielded = isSystemProtected(system),
                notHB = !obj.isHomeBase(),
                notStrongPointSector = !StrongholdSystem.isStrongpoint(obj.getSector(new Vector3i()));
        if (isStation && factionMatchesSystem && systemIsOwned && isVoidShielded && notHB && notStrongPointSector)
            return true;
        return false;
    }

    /**
     * is this system protected by a voidshield => all stations are invulernable?
     * @param system
     * @return
     */
    public boolean isSystemProtected(Vector3i system) {
        return getHPForSystem(system)>=hpRange[1];
    }

    protected StrongholdSystem getSystem(Vector3i system) {
        return system_hps.get(system);
    }

    /**
     * get health points of this system.
     * @param system system relative pos
     * @return
     */
    public int getHPForSystem(Vector3i system) {
        StrongholdSystem sys = system_hps.get(system);
        if (sys==null)
            return hpRange[0];
        else
            return sys.getHp();
    }

    @Override
    public void onDeserialize(PacketReadBuffer packetReadBuffer) {
        try {
            //read systems and their health
            int systems = packetReadBuffer.readInt();
            for (int i = 0; i < systems; i++) {
                StrongholdSystem s = new StrongholdSystem(packetReadBuffer);
                system_hps.put(s.getStellarPos(),s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialize(PacketWriteBuffer packetWriteBuffer) {


        try {
            //write systems and their health
            packetWriteBuffer.writeInt(system_hps.size());
            for (Map.Entry<Vector3i,StrongholdSystem> entry: system_hps.entrySet()) {
                long last = entry.getValue().lastUpdate;
                entry.getValue().lastUpdate = 0;
                entry.getValue().onSerialize(packetWriteBuffer);
                entry.getValue().lastUpdate = last; //rewrite in case the object is still used.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void synchFromServer(LinkedList<StrongholdSystem> systems) {
        if (GameClientState.instance==null)
            return;
        for (StrongholdSystem s: systems) {
            if (s.isFlagDelete()) {
                system_hps.remove(s.getStellarPos());
            }else {
                system_hps.put(s.getStellarPos(),s);
            }
        }
    }
}
