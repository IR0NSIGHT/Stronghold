package me.iron.stronghold.mod.implementation;

import me.iron.stronghold.mod.framework.AbstractControllableArea;
import me.iron.stronghold.mod.framework.IAreaEvent;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.Timer;

public class GenericNewsCollector implements IAreaEvent {
    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        System.out.println(area.getName()+" was conquered by " + area.getOwnerFaction() + ", taken from " + oldOwner);
    }

    @Override
    public void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue) {
        System.out.println(area.getName()+" can now "+ (area.canBeConquered()?"":"not")+" be conquered.");
    }

    @Override
    public void onUpdate(AbstractControllableArea area) {
        System.out.println(area.getName()+" was updated.");
    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, AbstractControllableArea child, boolean removed) {
        System.out.println("area "+parent.getName()+(removed?" had removed":" was added ")+ "child area "+child.getName());
    }

    @Override
    public void onParentChanged(AbstractControllableArea child, AbstractControllableArea parent, boolean removed) {
        System.out.println("area"+child.getName()+" had parent changed: removed?"+removed+" parent: "+parent.getName());
    }

    @Override
    public void onAttacked(Timer t, AbstractControllableArea area, int attackerFaction, Vector3i position) {
        System.out.println("area " + area.getName() +" is under attack at " + position + " by faction " + attackerFaction);
    }
}
