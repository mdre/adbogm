package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.adbogm.annotations.Entity;
import net.adbogm.annotations.OnlyAdd;

@Entity
public class OnlyAddParent {

    private final List<OnlyAddChild> items = new ArrayList<>();

    @OnlyAdd(attribute = "items")
    private final List<OnlyAddChild> newItems = new ArrayList<>();

    @OnlyAdd
    private final List<OnlyAddChild> notifications = new ArrayList<>();

    public void addItem(OnlyAddChild item) {
        this.newItems.add(item);
    }

    public void addNotification(OnlyAddChild notification) {
        this.notifications.add(notification);
    }

    public List<OnlyAddChild> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<OnlyAddChild> getNewItems() {
        return Collections.unmodifiableList(newItems);
    }

    public List<OnlyAddChild> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }
}
