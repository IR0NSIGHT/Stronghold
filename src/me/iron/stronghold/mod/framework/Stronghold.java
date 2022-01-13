package me.iron.stronghold.mod.framework;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
    private boolean synchFlag;

    private transient HashMap<Vector3i, Strongpoint> stroingpoint_sectors = new HashMap<>();
    private int ownedBySysOwner;
    private int ownedByNoone;
    protected long lastUpdate;
    private boolean flagDelete;
    public Stronghold(Vector3i system) {
        init();
    }

    protected Stronghold(Vector3i stellarPos, int owner) {
        this.stellarPos = stellarPos;
        this.owner = owner;
    }

    protected void init() {
        setStrongpoints(Stronghold.generatePoints(this.stellarPos));
    }

    public Stronghold(PacketReadBuffer buffer) {
        onDeserialize(buffer);
    }

    public void setStrongpoints(Vector3i[] pos) {
        if (stroingpoint_sectors == null)
            stroingpoint_sectors = new HashMap<>(pos.length);
        ownedByNoone = pos.length;
        stroingpoint_sectors.clear();
        for (Vector3i p: pos) {
            Strongpoint point = new Strongpoint(p);
            stroingpoint_sectors.put(p,point);
        }
        setSynchFlag(true);
    }

    public void update(long timeUnits) {
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
        for (Strongpoint sp: stroingpoint_sectors.values()) {
            changed = changed || sp.update();
            redundant = redundant && sp.isRedundant();
        }
        if (changed) {
            countOwnedBySysOwner();
            setSynchFlag(true);
        }

        //change defensepoints
        int diff = Math.max (1,(int) (timeUnits-lastUpdate));
        adjustPoints(ownedByNoone, ownedBySysOwner, stroingpoint_sectors.size(),diff);

        lastUpdate = timeUnits;
        if (hp<= StrongholdController.hpRange[0] && redundant) {
            //hp are minimal after update -> wont change until a strongpoint changes owner
            setFlagDelete(true);
        }
    }

    public boolean isStrongpoint(Vector3i sector) {
        return stroingpoint_sectors.get(sector)!=null;
    }

    public String getName() {
        return "Installation 05";
    }

    /**
     * adjust points based on how much time passed since last update and the owned vs total stronghold count.
     * @param owned
     * @param exist
     * @param timeUnits
     */
    private void adjustPoints(int neutral, int owned, int exist, int timeUnits) {
        int diff = owned-(exist-neutral-owned); //own vs all that are not mine/neutral
        if (neutral==exist)
            diff=-1;
        int newHP = hp + diff*timeUnits* StrongholdController.changePerTimeUnit; //-2 diff x 5 timeUnits = -10 points
        newHP = Math.min(StrongholdController.hpRange[1],Math.max(StrongholdController.hpRange[0],newHP));
        setDefensePoints(newHP);
        System.out.println(String.format("strongholds factor:%s, timeUnits: %s, totalChange: %s",diff,timeUnits,diff*timeUnits));
        setSynchFlag(true);
    }

    /**
     * updates internal counters
     * @return
     */
    private int countOwnedBySysOwner() {
        ownedBySysOwner = 0;
        ownedByNoone = 0;
        for (Strongpoint sp: stroingpoint_sectors.values()) {
            if (sp.getOwner()==owner)
                ownedBySysOwner++;
            else if (sp.getOwner()==0)
                ownedByNoone++;
        }
        return ownedBySysOwner;
    }

//serialization

    @Override
    public void onDeserialize(PacketReadBuffer buffer) {
        try {


            int strongholds = buffer.readInt();
            stroingpoint_sectors = new HashMap<>(strongholds);
            for (int i = 0; i < strongholds; i++) {
                Strongpoint p = new Strongpoint(buffer.readVector());
                p.onDeserialize(buffer);
                stroingpoint_sectors.put(p.getSector(), p);
            }

            setOwner(buffer.readInt());
            setStellarPos(buffer.readVector());
            setDefensePoints(buffer.readInt());

            setFlagDelete(buffer.readBoolean());
            setSynchFlag(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialize(PacketWriteBuffer buffer) {
        try {
            buffer.writeInt(stroingpoint_sectors.size());
            for (Map.Entry<Vector3i,Strongpoint> entry: stroingpoint_sectors.entrySet()) {
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

    //to string stuff

    @Override
    public String toString() {
        return String.format("owner: %s, pos:%s, hp: %s\n" +
                "own: %s, ene: %s, neut: %s, all: %s\n"+
                "updated: %s,\n" +
                "strongholds: %s",owner,stellarPos,hp,ownedBySysOwner,(stroingpoint_sectors.size()-ownedByNoone-ownedBySysOwner),ownedByNoone, stroingpoint_sectors.size(),lastUpdate,strongholdsToString());
    }

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
        for (Map.Entry<Vector3i,Strongpoint> entry: stroingpoint_sectors.entrySet()) {
            b.append(String.format("%s [%s]\n",entry.getKey(),tryGetFactionName(entry.getValue().getOwner())));
        }
        return b.toString();
    }

//getter and setter

    //todo how to define a position if the stronghold is not a starsytem anymore?
    public Vector3i getStellarPos() {
        return stellarPos;
    }

    public void setStellarPos(Vector3i stellarPos) {
        this.stellarPos = stellarPos;
        setSynchFlag(true);
    }

    public int getOwner() {
        return owner;
    }

    protected void setOwner(int owner) {
        this.owner = owner;
        setDefensePoints(StrongholdController.hpRange[0]); //changing ownership resets the stronghold.
        countOwnedBySysOwner();
        setSynchFlag(true);
    }

    public int getDefensePoints() {
        return hp;
    }

    protected void setDefensePoints(int hp) {
        this.hp = hp;
        setSynchFlag(true);
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

    //just for debugging
    private static Vector3i[] points = {
            new Vector3i(2,2,2),
            new Vector3i(VoidSystem.SYSTEM_SIZE-2,VoidSystem.SYSTEM_SIZE-2,VoidSystem.SYSTEM_SIZE-2)};

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


}
