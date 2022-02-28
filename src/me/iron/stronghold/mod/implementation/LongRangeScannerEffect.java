package me.iron.stronghold.mod.implementation;

import libpackage.drawer.MapDrawer;
import me.iron.stronghold.mod.framework.ActivateableAreaEffect;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.lwjgl.Sys;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class LongRangeScannerEffect extends ActivateableAreaEffect {
    private ArrayList<RadarContact> signals = new ArrayList<>(20);
    private long cooldown = 10*1000;
    private long lastScan;

    //clientside stuff


    public LongRangeScannerEffect() {
        super("LongRangeScanner");
    }

    @Override
    protected void onActiveUpdate(Timer timer) {
        super.onActiveUpdate(timer);
        //collect all player ships and all fleetships in area
        assert signals != null;
        if (isServer())
            if (getParent() instanceof StellarControllableArea && timer.currentTime>cooldown+lastScan) {
                signals.clear();
                signals.addAll(getObjectsInArea((StellarControllableArea) getParent(),timer.currentTime));
                for (RadarContact o: signals) {
                    System.out.println(o);
                }

                lastScan = timer.currentTime;
                requestSynchToClient(this);
            }
        if (isClient()) {

        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private Collection<RadarContact> getObjectsInArea(StellarControllableArea area, long currentTime) {
        Connection c = GameServerState.instance.getDatabaseIndex()._getConnection();
        Vector3i start = area.getDimensionsStart(), end = area.getDimensionsEnd();
        LinkedList<RadarContact> contacts = new LinkedList<>();
        try {
            String q = String.format("SELECT e.uid, e.name, e.type, e.faction, X,Y,Z \nFROM ENTITIES as e\n WHERE" +
                    " e.docked_root = -1 and " + //not docked to anything
                    " e.X >= %s AND e.X <= %s and" +
                    " e.Y >= %s AND e.Y <= %s and" +
                    " e.Z >= %s AND e.Z <= %s ",start.x,end.x,start.y,end.y,start.z,end.z);
            //System.out.println(q);
            ResultSet r = c.createStatement().executeQuery(q);
            while(r.next()) {
                RadarContact radarContact = new RadarContact(r.getString(1), //uid
                        r.getString(2), //name
                        SimpleTransformableSendableObject.EntityType.values()[r.getInt(3)], //type
                        r.getInt(4) ,//faction
                        new Vector3i(r.getInt(5),r.getInt(6),r.getInt(7)),
                        currentTime
                );
                contacts.add(radarContact);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    @Override
    public void updateFromObject(SendableUpdateable origin) {
        super.updateFromObject(origin);
        if (origin instanceof LongRangeScannerEffect) {
            signals.clear();
            signals.addAll(((LongRangeScannerEffect) origin).signals);
        }
    }

    static class RadarContact implements Serializable {
        String uid;
        String name;
        Vector3i sector;
        SimpleTransformableSendableObject.EntityType type;
        long timestamp;
        int factionid;

        public RadarContact(String uid, String name, SimpleTransformableSendableObject.EntityType type, int factionid, Vector3i sector, long timestamp) {
            this.uid = uid;
            this.name = name;
            this.sector = sector;
            this.type = type;
            this.timestamp = timestamp;
            this.factionid = factionid;
        }

        public String getUid() {
            return uid;
        }

        public String getName() {
            return name;
        }

        public Vector3i getSector() {
            return sector;
        }

        public SimpleTransformableSendableObject.EntityType getType() {
            return type;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getFactionid() {
            return factionid;
        }

        @Override
        public String toString() {
            return "RadarContact{" +
                    "uid='" + uid + '\'' +
                    ", name='" + name + '\'' +
                    ", sector=" + sector +
                    ", type=" + type +
                    ", timestamp=" + timestamp +
                    ", factionid=" + factionid +
                    '}';
        }
    }
}
