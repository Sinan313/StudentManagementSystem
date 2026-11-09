# Student Management System

A Java Swing desktop application for managing student records. It supports creating, viewing, updating, deleting, and searching student records, with data stored locally in a CSV file.

> **Internship:** Auspify Technologies — Software Development Internship  
> **Task:** Task 1 (Easy) — Student Management System

## Features

- **Add students:** Create student records with an automatically suggested student ID.
- **View students:** Display records in a table and sort by column.
- **Update students:** Select a record, edit its details, and save the changes.
- **Delete students:** Remove a selected record after a confirmation prompt.
- **Search:** Filter records by ID, name, age, email, phone, or course.
- **Input validation:** Check required fields and reject invalid or duplicate data.
- **CSV persistence:** Save changes to `data/students.csv` so records remain available after restarting the application.

## Technology

- Java (JDK 8 or newer)
- Java Swing (desktop user interface)
- CSV file storage
- Git and GitHub (for version control and project sharing)

No external Java libraries or database server are required.

## Run the application

### Windows

1. Install a JDK (version 8 or newer).
2. Extract or clone this repository.
3. Open the project folder.
4. Double-click `run.bat`.

Or open Command Prompt in the project folder and run:

```bat
run.bat
```

### Run automated tests on Windows

```bat
run.bat test
```

### macOS / Linux

```bash
chmod +x run.sh
./run.sh
```

Run the tests:

```bash
./run.sh test
```

## Project structure

```text
StudentManagementSystem/
├── src/
│   └── sms/
│       ├── Student.java
│       ├── StudentValidator.java
│       ├── StudentRepository.java
│       └── StudentManagementApp.java
├── test/
│   └── sms/
│       └── StudentRepositoryTest.java
├── data/
│   └── students.csv
├── screenshots/
├── run.bat
├── run.sh
├── .gitignore
└── README.md
```

## Data storage

Student information is stored in `data/students.csv`. The file uses these fields:

```text
id,name,age,email,phone,course
```

The CSV file can be opened using a text editor or spreadsheet application. Keep a backup before manually editing it.

## Validation

The application validates student ID, name, age, email, phone number, and course information. Student IDs must be unique. Invalid entries are rejected with an explanatory message.

## Testing

### Manual testing

The developer reported completing all six manual checks:

| Check | Result |
|---|---|
| Application launches | PASS |
| Add student | PASS |
| Update student | PASS |
| Delete student | PASS |
| Search students | PASS |
| Data persists in `students.csv` | PASS |

### Automated testing

The project includes `StudentRepositoryTest.java`, a standalone test suite described in the project as containing 12 tests. Run it with `run.bat test` on Windows or `./run.sh test` on macOS/Linux.

## Screenshots

The `screenshots/` folder contains examples of the student list, update form, search, validation feedback, and delete confirmation. Add or update screenshots there as the application evolves.

## Internship task completion

- [x] Design the user interface
- [x] Implement student record creation
- [x] Add update and delete functionality
- [x] Display student information
- [x] Test application functionality

## Repository

After creating your GitHub repository, replace this line with its URL:

`https://github.com/YOUR-USERNAME/YOUR-REPOSITORY`

## Future improvements

Possible extensions include exporting records to PDF or Excel, adding course filters, and replacing CSV storage with SQLite or MySQL.

---
Created as a practical project for the Auspify Technologies Software Development Internship.
