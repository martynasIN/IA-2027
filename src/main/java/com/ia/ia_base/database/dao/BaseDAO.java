package com.ia.ia_base.database.dao;

import com.ia.ia_base.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * IB Topic D.2 — Abstract class and inheritance
 *
 * BaseDAO provides shared database methods for all DAOs.
 * UserDAO, CategoryDAO, EventDAO all extend this class and
 * inherit executeQuery() and executeUpdate() for free.
 *
 * IB security — SQL injection prevention:
 *   SAFE:   "SELECT * FROM users WHERE id = ?"  → pass id as a parameter
 *   UNSAFE: "SELECT * FROM users WHERE id = " + id  → never do this!
 *   PreparedStatement escapes all parameters automatically.
 */
public abstract class BaseDAO<T> {
    protected DatabaseConnection dbConnection;
    
    public BaseDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }
    
    /**
     * Runs a SELECT query and returns a list of objects.
     * Each row is converted by mapResultSetToEntity() in the child class.
     *
     * @param sql    SQL query with ? placeholders
     * @param params values that replace the ? placeholders
     */
    protected List<T> executeQuery(String sql, Object... params) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }
            }
        }
        return results;
    }
    
    /**
     * Runs an INSERT, UPDATE or DELETE query.
     *
     * @param sql    SQL query with ? placeholders
     * @param params values that replace the ? placeholders
     * @return number of rows affected (usually 1)
     */
    protected int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = dbConnection.getConnection();
        try {
            // Ensure autocommit is enabled
            conn.setAutoCommit(true);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameters(stmt, params);
                int result = stmt.executeUpdate();
                // Changes are automatically committed due to autocommit=true
                return result;
            }
        } catch (SQLException e) {
            // Re-throw to let caller handle
            throw e;
        }
    }
    
    /**
     * Binds parameter values to the ? placeholders in the SQL query.
     * setObject() handles escaping automatically — this stops SQL injection.
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        
        for (int i = 0; i < params.length; i++) {
            // PreparedStatement.setObject() automatically escapes and quotes values
            // This prevents SQL injection attacks
            stmt.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * Converts one database row (ResultSet) into a Java object.
     * Must be implemented in each child DAO class.
     */
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;
    
    // -------------------------------------------------------
    // IB Topic D.2 — abstract methods (must be implemented
    // in every child class — UserDAO, EventDAO, CategoryDAO)
    // -------------------------------------------------------

    public abstract List<T> findAll() throws SQLException;

    public abstract T findById(int id) throws SQLException;

    public abstract int create(T entity) throws SQLException;

    public abstract int update(T entity) throws SQLException;

    public abstract int delete(int id) throws SQLException;
}

