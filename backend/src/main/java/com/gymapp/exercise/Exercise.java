package com.gymapp.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Exercice du catalogue, importé depuis le dataset externe.
 * Les colonnes jsonb / text[] (instructions, muscles secondaires) seront mappées
 * avec le module catalogue ; le module séance n'en a pas besoin.
 */
@Entity
@Table(name = "exercise")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "body_part", nullable = false)
    private String bodyPart;

    private String target;

    private String equipment;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "gif_path", nullable = false)
    private String gifPath;

    protected Exercise() {
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getBodyPart() {
        return bodyPart;
    }

    public String getTarget() {
        return target;
    }

    public String getEquipment() {
        return equipment;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getGifPath() {
        return gifPath;
    }
}
