import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestDataGenerator {

    public static void main(String[] args) {

        try {
            createDataset("Test_100", 100, 10, 1024);
            createDataset("Test_1000", 1000, 50, 4096);
            createDataset("Test_5000", 5000, 200, 4096);

            System.out.println("Тестові набори успішно створені!");

        } catch (IOException e) {
            System.out.println("Помилка створення тестових даних!");
            e.printStackTrace();
        }
    }

    private static void createDataset(
            String folderName,
            int fileCount,
            int directoryCount,
            int fileSize) throws IOException {

        Path root = Path.of(folderName);

        Files.createDirectories(root);

        for (int i = 1; i <= directoryCount; i++) {
            Files.createDirectories(root.resolve("Folder_" + i));
        }

        String filler = "Lorem ipsum test data multithreading ";

        for (int i = 1; i <= fileCount; i++) {

            int folderNumber = (i % directoryCount) + 1;

            Path file = root.resolve("Folder_" + folderNumber)
                    .resolve("file_" + i + ".txt");

            StringBuilder content = new StringBuilder();

            if (i % 10 == 0) {
    content.append("FOUND_ITEM ");
} else {
    content.append("NO_ITEM ");
}

            while (content.length() < fileSize) {
                content.append(filler);
            }

            content.setLength(fileSize);

            Files.writeString(file, content.toString());
        }

        System.out.println("Створено " + fileCount
                + " файлів у " + directoryCount
                + " директоріях (" + fileSize + " байт кожен): "
                + folderName);
    }
}