package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import me.iron.stronghold.mod.ModMain;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
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
    private Vector3i stellarPos;
    private int owner;
    private int hp = StrongholdController.hpRange[0]; //healthpoints
    private int balance;
    private boolean synchFlag;

    private transient HashMap<Vector3i, Strongpoint> strongpointHashMap = new HashMap<>();
    private int ownedBySysOwner;
    private int ownedByNoone;
    private UUID uuid;
    private boolean flagDelete;
    private StrongholdController c;

    protected long lastUpdate;

    protected Stronghold(StrongholdController c, Vector3i stellarPos, int owner) {
        this.uuid = UUID.randomUUID();
        this.stellarPos = stellarPos;
        this.owner = owner;
        this.c = c;
    }

    protected Stronghold(StrongholdController c, PacketReadBuffer buffer) {
        this.c = c;
        onDeserialize(buffer);
    }

    protected void init() {
        setStrongpoints(Stronghold.generatePoints(this.stellarPos));
    }

    protected static Vector3i[] generatePoints(Vector3i system) {
        system = new Vector3i(system);
        Random r = new Random(system.code());
        system.scale(VoidSystem.SYSTEM_SIZE); //convert to sector pos
        system.add(VoidSystem.SYSTEM_SIZE_HALF,VoidSystem.SYSTEM_SIZE_HALF,VoidSystem.SYSTEM_SIZE_HALF); //center pos of system
        int amout = 3 + r.nextInt(4);
        amout = (amout/2)*2+1; //always uneven
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
        if (strongpointHashMap == null)
            strongpointHashMap = new HashMap<>(pos.length);
        ownedByNoone = pos.length;
        strongpointHashMap.clear();
        for (Vector3i p: pos) {
            Strongpoint point = new Strongpoint(p, this);
            strongpointHashMap.put(p,point);
        }
        setSynchFlag(true);
    }

    protected void update(long timeUnits) {
        if (lastUpdate == 0)
            lastUpdate = timeUnits;

        try {
            int cOwner =GameServerState.instance.getUniverse().getStellarSystemFromStellarPosIfLoaded(stellarPos).getOwnerFaction();
            if (cOwner != owner)
                setOwner(cOwner);
        } catch (IOException e) {
            e.printStackTrace();
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
        balance = calculateBalance(strongpointHashMap.values());
        adjustPoints(balance, diff);

        lastUpdate = timeUnits;
        if (hp<= StrongholdController.hpRange[0] && redundant) {
            //hp are minimal after update -> wont change until a strongpoint changes owner
            setFlagDelete(true);
        }
    }

    protected void onDefensePointsChanged(int newPoints) {
        c.onDefensePointsChanged(this, newPoints);
    }

    protected void onStrongpointCaptured(Strongpoint p, int newOwner) {
        c.onStrongpointCaptured(p,newOwner);
        balance = calculateBalance(strongpointHashMap.values());
    }

    protected void onStrongholdBalanceChanged(int newBalance) {
        c.onStrongholdBalanceChanged(this,newBalance);
    }

    protected void onStrongholdOwnerChanged(int newOwner) {
        c.onStrongholdOwnerChanged(this,newOwner);
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
        }
        this.owner = owner;
        setDefensePoints(StrongholdController.hpRange[0]); //changing ownership resets the stronghold.
        countOwnedBySysOwner();
        setSynchFlag(true);
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
    private void adjustPoints(int balance, int timeUnits) {
        int diff = balance; //own vs all that are not mine/neutral
        int newHP = hp + diff*timeUnits* StrongholdController.changePerTimeUnit; //-2 diff x 5 timeUnits = -10 points
        newHP = Math.min(StrongholdController.hpRange[1],Math.max(StrongholdController.hpRange[0],newHP));
        setDefensePoints(newHP);
        System.out.println(String.format("strongholds factor:%s, timeUnits: %s, totalChange: %s",diff,timeUnits,diff*timeUnits));
        setSynchFlag(true);
    }

    private int calculateBalance(Collection<Strongpoint> points) {
        int balance = 0;
        if (getOwner() == 0)
            return -1;
        boolean allNeutral = true;
        for (Strongpoint p: points) {
            allNeutral = allNeutral&&p.getOwner()!=0;
            if (p.getOwner()==getOwner())
                balance += 1;
            if (p.getOwner()!=getOwner() && p.getOwner() != 0)
                balance -= 1;
        }
        ModMain.log("balance: " + balance);
        return allNeutral?-1:balance;
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

            setOwner(buffer.readInt());
            setStellarPos(buffer.readVector());
            setDefensePoints(buffer.readInt());

            setFlagDelete(buffer.readBoolean());
            setSynchFlag(buffer.readBoolean());
            balance = calculateBalance(strongpointHashMap.values());
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
                "strongholds: %s",owner,stellarPos,hp,ownedBySysOwner,(strongpointHashMap.size()-ownedByNoone-ownedBySysOwner),ownedByNoone, strongpointHashMap.size(),lastUpdate,strongholdsToString());
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isStrongpoint(Vector3i sector) {
        return strongpointHashMap.get(sector)!=null;
    }

    public String getName() {
        return "Installation 05";
    }

    public int getBalance() {return balance;}

    public static String tryGetFactionName(int faction) {
        FactionManager f = null;
        if (GameServerState.instance==null) {
            f = GameClientState.instance.getFactionManager();
        } else {
            f = GameServerState.instance.getFactionManager();
        }
        Faction owners = f.getFaction(faction);
        String name;
        if (owners!=null) {
            name ="'"+ owners.getName()+"'";
        } else
            name = "neutral";
        return name;
    }

    public String strongholdsToString() {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<Vector3i,Strongpoint> entry: strongpointHashMap.entrySet()) {
            b.append(String.format("%s [%s]\n",entry.getKey(),tryGetFactionName(entry.getValue().getOwner())));
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
