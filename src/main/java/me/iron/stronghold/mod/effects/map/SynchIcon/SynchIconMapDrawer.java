package me.iron.stronghold.mod.effects.map.SynchIcon;

import api.mod.StarMod;
import com.bulletphysics.linearmath.Transform;
import libpackage.drawer.MapDrawer;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.effects.map.AreaMapDrawer;
import me.iron.stronghold.mod.effects.map.MapUtilLib_NEW.AbstractMapDrawer;
import org.schema.game.client.view.effects.ConstantIndication;
import org.schema.game.client.view.effects.Indication;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.schine.graphicsengine.forms.Sprite;

import java.util.Collection;
import java.util.LinkedList;

public class SynchIconMapDrawer extends MapDrawer {
    public SynchIconMapDrawer(StarMod mod) {
        super(mod);
    }

    private LinkedList<Indication> indications = new LinkedList<>();
    /**
     * set and draw these items. overwrites all existing items.
     * @param icons
     */
    protected void setIcons(Collection<SynchMapIcon> icons) {
        this.clearMarkers();
        this.indications.clear();
        for (SynchMapIcon icon: icons) {
            SimpleMapMarker marker = new SimpleMapMarker(
                    spriteByIconId(icon.iconIndex),
                    subSpriteByIconId(icon.iconIndex),
                    icon.color,
                    MapDrawer.posFromSector(icon.sector,true)
            );
            marker.setScale(icon.scale);

            this.addMarker(marker);

            if (icon.displayMessage == null)
                continue;
            Transform t = new Transform(); t.setIdentity(); t.origin.set(AbstractMapDrawer.posFromSector(icon.sector,true));
            indications.add(new ConstantIndication(t,icon.displayMessage));
        }
        this.rebuildInternalList();
    }

    Sprite spriteByIconId(int iconId) {
        return AreaMapDrawer.areaSprite;
    }

    @Override
    public void galaxy_DrawLines(GameMapDrawer gameMapDrawer) {
        super.galaxy_DrawLines(gameMapDrawer);
        HudIndicatorOverlay.toDrawMapTexts.addAll(indications);
    }

    int subSpriteByIconId(int iconId) {
        return 0;
    }
}
