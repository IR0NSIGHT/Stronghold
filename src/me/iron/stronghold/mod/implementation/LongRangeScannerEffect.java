package me.iron.stronghold.mod.implementation;

import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.map.FactionRelation;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapDrawable;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapLine;
import me.iron.stronghold.mod.effects.map.RadarContactMarker;
import me.iron.stronghold.mod.effects.map.RadarMapDrawer;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.ActivateableAreaEffect;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.effects.Indication;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.Timer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class LongRangeScannerEffect extends ActivateableAreaEffect implements MapDrawable {
    private ArrayList<RadarContact> signals = new ArrayList<>(20);
    public static long cooldown = 60*1000;
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
        super.onActiveUpdate(timer);
        //collect all player ships and all fleetships in area
        assert signals != null;
        if (isServer())
            if (getParent() instanceof StellarControllableArea && timer.currentTime>cooldown+lastScan) {
                signals.clear();
                signals.addAll(getObjectsInArea((StellarControllableArea) getParent(),timer.currentTime));
                for (RadarContact o: signals) {
                    //System.out.println(o);
                }

                lastScan = timer.currentTime;
                requestSynchToClient(this);
            }
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
        }
    }

    @Override
    public LinkedList<SimpleMapMarker> getMarkers() {
        //TODO add ping effect on synch.
        LinkedList<SimpleMapMarker> markers = new LinkedList<>();
        for (RadarContact c: signals) {
            //update the relation
            c.setRelationWith(GameClientState.instance.getPlayer().getFactionId());
            markers.add(new RadarContactMarker(
                    RadarMapDrawer.radarSprite,
                    RadarMapDrawer.getSpriteIndexFromRelation(c.getRelation()),
                    RadarMapDrawer.getColorFromRelation(c.getRelation()),
                    c
            ));
        }
        return markers;
    }

    @Override
    public LinkedList<MapLine> getLines() {
        return new LinkedList<>();
    }

    @Override
    public LinkedList<Indication> getIndications() {
        return new LinkedList<>();
    }

    private boolean isAlliedOrOwn(int faction) {
        FactionRelation r = FactionRelation.getRelation(faction,((AbstractControllableArea)getParent()).getOwnerFaction(),GameClientState.instance.getFactionManager());
        return r.equals(FactionRelation.OWN)|| r.equals(FactionRelation.ALLY);
    }
    //TODO allow mapdrawables to decide when to update the markers.
    @Override
    public boolean isVisibleOnMap() {
        return isAlliedOrOwn(GameClientState.instance.getPlayer().getFactionId());
    }
}
