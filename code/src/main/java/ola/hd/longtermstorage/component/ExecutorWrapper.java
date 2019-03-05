package ola.hd.longtermstorage.component;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class ExecutorWrapper implements DisposableBean {

    private final ExecutorService executorService;

    public ExecutorWrapper() {
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    public void submit(Runnable runnable) {
        executorService.submit(runnable);
    }

    @Override
    public void destroy() throws Exception {
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);
    }
}
