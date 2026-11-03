package nl.adminpanel;

import java.util.ArrayList;
import java.util.List;

/** Simpele scheduler om acties over meerdere ticks uit te voeren. */
public final class Scheduler {
    private static final List<Task> TASKS = new ArrayList<>();

    private Scheduler() {
    }

    /** Voert 'action' {@code times} keer uit, elke {@code interval} ticks. */
    public static void repeat(int times, int interval, Runnable action) {
        TASKS.add(new Task(times, interval, action));
    }

    public static void tick() {
        TASKS.removeIf(Task::tick);
    }

    private static final class Task {
        private int left;
        private final int interval;
        private int wait = 1;
        private final Runnable action;

        Task(int left, int interval, Runnable action) {
            this.left = left;
            this.interval = interval;
            this.action = action;
        }

        /** @return true als de taak klaar is */
        boolean tick() {
            if (--wait > 0) return false;
            wait = interval;
            try {
                action.run();
            } catch (Exception e) {
                return true;
            }
            return --left <= 0;
        }
    }
}
