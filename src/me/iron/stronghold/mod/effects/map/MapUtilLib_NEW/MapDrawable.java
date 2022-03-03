package me.iron.stronghold.mod.effects.map.MapUtilLib_NEW;

import libpackage.markers.SimpleMapMarker;
import org.schema.game.client.view.effects.ConstantIndication;
import org.schema.game.client.view.effects.Indication;

import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 03.03.2022
 * TIME: 19:31
 */
public interface MapDrawable {
    LinkedList<SimpleMapMarker> getMarkers();
    LinkedList<MapLine> getLines();
    LinkedList<Indication> getIndications();
    boolean isVisibleOnMap();
}
