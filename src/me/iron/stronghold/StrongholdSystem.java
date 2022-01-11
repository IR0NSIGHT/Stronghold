package me.iron.stronghold;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.google.common.base.Verify;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 10.01.2022
 * TIME: 19:36
 * container for stellar system that acts as a stronghold.
 */
public class StrongholdSystem extends SimpleSerializerWrapper {
    private Vector3i stellarPos;
    private int owner;
    private int hp = SystemController.hpRange[0]; //healthpoints
    private boolean synchFlag;

    private HashMap<Vector3i, Integer> stronghold_owners;
    private int ownedBySysOwner;
    private int ownedByNoone;
    protected long lastUpdate;
    private boolean flagDelete;
    public StrongholdSystem(Vector3i system) {
        init();
    }

    public StrongholdSystem(Vector3i stellarPos, int owner) {
        this.stellarPos = stellarPos;
        this.owner = owner;
    }

    public void init() {
        setPoints(StrongholdSystem.generatePoints(this.stellarPos));
    }

    public StrongholdSystem(PacketReadBuffer buffer) {
        onDeserialize(buffer);
    }

    public void setPoints(Vector3i[] pos) {
        if (stronghold_owners == null)
            stronghold_owners = new HashMap<>(pos.length);
        ownedByNoone = pos.length;
        stronghold_owners.clear();
        for (Vector3i p: pos) {
            stronghold_owners.put(p,0);
        }
        setSynchFlag();
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

        updateLoadedPoints(stronghold_owners);
        int diff = Math.max (1,(int) (timeUnits-lastUpdate));
        adjustPoints(ownedByNoone, ownedBySysOwner,stronghold_owners.size(),diff);

        lastUpdate = timeUnits;
        if (hp<=SystemController.hpRange[0]) {
            //hp are minimal after update -> wont change until a strongpoint changes owner
            setFlagDelete(true);
        }
    }

    public void setPointOwner(int faction, Vector3i pos) {
        if (stronghold_owners.containsKey(pos)) {
            //adjust own counter
        //   if (faction==owner) {
        //       ownedBySysOwner++;
        //   }
        //   else if (stronghold_owners.get(pos)==owner) {
        //       ownedBySysOwner--;
        //   }
        //   //adjust neutral counter
        //   if (faction == 0)
        //       ownedByNoone++;
        //   else if (stronghold_owners.get(pos)==0)
        //       ownedByNoone--;
            stronghold_owners.put(pos,faction);
            countOwnedBySysOwner(); //TODO use above one, dont bruteforce.
            setSynchFlag();
        }
    }

    public boolean isProtected() {
        return (float)hp/SystemController.hpRange[1]>=0.5f;
    }

    private void updateLoadedPoints(HashMap<Vector3i, Integer> point_to_owner) {
        if (GameServerState.instance==null)
            return;

        Sector point;
        for (Map.Entry<Vector3i,Integer> e: point_to_owner.entrySet()) {
            if (GameServerState.instance.getUniverse().isSectorLoaded(e.getKey())) {
                try {
                    point = GameServerState.instance.getUniverse().getSector(e.getKey());
                    updatePoint(point);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * update this sector
     * @param sector
     */
    public void updatePoint(Sector sector) {
        assert stronghold_owners.containsKey(sector.pos); //sector is a strongpoint
        boolean isStation, notHb;
        for (SimpleTransformableSendableObject s: sector.getEntities()) {
            isStation = s.getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION);
            notHb = !s.isHomeBase();
            if (isStation && notHb) {
                setPointOwner(s.getFactionId(), sector.pos);
                return;
            }
        }
        setPointOwner(0,sector.pos);
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
        hp += diff*timeUnits*SystemController.changePerTimeUnit; //-2 diff x 5 timeUnits = -10 points
        hp = Math.min(SystemController.hpRange[2],Math.max(SystemController.hpRange[0],hp));
        System.out.println(String.format("strongholds factor:%s, timeUnits: %s, totalChange: %s",diff,timeUnits,diff*timeUnits));
        setSynchFlag();
    }

    /**
     * updates internal counters
     * @return
     */
    private int countOwnedBySysOwner() {
        ownedBySysOwner = 0;
        ownedByNoone = 0;
        for (Map.Entry<Vector3i,Integer> e: stronghold_owners.entrySet()) {
            if (e.getValue()==owner)
                ownedBySysOwner++;
            else if (e.getValue()==0)
                ownedByNoone++;
        }
        return ownedBySysOwner;
    }

//serialization

    @Override
    public void onDeserialize(PacketReadBuffer buffer) {
        try {
            owner = buffer.readInt();
            stellarPos = buffer.readVector();
            hp = buffer.readInt();
            int strongholds = buffer.readInt();
            stronghold_owners = new HashMap<>(strongholds);
            for (int i = 0; i < strongholds; i++) {
                stronghold_owners.put(buffer.readVector(), buffer.readInt());
            }
            flagDelete = buffer.readBoolean();
            countOwnedBySysOwner();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialize(PacketWriteBuffer buffer) {
        try {
            buffer.writeInt(owner);
            buffer.writeVector(stellarPos);
            buffer.writeInt(hp);
            buffer.writeInt(stronghold_owners.size());
            for (Map.Entry<Vector3i,Integer> entry: stronghold_owners.entrySet()) {
                buffer.writeVector(entry.getKey());
                buffer.writeInt(entry.getValue());
            }
            buffer.writeBoolean(flagDelete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //to string stuff

    @Override
    public String toString() {
        return String.format("owner: %s, pos:%s, hp: %s\n" +
                "own: %s, ene: %s, neut: %s, all: %s\n"+
                "protected: %s, updated: %s,\n" +
                "strongholds: %s",owner,stellarPos,hp,ownedBySysOwner,(stronghold_owners.size()-ownedByNoone-ownedBySysOwner),ownedByNoone,stronghold_owners.size(),isProtected(),lastUpdate,strongholdsToString());
    }

    public static String tryGetFactionName(int faction) {
        Faction owners = GameServerState.instance.getFactionManager().getFaction(faction);
        String name;
        if (owners!=null) {
            name ="'"+ owners.getName()+"'";
        } else
            name = "neutral";
        return name;
    }

    public String strongholdsToString() {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<Vector3i,Integer> entry: stronghold_owners.entrySet()) {
            b.append(String.format("%s [%s]\n",entry.getKey(),tryGetFactionName(entry.getValue())));
        }
        return b.toString();
    }

//getter and setter

    public Vector3i getStellarPos() {
        return stellarPos;
    }

    public void setStellarPos(Vector3i stellarPos) {
        this.stellarPos = stellarPos;
        setSynchFlag();
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
        setHp(SystemController.hpRange[0]); //changing ownership resets the stronghold.
        countOwnedBySysOwner();
        setSynchFlag();
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
        setSynchFlag();
    }

//flags for synch and deletion after sync

    public boolean isSynchFlag() {
        return synchFlag;
    }

    /**
     * synch this system to all clients on the next update
     */
    public void setSynchFlag() {
        this.synchFlag = true;
    }

    public boolean isFlagDelete() {
        return flagDelete;
    }

    /**
     * sets a flag so the client deletes this system after a synch
     */
    public void setFlagDelete(boolean flagDelete) {
        this.flagDelete = flagDelete;
    }

    //just for debugging
    private static Vector3i[] points = {
            new Vector3i(2,2,2),
            new Vector3i(VoidSystem.SYSTEM_SIZE-2,VoidSystem.SYSTEM_SIZE-2,VoidSystem.SYSTEM_SIZE-2)};

    //TODO write procedural method
    public static boolean isStrongpoint(Vector3i sector) {
        Vector3i system = new Vector3i(sector);
        sector.x = sector.x%VoidSystem.SYSTEM_SIZE;
        sector.y = sector.y%VoidSystem.SYSTEM_SIZE;
        sector.z = sector.z%VoidSystem.SYSTEM_SIZE;

        StrongholdSystem s = SystemController.getInstance().getSystem(system);
        if (s!=null) {
            return s.stronghold_owners.containsKey(sector);
        } else {
            Vector3i[] points = generatePoints(system);
            for (Vector3i p: points) {
                if (p.equals(sector))
                    return true;
            }
            return false;
        }
    }

    public static Vector3i[] generatePoints(Vector3i system) {
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
