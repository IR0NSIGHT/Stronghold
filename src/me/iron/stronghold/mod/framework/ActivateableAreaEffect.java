package me.iron.stronghold.mod.framework;

import org.schema.schine.graphicsengine.core.Timer;

public class ActivateableAreaEffect extends AbstractAreaEffect {
    public ActivateableAreaEffect() {
        super();
    }

    public ActivateableAreaEffect(String name) {
        super(name);
    }

    @Override
    public void update(Timer timer) {
        super.update(timer);
        if (isActive())
            onActiveUpdate(timer);
        else
            onInactiveUpdate(timer);
    }

    private boolean active; //TODO is saved but not loaded. why?

    /**
     * can this effect be set to x
     * @param to desired state to test
     * @return
     */
    protected boolean canToggle(boolean to) {
        return true;
    }

    /**
     * update + effect is active
     */
    protected void onActiveUpdate(Timer timer) {

    }

    /**
     * update + effect is deactivated
     */
    protected void onInactiveUpdate(Timer timer) {

    }

    protected void onActivate(ActivateableAreaEffect effect) {
        //System.out.println(getName()+" activated");
    }

    protected void onDeactivate(ActivateableAreaEffect effect) {
        //System.out.println(getName()+" deactivated");
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (isActive()!=active && canToggle(active)) {
            this.active = active;
            if (isActive())
                onActivate(this);
            else
                onDeactivate(this);
        }
        AreaManager.dlog("set active"+active+" on effect "+this);
    }

    @Override
    protected void destroy() {
        super.destroy();
        setActive(false);
    }

    @Override
    protected void synch(SendableUpdateable origin) {
       super.synch(origin);
        if (origin instanceof ActivateableAreaEffect) {
            this.active = ((ActivateableAreaEffect) origin).active;
        }
        AreaManager.dlog("synched Act.Effect from:\n"+origin+" \nto:\n"+this);
    }

    @Override
    public String toString() {
        return super.toString() +
                ", active=" + active;
    }
}
