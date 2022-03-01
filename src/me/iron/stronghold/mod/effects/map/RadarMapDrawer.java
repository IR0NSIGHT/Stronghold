package me.iron.stronghold.mod.effects.map;

import api.mod.StarMod;
import libpackage.drawer.MapDrawer;
import libpackage.drawer.SpriteLoader;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.implementation.RadarContact;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.schine.graphicsengine.forms.Sprite;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RadarMapDrawer extends MapDrawer {
    private Sprite radarSprite;
    private SimpleMapMarker marker;
    public RadarMapDrawer(StarMod mod) {
        super(mod);
        //loadSprites(mod);
        //rebuildInternalList();
    }

    @Override
    public void galaxy_DrawSprites(GameMapDrawer gameMapDrawer) {
        super.galaxy_DrawSprites(gameMapDrawer);
        generateMarkers(); //for next frame

    }

    private void generateMarkers() {
        clearMarkers();
        synchronized (sectors_contacts) {
            for (Map.Entry<Vector3i,Integer> sector: sectors_contacts.entrySet()) {
                RadarContactMarker m = new RadarContactMarker(radarSprite, 0,posFromSector(sector.getKey(),true), sector.getValue());
                m.setSize(0.015f);
                addMarker(m);
            }
        }
    }

    public void loadSprites(StarMod mod) {
        String folder = "me/iron/stronghold/mod/res/"; //starting at src to package.

        //load sprite
        SpriteLoader msl = new SpriteLoader(folder,"radar_dot.png",512,512,2,2);
        msl.loadSprite(mod);
        radarSprite = msl.getSprite();
    }
    private HashMap<Vector3i,Integer> sectors_contacts = new HashMap<>(20);
    public void addRadarContact(RadarContact radarContact) {
        synchronized (sectors_contacts) {
            int amount = 0;
            if (sectors_contacts.containsKey(radarContact.getSector())) {
                amount = sectors_contacts.get(radarContact.getSector());
            }
            sectors_contacts.put(radarContact.getSector(),amount+1);
        }
    }

    public void clearRadarContacts() {
        clearMarkers();
        synchronized (sectors_contacts) {
            sectors_contacts.clear();
        }
    }
}
