package com.ia.ia_base.models;

import java.time.LocalDate;

/**
 * Event represents one row from the 'events' table.
 *
 * IB Topic D — associations between objects:
 *   userId   — foreign key, links this event to the user who created it
 *   category — a full Category object (not just the id), loaded via SQL JOIN
 *
 * Notice that category is an object inside another object.
 * This is called composition / association in OOP.
 */
public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDate date;

    // Foreign key - which user created this event
    private int userId;

    // Category loaded via JOIN (not just the ID, the full Category object)
    private Category category;

    public Event() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return title;
    }
}