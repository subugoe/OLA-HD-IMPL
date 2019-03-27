package ola.hd.longtermstorage;

import ola.hd.longtermstorage.component.MutexFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;

public class MutexFactoryTests {

    private final int THREAD_COUNT = 16;

    @Test
    public void singleKeyTest() {
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

    @Test
    public void doubleKeyTest() {
        MutexFactory<Integer> mutexFactory = new MutexFactory<>();
        final int[] count = {0, 0};

        IntStream.range(0, THREAD_COUNT)
                .parallel()
                .forEach(i -> {
                    int index = i % 2;
                    synchronized (mutexFactory.getMutex(index)) {
                        count[index]++;
                    }
                });
        int expected = THREAD_COUNT / 2;
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> count[0] == expected  && count[1] == expected);
        Assert.assertEquals(count[0], expected);
        Assert.assertEquals(count[1], expected);
    }
}
