package com.studynotion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean approved = true;

    // Profile info embedded (instead of separate Profile model)
    private String gender;
    private String dateOfBirth;

    @Column(length = 1000)
    private String about;

    private String contactNumber;

    private String image;

    // Password reset
    private String resetPasswordToken;
    private LocalDateTime resetPasswordExpires;

    // Courses enrolled (for students) OR created (for instructors)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_courses",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CourseProgress> courseProgress = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum AccountType {
        Student, Instructor, Admin
    }

    // Full name helper
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
