package com.licenta.horeca.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nota oferita de client, de exemplu intre 1 si 5
    @Column(nullable = false)
    private Integer rating;

    // Comentariu optional
    private String comment;

    // Momentul in care feedback-ul a fost trimis
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Feedback() {
    }

    public Feedback(Integer rating, String comment) {
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}