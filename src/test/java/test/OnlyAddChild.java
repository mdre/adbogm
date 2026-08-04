package test;

import net.adbogm.annotations.Entity;

@Entity
public class OnlyAddChild {

    private String name;

    public OnlyAddChild() {
    }

    public OnlyAddChild(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
