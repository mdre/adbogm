package test;

import java.util.List;
import net.adbogm.annotations.Entity;

@Entity
public class OnlyAddHolder {

    private OnlyAddParent parent;
    private transient List<String> inherited;

    public OnlyAddHolder() {
    }

    public OnlyAddHolder(OnlyAddParent parent) {
        this.parent = parent;
    }

    public OnlyAddParent getParent() {
        return parent;
    }

    public OnlyAddParent getOrCreateParent() {
        if (this.parent == null) {
            this.parent = new OnlyAddParent();
        }
        return this.parent;
    }

    public void addItemToParent(OnlyAddChild child) {
        this.parent.addItem(child);
    }

    public void addItemToLazyParent(OnlyAddChild child) {
        if (this.parent == null) {
            this.parent = new OnlyAddParent();
        }
        this.parent.addItem(child);
    }

    public void addItemThroughGetter(OnlyAddChild child) {
        getOrCreateParent().addItem(child);
    }

    public void setInherited(List<String> inherited) {
        this.inherited = inherited;
    }
}
