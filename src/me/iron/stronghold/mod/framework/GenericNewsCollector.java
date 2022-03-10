package me.iron.stronghold.mod.framework;

import api.DebugFile;
import org.schema.common.util.linAlg.Vector3i;

public class GenericNewsCollector implements IAreaEvent {
    private String prefix;
    public GenericNewsCollector(String prefix) {
        this.prefix = prefix;
    }
    @Override
    public void onConquered(AbstractControllableArea area, int oldOwner) {
        broadcast(area.getName()+" was conquered by " + area.getOwnerFaction() + ", taken from " + oldOwner);
    }

    @Override
    public void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue) {
        broadcast(area.getName()+" can now "+ (area.canBeConquered()?"":"not")+" be conquered.");
    }

    @Override
    public void onUpdate(AbstractControllableArea area) {
        //broadcast(area.getName()+" was updated by parent "+ (area.getParent()!=null?area.getParent().getName():"null"));
    }

    @Override
    public void beforeOverwrite(AbstractControllableArea area) {

    }

    @Override
    public void onOverwrite(AbstractControllableArea area) {

    }

    @Override
    public void beforeDestroy(AbstractControllableArea area) {

    }

    @Override
    public void onDestroy(AbstractControllableArea area) {

    }

    @Override
    public void onChildChanged(AbstractControllableArea parent, SendableUpdateable child, boolean removed) {
        broadcast("area "+parent.getName()+(removed?" had removed":" was added ")+ "child area "+child.getName());
    }

    @Override
    public void onParentChanged(SendableUpdateable child, AbstractControllableArea parent, boolean removed) {
        broadcast("area"+child.getName()+" had parent changed: removed?"+removed+" parent: "+parent.getName());
    }

    @Override
    public void onAttacked(long time, AbstractControllableArea area, int attackerFaction, Vector3i position) {
        broadcast("area " + area.getName() +" is under attack at " + position + " by faction " + attackerFaction);
    }


    private void broadcast(String mssg) {
        //if (GameServerState.instance!=null) {
        //    ModPlayground.broadcastMessage(mssg);
        //}
        //System.out.println(prefix+mssg);
        //DebugFile.log(prefix+mssg);
    }
}
