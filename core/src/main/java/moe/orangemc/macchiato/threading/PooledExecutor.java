package macchiato.threading;

import java.util.LinkedList;
import java.util.Queue;

public class PooledExecutor implements Runnable {
    private final Queue<Runnable> tasks = new LinkedList<>();
    private final int id;

    PooledExecutor(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        Thread.currentThread().setDaemon(true);
        Thread.currentThread().setName("Macchiato Worker #" + id);

        while (true) {
            Runnable task = tasks.poll();
            if (task == null) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
                continue;
            }
            task.run();
        }
    }

    int taskSize() {
        return tasks.size();
    }

    void pushTask(Runnable runnable) {
        tasks.offer(runnable);
    }
}
