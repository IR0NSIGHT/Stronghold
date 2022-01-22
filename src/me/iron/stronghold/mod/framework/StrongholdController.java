package me.iron.stronghold.mod.framework;

import api.listener.Listener;
import api.listener.events.entity.SegmentControllerFullyLoadedEvent;
import api.listener.events.entity.SegmentControllerOverheatEvent;
import api.mod.StarLoader;
import api.mod.config.PersistentObjectUtil;
import api.network.PacketReadBuffer;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.events.IStrongholdEvent;
import me.iron.stronghold.mod.events.IStrongpointEvent;
import me.iron.stronghold.mod.playerUI.ScanHandler;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
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
public class StrongholdController {
    transient private LinkedList<IStrongholdEvent> holdEs = new LinkedList<>();
    transient private LinkedList<IStrongpointEvent> pointEs = new LinkedList<>();
    transient private long lastSave;

    transient private static StrongholdController instance;
    transient public static String getStrongholdTerm = "Stronghold";
    transient public static int[] hpRange = new int[]{-3600,300000}; //lower cap, point of failure, upper cap
    transient public static int changePerTimeUnit = 1;

    //private stuff
    transient private HashMap<Vector3i, Stronghold> strongholdHashMap = new HashMap<>();

    public StrongholdController() {
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
                    if (System.currentTimeMillis()>last+10000) {
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

        load();
    }

    /**
     * updates all logged systems that a player is currently in. does not create new stronghold systems by itself.
     * pure serverside
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
        if (synchList.size()!= 0) {
            ModMain.log("synching changed stronghold with client: "+synchList.size());
            new StrongholdPacket(synchList).sendToAll();
        }

        if (lastSave + 1000 * 60 * 15 < System.currentTimeMillis()) { //save timer every 15 minutes
            lastSave = System.currentTimeMillis();
            save();
        }
    }

    protected HashMap<Vector3i, Stronghold> getStrongholdHashMap() {
        return strongholdHashMap;
    }

    protected void reset() {
        strongholdHashMap.clear();
    }

    protected Stronghold getStronghold(Vector3i system) {
        return strongholdHashMap.get(system);
    }

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

    protected void onStrongholdOwnerChanged(Stronghold h, int newOwner) { //TODO sometimes fires when a strongpoint is conquered, not a system.
        for (IStrongholdEvent e: holdEs) {
            e.onStrongholdOwnerChanged(h, newOwner);
        }
    }

    protected void onStrongholdBalanceChanged(Stronghold h, int newBalance) {
        for (IStrongholdEvent e: holdEs) {
            e.onStrongholdBalanceChanged(h, newBalance);
        }
    }

    public void addStrongholdEventListener(IStrongholdEvent e) {
        holdEs.add(e);
    }

    public void addStrongpointEventListener(IStrongpointEvent e) {
        pointEs.add(e);
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

    public void onShutdown() {
        save();
    }

    public static StrongholdController getInstance() {
        return instance;
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
                int owners = 0;
                if (GameServerState.instance != null) {
                    owners = GameServerState.instance.getUniverse().getStellarSystemFromStellarPosIfLoaded(s).getOwnerFaction();
                }
                if (GameClientState.instance != null) {
                    VoidSystem sys = GameClientState.instance.getCurrentClientSystem();
                    if (sys != null) //only happens when player is currently joing, doesnt matter if he gets wrong info here.
                        owners = GameClientState.instance.getCurrentClientSystem().getOwnerFaction();
                }
                stronghold = new Stronghold(this, s, owners);
                stronghold.init();
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new NullPointerException("could not generate stronghold for sector " + s);
            }
        }
        return stronghold;
    }

    public void updateStrongholdFromBuffer(Vector3i system, PacketReadBuffer buffer) {
        Stronghold h = strongholdHashMap.get(system);
        if (h !=null)
            h.onDeserialize(buffer);
        else {
            h = new Stronghold(buffer);
            h.setController(this);
        }
        assert system.equals(h.getStellarPos());
        if (h.isFlagDelete()) {
            strongholdHashMap.remove(system);
        } else {
            strongholdHashMap.put(h.getStellarPos(),h);
        }
    }

    protected void load() {
        ModMain.log("stronghold loading data");
        strongholdHashMap.clear();
        StrongholdContainer c = getContainer();
        for (Stronghold s: c.getStrongHolds()) {
            s.setController(this);
            strongholdHashMap.put(s.getStellarPos(),s);
        }
        ModMain.log("stronghold loaded " + strongholdHashMap.values().size() + " strongholds.");
    }
    protected void save() {
        ModMain.log("saving Strongholds data.");
        StrongholdContainer c = getContainer();
         c.getStrongHolds().clear();
         c.setStrongholds(strongholdHashMap.values());
        PersistentObjectUtil.save(ModMain.instance.getSkeleton());
        ModMain.log("finished saving strongholds data");
    }

    private StrongholdContainer getContainer() {
        ArrayList<Object> os = PersistentObjectUtil.getObjects(ModMain.instance.getSkeleton(), StrongholdContainer.class);
        if (os.size()!=0) {
            return (StrongholdContainer)os.get(0);
        }
        StrongholdContainer c = new StrongholdContainer();
        PersistentObjectUtil.addObject(ModMain.instance.getSkeleton(), c);
        return c;
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

    public String toStringPretty() {
        StringBuilder b = new StringBuilder("All strongholds:\n");
        for (Stronghold h: getStrongholdHashMap().values()) {
            b.append(ScanHandler.getStrongholdInfo(h)).append("\n\n");
        }
        return "StrongholdController{" +
                "holdEs=" + holdEs +
                ", pointEs=" + pointEs +
                ", lastSave=" + lastSave +
                ", strongholdHashMap=" + b.toString() +
                '}';
    }

    //TODO move to utility
    public static void mutateSectorToSystem(Vector3i sector) {
        float x,y,z;
        x = sector.x; x= (float) Math.floor(x/VoidSystem.SYSTEM_SIZEf);
        y = sector.y; y= (float) Math.floor(y/VoidSystem.SYSTEM_SIZEf);
        z = sector.z; z= (float) Math.floor(z/VoidSystem.SYSTEM_SIZEf);
        sector.set((int)x,(int)y,(int)z);
    }
}
