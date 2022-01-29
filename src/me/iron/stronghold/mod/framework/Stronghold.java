package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.AmbienceUtils;
import me.iron.stronghold.mod.effects.CPNameGen;
import me.iron.stronghold.mod.voidshield.VoidShieldController;
import org.lwjgl.Sys;
import org.lwjgl.util.vector.Vector;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.*;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 19:36
 * container for stellar system that acts as a stronghold.
 */
public class Stronghold extends SimpleSerializerWrapper {
    transient private Vector3i stellarPos;
    transient private int owner;
    transient private int hp = StrongholdController.hpRange[0]; //healthpoints
    transient private int balance;
    transient private boolean synchFlag;

    transient private HashMap<Vector3i, Strongpoint> strongpointHashMap = new HashMap<>();
    transient private ArrayList<Strongpoint> strongpointOrder = new ArrayList<>();
    transient private int highestOwnCP;
    transient private int ownedBySysOwner;
    transient private int ownedByNoone;
    transient private UUID uuid;
    transient private boolean flagDelete;
    transient private StrongholdController c;

    transient protected long lastUpdate;

    protected Stronghold(StrongholdController c, Vector3i stellarPos, int owner) {
        this.uuid = UUID.randomUUID();
        this.stellarPos = stellarPos;
        this.owner = owner;
        this.c = c;
    }

    public Stronghold() {} //for auto deserializing

    protected Stronghold(PacketReadBuffer buffer) {
        onDeserialize(buffer);
    }

    /**
     * returns the enemy controlpoint highest in the order. can be null if no enemy cp
     * @return
     */
    protected Strongpoint getLastEnemyCP() {
       // if (lastEnemyOwned<0||lastEnemyOwned>=strongpointOrder.size())
       //     return null;
       // return strongpointOrder.get(lastEnemyOwned);
        return null;
    }

    protected Strongpoint getHighestOwnCP() {
        return null;
    }

    public static void main(String[] args) {
        //init controllers
        StrongholdController c = new StrongholdController();
        c.init();
        VoidShieldController v = new VoidShieldController(true);
        v.init();

        //make stronghold
        Stronghold h = new Stronghold();
        h.setStellarPos(new Vector3i(0,0,0));
        int ownerF = 420;
        h.setOwner(ownerF);
        h.setStrongpoints(new Vector3i[]{new Vector3i(0,0,0),new Vector3i(1,1,1), new Vector3i(2,2,2),new Vector3i(3,3,3)});
        h.setController(c);

        //add to controller
        c.getStrongholdHashMap().put(h.getStellarPos(),h);
        System.out.println("--print hold ---\n"+h.toString()+"--------\n");

    //   //start conquering CPs
    //   for (int i = 0; i < 2; i++) {
    //       System.out.println("setting CP "+h.strongpointOrder.get(i).getSector()+" to owner "+ownerF +":");

    //       h.strongpointOrder.get(i).setOwner(ownerF);
    //   }

        h.setDefensePoints(500);
        long t = 0;
        long first = System.currentTimeMillis();
        long last = System.currentTimeMillis();
        int i = 0;
        while (System.currentTimeMillis()<first+1000*35) {
            t+= System.currentTimeMillis()-last;
            last = System.currentTimeMillis();
            if (t>1000) {
                t = 0;
                i++;
                if (i%3==0&&i/3-1<h.strongpointOrder.size()) {
                    h.strongpointOrder.get(i/3-1).setOwner(420);
                }
                System.out.println("t="+i);
                if (i%5==0){
                    System.out.println("t+"+i);
                    System.out.println("--print hold ---\n"+h.toString()+"---------\n");

                    //for (Strongpoint p: h.strongpointOrder) {
                    //    VoidShieldController.isSectorVoidShielded(p.getSector());
                    //}
                }
            }


        }

        System.out.println("--print hold ---\n"+h.toString()+"---------\n");
    }

    protected void setController(StrongholdController c) {
        this.c = c;
    }

    protected void init() {
        setStrongpoints(Stronghold.generatePoints(this.stellarPos));
    }

    protected static Vector3i[] generatePoints(Vector3i system) {
        system = new Vector3i(system);
        Random r = new Random(system.code());
        system.scale(VoidSystem.SYSTEM_SIZE); //convert to sector pos
        system.add(VoidSystem.SYSTEM_SIZE_HALF,VoidSystem.SYSTEM_SIZE_HALF,VoidSystem.SYSTEM_SIZE_HALF); //center pos of system
        int amout = 1 + r.nextInt(3);
        //amout = (amout/2)*2+1; //always uneven
        Vector3i[] points = new Vector3i[amout];
        for (int i = 0; i < amout; i++) {
            Vector3f dir = new Vector3f(r.nextFloat()*(r.nextBoolean()?1:-1),r.nextFloat()*(r.nextBoolean()?1:-1),r.nextFloat()*(r.nextBoolean()?1:-1));
            dir.normalize(); dir.scale(VoidSystem.SYSTEM_SIZE_HALF-2);
            points[i] = new Vector3i(system);
            points[i].add((int)dir.x,(int)dir.y,(int)(dir.z));
        }
        return points;
    }

    protected void setStrongpoints(Vector3i[] pos) {
        //init map, fill map
        if (strongpointHashMap == null)
            strongpointHashMap = new HashMap<>(pos.length);
        ownedByNoone = pos.length;
        strongpointHashMap.clear();
        for (Vector3i p: pos) {
            Strongpoint point = new Strongpoint(p, this);
            strongpointHashMap.put(p,point);
        }
        updateOrder(strongpointHashMap.values());
        setSynchFlag(true);
    }

    protected void update(long timeUnits) {
        if (lastUpdate == 0)
            lastUpdate = timeUnits;

        if (GameServerState.instance!=null) { //has to work outside of starmade too for debug purposes
            try {
                int cOwner =GameServerState.instance.getUniverse().getStellarSystemFromStellarPosIfLoaded(stellarPos).getOwnerFaction();
                if (cOwner != owner)
                    setOwner(cOwner);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException ex) {
                ModMain.log("ignoring nullpointer in stronghold update");
                return;
            }
        }


        boolean changed = false; //have any strongpoints changed in the update?
        boolean redundant = true; //are all strongpoints redundant and could safely be deleted?
        for (Strongpoint sp: strongpointHashMap.values()) {
            changed = changed || sp.update();
            redundant = redundant && sp.isRedundant();
        }
        if (changed) {
            countOwnedBySysOwner();
            setSynchFlag(true);
        }

        //change defensepoints
        int diff = Math.max (1,(int) (timeUnits-lastUpdate));
        calculateBalance(strongpointOrder); //TODO remove once it works reliable eventbased.
        adjustPoints(balance, diff, strongpointHashMap.size());

        lastUpdate = timeUnits;
        if (hp<= StrongholdController.hpRange[0] && redundant) {
            //hp are minimal after update -> wont change until a strongpoint changes owner
            setFlagDelete(true);
        }
    }

    protected void onDefensePointsChanged(int newPoints) {
        if (c!=null)
            c.onDefensePointsChanged(this, newPoints);
    }

    protected void onStrongpointCaptured(Strongpoint p, int oldOwner) {
        if (c!=null)
            c.onStrongpointCaptured(p,oldOwner);
        calculateBalance(strongpointOrder);
        updateHighestCP();
    }

    protected void onStrongholdBalanceChanged(int newBalance) {
        if (c!=null)
            c.onStrongholdBalanceChanged(this,newBalance);
    }

    protected void onStrongholdOwnerChanged(int newOwner) {
        if (c!=null)
            c.onStrongholdOwnerChanged(this,newOwner);
        updateHighestCP();
    }

    protected void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    //todo how to define a position if the stronghold is not a starsytem anymore?
    protected Vector3i getStellarPos() {
        return stellarPos;
    }

    protected void setOwner(int owner) {
        if (this.getOwner() != owner) {
            onStrongholdOwnerChanged(owner);
            this.owner = owner;
            setDefensePoints(StrongholdController.hpRange[0]); //changing ownership resets the stronghold.
            countOwnedBySysOwner();
            setSynchFlag(true);
        }

    }

    protected void setDefensePoints(int hp) {
        if (hp != this.hp) {
            onDefensePointsChanged(hp);
            this.hp = hp;
            setSynchFlag(true);
        }
    }

//flags for synch and deletion after sync
    protected boolean isSynchFlag() {
        return synchFlag;
    }

    /**
     * synch this system to all clients on the next update
     */
    protected void setSynchFlag(boolean b) {
        this.synchFlag = b;
    }

    protected boolean isFlagDelete() {
        return flagDelete;
    }

    /**
     * sets a flag so the client deletes this system after a synch
     */
    protected void setFlagDelete(boolean flagDelete) {
        this.flagDelete = flagDelete;
    }

//private stuff
    /**
     * adjust points based on how much time passed since last update and who controls the strongpoints.
     * @param timeUnits
     */
    private void adjustPoints(int balance, int timeUnits, int sPsAmount) {
        int diff = balance; //own vs all that are not mine/neutral
        int newHP = hp + diff*timeUnits* StrongholdController.changePerTimeUnit; //-2 diff x 5 timeUnits = -10 points
        newHP = Math.min(StrongholdController.hpRange[1]*sPsAmount,Math.max(StrongholdController.hpRange[0],newHP));
        setDefensePoints(newHP);
        ModMain.log(String.format("updated stronghold %s balance :%s, timeUnits: %s, totalChange: %s",getName(),diff,timeUnits,diff*timeUnits));
        setSynchFlag(true);
    }

    private void calculateBalance(Collection<Strongpoint> points) {
        int balance = 0;
        if (getOwner() == 0) {
            this.balance = -1;
            return;
        }
        boolean allNeutral = true;
        for (Strongpoint p: points) {
            allNeutral = allNeutral&&p.getOwner()==0;
            //we or an ally owns this point.
            if (p.getOwner()==getOwner()||(GameServerState.instance != null && GameServerState.instance.getFactionManager().isFriend(p.getOwner(),getOwner())))
                balance += 1;
            //someone else owns this point.
            else if (p.getOwner()!=getOwner() && p.getOwner() != 0)
                balance -= 1;
        }
        balance = allNeutral?-1:balance;
        if (balance != this.balance && c!=null)
            c.onStrongholdBalanceChanged(this, balance);
        this.balance = balance;
    }
    private void updateOrder(Collection<Strongpoint> ps) {
        strongpointOrder.clear();
        strongpointOrder.ensureCapacity(ps.size());
        strongpointOrder.addAll(ps);
        Collections.sort(strongpointOrder, new Comparator<Strongpoint>() {
            @Override
            public int compare(Strongpoint o1, Strongpoint o2) {
                return (int)(o1.getSector().code()-o2.getSector().code());
            }
        });
        updateHighestCP();
    }
    private void updateHighestCP() {
        for (int i = 0; i < strongpointOrder.size(); i++) {
            int cpOwner = strongpointOrder.get(i).getOwner();
            //not neutral, own or allied
            if (cpOwner != getOwner() && (GameServerState.instance == null || !GameServerState.instance.getFactionManager().isFriend(cpOwner,getOwner()))) {
                highestOwnCP = i-1;
                return;
            }

        }
    }

    /**
     * the strongpoint that enemy forces have to conquer next.
     * if owner controls NO CPs, return null.
     * @return CP
     */
    public Strongpoint getNextOwnStrongpoint() {
        return (highestOwnCP>=0)?strongpointOrder.get(highestOwnCP):null;
    }

    /**
     * the controlpoint that can be conquered by owner forces next.
     * if owner controls all CPs => return null
     * @return CP
     */
    public Strongpoint getNextEnemyStrongpoint() {
        return (highestOwnCP+1<strongpointOrder.size())?strongpointOrder.get(highestOwnCP+1):null;
    }

    /**
     * updates internal counters
     * @return
     */
    private int countOwnedBySysOwner() {
        ownedBySysOwner = 0;
        ownedByNoone = 0;
        for (Strongpoint sp: strongpointHashMap.values()) {
            if (sp.getOwner()==owner)
                ownedBySysOwner++;
            else if (sp.getOwner()==0)
                ownedByNoone++;
        }
        return ownedBySysOwner;
    }

    private void updateStrongpoint(Vector3i pos, PacketReadBuffer buffer) {
        Strongpoint sp = strongpointHashMap.get(pos);
        if (sp == null) {
            sp = new Strongpoint(pos, this);
            strongpointHashMap.put(pos, sp);
        }
        sp.onDeserialize(buffer);
    }

//serialization
    @Override
    public void onDeserialize(PacketReadBuffer buffer) {
        try {
            setUuid(buffer.readObject(UUID.class));
            int strongholds = buffer.readInt();
            if (strongpointHashMap == null) {
                strongpointHashMap = new HashMap<>(strongholds);
            }
            HashSet<Vector3i> synched = new HashSet<>(); //log the strongpoints that exist on server
            for (int i = 0; i < strongholds; i++) {
                Vector3i pos = buffer.readVector();
                updateStrongpoint(new Vector3i(pos), buffer);
                synched.add(pos);
            }
            LinkedList<Vector3i> toDelete = new LinkedList<>(); //collect points that only exist on client, log for delete
            for (Vector3i pos : strongpointHashMap.keySet()) {
                if (!synched.contains(pos)) {
                    toDelete.add(pos);
                }
            }
            for (Vector3i pos: toDelete) { //delete unwanted.
                strongpointHashMap.remove(pos);
            }
            updateOrder(strongpointHashMap.values());

            setOwner(buffer.readInt());
            setStellarPos(buffer.readVector());
            setDefensePoints(buffer.readInt());

            setFlagDelete(buffer.readBoolean());
            setSynchFlag(buffer.readBoolean());
            calculateBalance(strongpointHashMap.values());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialize(PacketWriteBuffer buffer) {
        try {
            buffer.writeObject(uuid);
            buffer.writeInt(strongpointHashMap.size());
            for (Map.Entry<Vector3i,Strongpoint> entry: strongpointHashMap.entrySet()) {
                buffer.writeVector(entry.getKey());
                entry.getValue().onSerialize(buffer);
            }

            buffer.writeInt(owner);
            buffer.writeVector(stellarPos);
            buffer.writeInt(hp);

            buffer.writeBoolean(flagDelete);
            buffer.writeBoolean(synchFlag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//public interface
    @Override
    public String toString() {
        return String.format("owner: %s, pos:%s, hp: %s\n" +
                "own: %s, ene: %s, neut: %s, all: %s\n"+
                "updated: %s,\n" +
                "strongholds:\n%s",owner,stellarPos,hp,ownedBySysOwner,(strongpointHashMap.size()-ownedByNoone-ownedBySysOwner),ownedByNoone, strongpointHashMap.size(),lastUpdate,strongholdsToString());
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isStrongpoint(Vector3i sector) {
        return strongpointHashMap.containsKey(sector);
    }

    public Strongpoint getStrongpointFromSector(Vector3i sector) {
        return strongpointHashMap.get(sector);
    }

    public String getName() {
        return String.format("[%s]",(stellarPos==null)?"null pos":stellarPos.toStringPure());
    }

    public int getBalance() {return balance;}

    public String strongholdsToString() {
        StringBuilder b = new StringBuilder();
        CPNameGen gen = AmbienceUtils.getControlPointNameGen();
        Strongpoint p;
        for (int i = 0; i < strongpointOrder.size(); i++) {
            p = strongpointOrder.get(i);
            b.append(String.format("%s %s [%s] %s %s\n",gen.next(),p.getSector(), AmbienceUtils.tryGetFactionName(p.getOwner()),
                    (p.equals(getNextOwnStrongpoint())||p.equals(getNextEnemyStrongpoint())? "<<<":""),
                    (VoidShieldController.getInstance()!=null&&VoidShieldController.controlPointLockDownTill(p)>System.currentTimeMillis())
                            ?("locked for "+(VoidShieldController.controlPointLockDownTill(p)-System.currentTimeMillis())/1000+" seconds")
                            :""));
        }
        return b.toString();
    }

    //TODO replace with a more generic way of getting a position/borders
    public void setStellarPos(Vector3i stellarPos) {
        this.stellarPos = stellarPos;
        setSynchFlag(true);
    }

    public int getOwner() {
        return owner;
    }

    public int getDefensePoints() {
        return hp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stronghold that = (Stronghold) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
