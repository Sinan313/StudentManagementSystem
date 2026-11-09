package sms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores students in memory and saves every change to a CSV file, so records
 * survive restarts without needing a database server.
 *
 * Student IDs are unique and compared case-insensitively ("s001" == "S001").
 * If saving fails, the in-memory change is rolled back and the IOException is
 * rethrown, so memory and disk never disagree.
 */
public class StudentRepository {

    static final String HEADER = "id,name,age,email,phone,course";
    private static final Pattern AUTO_ID = Pattern.compile("(?i)S(\\d{1,9})");

    private final Path file;
    private final Map<String, Student> students = new LinkedHashMap<>();
    private int skippedLines;

    /** Opens (or lazily creates) the CSV file at the given path. */
    public StudentRepository(Path file) throws IOException {
        this.file = Objects.requireNonNull(file, "file");
        load();
    }

    // ---------------------------------------------------------------- Read

    public int size() {
        return students.size();
    }

    /** Number of unreadable or duplicate lines that were ignored while loading. */
    public int getSkippedLineCount() {
        return skippedLines;
    }

    public List<Student> findAll() {
        return new ArrayList<>(students.values());
    }

    public Optional<Student> findById(String id) {
        return Optional.ofNullable(students.get(key(id)));
    }

    /** Case-insensitive search across every field. A blank query returns everyone. */
    public List<Student> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Student> result = new ArrayList<>();
        for (Student s : students.values()) {
            if (q.isEmpty() || haystack(s).contains(q)) {
                result.add(s);
            }
        }
        return result;
    }

    /** Suggests the next free ID in the S001, S002, ... sequence. */
    public String nextId() {
        int max = 0;
        for (Student s : students.values()) {
            Matcher m = AUTO_ID.matcher(s.getId());
            if (m.matches()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        return String.format("S%03d", max + 1);
    }

    // --------------------------------------------------------------- Write

    /** @throws IllegalArgumentException if the ID is already used */
    public void add(Student student) throws IOException {
        Objects.requireNonNull(student, "student");
        String k = key(student.getId());
        if (students.containsKey(k)) {
            throw new IllegalArgumentException("A student with ID " + student.getId() + " already exists.");
        }
        students.put(k, student);
        try {
            save();
        } catch (IOException ex) {
            students.remove(k);
            throw ex;
        }
    }

    /** @throws NoSuchElementException if no student has this ID */
    public void update(Student student) throws IOException {
        Objects.requireNonNull(student, "student");
        String k = key(student.getId());
        Student previous = students.get(k);
        if (previous == null) {
            throw new NoSuchElementException("No student with ID " + student.getId() + ".");
        }
        students.put(k, student);
        try {
            save();
        } catch (IOException ex) {
            students.put(k, previous);
            throw ex;
        }
    }

    /** @return true if a student was removed, false if the ID was not found */
    public boolean delete(String id) throws IOException {
        String k = key(id);
        Student removed = students.remove(k);
        if (removed == null) {
            return false;
        }
        try {
            save();
        } catch (IOException ex) {
            students.put(k, removed);
            throw ex;
        }
        return true;
    }

    // ----------------------------------------------------------- File I/O

    private void load() throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && line.startsWith("﻿")) {
                line = line.substring(1); // byte-order mark added by Excel
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            if (i == 0 && line.trim().equalsIgnoreCase(HEADER)) {
                continue;
            }
            Student s = fromCsv(line);
            if (s == null || students.containsKey(key(s.getId()))) {
                skippedLines++;
                continue;
            }
            students.put(key(s.getId()), s);
        }
    }

    /** Writes to a temporary file first, then swaps it in, so a crash cannot corrupt the data. */
    private void save() throws IOException {
        Path target = file.toAbsolutePath();
        Path dir = target.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }

        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (Student s : students.values()) {
            sb.append(toCsv(s)).append('\n');
        }

        Path tmp = Files.createTempFile(dir, "students", ".tmp");
        try {
            Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------------------------------------------------------- CSV

    static String toCsv(Student s) {
        return quote(s.getId()) + "," + quote(s.getName()) + "," + s.getAge() + ","
                + quote(s.getEmail()) + "," + quote(s.getPhone()) + "," + quote(s.getCourse());
    }

    /** @return the parsed student, or null if the line is malformed */
    static Student fromCsv(String line) {
        List<String> f = parseCsvLine(line);
        if (f.size() != 6) {
            return null;
        }
        String id = f.get(0).trim();
        if (id.isEmpty()) {
            return null;
        }
        try {
            int age = Integer.parseInt(f.get(2).trim());
            return new Student(id, f.get(1), age, f.get(3), f.get(4), f.get(5));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String quote(String value) {
        String v = value.replace('\r', ' ').replace('\n', ' ');
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields;
    }

    // ------------------------------------------------------------ Helpers

    private static String key(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private static String haystack(Student s) {
        return (s.getId() + "\n" + s.getName() + "\n" + s.getAge() + "\n" + s.getEmail() + "\n"
                + s.getPhone() + "\n" + s.getCourse()).toLowerCase(Locale.ROOT);
    }
}
