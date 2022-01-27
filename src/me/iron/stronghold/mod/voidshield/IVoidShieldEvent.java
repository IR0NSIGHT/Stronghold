package me.iron.stronghold.mod.voidshield;

import me.iron.stronghold.mod.framework.Stronghold;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.damage.Damager;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 27.01.2022
 * TIME: 15:05
 */
public interface IVoidShieldEvent {
    /**
     * a voidshielded object is hit.
     * @param h
     * @param hitObject
     * @param d
     */
    void onShieldHit(Stronghold h, SegmentController hitObject, Damager d);

    void onShieldActivate(Stronghold h);

    void onShieldDeactivate(Stronghold h);
}
