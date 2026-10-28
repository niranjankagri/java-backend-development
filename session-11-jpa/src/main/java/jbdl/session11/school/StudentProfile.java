package jbdl.session11.school;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Extra details of one student (one-to-one). Saved and deleted together with the Student (cascade). */
@Entity
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    private String phone;

    protected StudentProfile() {
    }

    public StudentProfile(String city, String phone) {
        this.city = city;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public String getPhone() {
        return phone;
    }
}
