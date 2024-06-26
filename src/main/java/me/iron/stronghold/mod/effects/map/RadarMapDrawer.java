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
import java.util.Vector;

public class RadarMapDrawer extends MapDrawer {
    public static Sprite radarSprite;
    private SimpleMapMarker marker;
    public RadarMapDrawer(StarMod mod) {
        super(mod);
        //loadSprites(mod);
        //rebuildInternalList();
    }

    @Override
    public void galaxy_DrawSprites(GameMapDrawer gameMapDrawer) {
        super.galaxy_DrawSprites(gameMapDrawer);
    }

    public void loadSprites(StarMod mod) {
        String folder = "icons/"; //starting at src to package.

        //load sprite
        SpriteLoader msl = new SpriteLoader(folder,"radar_dot.png",512,512,2,2);
        msl.loadSprite(mod);
        radarSprite = msl.getSprite();
    }
    public void addRadarContact(RadarContact radarContact) {
        addMarker(new RadarContactMarker(
                radarSprite,
                getSpriteIndexFromRelation(radarContact.getRelation()),
                getColorFromRelation(radarContact.getRelation()),
                radarContact
        ));
    }

    public void clearRadarContacts() {
        clearMarkers();
    }

    public static int getSpriteIndexFromRelation(FactionRelation relation) {
        //assert relation!=null; //TODO fix and assert
        if (relation == null)
            return 0;
        switch (relation) {
            case OWN: return 1;
            case ALLY:
            case NEUTRAL:
                return 3;
            case ENEMY: return 2;
            case UNKNOWN:
            default:
                return 0;
        }
    }

    public static Vector4f getColorFromRelation(FactionRelation relation) {
        if (relation == null)
            return new Vector4f(1,1,1,1);
        switch (relation) {
            case OWN:
                return new Vector4f(0,1,0.052f,1); //green
            case ALLY:
                return new Vector4f(0,0.45f,1,1); //blue

            case ENEMY:
                return new Vector4f(1,0f,0,1); //red

            case NEUTRAL:
                return new Vector4f(0.658f,0.0f,0.57f,1); //purple

            case UNKNOWN:

            default:
                return new Vector4f(1,1,1,1); //white
        }
    }
}
