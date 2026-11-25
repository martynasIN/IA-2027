package com.ia.ia_base.database.dao;

import com.ia.ia_base.models.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * IB Topic D.2 — Inheritance (extends BaseDAO)
 *
 * Handles all database queries for the 'users' table.
 * Inherits executeQuery() and executeUpdate() from BaseDAO.
 * Only provides the SQL strings and maps rows to User objects.
 */
public class UserDAO extends BaseDAO<User> {

    @Override
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY email";
        return executeQuery(sql);
    }

    @Override
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> results = executeQuery(sql, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Finds a user by email — used during login.
     * Returns null if no user with that email exists.
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> results = executeQuery(sql, email);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public int create(User user) throws SQLException {
        String sql = "INSERT INTO users (email, password_hash) VALUES (?, ?)";
        return executeUpdate(sql, user.getEmail(), user.getPasswordHash());
    }

    @Override
    public int update(User user) throws SQLException {
        String sql = "UPDATE users SET email = ?, password_hash = ? WHERE id = ?";
        return executeUpdate(sql, user.getEmail(), user.getPasswordHash(), user.getId());
    }

    @Override
    public int delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        return executeUpdate(sql, id);
    }

    /**
     * Converts one database row into a User object.
     * Called automatically by executeQuery() for each row returned.
     */
    @Override
    protected User mapResultSetToEntity(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        return user;
    }
}