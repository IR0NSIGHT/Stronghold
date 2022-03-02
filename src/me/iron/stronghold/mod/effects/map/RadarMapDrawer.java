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
                0,//Math.min(radarContact.getAmount(),3),
                radarContact
        ));
    }

    public void clearRadarContacts() {
        clearMarkers();
    }
}
