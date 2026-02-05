package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lehrer_raum")
public class LehrerRaum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kuerzel;

    @Column(nullable = false)
    private String lehrername;

    @Column
    private String raum;

    // ======== Konstruktoren ========
    public LehrerRaum() {}

    public LehrerRaum(String kuerzel, String lehrername, String raum) {
        this.kuerzel = kuerzel;
        this.lehrername = lehrername;
        this.raum = raum;
    }

    // ======== Getter & Setter ========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKuerzel() {
        return kuerzel;
    }

    public void setKuerzel(String kuerzel) {
        this.kuerzel = kuerzel;
    }

    public String getLehrername() {
        return lehrername;
    }

    public void setLehrername(String lehrername) {
        this.lehrername = lehrername;
    }

    public String getRaum() {
        return raum;
    }

    public void setRaum(String raum) {
        this.raum = raum;
    }
}
