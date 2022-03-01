package me.iron.stronghold.mod.effects.map;

import libpackage.drawer.MapDrawer;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.implementation.RadarContact;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.schine.graphicsengine.forms.Sprite;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class RadarContactMarker extends SimpleMapMarker {
    private int amount;
    public RadarContactMarker(Sprite sprite, int subSpriteIndex, Vector3f mapPos, int amount) {
        super(sprite, subSpriteIndex, new Vector4f(1,1,1,1), mapPos);
        size =getScale();
        this.amount =amount;
    }

    public RadarContactMarker(RadarContact contact, Sprite sprite, int subSpriteIndex, Vector4f color) {
        super(sprite, subSpriteIndex, color, MapDrawer.posFromSector(contact.getSector(),true));
    }
    public void setSize(float size) {
        this.size = (0.5f+1f*(amount/4f))*size;
    }
    private float size;
    @Override
    public void preDraw(GameMapDrawer drawer) {
        super.preDraw(drawer);
        float point = (-1000+System.currentTimeMillis()%2000)/1000f;
        float amplitude = 0.5f;
        setScale(size *((amplitude*(float) Math.abs(Math.sin(point)))+amplitude));
    }
}
