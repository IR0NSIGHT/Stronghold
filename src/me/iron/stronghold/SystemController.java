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
    public static int[] hpRange = new int[]{-1000,0,1000}; //lower cap, point of failure, upper cap
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
           return new SystemController();
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

    }
    private void initServer() {
        new StarRunnable(){
            long last;
            @Override
            public void run() {
                try {
                    if (System.currentTimeMillis()>last+2000) {
                        update();
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
                if (event.getController().getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION) &&
                    StrongholdSystem.isStrongpoint(event.getController().getSector(new Vector3i())) &&
                    event.getController().getFactionId()!=0
                ) {
                    Vector3i system = event.getController().getSystem(new Vector3i());
                    Vector3i sector = event.getController().getSector(new Vector3i());
                    //station was loaded in a strongpoint sector
                    StrongholdSystem sys = system_hps.get(system);
                    if (sys == null) {
                        //instantiate a new stronghold system
                        try {
                            sys = new StrongholdSystem(system, GameServerState.instance.getUniverse().getStellarSystemFromStellarPos(system).getOwnerFaction());
                            sys.setPoints(StrongholdSystem.generatePoints(system));
                            system_hps.put(sys.getStellarPos(),sys);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    sys.setPointOwner(event.getController().getFactionId(),sector);
                    ModMain.log("station loaded in strongpoint:\n"+sys.toString());
                }
            }
        },ModMain.instance);

        StarLoader.registerListener(SegmentControllerOverheatEvent.class, new Listener<SegmentControllerOverheatEvent>() {
            @Override
            public void onEvent(SegmentControllerOverheatEvent event) {
                if (    event.getEntity().getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION) && //is station
                        StrongholdSystem.isStrongpoint(event.getEntity().getSector(new Vector3i())) //is the sector a strongpoint
                ) {
                    Vector3i sysP = event.getEntity().getSystem(new Vector3i());
                    StrongholdSystem system = system_hps.get(sysP);
                    if (system != null) {
                        system.setPointOwner(0,event.getEntity().getSector(new Vector3i()));
                    }//no else, dont need to instantiate one for a lost station.
                }
            }
        },ModMain.instance);
    }

    private void update() {
        ModMain.log("ping");
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
                    if (ss.isFlagDelete() || ss.getOwner() == 0 || ss.getHp() <= hpRange[0]) {
                        //karteileiche
                        toRemove.add(ss);
                        ss.setFlagDelete(true);
                        continue;
                    }

                    if (ss.isSynchFlag()) { //synch to client required?
                        synchList.add(ss);
                    }
                    ModMain.log(ss.toString());
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
                packetWriteBuffer.writeVector(entry.getKey());
                entry.getValue().onSerialize(packetWriteBuffer);
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
