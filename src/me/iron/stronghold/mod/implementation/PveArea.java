package me.iron.stronghold.mod.implementation;

import api.DebugFile;
import com.bulletphysics.linearmath.Transform;
import libpackage.drawer.MapDrawer;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.AbstractMapDrawer;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.MapLine;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.view.effects.ConstantIndication;
import org.schema.game.client.view.effects.Indication;
import org.schema.game.common.controller.Ship;

import javax.vecmath.Vector4f;
import java.util.LinkedList;

public class PveArea extends StellarControllableArea{
    public PveArea() {
        super();
    };
    public PveArea(Vector3i start, Vector3i end, String name) {
        super(start, end, name);
    }

    @Override
    protected void onFirstUpdatePersistent() {
        super.onFirstUpdatePersistent();

        //add effects: voidshield and info message on enter/leave
        boolean hasShield = false;
        boolean hasWelcome = false;
        for (SendableUpdateable c: getChildren()) {
            if (!hasShield && c instanceof PveShield)
                hasShield=true;
            if (!hasWelcome && c instanceof WelcomeMessageEffect) {
                hasWelcome = true;
            }
        }
        if (!hasShield)
            addChildObject(new PveShield("PVE-Shield"));
        if (!hasWelcome) {
            WelcomeMessageEffect hi = new WelcomeMessageEffect(this);
            hi.setGeneratorEntry("You have entered the PVE area '"+this.getName()+"'. Player-to-player combat is strictly forbidden.");

            hi.setGeneratorLeave("You are no longer in the PVE area '"+this.getName()+"'.");
            addChildObject(hi);
            //LongRangeScannerEffect radar = new LongRangeScannerEffect("LongRangeScanner for "+getName());
            //radar.setActive(true);
            //addChildObject(radar);
        }
    }

    @Override
    public void onAreaEntered(StellarControllableArea area, Vector3i enteredSector, Ship object) {
        super.onAreaEntered(area, enteredSector, object);
        DebugFile.log("[PVE AREA ENTERED]"+object.getUniqueIdentifier()+"["+object.getFactionId()+"]");
    }

    @Override
    public void onAreaLeft(StellarControllableArea area, Vector3i leftSector, Ship object) {
        super.onAreaLeft(area, leftSector, object);
        DebugFile.log("[PVE AREA LEFT]"+object.getUniqueIdentifier()+"["+object.getFactionId()+"]");
    }

    @Override
    public boolean canBeConquered() {
        return false;
    }

    @Override
    public boolean isVisibleOnMap() {
        return true;
    }

    @Override
    public LinkedList<Indication> getIndications() {
        LinkedList<Indication> out = new LinkedList<>();
        Transform t = new Transform(); t.setIdentity(); t.origin.set(AbstractMapDrawer.posFromSector(getDimensionsStart().toVector3f(),true));
        out.add(new ConstantIndication(t,"PVE AREA"));
        return out;
    }

    @Override
    public LinkedList<MapLine> getLines() {
        return AbstractMapDrawer.outlineSquare(getDimensionsStart().toVector3f(),getDimensionsEnd().toVector3f(),new Vector4f(0.52f,0,0.52f,1));
    }
}
