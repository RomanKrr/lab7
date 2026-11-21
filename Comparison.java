import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Comparison {

    private static final int MAX_THREADS = 4;
    private static final String SEARCH_WORD = "FOUND_ITEM";

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Вкажіть директорію для тестування!");
            return;
        }

        Path root = Path.of(args[0]);

        if (!Files.isDirectory(root)) {
            System.out.println("Директорія не існує!");
            return;
        }

        List<Path> directories;

        try (var stream = Files.walk(root)) {
            directories = stream
                    .filter(Files::isDirectory)
                    .toList();
        } catch (IOException e) {
            System.out.println("Помилка читання директорій!");
            return;
        }

        System.out.println("Директорій для перевірки: " + directories.size());

        // Варіант 1: окремий потік для кожної директорії

        AtomicInteger files1 = new AtomicInteger();
        AtomicInteger found1 = new AtomicInteger();

        long start1 = System.nanoTime();

        List<Thread> threads = directories.stream()
                .map(directory -> new Thread(() ->
                        searchDirectory(directory, files1, found1)))
                .toList();

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        long time1 = (System.nanoTime() - start1) / 1_000_000;

        // Варіант 2: обмежений пул потоків

        AtomicInteger files2 = new AtomicInteger();
        AtomicInteger found2 = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

        CountDownLatch latch = new CountDownLatch(directories.size());

        long start2 = System.nanoTime();

        for (Path directory : directories) {

            executor.submit(() -> {
                try {
                    searchDirectory(directory, files2, found2);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            return;
        }

        executor.shutdown();

        long time2 = (System.nanoTime() - start2) / 1_000_000;

        // Результати

        System.out.println("\n========== ПОРІВНЯННЯ ==========");

        System.out.println("\nВаріант 1: окремий потік для кожної директорії");
        System.out.println("Кількість потоків: " + threads.size());
        System.out.println("Перевірено файлів: " + files1.get());
        System.out.println("Знайдено збігів: " + found1.get());
        System.out.println("Час виконання: " + time1 + " мс");

        System.out.println("\nВаріант 2: пул потоків");
        System.out.println("Максимальна кількість потоків: " + MAX_THREADS);
        System.out.println("Перевірено файлів: " + files2.get());
        System.out.println("Знайдено збігів: " + found2.get());
        System.out.println("Час виконання: " + time2 + " мс");

        System.out.println("\n========== РІЗНИЦЯ ==========");

        if (time1 > time2) {
            System.out.println("Пул потоків швидший на "
                    + (time1 - time2) + " мс");
        } else if (time2 > time1) {
            System.out.println("Окремі потоки швидші на "
                    + (time2 - time1) + " мс");
        } else {
            System.out.println("Час виконання однаковий");
        }

        System.out.println("=============================");
    }

    private static void searchDirectory(
            Path directory,
            AtomicInteger totalFiles,
            AtomicInteger foundFiles) {

        try (var stream = Files.list(directory)) {

            for (Path file : stream.toList()) {

                if (Files.isRegularFile(file)) {

                    totalFiles.incrementAndGet();

                    try {
                        String content = Files.readString(file);

                        if (content.contains(SEARCH_WORD)) {
                            foundFiles.incrementAndGet();
                        }

                    } catch (IOException e) {
                        System.out.println("Помилка читання: " + file);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Помилка доступу до: " + directory);
        }
    }
}