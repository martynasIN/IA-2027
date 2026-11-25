package com.ia.ia_base.util;

import com.ia.ia_base.models.User;

/**
 * IB Topic D.2 — Singleton design pattern
 *
 * Stores the currently logged-in user.
 * Any controller can get it by calling:
 *   SessionManager.getInstance().getCurrentUser()
 *
 * logout() sets currentUser to null — always call it before going back to Login.
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
        // Private constructor — Singleton pattern
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}