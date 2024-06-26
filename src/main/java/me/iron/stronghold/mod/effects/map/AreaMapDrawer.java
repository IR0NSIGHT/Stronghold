package me.iron.stronghold.mod.effects.map;

import api.mod.StarMod;
import libpackage.drawer.SpriteLoader;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.ModMain;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.AbstractMapDrawer;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapDrawable;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapLine;
import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.IAreaEvent;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import me.iron.stronghold.mod.implementation.StellarControllableArea;
import org.lwjgl.opengl.GL11;
import org.schema.common.util.linAlg.Vector3i;

import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.effects.Indication;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.core.ResourceException;
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
public class AreaMapDrawer extends AbstractMapDrawer implements IAreaEvent {
    public static Sprite areaSprite;

    private LinkedList<MapLine> lines = new LinkedList<>(); //only access in graphics thread!
    private LinkedList<Indication> indications = new LinkedList<>();
    private boolean updateFlag;
    public AreaMapDrawer(StarMod mod) throws ResourceException {
        super(mod);
        String folder = "icons/"; //starting at src to package.

        //load sprite
        SpriteLoader msl = new SpriteLoader(folder,"area_map.png",512,512,2,2);
        msl.loadSprite(mod);
        areaSprite = msl.getSprite();
        if (areaSprite == null)
            throw new ResourceException("could not load sprite");
        ModMain.areaManager.addListener(this);
    }

    @Override
    public void system_PreDraw(GameMapDrawer gameMapDrawer, Vector3i vector3i, boolean b) {
        super.system_PreDraw(gameMapDrawer, vector3i, b);
        updateAreas(gameMapDrawer);
    }

    @Override
    public void galaxy_DrawLines(GameMapDrawer gameMapDrawer) {
        super.galaxy_DrawLines(gameMapDrawer);
        for (MapLine l: lines) {
            drawLine(l.getFrom(), l.getTo(), l.getColor());
        }
        HudIndicatorOverlay.toDrawMapTexts.addAll(indications);
    }

    private synchronized void setUpdateFlag() {
        updateFlag = true;
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

    private synchronized void updateAreas(GameMapDrawer gameMapDrawer) {

        //get all areas the player is currently in //TODO is area test thread safe?
        if (!(ModMain.areaManager != null && GameClientState.instance.getPlayer() != null && updateFlag))
            return;

        updateFlag = false;
        lines.clear();
        clearMarkers();
        indications.clear();
        //draw outlines for ALL areas
        for (SendableUpdateable su: ModMain.areaManager.getAllObjects()) {

            if (su instanceof MapDrawable && ((MapDrawable) su).isVisibleOnMap()) {
                LinkedList<MapLine> linesS = ((MapDrawable) su).getLines();
                if (su instanceof StellarControllableArea && !((StellarControllableArea) su).isSectorInArea(gameMapDrawer.getGameMapPosition().get(new Vector3i()))) {
                    //not in area, tone down brightness
                    for (MapLine l: linesS) {
                        l.getColor().w = 0.2f;
                    }
                }
                lines.addAll(linesS);
            }
        }
        //draw indications and markers for the ones where camera is at
        Vector3i currentPos = gameMapDrawer.getGameMapPosition().get(new Vector3i());
        for (StellarControllableArea sc : ModMain.areaManager.getAreaFromSector(currentPos)) {
            if (!sc.isVisibleOnMap())
                continue;
            for(SimpleMapMarker m: ((MapDrawable) sc).getMarkers()) {
                addMarker(m);
            }
            indications.addAll(((MapDrawable) sc).getIndications());
        }
    }

    @Override
    protected void onSectorChanged(Vector3i oldS, Vector3i newS) {
        super.onSectorChanged(oldS, newS);
       setUpdateFlag();
    }

    @Override
    protected void onCameraPosChanged(Vector3i oldS, Vector3i newS) {
        super.onCameraPosChanged(oldS, newS);
       setUpdateFlag();
    }

    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {

    }

    @Override
    public void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue) {

    }

    @Override
    public void onUpdate(AbstractControllableArea area) {
       setUpdateFlag();
    }

    @Override
    public void beforeOverwrite(AbstractControllableArea area) {

    }

    @Override
    public void onOverwrite(AbstractControllableArea area) {
       setUpdateFlag();
    }

    @Override
    public void beforeDestroy(AbstractControllableArea area) {

    }

    @Override
    public void onDestroy(AbstractControllableArea area) {

    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, SendableUpdateable child, boolean removed) {

    }

    @Override
    public void onParentChanged(SendableUpdateable child, AbstractControllableArea parent, boolean removed) {

    }

    @Override
    public void onAttacked(long time, AbstractControllableArea area, int attackerFaction, Vector3i position) {

    }
}

