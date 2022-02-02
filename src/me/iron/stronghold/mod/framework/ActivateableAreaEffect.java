package me.iron.stronghold.mod.framework;

public class ActivateableAreaEffect extends AbstractAreaEffect {
    public ActivateableAreaEffect(String name) {
        super(name);
    }

    boolean active;

    protected void onActivate(ActivateableAreaEffect effect) {
        System.out.println(getName()+" activated");
    }

    protected void onDeactivate(ActivateableAreaEffect effect) {
        System.out.println(getName()+" deactivated");
    }

    public boolean isActive() {
        return active;
    }

    protected void setActive(boolean active) {
        if (isActive()!=active) {
            this.active = active;
            if (isActive())
                onActivate(this);
            else
                onDeactivate(this);
        }
    }

    @Override
    protected void updateFromObject(SendableUpdateable origin) {
        super.updateFromObject(origin);
        if (origin instanceof ActivateableAreaEffect) {
            setActive (((ActivateableAreaEffect) origin).isActive());
        }
    }
}
