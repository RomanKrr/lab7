import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Phaser;

public class Main {

    private static final int MAX_THREADS = 4;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Вкажіть директорію для пошуку!");
            System.out.println("Приклад: java Main \"C:\\TestFolder\"");
            return;
        }

        Path rootDirectory = Path.of(args[0]);

        if (!rootDirectory.toFile().exists()
                || !rootDirectory.toFile().isDirectory()) {

            System.out.println("Вказана директорія не існує!");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

        ConcurrentHashMap<String, ThreadStatistics> statistics =
                new ConcurrentHashMap<>();

        AtomicInteger totalFiles = new AtomicInteger(0);
        AtomicInteger totalDirectories = new AtomicInteger(0);

        Phaser phaser = new Phaser(1);

        long startTime = System.nanoTime();

        phaser.register();

        executor.submit(new SearchTask(
                rootDirectory,
                executor,
                statistics,
                totalFiles,
                totalDirectories,
                phaser
        ));

        phaser.arriveAndAwaitAdvance();

        executor.shutdown();

        long endTime = System.nanoTime();

        long totalTimeMs = (endTime - startTime) / 1_000_000;

        printStatistics(
                statistics,
                totalFiles.get(),
                totalDirectories.get(),
                totalTimeMs
        );
    }

    private static void printStatistics(
            ConcurrentHashMap<String, ThreadStatistics> statistics,
            int totalFiles,
            int totalDirectories,
            long totalTimeMs) {

        System.out.println("\n========== РЕЗУЛЬТАТ ПОШУКУ ==========");

        System.out.println("Знайдено файлів: " + totalFiles);
        System.out.println("Перевірено директорій: " + totalDirectories);
        System.out.println("Кількість потоків: " + statistics.size());
        System.out.println("Загальний час: " + totalTimeMs + " мс");

        System.out.println("\n========== СТАТИСТИКА ПОТОКІВ ==========");

        for (ThreadStatistics stat : statistics.values()) {

            System.out.println("\nПотік: " + stat.getThreadName());

            System.out.println(
                    "Знайдено файлів: " + stat.getFilesFound()
            );

            System.out.println(
                    "Активний час: " + stat.getActiveTimeMs() + " мс"
            );

            System.out.printf(
                    "Частка загального часу: %.2f%%\n",
                    stat.getTimePercentage(totalTimeMs)
            );
        }

        System.out.println("\n======================================");
    }
}