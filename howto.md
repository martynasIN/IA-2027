# How This Project Works — IB CS IA Guide

This document explains how the **Events App** example project is structured so that you can adapt it for your own IB CS Internal Assessment.

---

## Table of Contents

1. [What the App Does](#1-what-the-app-does)
2. [Prerequisites & Setup](#2-prerequisites--setup)
3. [Application Architecture](#3-application-architecture)
4. [Layer 1 — Database (MySQL)](#4-layer-1--database-mysql)
5. [Layer 2 — Models (Java Classes)](#5-layer-2--models-java-classes)
6. [Layer 3 — DAOs (Database Access)](#6-layer-3--daos-database-access)
7. [Layer 4 — Controllers & Views (UI)](#7-layer-4--controllers--views-ui)
8. [Key Utilities](#8-key-utilities)
9. [How to Add a New Entity to Your IA](#9-how-to-add-a-new-entity-to-your-ia)
10. [Common Patterns Reference](#10-common-patterns-reference)
11. [Using an External API](#11-using-an-external-api)

---

## 1. What the App Does

| Screen | What happens |
|---|---|
| **Login** | User enters email + password → verified against the database |
| **Registration** | New user is saved with a hashed password |
| **My Events** | Shows only the logged-in user's events (filtered by `user_id`) with their category loaded via a SQL JOIN |
| **Add / Edit Event** | Modal form — saves or updates a row in the `events` table |
| **Delete Event** | Removes the selected row from the database |

The three database tables demonstrate:

- **One-to-many**: one `User` owns many `Events`
- **Many-to-one**: many `Events` share one `Category`
- **SQL JOIN**: the Events query fetches the category name in the same query without a second database call

---

## 2. Prerequisites & Setup

### Software required

| Tool | Purpose |
|---|---|
| Java 24 | Programming language runtime |
| Maven | Build tool (compiles, runs the project) |
| MySQL Server | Database engine |
| IntelliJ IDEA | IDE (recommended) |
| Scene Builder | Visual FXML editor |

### Step 1 — Create the database

Run the setup script once in your MySQL terminal:

```bash
mysql -u root -p < database_setup.sql
```

Or paste the contents of `database_setup.sql` into MySQL Workbench / phpMyAdmin.

This creates the `events_app` database with three tables and inserts five default categories.

### Step 2 — Configure the connection

Copy `.env.example` → rename to `.env` and fill in your MySQL credentials:

```
DB_URL=jdbc:mysql://localhost:3306/
DB_NAME=events_app
DB_USER=root
DB_PASSWORD=your_password_here
```

> `.env` is listed in `.gitignore` — it will never be committed to Git. Never store passwords in source code.

### Step 3 — Run the application

```bash
mvn clean javafx:run
```

---

## 3. Application Architecture

The project is divided into four layers. Each layer has a single responsibility:

```
┌────────────────────────────────────────────────┐
│  Layer 4 — UI (FXML views + Controllers)       │  User sees and interacts with this
├────────────────────────────────────────────────┤
│  Layer 3 — DAOs (Data Access Objects)          │  Runs SQL queries
├────────────────────────────────────────────────┤
│  Layer 2 — Models (Plain Java Classes)         │  Represent rows from the database
├────────────────────────────────────────────────┤
│  Layer 1 — Database (MySQL tables)             │  Stores data permanently
└────────────────────────────────────────────────┘
```

**Flow of a user action** (e.g. clicking "Add Event"):

```
Button click (FXML)
  → EventsController.handleAddEvent()       [Layer 4]
    → EventFormController.handleSave()      [Layer 4]
      → EventDAO.create(event)              [Layer 3]
        → PreparedStatement INSERT          [Layer 1]
```

### Package structure

```
src/main/java/com/ia/ia_base/
│
├── MainApplication.java          ← starts the app
├── database/
│   ├── DatabaseConnection.java   ← opens MySQL connection (Singleton)
│   └── dao/
│       ├── BaseDAO.java          ← shared SQL helpers
│       ├── UserDAO.java
│       ├── CategoryDAO.java
│       └── EventDAO.java         ← demonstrates JOIN
├── models/
│   ├── User.java
│   ├── Category.java
│   └── Event.java
├── controllers/
│   ├── BaseController.java       ← shared UI helpers
│   ├── LoginController.java
│   ├── RegistrationController.java
│   ├── EventsController.java
│   └── EventFormController.java
└── util/
    ├── SessionManager.java       ← stores logged-in user (Singleton)
    ├── AlertManager.java         ← shows pop-up dialogs
    ├── PasswordHasher.java       ← BCrypt hashing
    ├── ApiClient.java            ← HTTP fetch + JSON parsing
    └── WindowManager.java        ← opens new windows

src/main/resources/com/ia/ia_base/views/
├── LoginView.fxml
├── RegistrationView.fxml
├── EventsView.fxml
└── EventFormView.fxml
```

---

## 4. Layer 1 — Database (MySQL)

### Schema

```sql
users          categories
──────         ──────────
id  (PK)       id  (PK)
email          name
password_hash

events
──────────────────────────────
id          (PK)
title
description
event_date
user_id     (FK → users.id)
category_id (FK → categories.id)
```

### Foreign keys

A **foreign key** is a column that references the primary key of another table, creating a link between rows.

```sql
-- event_date belongs to user with id = 3
INSERT INTO events (title, user_id, category_id) VALUES ('Team meeting', 3, 1);
```

If a user is deleted, all their events are automatically deleted too (`ON DELETE CASCADE`).

### The JOIN query

A **JOIN** combines rows from two tables into one result set:

```sql
SELECT e.id, e.title, e.event_date,
       c.name AS category_name        -- comes from categories table
FROM events e
JOIN categories c ON e.category_id = c.id
WHERE e.user_id = ?
ORDER BY e.event_date DESC;
```

Without a JOIN you would need two separate queries. With a JOIN you get everything in one go.

---

## 5. Layer 2 — Models (Java Classes)

A **model** is a plain Java class whose fields match the columns of a database table.

### Example — `Event.java`

```java
public class Event {
    private int      id;
    private String   title;
    private String   description;
    private LocalDate date;
    private int      userId;      // FK stored as int
    private Category category;    // full object loaded via JOIN

    // getters and setters ...
}
```

Key points:
- `userId` stores the **integer foreign key** (what goes in the database column).
- `category` stores a full `Category` object — this is populated from the JOIN result, not from a separate query.
- Every model must have **getters and setters** because JavaFX reads them when populating the TableView.

### `Category.toString()` matters

```java
@Override
public String toString() {
    return name;
}
```

JavaFX `ComboBox` calls `toString()` on each item to decide what text to display in the dropdown. Without this override it would show something like `com.ia.ia_base.models.Category@1a2b3c`.

---

## 6. Layer 3 — DAOs (Database Access)

**DAO (Data Access Object)** is a design pattern: each entity gets its own class that contains all the SQL for that table.

### `BaseDAO<T>` — the shared foundation

`BaseDAO` is an **abstract generic class** that provides two reusable methods so you never write connection-opening boilerplate yourself:

```java
// For SELECT queries — returns a List<T>
protected List<T> executeQuery(String sql, Object... params)

// For INSERT / UPDATE / DELETE — returns number of affected rows
protected int executeUpdate(String sql, Object... params)
```

Both methods use `PreparedStatement`, which prevents **SQL injection** — a critical security requirement for any application that accepts user input.

**SQL injection — unsafe vs safe:**

```java
// UNSAFE — never do this
String sql = "SELECT * FROM users WHERE email = '" + email + "'";

// SAFE — always use ? placeholders
String sql = "SELECT * FROM users WHERE email = ?";
executeQuery(sql, email);   // email is safely escaped
```

### Creating a DAO — required steps

Every DAO must:
1. Extend `BaseDAO<YourModel>`
2. Implement `mapResultSetToEntity(ResultSet rs)` — converts one database row to one Java object
3. Implement the five abstract methods: `findAll`, `findById`, `create`, `update`, `delete`

### `EventDAO` — the JOIN example

```java
// The base SELECT that always JOINs with categories
private static final String SELECT_WITH_CATEGORY =
    "SELECT e.id, e.title, e.description, e.event_date, e.user_id, e.category_id, " +
    "       c.name AS category_name " +
    "FROM events e " +
    "JOIN categories c ON e.category_id = c.id";

// Filter by user — adds WHERE clause
public List<Event> findByUserId(int userId) throws SQLException {
    String sql = SELECT_WITH_CATEGORY + " WHERE e.user_id = ? ORDER BY e.event_date DESC";
    return executeQuery(sql, userId);
}
```

In `mapResultSetToEntity`, the JOIN result gives us `category_name` as an extra column:

```java
@Override
protected Event mapResultSetToEntity(ResultSet rs) throws SQLException {
    Event event = new Event();
    event.setId(rs.getInt("id"));
    event.setTitle(rs.getString("title"));
    // ...

    // Build Category from the JOIN columns — no extra query needed
    Category category = new Category(
        rs.getInt("category_id"),
        rs.getString("category_name")   // available because of the JOIN
    );
    event.setCategory(category);

    return event;
}
```

---

## 7. Layer 4 — Controllers & Views (UI)

JavaFX uses **FXML** files for the visual layout and **Controller** classes for the logic.

### How FXML and Controller connect

An FXML file declares the controller at the top:

```xml
<VBox fx:controller="com.ia.ia_base.controllers.EventFormController">
```

Inside the FXML, `fx:id` links a UI element to a Java field:

```xml
<TextField fx:id="titleField" promptText="Event title"/>
```

In the controller, `@FXML` annotation tells JavaFX to inject that element:

```java
@FXML
private TextField titleField;   // name must match fx:id exactly
```

A button's `onAction` links to a controller method:

```xml
<Button text="Save" onAction="#handleSave"/>
```

```java
@FXML
private void handleSave() {
    String title = titleField.getText();
    // ...
}
```

### `initialize()` — automatic setup

JavaFX calls `initialize()` automatically after the FXML is loaded. Use it to:
- Create DAO instances
- Load data from the database
- Set up TableView columns

```java
@Override
public void initialize(URL location, ResourceBundle resources) {
    eventDAO = new EventDAO();
    setupTableColumns();
    loadEvents();
}
```

### Switching screens

`BaseController.changeScene()` replaces the content of the current window:

```java
changeScene("views/EventsView.fxml");
stage.setTitle("Events App - My Events");
stage.setWidth(900);
stage.setHeight(600);
```

### Opening a modal form

A **modal** window blocks the parent window until it is closed. The key call is `showAndWait()` — it **pauses execution** in the calling method until the modal closes.

```java
// In EventsController
FXMLLoader loader = new FXMLLoader(
    getClass().getResource("/com/ia/ia_base/views/EventFormView.fxml")
);
Parent root = loader.load();

EventFormController formController = loader.getController();
formController.setEvent(null);    // null = add mode
formController.setStage(formStage);

Stage formStage = new Stage();
formStage.initModality(Modality.APPLICATION_MODAL);
formStage.setScene(new Scene(root, 420, 360));
formStage.showAndWait();   // ← pauses here until form closes

loadEvents();              // ← runs only after form is closed
```

### TableView setup

TableView columns are bound to model fields using **lambda expressions**:

```java
// Simple field → SimpleStringProperty
titleColumn.setCellValueFactory(
    cellData -> new SimpleStringProperty(cellData.getValue().getTitle())
);

// Field from a related object (loaded via JOIN)
categoryColumn.setCellValueFactory(cellData -> {
    Category cat = cellData.getValue().getCategory();
    return new SimpleStringProperty(cat != null ? cat.getName() : "");
});
```

---

## 8. Key Utilities

### `DatabaseConnection` — Singleton pattern

The **Singleton** pattern ensures that only **one** database connection is created for the whole application, no matter how many times it is requested:

```java
// Always returns the same instance
DatabaseConnection.getInstance().getConnection();
```

Calling `getInstance()` from ten different controllers still uses the same connection object.

### `SessionManager` — who is logged in?

After login, the `User` object is stored in `SessionManager`:

```java
// Store on login
SessionManager.getInstance().setCurrentUser(user);

// Read from any controller
User me = SessionManager.getInstance().getCurrentUser();
int myId = me.getId();
```

`SessionManager` is also a Singleton — the same instance is shared across the whole application.

### `PasswordHasher` — BCrypt

Passwords must **never** be stored in plain text. BCrypt hashes the password with a random salt:

```java
// Registration — hash before saving
String hash = PasswordHasher.hashPassword(password);
user.setPasswordHash(hash);
userDAO.create(user);

// Login — verify without reversing the hash
boolean valid = PasswordHasher.verifyPassword(enteredPassword, storedHash);
```

### `AlertManager` — pop-up dialogs

```java
AlertManager.showInfo("Success", "Event saved successfully.");
AlertManager.showError("Error", "Please fill in all fields.");
AlertManager.showWarning("Warning", "No item selected.");
boolean confirmed = AlertManager.confirmDelete("Team meeting");
```

### `ApiClient` — fetch data from an external API

See the full explanation in [Section 11](#11-using-an-external-api).

```java
// Load configured endpoints from .env
List<ApiClient.ApiEndpoint> endpoints = ApiClient.loadEndpoints();

// Fetch JSON text from a URL (with optional API key)
String json = ApiClient.fetchJson(endpoint.url(), endpoint.key());

// Parse JSON into Event objects
List<Event> events = ApiClient.parseEvents(json, categoryDAO.findAll());
```

---

## 9. How to Add a New Entity to Your IA

Follow these four steps whenever you need a new type of data in your application.

### Step 1 — Add a table to `database_setup.sql`

```sql
CREATE TABLE IF NOT EXISTS books (
    id          INT          AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    author      VARCHAR(255),
    year        INT,
    user_id     INT          NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

Re-run the setup script after any schema change.

### Step 2 — Create a Model class

```java
// src/main/java/com/ia/ia_base/models/Book.java
public class Book {
    private int    id;
    private String title;
    private String author;
    private int    year;
    private int    userId;

    // Add getters and setters for every field
    public int    getId()     { return id; }
    public void   setId(int id) { this.id = id; }
    // ...

    @Override
    public String toString() { return title; }
}
```

### Step 3 — Create a DAO class

```java
// src/main/java/com/ia/ia_base/database/dao/BookDAO.java
public class BookDAO extends BaseDAO<Book> {

    @Override
    public List<Book> findAll() throws SQLException {
        return executeQuery("SELECT * FROM books ORDER BY title");
    }

    public List<Book> findByUserId(int userId) throws SQLException {
        return executeQuery("SELECT * FROM books WHERE user_id = ?", userId);
    }

    @Override
    public Book findById(int id) throws SQLException {
        List<Book> r = executeQuery("SELECT * FROM books WHERE id = ?", id);
        return r.isEmpty() ? null : r.get(0);
    }

    @Override
    public int create(Book book) throws SQLException {
        return executeUpdate(
            "INSERT INTO books (title, author, year, user_id) VALUES (?, ?, ?, ?)",
            book.getTitle(), book.getAuthor(), book.getYear(), book.getUserId()
        );
    }

    @Override
    public int update(Book book) throws SQLException {
        return executeUpdate(
            "UPDATE books SET title = ?, author = ?, year = ? WHERE id = ?",
            book.getTitle(), book.getAuthor(), book.getYear(), book.getId()
        );
    }

    @Override
    public int delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM books WHERE id = ?", id);
    }

    @Override
    protected Book mapResultSetToEntity(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getInt("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setYear(rs.getInt("year"));
        book.setUserId(rs.getInt("user_id"));
        return book;
    }
}
```

### Step 4 — Create FXML View + Controller

Copy `EventsView.fxml` and `EventsController.java` as starting templates.
Rename them to `BooksView.fxml` and `BooksController.java`.
Update:
- The `fx:controller` attribute in the FXML
- The column names and `setCellValueFactory` bindings in the controller
- The DAO type from `EventDAO` to `BookDAO`
- The model type from `Event` to `Book`

To navigate to the new screen from anywhere:

```java
changeScene("views/BooksView.fxml");
stage.setTitle("My Books");
```

---

## 10. Common Patterns Reference

### Read all items and show in TableView

```java
// In initialize() or a loadData() method
List<Book> books = bookDAO.findByUserId(
    SessionManager.getInstance().getCurrentUser().getId()
);
booksTable.setItems(FXCollections.observableArrayList(books));
```

### Save a new item from a form

```java
Book book = new Book();
book.setTitle(titleField.getText().trim());
book.setUserId(SessionManager.getInstance().getCurrentUser().getId());
bookDAO.create(book);
closeWindow();
```

### Update an existing item

```java
selectedBook.setTitle(titleField.getText().trim());
bookDAO.update(selectedBook);
```

### Delete the selected row

```java
Book selected = booksTable.getSelectionModel().getSelectedItem();
if (selected == null) {
    AlertManager.showWarning("No Selection", "Please select an item first.");
    return;
}
if (AlertManager.confirmDelete(selected.getTitle())) {
    bookDAO.delete(selected.getId());
    loadData(); // refresh the table
}
```

### Load related data into a ComboBox

```java
// In initialize()
List<Category> categories = categoryDAO.findAll();
categoryComboBox.setItems(FXCollections.observableArrayList(categories));

// category.toString() controls what text the ComboBox shows
```

### JOIN query template

Use this when you want to display data from two tables together:

```java
private static final String SELECT_WITH_JOIN =
    "SELECT a.id, a.title, a.other_field, " +
    "       b.name AS related_name " +      // column from joined table
    "FROM table_a a " +
    "JOIN table_b b ON a.b_id = b.id";      // join condition

// In mapResultSetToEntity:
RelatedObject rel = new RelatedObject(rs.getInt("b_id"), rs.getString("related_name"));
entity.setRelatedObject(rel);
```

---

## 11. Using an External API

Many IA projects need to fetch data from an external service — weather, sports results, event feeds, etc. This section explains how that works in this project and how to adapt it for your own API.

### What is an API?

An **API** (Application Programming Interface) is a URL you send an HTTP request to, and it sends back data — usually in **JSON** format.

```
Your app  →  GET https://example.com/data.json  →  Server
Your app  ←  {"items": [{...}, {...}]}           ←  Server
```

A **public API** requires no login — just the URL.
A **private API** requires a secret key sent with every request.

### How This App Uses an API

The flow has three steps:

**Step 1 — Configure the endpoint in `.env`**

```
API_1_NAME=Vilnius Events
API_1_URL=https://comealone.app/feed/v1/city/vilnius.json
API_1_KEY=
```

For a private API that requires a key, put the key in `API_1_KEY`.
You can add more sources by incrementing the number: `API_2_URL`, `API_3_URL`, etc.

**Step 2 — Send the HTTP request**

`ApiClient.fetchJson()` uses `java.net.http.HttpClient` — built into Java 11+, no extra library needed:

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .GET()
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
String json = response.body(); // the full JSON text as a String
```

If a key is configured, it is added as an HTTP header:

```java
builder.header("Authorization", "Bearer " + apiKey);
```

**Step 3 — Parse JSON and save to the database**

`ApiClient.parseEvents()` reads each field from the JSON text and builds `Event` objects:

```java
List<Category> categories = categoryDAO.findAll();
List<Event> apiEvents = ApiClient.parseEvents(json, categories);

for (Event event : apiEvents) {
    if (!eventDAO.existsByTitleAndDate(event.getTitle(), event.getDate(), event.getUserId())) {
        eventDAO.create(event);  // skip if already imported
    }
}
```

### What is JSON?

JSON is plain text that represents structured data. Every value is one of:

| Type | Example |
|---|---|
| String | `"Hello"` |
| Number | `42` or `3.14` |
| Boolean | `true` or `false` |
| Null | `null` |
| Object | `{"name": "Jazz Night", "price": "free"}` |
| Array | `[{...}, {...}, {...}]` |

A typical API response looks like one of these:

```json
[{"title": "Jazz Night", "date": "2026-12-07"}]
```
or wrapped in an object:
```json
{"events": [{"title": "Jazz Night", "date": "2026-12-07"}]}
```

### How JSON is Parsed Here (No Library)

This project parses JSON using only `java.util.regex` from the standard library.

**Splitting the array into objects — `splitJsonObjects()`**

The method walks through the JSON character by character, tracking brace depth and whether we are inside a string value. When depth returns to zero, one complete `{...}` object has been found.

```java
// tracks depth: { increases it, } decreases it
// inString: ignore braces that appear inside "quoted values"
if (c == '{') { if (depth == 0) start = i; depth++; }
else if (c == '}') { depth--; if (depth == 0) objects.add(...); }
```

**Reading a field value — `getString()`**

A regular expression matches the pattern `"fieldName": "value"`:

```java
Pattern p = Pattern.compile(
    "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
```

This handles escaped characters inside the value (e.g. `\"`, `\\`, `\n`).

**Handling wrapped responses — `extractJsonArray()`**

If the response is `{"events": [...]}` instead of `[...]`, the method finds the `[` after the key name and walks to the matching `]`:

```java
String arrayJson = json.stripLeading().startsWith("[")
    ? json                             // already an array
    : extractJsonArray(json, "events"); // extract from wrapper object
```

### How to Adapt for Your Own API

**Step 1 — Set the endpoint in `.env`:**
```
API_1_NAME=My Source
API_1_URL=https://your-api.com/data.json
API_1_KEY=secret123
```

**Step 2 — If the array is under a different key, update this line in `parseEvents()`:**
```java
: extractJsonArray(json, "events");   // ← change "events" to your key
```

**Step 3 — Update the field names to match your API's JSON:**
```java
event.setTitle(getString(obj, "name", "Untitled"));  // your title field
event.setDescription(getString(obj, "details", "")); // your description field
```

**Step 4 — Update the date parsing if your API uses a different format:**
```java
// ISO 8601 datetime — already handled:
// "2026-12-07T18:00:00+02:00"
OffsetDateTime.parse(start, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()

// Plain date — change to:
// "2026-12-07"
LocalDate.parse(start)
```

### Common API Formats

| Situation | What to do |
|---|---|
| Public API (no key) | Leave `API_1_KEY=` empty |
| Key in Authorization header | Set `API_1_KEY=yourkey` — it is sent automatically |
| Response is a plain array `[...]` | Works automatically |
| Response is wrapped `{"data":[...]}` | Change `"events"` → `"data"` in `extractJsonArray()` |
| Date format `"2026-12-07"` | Use `LocalDate.parse(start)` instead of `OffsetDateTime.parse` |
| Date format `"07/12/2026"` | Use `LocalDate.parse(start, DateTimeFormatter.ofPattern("dd/MM/yyyy"))` |

---

## IB CS IA Checklist

Before submitting, make sure you can explain:

- [ ] What each class is responsible for (single responsibility principle)
- [ ] Why Singleton is used for `DatabaseConnection` and `SessionManager`
- [ ] How `PreparedStatement` prevents SQL injection
- [ ] What a foreign key is and how it enforces data integrity
- [ ] How a JOIN works and why it is more efficient than two separate queries
- [ ] How passwords are stored securely (BCrypt, salting)
- [ ] The flow from a button click to a database change and back to the UI
- [ ] What an HTTP GET request is and how `HttpClient` sends one
- [ ] How JSON is structured and how a field value is extracted from it
- [ ] Why duplicate events are checked before inserting API data