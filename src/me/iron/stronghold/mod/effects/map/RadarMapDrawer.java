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
    }

    public void loadSprites(StarMod mod) {
        String folder = "me/iron/stronghold/mod/res/"; //starting at src to package.

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

    private int getSpriteIndexFromRelation(FactionRelation relation) {
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

    private Vector4f getColorFromRelation(FactionRelation relation) {
        switch (relation) {
            case OWN:
                return new Vector4f(0,1,0.052f,1);
            case ALLY:
                return new Vector4f(0,0.45f,1,1);

            case ENEMY:
                return new Vector4f(1,0f,0,1);

            case NEUTRAL:
                return new Vector4f(0.658f,0.0f,0.57f,1);

            case UNKNOWN:

            default:
                return new Vector4f(1,1,1,1);
        }
    }
}
