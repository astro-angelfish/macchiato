package macchiato.threading;

import java.util.LinkedList;
import java.util.List;

public class PooledScheduler {
    private static PooledScheduler instance = null;

    private final List<PooledExecutor> executors = new LinkedList<>();

    public static PooledScheduler getInstance() {
        if (instance == null) {
            instance = new PooledScheduler();
        }
        return instance;
    }

    public PooledScheduler() {
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i ++) {
            PooledExecutor pooledExecutor = new PooledExecutor(i);
            new Thread(pooledExecutor).start();
            executors.add(pooledExecutor);
        }
    }

    public void scheduleTask(Runnable task) {
        PooledExecutor selected = null;
        int numTasks = Integer.MAX_VALUE;

        for (PooledExecutor pooledExecutor : executors) {
            if (numTasks > pooledExecutor.taskSize()) {
                numTasks = pooledExecutor.taskSize();
                selected = pooledExecutor;
                if (numTasks == 0) {
                    break;
                }
            }
        }

        if (selected != null) {
            selected.pushTask(task);
        } else {
            throw new IllegalStateException("Macchiato cannot even find an available worker.");
        }
    }
}
