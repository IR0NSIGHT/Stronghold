package me.iron.stronghold.mod.events;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * STARMADE MOD
 * CREATOR: Max1M
 * DATE: 12.01.2022
 * TIME: 18:23
 */
public class GenericEvent {
    private static HashMap<Class<? extends GenericEvent>,LinkedList<Listener>> listeners = new HashMap<>();

    public static void clearListeners() {
        listeners.clear();
    }
    public  static <E extends GenericEvent> void addListener(Class<? extends GenericEvent> eventClass,Listener listener) {
        LinkedList<Listener> runnables = listeners.get(eventClass);
        if (runnables == null) {
            runnables = new LinkedList<>();
            listeners.put(eventClass, runnables);
        }
        runnables.add(listener);
    }

    public void fire() {
        LinkedList<Listener> rs = listeners.get(this.getClass());
        if (rs != null) {
            for (Listener r: rs)
                r.run(this);
        }
    }
}
