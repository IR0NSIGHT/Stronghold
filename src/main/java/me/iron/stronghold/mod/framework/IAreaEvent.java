package me.iron.stronghold.mod.framework;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

public interface IAreaEvent {
    void onConquered(AbstractControllableArea area, int oldOwner);
    void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue);
    void onUpdate(AbstractControllableArea area);

    void beforeOverwrite(AbstractControllableArea area);
    void onOverwrite(AbstractControllableArea area);
    void beforeDestroy(AbstractControllableArea area);
    void onDestroy(AbstractControllableArea area);

    void onChildChanged(AbstractControllableArea parent, SendableUpdateable child, boolean removed);
    void onParentChanged(SendableUpdateable child, AbstractControllableArea parent, boolean removed);

    /**
     * called when the area braodcasts a "under attack" event. is fired once per second max for one area! do not rely on it if you need to catch all under fire events!
     * @param area
     * @param attackerFaction
     * @param position
     */
    void onAttacked(long time, AbstractControllableArea area, int attackerFaction, Vector3i position);
}
