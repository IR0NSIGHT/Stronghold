package me.iron.stronghold.mod.effects.map.MapUtilLib_NEW;

import api.mod.StarMod;
import libpackage.drawer.MapDrawer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gamemap.GameMapDrawer;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 03.03.2022
 * TIME: 17:36
 */
public class AbstractMapDrawer extends MapDrawer {
    private static final float sectorScale = 6.25F;
    private static final Vector3f halfSectorOffset = new Vector3f(3.125F, 3.125F, 3.125F);

    public AbstractMapDrawer(StarMod mod) {
        super(mod);
        //TODO add button to enable/disable this drawer to the mappanel
       // GameClientState.instance.getWorldDrawer().getGuiDrawer().getPlayerPanel().map
    }
    private Vector3i sector = new Vector3i();
    private Vector3i system = new Vector3i();

    protected boolean sectorChanged;
    protected boolean systemChanged;
    //TODO galaxy changed

    //first called event in GameMapDrawer.draw()
    @Override
    public void galaxy_PreDraw(GameMapDrawer gameMapDrawer) {
        sectorChanged = false;
        systemChanged = false;
        if (GameClientState.instance.getPlayer()!=null && !GameClientState.instance.getPlayer().getCurrentSector().equals(sector)) {
            //sector has changed.
            onSectorChanged(sector,GameClientState.instance.getPlayer().getCurrentSector());
            if (!GameClientState.instance.getPlayer().getCurrentSystem().equals(system)) {
                onSystemChanged(system,GameClientState.instance.getPlayer().getCurrentSystem());
            }
        }
        super.galaxy_PreDraw(gameMapDrawer);
    }

    /**
     * called before draw
     * @param oldS
     * @param newS
     */
    protected void onSectorChanged(Vector3i oldS, Vector3i newS) {
        sectorChanged = true;
        sector.set(newS);
    }

    /**
     * called before draw
     * @param oldS
     * @param newS
     */
    protected void onSystemChanged(Vector3i oldS, Vector3i newS) {
        systemChanged = true;
        system.set(newS);
    }

    public static Vector3f posFromSector(Vector3f sector, boolean isSprite) {
        if (isSprite) {
            sector.add(new Vector3f(-8.0F, -8.0F, -8.0F));
        }

        sector.scale(6.25F);
        sector.add(halfSectorOffset);
        return sector;
    }


    public static LinkedList<MapLine> outlineSquare(Vector3f sectorStart, Vector3f sectorEnd, Vector4f color) {
        LinkedList<MapLine> out = new LinkedList<>();
        Vector3f from = AbstractMapDrawer.posFromSector(sectorStart,false), to = AbstractMapDrawer.posFromSector(sectorEnd,false);
        Vector3f diff = new Vector3f(to); diff.sub(from); Vector3f temp = new Vector3f();

        //front square
        out.add(new MapLine(
                new Vector3f(from),
                new Vector3f(from.x,to.y,from.z), //00-01
                color));
        out.add(new MapLine(
                new Vector3f(from),
                new Vector3f(to.x,from.y,from.z), //00-10
                color));
        out.add(new MapLine(
                new Vector3f(from.x,to.y,from.z),
                new Vector3f(to.x,to.y,from.z), //01-11
                color));
        out.add(new MapLine(
                new Vector3f(to.x,from.y,from.z),
                new Vector3f(to.x,to.y,from.z), //10-11
                color));

        //back square
        out.add(new MapLine(
                new Vector3f(from.x,from.y,to.z),
                new Vector3f(from.x,to.y,to.z), //00-01
                color));
        out.add(new MapLine(
                new Vector3f(from.x,from.y,to.z),
                new Vector3f(to.x,from.y,to.z), //00-10
                color));
        out.add(new MapLine(
                new Vector3f(from.x,to.y,to.z),
                new Vector3f(to.x,to.y,to.z), //01-11
                color));
        out.add(new MapLine(
                new Vector3f(to.x,from.y,to.z),
                new Vector3f(to.x,to.y,to.z), //10-11
                color));

        //z0 to z1 connection
        out.add(new MapLine(
                new Vector3f(from.x,from.y,from.z),//000
                new Vector3f(from.x,from.y,to.z),
                color));
        out.add(new MapLine(
                new Vector3f(to.x,to.y,from.z),//110
                new Vector3f(to.x,to.y,to.z),
                color));
        out.add(new MapLine(
                new Vector3f(from.x,to.y,from.z),//010
                new Vector3f(from.x,to.y,to.z),
                color));
        out.add(new MapLine(
                new Vector3f(to.x,from.y,from.z),//100
                new Vector3f(to.x,from.y,to.z),
                color));

        return out;
    }

    public static String getFactionName(int fId) {
        return GameClientState.instance.getFactionManager().getFactionName(fId);
    }
}
