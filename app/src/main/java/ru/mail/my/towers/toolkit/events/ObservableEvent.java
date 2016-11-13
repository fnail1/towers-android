package ru.mail.my.towers.toolkit.events;

/**
 *
 */
public abstract class ObservableEvent<Handler,Sender, Argument> extends ObservableEventBase<Handler,Sender, Argument> {

    private final Sender sender;

    public ObservableEvent(Sender sender) {
        this.sender = sender;
    }

    public void fire(Argument args) {
        super.fire(sender, args);
    }
}
