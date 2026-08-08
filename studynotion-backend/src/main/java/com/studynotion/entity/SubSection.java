package com.studynotion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sub_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String timeDuration;

    @Column(length = 2000)
    private String description;

    private String videoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;
}
