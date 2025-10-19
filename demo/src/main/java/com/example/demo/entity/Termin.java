package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "termine")
public class Termin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lehrername;
    private String schuelername;
    private String uhrzeit; // z.B. "17:00"

    public Termin() {
    }

    public Termin(String lehrername, String schuelername, String uhrzeit) {
        this.lehrername = lehrername;
        this.schuelername = schuelername;
        this.uhrzeit = uhrzeit;
    }

    // Getter & Setter
    public Long getId() {
        return id;
    }

    public String getLehrername() {
        return lehrername;
    }

    public void setLehrername(String lehrername) {
        this.lehrername = lehrername;
    }

    public String getSchuelername() {
        return schuelername;
    }

    public void setSchuelername(String schuelername) {
        this.schuelername = schuelername;
    }

    public String getUhrzeit() {
        return uhrzeit;
    }

    public void setUhrzeit(String uhrzeit) {
        this.uhrzeit = uhrzeit;
    }
}
