package me.iron.stronghold.mod.effects;

public class CPNameGen {
    private String[] names = new String[]{"Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Gamma"};
    private int idx = 0;

    public String next() {
        return names[idx++ % names.length];
    }
}
