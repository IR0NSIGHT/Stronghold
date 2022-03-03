package me.iron.stronghold.mod.effects.map.MapUtilLib_NEW;

import libpackage.drawer.MapDrawer;
import org.schema.common.util.linAlg.Vector3i;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 03.03.2022
 * TIME: 19:37
 */
public class MapLine {
    private Vector3f from;
    private Vector3f to;
    private Vector4f color;

    public Vector3f getFrom() {
        return from;
    }

    public Vector3f getTo() {
        return to;
    }

    public Vector4f getColor() {
        return color;
    }

    public MapLine(Vector3i fromSec, Vector3i toSec, Vector4f color) {
        from = MapDrawer.posFromSector(fromSec, false);
        to = MapDrawer.posFromSector(toSec, false);
        this.color = color;
    }

    public MapLine(Vector3f from, Vector3f to, Vector4f color) {
        this.from = from;
        this.to = to;
        this.color = color;
    }
}
