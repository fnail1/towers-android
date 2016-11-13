package ru.mail.my.towers.toolkit;


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UiExclusiveExecutor {

    private final int minDelay;
    private final Runnable task;
    private final AtomicInteger sync = new AtomicInteger(0);
    private final Runnable internalTask;

    private final AtomicBoolean forced = new AtomicBoolean();

    public UiExclusiveExecutor(int delay, Runnable task) {
        minDelay = delay;
        this.task = task;
        if (minDelay > 0) {
            internalTask = new Runnable() {
                Runnable mRestartTask = () -> {
                    if (sync.getAndSet(0) > 1) {
                        execute(false);
                    }
                };
                @Override
                public void run() {
                    UiExclusiveExecutor.this.task.run();
                    ThreadPool.UI.postDelayed(mRestartTask, forced.getAndSet(false) ? 0 : minDelay);
                }
            };
        } else {
            internalTask = () -> {
                UiExclusiveExecutor.this.task.run();
                if (sync.getAndSet(0) > 1) {
                    execute(false);
                }
            };
        }
    }

    public void execute(boolean forced) {
        if (sync.getAndIncrement() > 0) {
            if (forced) {
                this.forced.set(true);
            }
            return;
        }
        ThreadPool.UI.post(internalTask);
    }
}
