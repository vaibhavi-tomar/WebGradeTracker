import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single student: name, roll number, and a list of subject marks.
 * The class knows how to calculate its own statistics, and how to turn
 * itself into a Map (for JSON output) or a CSV line (for file storage).
 */
public class Student {
    private String name;
    private int rollNumber;
    private List<Integer> marks;

    public Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.marks = new ArrayList<>();
    }

    public void addMark(int mark) {
        marks.add(mark);
    }

    public String getName() {
        return name;
    }

    public int getRollNumber() {
        return rollNumber;
    }

    public List<Integer> getMarks() {
        return marks;
    }

    public double getAverage() {
        if (marks.isEmpty()) return 0.0;
        int sum = 0;
        for (int m : marks) sum += m;
        return (double) sum / marks.size();
    }

    public int getHighest() {
        if (marks.isEmpty()) return 0;
        int max = marks.get(0);
        for (int m : marks) if (m > max) max = m;
        return max;
    }

    public int getLowest() {
        if (marks.isEmpty()) return 0;
        int min = marks.get(0);
        for (int m : marks) if (m < min) min = m;
        return min;
    }

    public String getGrade() {
        double avg = getAverage();
        if (avg >= 90) return "A+";
        if (avg >= 80) return "A";
        if (avg >= 70) return "B";
        if (avg >= 60) return "C";
        if (avg >= 50) return "D";
        return "F";
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Converts this student into a Map, ready to be sent as JSON. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rollNumber", rollNumber);
        map.put("name", name);
        map.put("marks", marks);
        map.put("average", round2(getAverage()));
        map.put("highest", getHighest());
        map.put("lowest", getLowest());
        map.put("grade", getGrade());
        return map;
    }

    public String toFileString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rollNumber).append(",").append(name);
        for (int m : marks) sb.append(",").append(m);
        return sb.toString();
    }

    public static Student fromFileString(String line) {
        String[] parts = line.split(",");
        int roll = Integer.parseInt(parts[0].trim());
        String name = parts[1].trim();
        Student s = new Student(name, roll);
        for (int i = 2; i < parts.length; i++) {
            s.addMark(Integer.parseInt(parts[i].trim()));
        }
        return s;
    }
}