package com.ia.ia_base.models;

/**
 * IB Topic D.2 — Encapsulation
 *
 * User represents one row from the 'users' table.
 * Fields are private — access only through getters and setters.
 *
 * We never store the plain password, only the hash.
 * See PasswordHasher for how hashing works.
 */
public class User {
    private int id;
    private String email;
    private String passwordHash;

    public User() {
    }

    public User(int id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        return email;
    }
}