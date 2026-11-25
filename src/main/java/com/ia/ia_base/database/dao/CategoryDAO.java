package com.ia.ia_base.database.dao;

import com.ia.ia_base.models.Category;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Handles all database queries for the 'categories' table.
 * Extends BaseDAO<Category> — inherits all query methods.
 */
public class CategoryDAO extends BaseDAO<Category> {

    @Override
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT * FROM categories ORDER BY name";
        return executeQuery(sql);
    }

    @Override
    public Category findById(int id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ?";
        List<Category> results = executeQuery(sql, id);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public int create(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name) VALUES (?)";
        return executeUpdate(sql, category.getName());
    }

    @Override
    public int update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ? WHERE id = ?";
        return executeUpdate(sql, category.getName(), category.getId());
    }

    @Override
    public int delete(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ?";
        return executeUpdate(sql, id);
    }

    /**
     * Converts one database row into a Category object.
     */
    @Override
    protected Category mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Category(rs.getInt("id"), rs.getString("name"));
    }
}