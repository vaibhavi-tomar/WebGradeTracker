import java.io.*;
import java.util.*;

/**
 * Holds the in-memory list of students and all the "business logic":
 * adding, deleting, summarizing, and saving/loading from disk.
 * The Server class (HTTP layer) never touches the ArrayList directly —
 * it only talks to this class. That separation is what makes it easy
 * to later swap the storage (e.g. to a real database) without touching
 * any HTTP code.
 */
public class StudentStore {

    private final List<Student> students = new ArrayList<>();
    private static final String FILE_NAME = "students_data.txt";

    public StudentStore() {
        load();
    }

    public synchronized List<Student> getAll() {
        return students;
    }

    /** Returns the new Student, or null if the roll number already exists. */
    public synchronized Student add(String name, int rollNumber, List<Integer> marks) {
        for (Student s : students) {
            if (s.getRollNumber() == rollNumber) return null;
        }
        Student student = new Student(name, rollNumber);
        for (int m : marks) student.addMark(m);
        students.add(student);
        save();
        return student;
    }

    public synchronized boolean delete(int rollNumber) {
        boolean removed = students.removeIf(s -> s.getRollNumber() == rollNumber);
        if (removed) save();
        return removed;
    }

    public synchronized Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();

        if (students.isEmpty()) {
            result.put("totalStudents", 0);
            return result;
        }

        double total = 0;
        Student topper = students.get(0);
        Student weakest = students.get(0);

        for (Student s : students) {
            total += s.getAverage();
            if (s.getAverage() > topper.getAverage()) topper = s;
            if (s.getAverage() < weakest.getAverage()) weakest = s;
        }
        double classAverage = Math.round((total / students.size()) * 100.0) / 100.0;

        List<Student> ranked = new ArrayList<>(students);
        ranked.sort((a, b) -> Double.compare(b.getAverage(), a.getAverage()));

        List<Map<String, Object>> rankedJson = new ArrayList<>();
        int rank = 1;
        for (Student s : ranked) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("name", s.getName());
            entry.put("average", Math.round(s.getAverage() * 100.0) / 100.0);
            entry.put("grade", s.getGrade());
            rankedJson.add(entry);
        }

        result.put("totalStudents", students.size());
        result.put("classAverage", classAverage);
        result.put("topper", topper.toMap());
        result.put("weakest", weakest.toMap());
        result.put("ranked", rankedJson);
        return result;
    }

    private void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Student s : students) {
                writer.println(s.toFileString());
            }
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    private void load() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    students.add(Student.fromFileString(line));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }
}
