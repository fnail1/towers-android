package ru.mail.my.towers.toolkit.events;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class ObservableEventBase<Handler, Sender, Argument> {

    private final List<Handler> handlers = new ArrayList<>();
    private int lockEditing = 0;
    private List<PendingEditBase<Handler>> pendingActions;

    public Handler add(Handler handler) {
        synchronized (handlers) {
            if (lockEditing > 0) {
                getEditQueue().add(new PendingAdding<>(handler));
            } else {
                handlers.add(handler);
            }
        }
        return handler;
    }

    public void remove(Handler handler) {
        synchronized (handlers) {
            if (lockEditing > 0) {
                getEditQueue().add(new PendingRemoving<>(handler));
            } else {
                handlers.remove(handler);
            }
        }
    }

    private List<PendingEditBase<Handler>> getEditQueue() {
        if (pendingActions == null) {
            pendingActions = new ArrayList<>();
        }
        return pendingActions;
    }

    protected void fire(Sender sender, Argument args) {
        synchronized (handlers) {
            lockEditing++;
        }

        try {
            for (Handler handler : handlers) {
                notifyHandler(handler, sender, args);
            }
        } finally {
            synchronized (handlers) {
                lockEditing--;
                if (lockEditing == 0 && pendingActions != null) {
                    for (PendingEditBase<Handler> action : pendingActions)
                        action.run(handlers);
                    pendingActions = null;
                }
            }
        }
    }

    protected abstract void notifyHandler(Handler handler, Sender sender, Argument args);

    private static abstract class PendingEditBase<Handler> {
        protected final Handler argument;

        private PendingEditBase(Handler argument) {
            this.argument = argument;
        }

        public abstract void run(List<Handler> collection);
    }

    private static class PendingAdding<Handler> extends PendingEditBase<Handler> {
        public PendingAdding(Handler argument) {
            super(argument);
        }

        @Override
        public void run(List<Handler> collection) {
            collection.add(argument);
        }
    }

    private static class PendingRemoving<Handler> extends PendingEditBase<Handler> {
        public PendingRemoving(Handler argument) {
            super(argument);
        }

        @Override
        public void run(List<Handler> collection) {
            collection.remove(argument);
        }
    }

}
