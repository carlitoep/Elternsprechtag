package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Verifizierung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String schuelername;
    private String token;
    private boolean bestaetigt;

    // Getter & Setter
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSchuelername() {
        return schuelername;
    }

    public String getToken() {
        return token;
    }

    public boolean getBestaetigt() {
        return bestaetigt;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setBestaetigt(Boolean bestaetigt) {
        this.bestaetigt = bestaetigt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSchuelername(String schuelername) {
        this.schuelername = schuelername;
    }
}
