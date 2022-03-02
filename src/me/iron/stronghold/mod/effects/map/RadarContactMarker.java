package me.iron.stronghold.mod.effects.map;

import api.ModPlayground;
import libpackage.drawer.MapDrawer;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.implementation.RadarContact;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.schine.graphicsengine.forms.Sprite;
import javax.vecmath.Vector4f;

public class RadarContactMarker extends SimpleMapMarker {
    private float size;
    private RadarContact contact;
    public RadarContactMarker(Sprite sprite, int subSpriteIndex, RadarContact contact) {
        super(sprite, subSpriteIndex, new Vector4f(1,1,1,1), MapDrawer.posFromSector(contact.getSector(),true));
        sprite.setBillboard(true);
        sprite.setDepthTest(true);
        sprite.setBlend(true);
        sprite.setFlip(true);
        this.contact = contact;
        size = getScale()/20*contact.getAmount();
        setScale(size);
    }

    @Override
    public void preDraw(GameMapDrawer drawer) {
        float point = (System.currentTimeMillis()-contact.getTimestamp())/1000f;
        float lifePercent = (-1/4f*point+1);
        setScale((float) (size * (0.5f+0.5f*Math.abs(Math.cos(point*2f)))));


        Vector4f colorNew = new Vector4f(1,1,1,1); colorNew.w = lifePercent;
        setColor(colorNew);
        //ModPlayground.broadcastMessage("y val="+lifePercent);
        super.preDraw(drawer);

    }
}
