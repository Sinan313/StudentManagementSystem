package sms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Dependency-free tests (no JUnit needed).
 * Run: java -cp out sms.StudentRepositoryTest   (exit code 1 if anything fails)
 */
public class StudentRepositoryTest {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<Path> tempDirs = new ArrayList<>();

    private interface Check {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        run("add then find", StudentRepositoryTest::addThenFind);
        run("duplicate ID rejected (case-insensitive)", StudentRepositoryTest::duplicateRejected);
        run("update changes the record", StudentRepositoryTest::updateChangesRecord);
        run("update of unknown ID fails", StudentRepositoryTest::updateUnknownFails);
        run("delete removes the record", StudentRepositoryTest::deleteRemoves);
        run("data persists across restarts", StudentRepositoryTest::persistsAcrossRestarts);
        run("commas and quotes survive the CSV round trip", StudentRepositoryTest::csvRoundTrip);
        run("search is case-insensitive and covers all fields", StudentRepositoryTest::searchWorks);
        run("nextId follows the S001 sequence", StudentRepositoryTest::nextIdSequence);
        run("bad lines in the file are skipped, not fatal", StudentRepositoryTest::badLinesSkipped);
        run("validator accepts good input", StudentRepositoryTest::validatorAccepts);
        run("validator flags every bad field", StudentRepositoryTest::validatorRejects);

        for (Path dir : tempDirs) {
            deleteRecursively(dir);
        }
        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------- Tests

    private static void addThenFind() throws Exception {
        StudentRepository repo = newRepo();
        repo.add(student("S001", "Asha Nair"));
        repo.add(student("S002", "Ravi Kumar"));
        check(repo.size() == 2, "size should be 2");
        check(repo.findById("S002").get().getName().equals("Ravi Kumar"), "findById should return Ravi");
        check(!repo.findById("S999").isPresent(), "unknown id should be empty");
    }

    private static void duplicateRejected() throws Exception {
        StudentRepository repo = newRepo();
        repo.add(student("S001", "Asha Nair"));
        try {
            repo.add(student("s001", "Someone Else"));
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            check(repo.size() == 1, "failed add must not change the repository");
        }
    }

    private static void updateChangesRecord() throws Exception {
        StudentRepository repo = newRepo();
        repo.add(student("S001", "Asha Nair"));
        repo.update(new Student("S001", "Asha N. Menon", 21, "asha@new.com", "+91 90000 11111", "Physics"));
        Student s = repo.findById("S001").get();
        check(s.getName().equals("Asha N. Menon") && s.getAge() == 21 && s.getCourse().equals("Physics"),
                "fields should be updated");
        check(repo.size() == 1, "update must not add a row");
    }

    private static void updateUnknownFails() throws Exception {
        StudentRepository repo = newRepo();
        try {
            repo.update(student("S404", "Nobody"));
            throw new AssertionError("expected NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // ok
        }
    }

    private static void deleteRemoves() throws Exception {
        StudentRepository repo = newRepo();
        repo.add(student("S001", "Asha Nair"));
        check(repo.delete("s001"), "first delete should return true");
        check(!repo.delete("S001"), "second delete should return false");
        check(repo.size() == 0, "repository should be empty");
    }

    private static void persistsAcrossRestarts() throws Exception {
        Path file = tempFile();
        StudentRepository first = new StudentRepository(file);
        first.add(student("S001", "Asha Nair"));
        first.add(student("S002", "Ravi Kumar"));
        first.update(new Student("S002", "Ravi K.", 25, "r@x.com", "9876543210", "Mathematics"));
        first.delete("S001");

        StudentRepository second = new StudentRepository(file);
        check(second.size() == 1, "reloaded repository should have 1 student");
        check(second.findById("S002").get().getName().equals("Ravi K."), "update should be on disk");
        check(!second.findById("S001").isPresent(), "delete should be on disk");
    }

    private static void csvRoundTrip() throws Exception {
        Path file = tempFile();
        Student tricky = new Student("S001", "Meera \"MJ\" Joseph, Jr.", 20, "meera@x.com",
                "+91 98765 43210", "B.Tech, \"Computer\" Science");
        new StudentRepository(file).add(tricky);
        Student loaded = new StudentRepository(file).findById("S001").get();
        check(tricky.equals(loaded), "expected " + tricky + " but got " + loaded);
    }

    private static void searchWorks() throws Exception {
        StudentRepository repo = newRepo();
        repo.add(new Student("S001", "Asha Nair", 20, "asha@x.com", "9000000001", "Computer Science"));
        repo.add(new Student("S002", "Ravi Kumar", 22, "ravi@y.org", "9000000002", "Physics"));
        check(repo.search("").size() == 2, "blank query returns everyone");
        check(repo.search("ASHA").size() == 1, "name match, any case");
        check(repo.search("physics").size() == 1, "course match");
        check(repo.search("y.org").size() == 1, "email match");
        check(repo.search("9000000002").size() == 1, "phone match");
        check(repo.search("22").size() == 1, "age match");
        check(repo.search("zzz").isEmpty(), "no match returns empty list");
    }

    private static void nextIdSequence() throws Exception {
        StudentRepository repo = newRepo();
        check(repo.nextId().equals("S001"), "empty repo starts at S001");
        repo.add(student("S007", "A"));
        repo.add(student("ROLL-12", "B"));
        check(repo.nextId().equals("S008"), "continues after the highest S-number");
    }

    private static void badLinesSkipped() throws Exception {
        Path file = tempFile();
        String content = StudentRepository.HEADER + "\n"
                + "S001,Asha Nair,20,asha@x.com,9000000001,Physics\n"
                + "this is not a student line\n"
                + "S002,Ravi,not-a-number,r@x.com,9000000002,Physics\n"
                + "\n"
                + "s001,Duplicate Id,30,d@x.com,9000000003,Physics\n";
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        StudentRepository repo = new StudentRepository(file);
        check(repo.size() == 1, "only the good line should load, got " + repo.size());
        check(repo.getSkippedLineCount() == 3, "3 lines skipped, got " + repo.getSkippedLineCount());
    }

    private static void validatorAccepts() {
        Map<String, String> errors = StudentValidator.validate(
                "S001", "  Asha   Nair ", "20", "asha@example.com", "+91 98765-43210", "Computer Science");
        check(errors.isEmpty(), "expected no errors but got " + errors);
        Student s = StudentValidator.build(
                "S001", "  Asha   Nair ", "20", "asha@example.com", "+91 98765-43210", "Computer Science");
        check(s.getName().equals("Asha Nair"), "extra spaces should be normalised");
    }

    private static void validatorRejects() {
        Map<String, String> errors = StudentValidator.validate("", "", "abc", "not-an-email", "12", "");
        check(errors.keySet().containsAll(Arrays.asList("id", "name", "age", "email", "phone", "course")),
                "every field should be flagged, got " + errors.keySet());
        check(StudentValidator.validate("S1", "Asha", "4", "a@b.co", "1234567", "X").containsKey("age"),
                "age 4 is too low");
        check(StudentValidator.validate("S1", "Asha", "101", "a@b.co", "1234567", "X").containsKey("age"),
                "age 101 is too high");
        check(StudentValidator.validate("S1", "R2D2", "20", "a@b.co", "1234567", "X").containsKey("name"),
                "digits in a name are rejected");
        check(StudentValidator.validate("S 1", "Asha", "20", "a@b.co", "1234567", "X").containsKey("id"),
                "spaces in an ID are rejected");
    }

    // ----------------------------------------------------------- Helpers

    private static Student student(String id, String name) {
        return new Student(id, name, 20, "test@example.com", "9876543210", "Computer Science");
    }

    private static StudentRepository newRepo() throws IOException {
        return new StudentRepository(tempFile());
    }

    private static Path tempFile() throws IOException {
        Path dir = Files.createTempDirectory("sms-test");
        tempDirs.add(dir);
        return dir.resolve("students.csv");
    }

    private static void run(String name, Check check) {
        try {
            check.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + "  ->  " + t);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
