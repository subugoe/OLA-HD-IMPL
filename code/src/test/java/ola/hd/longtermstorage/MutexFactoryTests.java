package ola.hd.longtermstorage;

import ola.hd.longtermstorage.component.MutexFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;

public class MutexFactoryTests {

    private final int THREAD_COUNT = 4;

    @Test
    public void testSingleKey() {
        MutexFactory<String> mutexFactory = new MutexFactory<>();
        String id = UUID.randomUUID().toString();
        final int[] count = {0};

        IntStream.range(0, THREAD_COUNT)
                .parallel()
                .forEach(i -> {
                    synchronized (mutexFactory.getMutex(id)) {
                        count[0]++;
                    }
                });
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> count[0] == THREAD_COUNT);
        Assert.assertEquals(count[0], THREAD_COUNT);
    }
}
