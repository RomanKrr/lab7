import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Phaser;

public class SearchTask implements Runnable {

    private final Path directory;
    private final ExecutorService executor;
    private final ConcurrentHashMap<String, ThreadStatistics> statistics;
    private final AtomicInteger totalFiles;
    private final AtomicInteger totalDirectories;
    private final Phaser phaser;

    public SearchTask(
            Path directory,
            ExecutorService executor,
            ConcurrentHashMap<String, ThreadStatistics> statistics,
            AtomicInteger totalFiles,
            AtomicInteger totalDirectories,
            Phaser phaser) {

        this.directory = directory;
        this.executor = executor;
        this.statistics = statistics;
        this.totalFiles = totalFiles;
        this.totalDirectories = totalDirectories;
        this.phaser = phaser;
    }

    @Override
    public void run() {

        long startTime = System.nanoTime();

        String threadName = Thread.currentThread().getName();

        ThreadStatistics threadStats = statistics.computeIfAbsent(
                threadName,
                name -> new ThreadStatistics(name)
        );

        int filesInDirectory = 0;

        totalDirectories.incrementAndGet();

        try (var entries = Files.list(directory)) {

            var iterator = entries.iterator();

            while (iterator.hasNext()) {

                Path entry = iterator.next();

                if (Files.isDirectory(entry)) {

                    phaser.register();

                    try {
                        executor.submit(new SearchTask(
                                entry,
                                executor,
                                statistics,
                                totalFiles,
                                totalDirectories,
                                phaser
                        ));
                    } catch (RuntimeException e) {
                        phaser.arriveAndDeregister();
                        throw e;
                    }

                } else if (Files.isRegularFile(entry)) {

                    filesInDirectory++;
                    totalFiles.incrementAndGet();
                }
            }

        } catch (IOException e) {

            System.out.println("Помилка доступу до: " + directory);
        }

        long endTime = System.nanoTime();

        long elapsedTime = Math.max(
                1,
                (endTime - startTime) / 1_000_000
        );

        synchronized (threadStats) {

            threadStats.addFilesFound(filesInDirectory);
            threadStats.addActiveTime(elapsedTime);
        }

        phaser.arriveAndDeregister();
    }
}