package com.project.farming.domain.plant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "plant_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Plant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long plantId;

    @Column(unique = true, nullable = false)
    private String name;

    private String englishName;
    private String species;
    private String season;
    private String imageUrl;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDate.now();
    }

    public void updatePlant(String name, String englishName, String species,
                            String season, String imageUrl) {
        this.name = name;
        this.englishName = englishName;
        this.species = species;
        this.season = season;
        this.imageUrl = imageUrl;
    }
}
