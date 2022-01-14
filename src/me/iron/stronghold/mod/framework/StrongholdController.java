package me.iron.stronghold.mod.framework;

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
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.sounds.SoundManager;
import me.iron.stronghold.mod.events.IStrongholdEvent;
import me.iron.stronghold.mod.events.IStrongpointEvent;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.common.data.world.StellarSystem;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;

import java.io.IOException;
import java.util.*;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 17:52
 * singelton, manager class that holds, manages, updates, sends, deletes, saves all strongholds.
 */
public class StrongholdController extends SimpleSerializerWrapper {
    private LinkedList<IStrongholdEvent> holdEs = new LinkedList<>();
    private LinkedList<IStrongpointEvent> pointEs = new LinkedList<>();

    private static StrongholdController instance;
    public static StrongholdController getInstance() {
        return instance;
    }
    public static String getStrongholdTerm = "Stronghold";
    public static int[] hpRange = new int[]{-3600,300000}; //lower cap, point of failure, upper cap
    public static int changePerTimeUnit = 1;

    //private stuff
    private HashMap<Vector3i, Stronghold> strongholdHashMap = new HashMap<>();

    /**
     * loads a systemcontroller if it exists, otherwise instantiates one.
     * @param skeleton
     * @return
     */
    public static StrongholdController loadOrNew(ModSkeleton skeleton) {
       ArrayList<Object> objs = PersistentObjectUtil.getObjects(skeleton, StrongholdController.class);
       if (!objs.isEmpty()) {
           return (StrongholdController)objs.get(0);
       } else {
           StrongholdController c = new StrongholdController();
           PersistentObjectUtil.addObject(skeleton,c);
           return c;
       }
    }

    public StrongholdController() {
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
        strongholdHashMap.clear();
    }

    private void initClient() {
    /*    new StarRunnable(){
            @Override
            public void run() {
                try {
                    GameClientState.instance.getWorldDrawer().getGuiDrawer().getHud().getRadar().getLocation().setTextSimple(
                            new Object(){
                                @Override
                                public String toString() {
                                    Vector3i system = GameClientState.instance.getPlayer().getCurrentSystem();
                                    Stronghold sys = strongholdHashMap.get(system);
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
        }.runTimer(ModMain.instance,5); */
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
                if (!event.isServer())
                    return;

                if (!event.getController().getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION))
                    return; //dont even have to test for anything non-station.
                try {
                    Vector3i system = event.getController().getSystem(new Vector3i());
                    int owner = GameServerState.instance.getUniverse().getStellarSystemFromStellarPos(system).getOwnerFaction();
                    updateStronghold(system,owner);
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
                    updateStronghold(system,owner);
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
        LinkedList<Stronghold> toRemove = new LinkedList<>();
        LinkedList<Stronghold> synchList = new LinkedList<>();
        //update all systems that players are inside of rn
        for (PlayerState p: GameServerState.instance.getPlayerStatesByName().values()) {
            Vector3i system = p.getCurrentSystem();
            Stronghold ss = strongholdHashMap.get(system);
            if (ss != null) {
                ss.update(System.currentTimeMillis()/1000);
            //    ModMain.log(ss.toString());
                if (ss.isFlagDelete()) {
                    //karteileiche
                    toRemove.add(ss);
                    ss.setFlagDelete(true);
                    continue;
                }

                if (ss.isSynchFlag()) { //synch to client required?
                    synchList.add(ss);
                    ss.setSynchFlag(false);
                }
            }
        }

        for (Stronghold s: toRemove) {
            strongholdHashMap.remove(s.getStellarPos());
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
    public void updateStronghold(Vector3i system, int ownerFaction) {
        Stronghold sys = strongholdHashMap.get(system);
        if (sys==null) {
            sys = new Stronghold(this, system, ownerFaction);
            sys.init();
        }
        sys.update(System.currentTimeMillis()/1000);
        if (!sys.isFlagDelete()) {
            strongholdHashMap.put(sys.getStellarPos(),sys);
        }
    }

    public void synchClientFull(final PlayerState p) {
        final LinkedList<Stronghold> systems = new LinkedList<>();
        for (Stronghold s: strongholdHashMap.values()) {
            systems.add(s);
        }
        PacketUtil.sendPacket(p, new StrongholdPacket(systems));
    }

    protected Stronghold getStronghold(Vector3i system) {
        return strongholdHashMap.get(system);
    }

    /**
     * gets stronghold that this sector is in. never null
     * @param s
     * @return
     */
    public Stronghold getStrongholdFromSector(Vector3i s) {
        s = new Vector3i(s);
        mutateSectorToSystem(s);
        Stronghold stronghold = getStronghold(s);
        if (stronghold == null) {
            try {
                int owners = GameServerState.instance.getUniverse().getStellarSystemFromStellarPosIfLoaded(s).getOwnerFaction();
                stronghold = new Stronghold(this, s, owners);
                stronghold.init();
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new NullPointerException("could not generate stronghold for sector " + s);
            }
        }
        return stronghold;
    }

    public void updateStronghold(UUID uuid, PacketReadBuffer buffer) {
        for (Stronghold x: strongholdHashMap.values()) {
            if (x.getUuid()!=null && uuid.equals(x.getUuid()))
                x.onDeserialize(buffer);
                return;
        }
        Stronghold s = new Stronghold(this, buffer);
        s.setUuid(uuid);
        strongholdHashMap.put(s.getStellarPos(),s);
    }

    //event stuff

    protected void onStrongpointCaptured(Strongpoint p, int newOwner) {
        for (IStrongpointEvent e: pointEs) {
            e.onStrongpointOwnerChanged(p,newOwner);
        }
    }

    protected void onDefensePointsChanged(Stronghold h, int newPoints) {
        for (IStrongholdEvent e: holdEs) {
            e.onDefensepointsChanged(h, newPoints);
        }
    }

    protected void onStrongholdOwnerChanged(Stronghold h, int newOwner) {
        for (IStrongholdEvent e: holdEs) {
            e.onStrongholdOwnerChanged(h, newOwner);
        }
    }

    public void addStrongholdEventListener(IStrongholdEvent e) {
        holdEs.add(e);
    }

    public void addStrongpointEventListener(IStrongpointEvent e) {
        pointEs.add(e);
    }

    //serialization

    @Override
    public void onDeserialize(PacketReadBuffer packetReadBuffer) {
        try {
            //read systems and their health
            int systems = packetReadBuffer.readInt();
            for (int i = 0; i < systems; i++) {
                UUID uuid = packetReadBuffer.readObject(UUID.class);
                Stronghold s = new Stronghold(this, packetReadBuffer);
                s.setUuid(uuid);
                strongholdHashMap.put(s.getStellarPos(),s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialize(PacketWriteBuffer packetWriteBuffer) {


        try {
            //write systems and their health
            packetWriteBuffer.writeInt(strongholdHashMap.size());
            for (Map.Entry<Vector3i, Stronghold> entry: strongholdHashMap.entrySet()) {
                long last = entry.getValue().lastUpdate;
                entry.getValue().lastUpdate = 0;
                packetWriteBuffer.writeVector(entry.getKey());
                entry.getValue().onSerialize(packetWriteBuffer);
                entry.getValue().lastUpdate = last; //rewrite in case the object is still used.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void synchFromServer(LinkedList<Stronghold> systems) {
        if (GameClientState.instance==null)
            return;
        for (Stronghold s: systems) {
            if (s.isFlagDelete()) {
                strongholdHashMap.remove(s.getStellarPos());
            }else {
                strongholdHashMap.put(s.getStellarPos(),s);
            }
        }
    }

    //TODO move to utility
    public static void mutateSectorToSystem(Vector3i sector) {
        sector.x = sector.x/VoidSystem.SYSTEM_SIZE;
        sector.y = sector.y/VoidSystem.SYSTEM_SIZE;
        sector.z = sector.z/VoidSystem.SYSTEM_SIZE;
    }
}
