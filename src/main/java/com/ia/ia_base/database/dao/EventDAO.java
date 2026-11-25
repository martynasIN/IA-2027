package com.ia.ia_base.database.dao;

import com.ia.ia_base.models.Category;
import com.ia.ia_base.models.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Handles all database queries for the 'events' table.
 *
 * IB SQL concept — JOIN:
 *   Every query joins 'events' with 'categories' so we get
 *   the category name in one query without a second database call.
 *
 *   SELECT e.*, c.name AS category_name
 *   FROM events e
 *   JOIN categories c ON e.category_id = c.id
 */
public class EventDAO extends BaseDAO<Event> {

    // Base SELECT with JOIN — reused by every query below
    // so we always get the category name alongside event data.
    private static final String SELECT_WITH_CATEGORY =
        "SELECT e.id, e.title, e.description, e.event_date, e.user_id, e.category_id, " +
        "       c.name AS category_name " +
        "FROM events e " +
        "JOIN categories c ON e.category_id = c.id";

    /** Returns all events from all users, ordered by date. */
    @Override
    public List<Event> findAll() throws SQLException {
        String sql = SELECT_WITH_CATEGORY + " ORDER BY e.event_date DESC";
        return executeQuery(sql);
    }

    /**
     * Returns events that belong to one specific user.
     * WHERE filters by user_id — only that user's events are returned.
     */
    public List<Event> findByUserId(int userId) throws SQLException {
        String sql = SELECT_WITH_CATEGORY + " WHERE e.user_id = ? ORDER BY e.event_date DESC";
        return executeQuery(sql, userId);
    }

    @Override
    public Event findById(int id) throws SQLException {
        String sql = SELECT_WITH_CATEGORY + " WHERE e.id = ?";
        List<Event> results = executeQuery(sql, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Inserts a new event.
     * Stores user_id and category_id as foreign keys.
     */
    @Override
    public int create(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, description, event_date, user_id, category_id) " +
                     "VALUES (?, ?, ?, ?, ?)";
        return executeUpdate(sql,
            event.getTitle(),
            event.getDescription(),
            event.getDate(),         // LocalDate is automatically converted to SQL DATE
            event.getUserId(),
            event.getCategory().getId()
        );
    }

    /**
     * Updates an existing event.
     * user_id is not changed — an event always belongs to its original creator.
     */
    @Override
    public int update(Event event) throws SQLException {
        String sql = "UPDATE events SET title = ?, description = ?, event_date = ?, category_id = ? " +
                     "WHERE id = ?";
        return executeUpdate(sql,
            event.getTitle(),
            event.getDescription(),
            event.getDate(),
            event.getCategory().getId(),
            event.getId()
        );
    }

    @Override
    public int delete(int id) throws SQLException {
        String sql = "DELETE FROM events WHERE id = ?";
        return executeUpdate(sql, id);
    }

    /**
     * Returns true if an event with the same title, date, and user already exists.
     * Used by the API importer to skip events that were already imported before.
     *
     * Note: this method uses a direct Connection because BaseDAO.executeQuery()
     * maps rows to Event objects — it cannot handle a plain COUNT(*) result.
     */
    public boolean existsByTitleAndDate(String title, LocalDate date, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM events WHERE title = ? AND event_date = ? AND user_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setObject(2, date);
            stmt.setInt(3, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Converts one database row into an Event object.
     * Thanks to the JOIN, we also read category_name and build a
     * Category object here — no second query needed.
     */
    @Override
    protected Event mapResultSetToEntity(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setUserId(rs.getInt("user_id"));

        // Convert SQL DATE → Java LocalDate
        java.sql.Date sqlDate = rs.getDate("event_date");
        if (sqlDate != null) {
            event.setDate(sqlDate.toLocalDate());
        }

        // Build Category object from JOIN columns (no extra query needed!)
        Category category = new Category(
            rs.getInt("category_id"),
            rs.getString("category_name")
        );
        event.setCategory(category);

        return event;
    }
}