module com.ia.ia_base {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.base;
    requires java.sql;
    requires java.net.http;
    requires jbcrypt;

    opens com.ia.ia_base to javafx.fxml;
    opens com.ia.ia_base.controllers to javafx.fxml;

    exports com.ia.ia_base;
    exports com.ia.ia_base.controllers;
    exports com.ia.ia_base.models;
    exports com.ia.ia_base.database;
    exports com.ia.ia_base.database.dao;
    exports com.ia.ia_base.util;
}