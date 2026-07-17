# 📒 The Grade Ledger — Student Grade Tracker

A full-stack student grade management app: a **Java backend** (plain JDK, no frameworks) exposing a REST API, and a **JavaScript frontend** that consumes it — built, containerized, and deployed to the cloud.

**🔗 Live demo:** https://webgradetracker.onrender.com
*(Free-tier hosting — the app may take 30–60 seconds to wake up on the first visit after inactivity.)*

---

## ✨ Features

- **Register students** with a name, roll number, and any number of subject marks
- **Automatic calculations** — average, highest, lowest, and letter grade per student
- **Class report** — class average, topper, weakest performer, and a full ranked leaderboard
- **Delete students** from the ledger
- **Data persistence** — records are saved to disk and reloaded automatically on server restart
- **Input validation** on both frontend and backend (no crashes on bad input, no duplicate roll numbers, marks bounded 0–100)
- **No external dependencies** — the backend runs on nothing but the standard JDK (built-in `HttpServer`, hand-rolled JSON parser)

## 🏗️ Architecture

```
Browser (HTML/CSS/JS)
        │  fetch() → JSON over HTTP
        ▼
Java HttpServer (Server.java)
        │
        ▼
StudentStore  ──────────────►  students_data.txt
(business logic)                (persistence)
        │
        ▼
   Student.java
  (data model)
```

The frontend and backend are fully decoupled — the JS talks to the Java server only through a REST API, the same way it would talk to any backend written in any language.

## 🔌 API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/students` | List all students |
| `POST` | `/api/students` | Add a student — body: `{ "name": "...", "rollNumber": 101, "marks": [88, 92, 76] }` |
| `DELETE` | `/api/students/{roll}` | Remove a student by roll number |
| `GET` | `/api/summary` | Class average, topper, weakest student, and ranking |

## 📁 Project Structure

```
WebGradeTracker/
├── Dockerfile              # container build for deployment
├── backend/                # Java REST API
│   ├── Json.java           # minimal JSON parser/writer (no external lib)
│   ├── Student.java        # data model
│   ├── StudentStore.java   # business logic + file persistence
│   └── Server.java         # HTTP server + routes
└── frontend/                # JavaScript client
    ├── index.html
    ├── style.css
    └── script.js
```

## 🚀 Running it locally

Requires JDK 8+ (`javac -version` to check).

```bash
git clone https://github.com/vaibhavi-tomar/WebGradeTracker.git
cd WebGradeTracker
javac backend/*.java -d backend
java -cp backend Server
```

Then open **http://localhost:8080** in your browser.

## ☁️ Deployment

This app is containerized with Docker and deployed on [Render](https://render.com). The `Dockerfile` builds a small JDK image, compiles the Java source, and runs the server — Render supplies the port at runtime via the `PORT` environment variable, which the server reads automatically.

## 🛠️ Tech Stack

- **Backend:** Java (JDK `HttpServer`, no frameworks)
- **Frontend:** HTML, CSS, vanilla JavaScript (`fetch` API)
- **Deployment:** Docker + Render

## 🔮 Possible future improvements

- Edit existing student marks (currently add/delete only)
- Export the ledger as CSV or PDF
- Persistent database instead of a flat file (for real long-term storage)
- Authentication for multi-teacher use

---

Built as a learning project to practice full-stack architecture: separating a data model, business logic, an HTTP API layer, and a decoupled frontend.
