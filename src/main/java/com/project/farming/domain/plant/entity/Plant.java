package com.project.farming.domain.plant.entity;

import com.project.farming.global.image.entity.ImageFile;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long plantId;

    @Column(unique = true, nullable = false)
    private String plantName;

    private String plantEnglishName;
    private String species; // 식물 분류
    private String season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_image_file_id", nullable = false)
    private ImageFile plantImageFile;

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

    public void updatePlantInfo(
            String plantName, String plantEnglishName,
            String species, String season) {
        
        this.plantName = plantName;
        this.plantEnglishName = plantEnglishName;
        this.species = species;
        this.season = season;
    }

    public void updatePlantImage(ImageFile plantImageFile) {
        this.plantImageFile = plantImageFile;
    }
}
