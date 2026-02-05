package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lehrer_zuordnung")
public class LehrerZuordnung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String schueler;

    @Column(nullable = false)
    private String lehrerKuerzel;

    @Column
    private String fach;

    // ======== Konstruktoren ========
    public LehrerZuordnung() {}

    public LehrerZuordnung(String schueler, String lehrerKuerzel, String fach) {
        this.schueler = schueler;
        this.lehrerKuerzel = lehrerKuerzel;
        this.fach = fach;
    }

    // ======== Getter & Setter ========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchueler() {
        return schueler;
    }

    public void setSchueler(String schueler) {
        this.schueler = schueler;
    }

    public String getLehrerKuerzel() {
        return lehrerKuerzel;
    }

    public void setLehrerKuerzel(String lehrerKuerzel) {
        this.lehrerKuerzel = lehrerKuerzel;
    }

    public String getFach() {
        return fach;
    }

    public void setFach(String fach) {
        this.fach = fach;
    }
}
