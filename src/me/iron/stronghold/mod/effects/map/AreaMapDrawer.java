package me.iron.stronghold.mod.effects.map;

import api.mod.StarMod;
import libpackage.drawer.SpriteLoader;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.AbstractMapDrawer;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapDrawable;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapLine;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.lwjgl.opengl.GL11;
import org.schema.common.util.linAlg.Vector3i;

import org.schema.game.client.view.effects.Indication;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.forms.Sprite;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.LinkedList;


/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 03.03.2022
 * TIME: 15:30
 */
public class AreaMapDrawer extends AbstractMapDrawer {
    public static Sprite areaSprite;

    private LinkedList<MapLine> lines = new LinkedList<>(); //only access in graphics thread!
    private LinkedList<Indication> indications = new LinkedList<>();
    public AreaMapDrawer(StarMod mod) {
        super(mod);
        String folder = "me/iron/stronghold/mod/res/"; //starting at src to package.

        //load sprite
        SpriteLoader msl = new SpriteLoader(folder,"area_map.png",512,512,2,2);
        msl.loadSprite(mod);
        areaSprite = msl.getSprite();
    }
    //TODO onSectorChange
    //TODO onSystemChange
    //TODO onGalaxyChange

    @Override
    public void system_PreDraw(GameMapDrawer gameMapDrawer, Vector3i vector3i, boolean b) {
        super.system_PreDraw(gameMapDrawer, vector3i, b);
        updateAreas();
    }

    @Override
    public void galaxy_DrawLines(GameMapDrawer gameMapDrawer) {
        super.galaxy_DrawLines(gameMapDrawer);
        for (MapLine l: lines) {
            drawLine(l.getFrom(), l.getTo(), l.getColor());
        }
        HudIndicatorOverlay.toDrawMapTexts.addAll(indications);
    }

    /**
     * must happen in GL context during a draw listener.
     * @param from mappos
     * @param to mappos
     * @param color color
     */
    private void drawLine(Vector3f from, Vector3f to, Vector4f color) {
        GlUtil.glColor4f(color);
        GL11.glVertex3f(from.x, from.y, from.z);
        GL11.glVertex3f(to.x, to.y, to.z);
    }

    private void updateAreas() {
        //get all areas the player is currently in //TODO is area test thread safe?
        if (ModMain.areaManager == null || !sectorChanged)
            return;
        lines.clear();
        clearMarkers();
        for (SendableUpdateable su: ModMain.areaManager.getAllObjects()) {
            if (su instanceof MapDrawable && ((MapDrawable) su).isVisibleOnMap()) {
                //TODO filter out areas that i am not in/switch to chunk based drawing.
                lines.addAll(((MapDrawable) su).getLines());
                //((MapDrawable) su).getIndications() TODO indications
                for(SimpleMapMarker m: ((MapDrawable) su).getMarkers()) {
                    addMarker(m);
                }
                indications.addAll(((MapDrawable) su).getIndications());
            }
        }
    }





    @Override
    protected void onSectorChanged(Vector3i oldS, Vector3i newS) {
        super.onSectorChanged(oldS, newS);
    }
}

