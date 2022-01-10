package me.iron.stronghold;

import api.mod.config.SimpleSerializerWrapper;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.VoidSystem;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private long lastUpdate;
    private boolean flagDelete;

    public StrongholdSystem(Vector3i stellarPos, int owner) {
        this.stellarPos = stellarPos;
        this.owner = owner;
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

        int diff = Math.max (1,(int) (timeUnits-lastUpdate));
        adjustPoints(ownedByNoone, ownedBySysOwner,stronghold_owners.size(),diff);

        lastUpdate = timeUnits;
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


    public static void main(String[] args) {
        StrongholdSystem s = new StrongholdSystem(new Vector3i(2,2,2),10001);
        Vector3i[] points = new Vector3i[8];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3i(i,i,i);
        }
        s.setPoints(points);

        for (Vector3i pos: points) {
            s.setPointOwner(10001,pos);
        }
        s.update(1);

        //  System.out.println(s.toString());
        s.update(1400);
        //System.out.println(s.toString());
        s.update(2);
        System.out.println(s.toString());
        System.out.println("is protected: " + s.isProtected());
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


    public boolean isProtected() {
        return (float)hp/SystemController.hpRange[1]>=0.5f;
    }

    @Override
    public void onDeserialize(PacketReadBuffer buffer) {
        try {
            owner = buffer.readInt();
            stellarPos = buffer.readVector();
            hp = buffer.readInt();
            lastUpdate = buffer.readLong();
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
            buffer.writeLong(lastUpdate);
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

    @Override
    public String toString() {
        return String.format("owner: %s, pos:%s, hp: %s\n" +
                "own: %s, ene: %s, neut: %s, all: %s\n"+
                "protected: %s, updated: %s,\n" +
                "strongholds: %s",owner,stellarPos,hp,ownedBySysOwner,(stronghold_owners.size()-ownedByNoone-ownedBySysOwner),ownedByNoone,stronghold_owners.size(),isProtected(),lastUpdate,strongholdsToString());
    }

    public String strongholdsToString() {
        StringBuilder b = new StringBuilder("strongholds:\n");
        for (Map.Entry<Vector3i,Integer> entry: stronghold_owners.entrySet()) {
            b.append(String.format("%s [%s]\n",entry.getKey(),entry.getValue()));
        }
        return b.toString();
    }

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
        setSynchFlag();
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
        setSynchFlag();
    }

    public boolean isSynchFlag() {
        return synchFlag;
    }

    public void setSynchFlag() {
        this.synchFlag = true;
    }

    //just for debugging
    private static Vector3i[] points = {new Vector3i(0,0,0),new Vector3i(1,1,1)};

    //TODO write procedural method
    public static boolean isStrongpoint(Vector3i sector) {
        //get system:
        sector = new Vector3i(sector);
        sector.x = sector.x % VoidSystem.SYSTEM_SIZE;
        sector.y = sector.y % VoidSystem.SYSTEM_SIZE;
        sector.z = sector.z % VoidSystem.SYSTEM_SIZE;
        for (Vector3i pos: points) {
            if (sector.equals(pos))
                return true;
        }
        return false;
    }

    public static Vector3i[] generatePoints(Vector3i system) {
        system = new Vector3i(system);
        system.scale(VoidSystem.SYSTEM_SIZE);
        Vector3i[] points = new Vector3i[]{
                new Vector3i(0,0,0),
                new Vector3i(1,1,1)
        };
        for (Vector3i p: points) {
            p.add(system);
        }
        return points;
    }

    public boolean isFlagDelete() {
        return flagDelete;
    }

    public void setFlagDelete(boolean flagDelete) {
        this.flagDelete = flagDelete;
    }
}
