# Container Management System

A full-stack Spring Boot web application built to digitize the manual Excel-based
process of tracking truck container dimensions during an internship at Maruti Suzuki.
Replaces spreadsheet upkeep with a database-backed system that automatically
calculates container volume, supports search, and imports/exports Excel data.

---

## 1. Project Overview

| | |
|---|---|
| **Purpose** | Store truck container dimensions, auto-calculate volume, and manage records through a web UI instead of manual Excel sheets |
| **Core business rule** | `Volume = Length × Width × Height`, always calculated server-side — never accepted as user input |
| **Backend** | Java 21, Spring Boot 3, Spring MVC, Spring Data JPA (Hibernate) |
| **Frontend** | HTML5, CSS3, Bootstrap 5, vanilla JavaScript, Thymeleaf |
| **Database** | MySQL |
| **Excel I/O** | Apache POI (import and export) |
| **Build tool** | Maven |

### Feature checklist

- ✅ Login page (UI only — see [Known Limitations](#7-known-limitations--future-scope))
- ✅ Dashboard (Total Containers, Average Volume, Latest 5 Containers)
- ✅ Add / View / Edit / Delete containers
- ✅ Automatic volume calculation (backend-enforced)
- ✅ Search by Truck Number and Vendor Code
- ✅ Export all records to Excel
- ✅ Import records from Excel (with per-row validation and a results summary)
- ✅ Field-level validation with clear error messages
- ✅ Centralized exception handling
- ✅ Responsive Bootstrap UI with a collapsible sidebar

---

## 2. Final Folder Structure

```
container-management/
├── pom.xml
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/example/containermanagement/
    │   │   ├── ContainerManagementApplication.java   # Spring Boot entry point
    │   │   │
    │   │   ├── controller/
    │   │   │   ├── ContainerController.java           # REST API (/api/containers/**)
    │   │   │   └── ViewController.java                 # Page routing (Thymeleaf views)
    │   │   │
    │   │   ├── service/
    │   │   │   ├── ContainerService.java                # Business logic contract
    │   │   │   └── ContainerServiceImpl.java             # Volume calc, search, import, export
    │   │   │
    │   │   ├── repository/
    │   │   │   └── ContainerRepository.java              # Spring Data JPA repository
    │   │   │
    │   │   ├── entity/
    │   │   │   └── Container.java                        # JPA entity / DB table mapping
    │   │   │
    │   │   ├── dto/
    │   │   │   └── ImportResult.java                     # Excel import summary response
    │   │   │
    │   │   └── exception/
    │   │       ├── ResourceNotFoundException.java         # 404 for missing containers
    │   │       ├── ErrorResponse.java                     # Standard error JSON shape
    │   │       └── GlobalExceptionHandler.java             # Centralized error handling
    │   │
    │   └── resources/
    │       ├── application.properties                      # DB, server, logging, upload config
    │       ├── templates/
    │       │   ├── login.html
    │       │   ├── dashboard.html
    │       │   ├── container-list.html
    │       │   ├── add-container.html
    │       │   ├── edit-container.html
    │       │   └── fragments/
    │       │       └── sidebar.html                         # Shared nav, reused across pages
    │       └── static/
    │           ├── css/
    │           │   └── style.css                            # Design tokens, layout, components
    │           └── js/
    │               ├── api.js                                # Shared fetch wrapper for REST calls
    │               ├── layout.js                             # Mobile sidebar toggle
    │               ├── login.js
    │               ├── dashboard.js
    │               ├── container-list.js
    │               └── container-form.js                     # Shared by Add & Edit forms
    │
    └── test/
        └── java/com/example/containermanagement/            # (reserved for future unit tests)
```

**Design notes:**
- Standard layered architecture: Controller → Service → Repository → Database.
- `ViewController` and `ContainerController` are kept separate — one returns HTML view names, the other returns JSON. Neither touches the database directly.
- `dto/` was introduced this cycle specifically for `ImportResult`, since it's a response shape that doesn't correspond to a database table and shouldn't live in `entity/`.
- The previously-empty `config/` package was removed — it had never been used since Day 1.

---

## 3. Database Schema

**Database:** `container_management_db` (MySQL, auto-created on first run via `createDatabaseIfNotExist=true`)

**Table:** `container`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | |
| `truck_number` | `VARCHAR(50)` | `NOT NULL` | Indexed (`idx_truck_number`) — used by search |
| `vendor_code` | `VARCHAR(50)` | `NOT NULL` | Indexed (`idx_vendor_code`) — used by search |
| `length` | `DECIMAL(10,2)` | `NOT NULL` | Must be > 0 |
| `width` | `DECIMAL(10,2)` | `NOT NULL` | Must be > 0 |
| `height` | `DECIMAL(10,2)` | `NOT NULL` | Must be > 0 |
| `volume` | `DECIMAL(15,2)` | `NOT NULL` | Always calculated server-side: `length × width × height` |

```sql
CREATE TABLE container (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    truck_number  VARCHAR(50)    NOT NULL,
    vendor_code   VARCHAR(50)    NOT NULL,
    length        DECIMAL(10,2)  NOT NULL,
    width         DECIMAL(10,2)  NOT NULL,
    height        DECIMAL(10,2)  NOT NULL,
    volume        DECIMAL(15,2)  NOT NULL
);

CREATE INDEX idx_truck_number ON container(truck_number);
CREATE INDEX idx_vendor_code  ON container(vendor_code);
```

`BigDecimal`/`DECIMAL` is used throughout instead of `double`/`FLOAT` to avoid floating-point
rounding errors in the volume calculation — important for data that could feed into
logistics or billing decisions later.

---

## 4. Project Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        BROWSER (Client)                       │
│                                                                 │
│   Thymeleaf-rendered pages (login, dashboard, containers...)  │
│   + JavaScript (api.js, container-list.js, etc.)               │
└───────────────────────────┬─────────────────────────────────┘
                             │  HTTP (fetch / form navigation)
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                         │
│                                                                 │
│   ViewController          →  returns Thymeleaf view names      │
│   ContainerController      →  REST API, returns JSON            │
│   GlobalExceptionHandler   →  intercepts & formats all errors  │
└───────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                        SERVICE LAYER                           │
│                                                                 │
│   ContainerService (interface) / ContainerServiceImpl          │
│   - Volume calculation (length × width × height)                │
│   - Search delegation                                          │
│   - Excel export (Apache POI, in-memory)                        │
│   - Excel import (Apache POI, row validation, batch save)       │
└───────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      REPOSITORY LAYER                          │
│                                                                 │
│   ContainerRepository extends JpaRepository<Container, Long>   │
│   (Spring Data JPA / Hibernate)                                 │
└───────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    MySQL — container_management_db             │
│                         table: container                        │
└─────────────────────────────────────────────────────────────┘
```

Each layer only talks to the one directly below it — controllers never touch the
repository, and the repository never contains business logic. This is what makes the
volume-calculation rule impossible to bypass: it lives in exactly one place
(`ContainerServiceImpl`), and every path that creates or updates a container
(manual form, REST API, Excel import) passes through that same service method.

---

## 5. API Documentation

Base path: `/api/containers`

| Method | Endpoint | Description | Request Body | Success Response |
|---|---|---|---|---|
| `POST` | `/api/containers` | Add a new container | `{truckNumber, vendorCode, length, width, height}` | `201 Created` + container (with calculated `volume`, `id`) |
| `GET` | `/api/containers` | Get all containers | — | `200 OK` + array |
| `GET` | `/api/containers/{id}` | Get one container by ID | — | `200 OK` + container, or `404` |
| `PUT` | `/api/containers/{id}` | Update a container | Same shape as POST | `200 OK` + updated container |
| `DELETE` | `/api/containers/{id}` | Delete a container | — | `204 No Content` |
| `GET` | `/api/containers/search?truckNumber=` | Search by truck number (partial, case-insensitive) | — | `200 OK` + array |
| `GET` | `/api/containers/search?vendorCode=` | Search by vendor code (partial, case-insensitive) | — | `200 OK` + array |
| `GET` | `/api/containers/export` | Download all records as `.xlsx` | — | `200 OK`, file stream |
| `POST` | `/api/containers/import` | Upload `.xlsx` (multipart `file` field) to bulk-import | `multipart/form-data` | `200 OK` + `{importedCount, failedCount, failureReasons[]}` |

**Note:** `volume` is never accepted in request bodies for POST/PUT — even if sent, the
backend recalculates and overwrites it in `ContainerServiceImpl`.

### Error response shape (all error cases)

```json
{
  "timestamp": "2026-07-22T10:15:30",
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "validationErrors": {
    "truckNumber": "Truck number cannot be empty",
    "length": "Length must be greater than zero"
  }
}
```
`validationErrors` is only present for field-validation failures (400s from `@Valid`);
it's omitted for 404s and 500s.

### Excel Import — file format

| Column A | Column B | Column C | Column D | Column E |
|---|---|---|---|---|
| Truck Number | Vendor Code | Length | Width | Height |

- Row 1 must be the header row (skipped automatically).
- Column F (Volume) must **not** exist — the backend calculates it.
- Fully blank rows are silently skipped (not counted as failures).
- Rows with missing/invalid data are skipped and counted separately; the rest of the file still imports.

---

## 6. Installation & Setup

### Prerequisites
- Java 21 (`java -version` to check)
- Maven
- MySQL Server, running locally

### Steps

1. **Clone/extract the project**, then navigate into it:
   ```bash
   cd container-management
   ```

2. **Set your MySQL password** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.password=your_mysql_password
   ```
   (The database `container_management_db` is created automatically on first run.)

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Open the app** in your browser:
   ```
   http://localhost:8080
   ```
   You'll be redirected to the login page, then into the app.

5. **Verify the schema** (optional):
   ```sql
   USE container_management_db;
   DESCRIBE container;
   ```

### Testing the API directly (Postman / curl)

```bash
curl -X POST http://localhost:8080/api/containers \
  -H "Content-Type: application/json" \
  -d '{"truckNumber":"MH12AB1234","vendorCode":"V001","length":10,"width":5,"height":3}'
```

---

## 7. Known Limitations & Future Scope

**Current limitations (by design, for this project's scope):**
- **Login is UI-only.** There's no Spring Security or user table yet — the login form
  redirects to the dashboard without checking credentials. Anyone who knows the URL
  can reach `/dashboard` or `/containers` directly.
- **No pagination.** The container list loads every record at once — fine for
  hundreds of rows, but would need pagination (`Pageable` in the repository) at
  larger scale.
- **No audit trail.** There's no record of who added/edited/deleted a container or when.
- **Single MySQL instance, no connection pooling tuning** beyond Spring Boot defaults.

**Possible future enhancements:**
1. **Real authentication** — Spring Security with a `User` entity, hashed passwords,
   and role-based access (e.g., Viewer vs Admin).
2. **Pagination & sorting** on the container list, using `Pageable`/`Sort` from Spring Data.
3. **Audit fields** — `createdAt`, `updatedAt`, `createdBy` on `Container`, via
   Spring Data JPA auditing (`@CreatedDate`, `@LastModifiedDate`).
4. **Soft delete** instead of hard delete, for record recovery.
5. **Unit and integration tests** — the `src/test` directory is scaffolded but empty;
   `ContainerServiceImplTest` (volume calculation, import validation) and
   `ContainerControllerTest` (via `MockMvc`) would be logical first additions.
6. **Dockerize** the app + MySQL with `docker-compose` for easier setup.
7. **Rate limiting / file size validation UI feedback** for the Excel import, beyond
   the current 10MB server-side cap.
8. **Dashboard trends** — e.g., a small chart of container volume over time, once
   `createdAt` timestamps exist to chart against.

---

## 8. Tech Stack Summary

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Web | Spring MVC |
| Data | Spring Data JPA + Hibernate |
| Database | MySQL |
| Templating | Thymeleaf |
| Frontend | Bootstrap 5, vanilla JavaScript |
| Excel I/O | Apache POI 5.2.5 |
| Build | Maven |
| Boilerplate reduction | Lombok |
