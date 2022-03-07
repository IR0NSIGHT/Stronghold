package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.framework.ActivateableAreaEffect;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

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
        super();
    }

    public LongRangeScannerEffect(String name) {
        super(name);
    }

    @Override
    protected void onActiveUpdate(Timer timer) {
        cooldown = 3000;
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
                if (isClient()) {
                    drawContacts(signals);
                }
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
            String q = String.format("SELECT faction, count(*), X,Y,Z \nFROM ENTITIES as e\n WHERE" +
                    " e.docked_root = -1 and " + //not docked to anything
                    " e.X >= %s AND e.X <= %s and" +
                    " e.Y >= %s AND e.Y <= %s and" +
                    " e.Z >= %s AND e.Z <= %s " +
                    " group by faction, x, y, z; ",start.x,end.x,start.y,end.y,start.z,end.z);
            ResultSet r = c.createStatement().executeQuery(q);
            while(r.next()) {
                RadarContact radarContact = new RadarContact(
                        r.getInt(1) ,//faction
                        r.getInt(2), //amount
                        new Vector3i(r.getInt(3),r.getInt(4),r.getInt(5)), //sector
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
            if (isClient()) {
                drawContacts(signals);
            }
        }
    }
    private void drawContacts(ArrayList<RadarContact> signal) {
        ModMain.radarMapDrawer.clearRadarContacts();
        for (RadarContact c: signals) {
            ModMain.radarMapDrawer.addRadarContact(c);
            //break;
        }
    }
}
