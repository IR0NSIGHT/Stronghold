package me.iron.stronghold.mod.effects.map;

import api.ModPlayground;
import libpackage.drawer.MapDrawer;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.implementation.LongRangeScannerEffect;
import me.iron.stronghold.mod.implementation.RadarContact;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.schine.graphicsengine.forms.Sprite;
import javax.vecmath.Vector4f;

public class RadarContactMarker extends SimpleMapMarker {
    private float size;
    private RadarContact contact;
    private Vector4f displayColor;
    public RadarContactMarker(Sprite sprite, int subSpriteIndex, Vector4f color, RadarContact contact) {
        super(sprite, subSpriteIndex, new Vector4f(1,1,1,1), MapDrawer.posFromSector(contact.getSector(),true));
        displayColor = color;
        sprite.setBillboard(true);
        sprite.setDepthTest(true);
        sprite.setBlend(true);
        sprite.setFlip(true);
        this.contact = contact;
        size = 0.025f;
        size*=(0.3f+0.1f*Math.min(contact.getAmount(),7));
        if (contact.getFactionid()==1) //roids
            size*=1.5f;
        setScale(size);
    }

    @Override
    public void preDraw(GameMapDrawer drawer) {
        float point = (System.currentTimeMillis()-contact.getTimestamp());
        float lifePercent = 1-(point/ LongRangeScannerEffect.cooldown);
        //pulse at a 1 second frequency
        setScale((float) (size * (0.7f+0.3f*Math.abs(Math.cos(((System.currentTimeMillis()%1000)-500)/500f)))));


        Vector4f colorNew = displayColor; colorNew.w = Math.max(0.2f,lifePercent);
        setColor(colorNew);
        //ModPlayground.broadcastMessage("y val="+lifePercent);
        super.preDraw(drawer);

    }
}
