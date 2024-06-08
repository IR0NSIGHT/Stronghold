package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.effects.map.FactionRelation;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.GameServerState;

import java.io.Serializable;

public class RadarContact implements Serializable {
    Vector3i sector;
    int amount; //of contacts in sector
    long timestamp;
    int factionid;
    FactionRelation relation;

    public RadarContact(int factionid, int amount, Vector3i sector,long timestamp) {
        this.sector = sector;
        this.timestamp = timestamp;
        this.factionid = factionid;
        this.amount = amount;
    }
    public Vector3i getSector() {
        return sector;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getFactionid() {
        return factionid;
    }

    public int getAmount() {
        return amount;
    }

    public FactionRelation getRelation() {
        return relation;
    }

    @Override
    public String toString() {
        return "RadarContact{" +
                "sector=" + sector +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", factionid=" + factionid +
                ", relation=" + relation +
                '}';
    }

    public void setRelationWith(int ownF) {
        FactionManager fm = null;
        if (GameClientState.instance != null) {
            fm = GameClientState.instance.getFactionManager();
        } else if (GameServerState.instance != null) {
            GameServerState.instance.getFactionManager();
        }
        assert fm != null;
        if (ownF == factionid) {
            relation = FactionRelation.OWN;
        }else if(fm.isFriend(ownF, factionid)) {
            relation = FactionRelation.ALLY;
       // } else if (fm.isEnemy(ownF,factionid)) {
       //     relation = FactionRelation.ENEMY;
        } else {
            relation = FactionRelation.UNKNOWN;
        }
        //TODO allow enemy or neutral contacts !!if this faction can see them!! -> own ships in proximity or sth.
        //->search radar shows all signals, engagement radar can scan single ones directly for friend/foe-signature?
    }
}
