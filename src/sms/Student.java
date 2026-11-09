package sms;

import java.util.Objects;

/**
 * A single student record.
 *
 * The class is immutable: to change a student, build a new Student with the
 * same ID and pass it to {@link StudentRepository#update(Student)}.
 */
public final class Student {

    private final String id;
    private final String name;
    private final int age;
    private final String email;
    private final String phone;
    private final String course;

    public Student(String id, String name, int age, String email, String phone, String course) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.age = age;
        this.email = Objects.requireNonNull(email, "email");
        this.phone = Objects.requireNonNull(phone, "phone");
        this.course = Objects.requireNonNull(course, "course");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCourse() {
        return course;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Student)) {
            return false;
        }
        Student other = (Student) o;
        return age == other.age
                && id.equals(other.id)
                && name.equals(other.name)
                && email.equals(other.email)
                && phone.equals(other.phone)
                && course.equals(other.course);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age, email, phone, course);
    }

    @Override
    public String toString() {
        return "Student{" + id + ", " + name + ", " + age + ", " + email + ", " + phone + ", " + course + "}";
    }
}
