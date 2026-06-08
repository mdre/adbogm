package test;

import net.adbogm.annotations.Entity;

@Entity
public class OnlyAddHolder {

    private OnlyAddParent parent;

    public OnlyAddHolder() {
    }

    public OnlyAddHolder(OnlyAddParent parent) {
        this.parent = parent;
    }

    public OnlyAddParent getParent() {
        return parent;
    }

    public void addItemToParent(OnlyAddChild child) {
        this.parent.addItem(child);
    }
}
