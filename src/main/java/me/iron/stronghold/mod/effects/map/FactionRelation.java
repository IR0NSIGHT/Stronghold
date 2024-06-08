package me.iron.stronghold.mod.effects.map;

import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.GameServerState;

import javax.management.relation.Relation;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 02.03.2022
 * TIME: 13:13
 */
public enum FactionRelation {
    ALLY,
    NEUTRAL,
    OWN,
    ENEMY,
    UNKNOWN;
    public static FactionRelation getRelation(int ownF, int factionid, FactionManager fm) {
        assert fm != null;
        FactionRelation relation = null;
        if (ownF == factionid && ownF != 0) {
            relation = FactionRelation.OWN;
        }else if(fm.isFriend(ownF, factionid)) {
            relation = FactionRelation.ALLY;
        } else if (fm.isEnemy(ownF,factionid)) {
             relation = FactionRelation.ENEMY;
        } else {
            relation = FactionRelation.UNKNOWN;
        }
        return relation;
    }
}
