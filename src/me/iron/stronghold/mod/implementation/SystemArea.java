package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.SendableUpdateable;
import org.schema.schine.graphicsengine.core.Timer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * an area that is owned by the faction that at least once controls all child areas.
 * an attacker will have to conquer ALL child areas => auto gains control.
 */
public class SystemArea extends AbstractControllableArea {
    public SystemArea(){}
    public SystemArea(String name) {
        super(name);
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
    }

    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        super.onConquered(area, oldOwner);
        //if area is not me => has to be a child
        if (!this.equals(area)) {
            int total = children.size();
            HashMap<Integer,Integer> owners_to_amount = new HashMap<>(children.size());
            for (SendableUpdateable s: children) {
                if (s instanceof AbstractControllableArea) {
                    AbstractControllableArea a = (AbstractControllableArea) s;
                    Integer i = owners_to_amount.get(a.getOwnerFaction());
                    if (i == null) {
                        i = 0;
                    }
                    i++;
                    owners_to_amount.put(a.getOwnerFaction(),i);
                }
            }
            int rulingFaction = this.ownerFaction; int amount=0;
            for (Map.Entry<Integer,Integer> faction_areas: owners_to_amount.entrySet()) {
                //own the most areas OR (you are owner and control equal areas)
                if (faction_areas.getValue()>amount) {
                    rulingFaction = faction_areas.getKey();
                    amount = faction_areas.getValue();
                }
            }
            //own all areas => conquer the parent
            if (amount==total)
                setOwnerFaction(rulingFaction);
        }
    }
}
