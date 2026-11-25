package com.ia.ia_base.models;

/**
 * Category represents one row from the 'categories' table.
 *
 * IB relationship: one Category has many Events (one-to-many).
 * In the database this is modelled with a foreign key: events.category_id → categories.id
 *
 * toString() returns the name so JavaFX ComboBox displays it correctly.
 */
public class Category {
    private int id;
    private String name;

    public Category() {
    }

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * toString() is used by ComboBox to display category name in dropdown.
     */
    @Override
    public String toString() {
        return name;
    }
}