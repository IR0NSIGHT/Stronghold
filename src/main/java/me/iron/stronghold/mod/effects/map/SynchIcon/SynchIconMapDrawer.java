package me.iron.stronghold.mod.effects.map.SynchIcon;

import api.mod.StarMod;
import libpackage.drawer.MapDrawer;
import libpackage.markers.SimpleMapMarker;
import me.iron.stronghold.mod.effects.map.AreaMapDrawer;
import org.schema.schine.graphicsengine.forms.Sprite;

import java.util.Collection;

public class SynchIconMapDrawer extends MapDrawer {
    public SynchIconMapDrawer(StarMod mod) {
        super(mod);
    }


    /**
     * set and draw these items. overwrites all existng items.
     * @param icons
     */
    public void setIcons(Collection<SynchMapIcon> icons) {
        this.clearMarkers();
        for (SynchMapIcon icon: icons) {
            this.addMarker(new SimpleMapMarker(
                    spriteByIconId(icon.iconIndex),
                    subSpriteByIconId(icon.iconIndex),
                    icon.color,
                    MapDrawer.posFromSector(icon.sector,true)
            ));
        }
        this.rebuildInternalList();
    }

    Sprite spriteByIconId(int iconId) {
        return AreaMapDrawer.areaSprite;
    }

    int subSpriteByIconId(int iconId) {
        return 0;
    }
}
