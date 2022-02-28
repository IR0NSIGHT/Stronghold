package me.iron.stronghold.mod.effects.map;

import api.mod.StarMod;
import libpackage.drawer.MapDrawer;
import libpackage.drawer.SpriteLoader;
import libpackage.markers.SimpleMapMarker;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.schine.graphicsengine.forms.Sprite;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

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
        if (marker == null) {
            Vector3f mappos = MapDrawer.posFromSector(new Vector3i(2,2,2),true);
            marker = new SimpleMapMarker(radarSprite,0,new Vector4f(1,1,1,1), mappos);
            addMarker(marker);
            rebuildInternalList();
        }

        super.galaxy_DrawSprites(gameMapDrawer);
    }

    public void loadSprites(StarMod mod) {
        String folder = "me/iron/stronghold/mod/res/"; //starting at src to package.

        //load sprite
        SpriteLoader msl = new SpriteLoader(folder,"radar_dot.png",128,128,1,1);
        msl.loadSprite(mod);
        radarSprite = msl.getSprite();
    }
}
