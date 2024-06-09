package me.iron.stronghold.mod.warpgate;

import api.mod.config.PersistentObjectUtil;
import me.iron.stronghold.mod.ModMain;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SpaceStation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

public class WarpgateContainer implements Serializable {
    public ArrayList<SaveableGate> gates = new ArrayList<>();

    public static WarpgateContainer load() {
        try {
            java.util.ArrayList<Object> list = PersistentObjectUtil.getObjects(ModMain.instance.getSkeleton(), WarpgateContainer.class);
            if (!list.isEmpty())
                return (WarpgateContainer) list.get(0);
        } catch (Exception ex) {
            ModMain.LogError("WarpGateContainer loading failed, instantiate new one", ex);
        }
        return new WarpgateContainer();
    }

    public void add(SaveableGate gate) {
        remove(gate.UID);
        gates.add(gate);
    }

    public void remove(SaveableGate gate) {
        gates.remove(gate);
    }

    public void remove(String uid) {
        LinkedList<SaveableGate> toKill = new LinkedList<>();
        for (SaveableGate gate : gates) {
            if (Objects.equals(gate.UID, uid))
                toKill.add(gate);
        }
        for (SaveableGate gate : toKill)
            remove(gate);
    }

    public void save() {
        PersistentObjectUtil.removeAllObjects(ModMain.instance.getSkeleton(), WarpgateContainer.class);
        PersistentObjectUtil.addObject(ModMain.instance.getSkeleton(), this);
        PersistentObjectUtil.save(ModMain.instance.getSkeleton());
    }

    public static class SaveableGate {
        public Vector3i position;
        public Vector3i destination;
        public String UID;
        public String realName;
        public int factionId;

        public SaveableGate(SpaceStation station, Vector3i destination) {
            this.position = station.getSector(new Vector3i());
            this.destination = destination;
            this.UID = station.getUniqueIdentifier();
            this.realName = station.getRealName();
            this.factionId = station.getFactionId();
        }

        @Override
        public String toString() {
            return "SaveableGate{" +
                    "position=" + position +
                    ", destination=" + destination +
                    ", UID='" + UID + '\'' +
                    ", realName='" + realName + '\'' +
                    ", factionId=" + factionId +
                    '}';
        }
    }
}
