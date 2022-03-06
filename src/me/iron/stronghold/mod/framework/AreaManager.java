package me.iron.stronghold.mod.framework;

import api.DebugFile;
import api.listener.Listener;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The area manager is the central manager/container that controls all areas and all effect objects => SendableUpdatable (SU).
 * All objects build a single, non-cyclic tree with unlimited children, the area manager is the root. The AM calls operations on its children, which cascades into the whole tree.
 * The AM is the Interface to access specific SUs. The AM has a chunkmanager, a supporting datastructure (grid system) to improve performance when finding all areas a player is in.
 * The AM manages loading and saving to Persistence.
 * The AM handles updating and synching the whole tree for update, instantiate and delete operations.
 * All bubbling events go through the AreaManager, add listeners here to catch all events.
 */
public class AreaManager extends AbstractControllableArea {
    private boolean client;
    private boolean server;
    private final Timer timer = new Timer();
    private ChunkManager chunkManager = new ChunkManager(this);
    private AbstractAreaContainer container = new AbstractAreaContainer();
    private HashMap<Long,SendableUpdateable> UID_to_object = new HashMap<>();
    public AreaManager() { //is a singelton (hopefully)
        super("AbstractAreaManager");
        UID_to_object.put(getUID(),this);
        assert getUID() == -1;
        addListener(chunkManager);
    }

    /**
     * AM is on a server (includes hosting a SP), not exclusive with setClient
     * @param m
     */
    public void setServer(StarMod m){
        server = true;
        new StarRunnable(){
            @Override
            public void run() {
                if (timer.currentTime + 5000<System.currentTimeMillis()) {
                    update(timer);
                }
            }
        }.runTimer(m,10);
        load();
        StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
            @Override
            public void onEvent(PlayerJoinWorldEvent playerJoinWorldEvent) {
                final String playername = playerJoinWorldEvent.getPlayerName();
                new StarRunnable(){
                    @Override
                    public void run() {
                        if (GameServerState.instance.getPlayerStatesByName().containsKey(playername)) {
                            AbstractAreaContainer c = new AbstractAreaContainer();
                            addAllToContainer(c);
                            UpdatePacket up= new UpdatePacket();
                            up.addContainer(c);
                            PlayerState p = GameServerState.instance.getPlayerStatesByName().get(playername);
                            PacketUtil.sendPacket(p, up);
                            cancel();
                        }
                    }
                }.runTimer(ModMain.instance,10);

            }
        },ModMain.instance);
    };

    /**
     * AM is on a client (includes SP), not exclusive with setServer
     */
    public void setClient(){
        client = true;
        new StarRunnable(){
            @Override
            public void run() {
                update(t);
            }
        }.runTimer(ModMain.instance,100);
    };

    @Override
    public long getUID() {
        return -1; //area manager has a locked UID, its a singelton and should be constant across machines and restarts.
    }

    /**
     * call on shutdown to properly close AM
     */
    public void onShutdown() {
        if (server) {
            save();
        }
        //destroy();
    }

    /**
     * load from PersistenceObject file
     */
    public void load() {
        if (ModMain.instance != null && server) {
            ArrayList<Object> os = PersistentObjectUtil.getObjects(ModMain.instance.getSkeleton(), container.getClass());
            if (!os.isEmpty()) {
                clear();
                loadFromContainer ((AbstractAreaContainer)os.get(0));
            }
        }
    }

    /**
     * save to PersistenceObeject file
     */
    public void save() {
        //the saving is basically all code reused from the network synching.
        if (ModMain.instance != null) {
            //update all areas on last time
            update(timer);
            //mark all areas as synch so they get saved to container.
            AbstractAreaContainer container = new AbstractAreaContainer();
            addAllToContainer(container);
            //save the whole container.
            PersistentObjectUtil.removeAllObjects(ModMain.instance.getSkeleton(), container.getClass());
            PersistentObjectUtil.addObject(ModMain.instance.getSkeleton(), container);
            PersistentObjectUtil.save(ModMain.instance.getSkeleton());
            log("saved areamanager to POU");
        }
    }

    /**
     * clear Manager. DOES NOT SYNCH!
     */
    public void clear() {
        for (SendableUpdateable c: children) {
            c.destroy();
        }
        UID_to_object.clear();
        children.clear();
        chunkManager.destroy();
        chunkManager = new ChunkManager(this);
        addListener(chunkManager);
    }

    protected void loadFromContainer(AbstractAreaContainer container) {
        log("load from container.");
        //instantiate tree structure of empty object
        if (container.getTree() != null) {
            log("instantiate from tree, start with"+container.getTree().className);
            this.instantiateArea(container.getTree(),null);
        }
        else
            log("nothing to instantiate from container.");

        log("Loading: after instantiation:\n"+printObject(this));

        //update objects with values
        Iterator<SendableUpdateable> it = container.getSynchObjectIterator();
        while (it.hasNext()) {
            SendableUpdateable o2 = it.next();
            this.updateObject(o2);
        }

        log("Loading: After synching:\n"+printObject(this));

        Iterator<Long> delete = container.getDeleteUIDIterator();
        while (delete.hasNext()) {
            this.removeObject(delete.next());
        }
    }

    private void addAllToContainer(AbstractAreaContainer container) {
        for (SendableUpdateable su: UID_to_object.values()) {
            if (su.equals(this))
                continue;
            container.addForInstantiation(su);
           // container.addForSynch(su);
        }
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }

    @Override
    public void update(Timer timer) {
        timer.lastUpdate = timer.currentTime;
        timer.currentTime = System.currentTimeMillis();
        updateLoaded(timer);
        //collect all children that want to be synched.
        if (container.isEmpty() || client)
            return;
        log("synching server->client");
        UpdatePacket p = new UpdatePacket();
        p.addContainer(container);
        p.sendToAll();
        container = new AbstractAreaContainer(); //cant clear old one bc serialization is on other thread, takes longer
        //testMain.simulateNetwork(p);
    }

    //TODO update all on saving

    /**
     * will update any direct children (highest level areas) that a player is inside of.
     * @param timer
     */
    public void updateLoaded(Timer timer) {
        if (GameServerState.instance != null) {
            //collect all loaded areas => all areas in chunks that players are currently in.
            HashSet<SendableUpdateable> loadedAreas = new HashSet<>(getChildren().size());
            for (PlayerState p: GameServerState.instance.getPlayerStatesByName().values()) {
                for (StellarControllableArea a: chunkManager.getAreasFromSector(p.getCurrentSector())) {
                    loadedAreas.add(getRoot(a));
                }
            }

            for (SendableUpdateable child: loadedAreas) {
                System.out.println("updating loaded stellar area: "+child.getName());
                child.update(timer);
            }
            //problem: if player is inside of an area, that is not a direct child to area manager, the area isnt updated.
            //=> climb up in tree until hitting a direct child of areamanager
        }
    }

    /**
     * walk up tree until root (child of area manager) is found or no more parent (illegal)
     * @param area in tree.
     * @return root of tree this area is in.
     */
    public SendableUpdateable getRoot(SendableUpdateable area) {
        if (area.getParent()!=null && !(area.getParent() instanceof AreaManager))
            return getRoot(area.getParent());
        else
            return area;
    }

    /**
     * update this object
     * @param origin
     */
    protected void updateObject(SendableUpdateable origin) {
        long UID = origin.getUID();
        SendableUpdateable target= UID_to_object.get(UID);
        assert target.getClass().getName().equals(origin.getClass().getName()):"tried updating "+target.getClass().getName()+" with origin obj "+ origin.getClass().getName();
        if (target != null) {
            target.updateFromObject(origin);
            log("updating object "+target.getName());
        } else {
            System.err.println("area "+origin.getName()+"("+origin.getUID()+") has no local counterpart. cant update.");
        }
    }

    @Override
    public void addChildObject(SendableUpdateable child) {
        super.addChildObject(child);
    }

    @Override
    public void requestSynchToClient(SendableUpdateable area) {
        super.requestSynchToClient(area);
        if (client)
            return;
        log("[manager]area " + area.getName() +" request synch");
        container.addForSynch(area);
    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, SendableUpdateable child, boolean removed) {
        super.onChildChanged(parent, child, removed);
        if (client)
            log("child changed on client: class="+child+" name='"+child.getName() +"' was "+(removed?"removed":"added"));

        if (removed){

        } else {
            //child was added
            UID_to_object.put(child.getUID(), child);
            if (server && !client) {
                //request that clientmanager instantiates an empty child and creates the parent->child and child->parent connections.
                //collect parent chain from child to manager and the parents classes

                container.addForInstantiation(child);
            }

        }
    }

    @Override
    public void beforeDestroy(AbstractControllableArea area) {
        super.beforeDestroy(area);
        for (IAreaEvent e: listeners) {
            e.beforeDestroy(area);
        }
    }

    @Override
    public void onOverwrite(AbstractControllableArea area) {
        for (IAreaEvent e: listeners) {
            e.onOverwrite(area);
        }
    }

    /**
     * will safely destroy and remove this object, cascade destroy+remove into children.
     * @param UID
     */
    public void removeObject(long UID) {
        SendableUpdateable obj = UID_to_object.get(UID);
        if (obj != null) {
            if (obj instanceof AbstractControllableArea) {
                for (SendableUpdateable s: ((AbstractControllableArea) obj).children) {
                    removeObject(s.getUID());
                }
            }
            UID_to_object.remove(UID);
            if (server && !client)
                container.addForDeletion(obj);
            obj.destroy();

        }
    }

    /**
     * instantiate empty area/sendable in the tree with this parent. parent HAS to exist.
     * @param dummy dummy that carries information
     * @param parent must exist in tree
     */
    protected void instantiateArea(AbstractAreaContainer.DummyArea dummy,@Nullable Long parent) {
        long UID = dummy.UID;
        String className = dummy.className;
        log("instantiate area: UID=" + UID +" ,class="+ className );
        if (parent == null && className.equals(getClass().getName())) { //is new object a singelton, and exists already with a different UID?
            //c
            UID_to_object.put(UID, this); //schreibe selbst an parent UID im dictionary.
            //broadcast("no parent, recurse");
            for (AbstractAreaContainer.DummyArea child: dummy.children)
                instantiateArea(child, UID);
            return;
        }
        //manager->parent->...->parent->child/leaf
        //UID is unknown, parent UID is known.
        if (UID_to_object.containsKey(UID)) {
            System.out.println("warning: instantiating area with existing UID. predecessor: "+UID_to_object.get(UID)+" new obj:"+dummy);
        }
        if (UID_to_object.containsKey(parent)) {
            try {
                Class<?> clazz = Class.forName(className);
                Object o = clazz.newInstance();
                assert o instanceof SendableUpdateable:"instantiated new object is not a sendableUpdatable:"+o.getClass().getName();
                AbstractControllableArea parentObj = (AbstractControllableArea)UID_to_object.get(parent);
                SendableUpdateable child = (SendableUpdateable) o;
                child.setUID(UID);
                log("instantiating child type " + className + " to parent type " + parentObj.getClass().getName());
                //link parent and child
                parentObj.addChildObject(child);

                UID_to_object.put(UID,child);
                //recurse
                for (AbstractAreaContainer.DummyArea childX: dummy.children) {
                    instantiateArea(childX, UID);
                }

            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            log("denied instantiating area: "+dummy.UID+" "+dummy.className+" bc parent dont match or UID already known.");
        }
    }

    /**
     * log this message
     * @param mssg
     */
    private void log(String mssg) {
        System.out.println("[Manager]"+(client?"[client]":"")+(server?"[server]":"")+mssg);
        DebugFile.log("[Manager]"+(client?"[client]":"")+(server?"[server]":"")+mssg);
    }

    public LinkedList<StellarControllableArea> getAreaFromSector(Vector3i sector) {
        return chunkManager.getAreasFromSector(sector);
    }

    public Collection<SendableUpdateable> getAllObjects() {
        return UID_to_object.values();
    }

    public String printObject(long UID) {
        SendableUpdateable su= UID_to_object.get(UID);
        if (su != null)
            return printObject(su);
        else
            return "NULLOBJECT";
    }

    public String printObject(SendableUpdateable su) {

        return printRecursion(su,"");
    }

    private String printRecursion(SendableUpdateable su, String prefix) {
        StringBuilder out = new StringBuilder(prefix + su.toString()+"\n");
        if (su instanceof AbstractControllableArea) {
            int max = ((AbstractControllableArea) su).getChildren().size();
            for (int i = 0; i < max; i++) {
                SendableUpdateable c = ((AbstractControllableArea) su).getChildren().get(i);
                out.append(printRecursion(c, prefix + "\t"));
                if (i != 0 && i != max-1)
                    out.append("\n");
            }
        }
        return out.toString();
    }

    public boolean isClient() {
        return client;
    }

    public boolean isServer() {
        return server;
    }
}
