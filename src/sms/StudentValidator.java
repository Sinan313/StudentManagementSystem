package sms;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Checks the raw text typed into the form and builds a {@link Student} from it.
 *
 * validate() returns a map of field key -> error message. An empty map means
 * the input is valid and build() can be called safely.
 * Field keys: "id", "name", "age", "email", "phone", "course".
 */
public final class StudentValidator {

    public static final int MIN_AGE = 5;
    public static final int MAX_AGE = 100;

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_\\-]{1,15}");
    private static final Pattern NAME = Pattern.compile("\\p{L}[\\p{L} .'\\-]{1,59}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s,]+@[^@\\s,]+\\.[^@\\s,]{2,}");
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9][0-9 \\-]{5,18}[0-9]");

    private StudentValidator() {
    }

    public static Map<String, String> validate(String id, String name, String age,
                                               String email, String phone, String course) {
        Map<String, String> errors = new LinkedHashMap<>();

        id = clean(id);
        name = normaliseSpaces(name);
        age = clean(age);
        email = clean(email);
        phone = clean(phone);
        course = normaliseSpaces(course);

        if (id.isEmpty()) {
            errors.put("id", "Student ID is required.");
        } else if (!ID.matcher(id).matches()) {
            errors.put("id", "Student ID can only contain letters, digits, '-' and '_' (max 15 characters).");
        }

        if (name.isEmpty()) {
            errors.put("name", "Name is required.");
        } else if (!NAME.matcher(name).matches()) {
            errors.put("name", "Name must be 2-60 characters and use only letters, spaces, '.', ''' and '-'.");
        }

        if (age.isEmpty()) {
            errors.put("age", "Age is required.");
        } else {
            try {
                int value = Integer.parseInt(age);
                if (value < MIN_AGE || value > MAX_AGE) {
                    errors.put("age", "Age must be between " + MIN_AGE + " and " + MAX_AGE + ".");
                }
            } catch (NumberFormatException ex) {
                errors.put("age", "Age must be a whole number.");
            }
        }

        if (email.isEmpty()) {
            errors.put("email", "Email is required.");
        } else if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            errors.put("email", "Enter a valid email address, e.g. name@example.com.");
        }

        if (phone.isEmpty()) {
            errors.put("phone", "Phone number is required.");
        } else if (!PHONE.matcher(phone).matches()) {
            errors.put("phone", "Phone must be 7-20 digits; spaces, '-' and a leading '+' are allowed.");
        }

        if (course.isEmpty()) {
            errors.put("course", "Course is required.");
        } else if (course.length() > 60) {
            errors.put("course", "Course must be at most 60 characters.");
        }

        return errors;
    }

    /** Builds a Student from text that has already passed {@link #validate}. */
    public static Student build(String id, String name, String age,
                                String email, String phone, String course) {
        return new Student(clean(id), normaliseSpaces(name), Integer.parseInt(clean(age)),
                clean(email), clean(phone), normaliseSpaces(course));
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normaliseSpaces(String s) {
        return clean(s).replaceAll("\\s+", " ");
    }
}
