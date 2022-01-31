package me.iron.stronghold.mod.framework;

public interface IAreaEvent {
    void onConquered(AbstractControllableArea area, int oldOwner);
    void onCanBeConqueredChanged(AbstractControllableArea area, boolean oldValue);
    void onUpdate(AbstractControllableArea area);
    void onChildChanged(AbstractControllableArea parent, AbstractControllableArea child, boolean removed);
    void onParentChanged(AbstractControllableArea child, AbstractControllableArea parent, boolean removed);
}
