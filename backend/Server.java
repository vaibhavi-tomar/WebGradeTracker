import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * The HTTP layer. This class ONLY deals with requests/responses —
 * it delegates all real work to StudentStore. This is what a
 * "backend" means in a client/server web app: it exposes data
 * over HTTP so any frontend (JS, mobile app, etc.) can talk to it.
 *
 * Endpoints:
 *   GET    /api/students          -> list all students (JSON)
 *   POST   /api/students          -> add a student   (JSON body)
 *   DELETE /api/students/{roll}   -> delete a student
 *   GET    /api/summary           -> class summary report (JSON)
 *   GET    /                      -> serves the frontend (index.html, etc.)
 */
public class Server {

    private static final StudentStore store = new StudentStore();

    public static void main(String[] args) throws IOException {
        // Cloud platforms (Render, Railway, etc.) assign a port dynamically
        // via the PORT environment variable. Fall back to 8080 for local use.
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null) ? Integer.parseInt(portEnv) : 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/students", Server::handleStudents);
        server.createContext("/api/summary", Server::handleSummary);
        server.createContext("/", Server::handleStatic);

        server.setExecutor(null); // default single-threaded executor
        server.start();

        System.out.println("=====================================");
        System.out.println(" Grade Tracker server running at:");
        System.out.println(" http://localhost:" + port + "/");
        System.out.println("=====================================");
    }

    // ---------- /api/students ----------

    private static void handleStudents(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (method.equals("GET") && path.equals("/api/students")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Student s : store.getAll()) list.add(s.toMap());
            sendJson(exchange, 200, list);
            return;
        }

        if (method.equals("POST") && path.equals("/api/students")) {
            handleAddStudent(exchange);
            return;
        }

        if (method.equals("DELETE") && path.startsWith("/api/students/")) {
            handleDeleteStudent(exchange, path);
            return;
        }

        sendJson(exchange, 404, errorMap("Route not found"));
    }

    private static void handleAddStudent(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange);
            Map<?, ?> data = (Map<?, ?>) Json.parse(body);

            Object nameObj = data.get("name");
            Object rollObj = data.get("rollNumber");
            Object marksObj = data.get("marks");

            if (!(nameObj instanceof String) || ((String) nameObj).trim().isEmpty()) {
                sendJson(exchange, 400, errorMap("Name is required"));
                return;
            }
            if (!(rollObj instanceof Number)) {
                sendJson(exchange, 400, errorMap("Roll number is required"));
                return;
            }
            if (!(marksObj instanceof List)) {
                sendJson(exchange, 400, errorMap("Marks must be a list of numbers"));
                return;
            }

            String name = ((String) nameObj).trim();
            int roll = ((Number) rollObj).intValue();
            List<Integer> marks = new ArrayList<>();
            for (Object m : (List<?>) marksObj) {
                if (!(m instanceof Number)) {
                    sendJson(exchange, 400, errorMap("Each mark must be a number"));
                    return;
                }
                int value = ((Number) m).intValue();
                if (value < 0 || value > 100) {
                    sendJson(exchange, 400, errorMap("Marks must be between 0 and 100"));
                    return;
                }
                marks.add(value);
            }

            Student added = store.add(name, roll, marks);
            if (added == null) {
                sendJson(exchange, 409, errorMap("Roll number " + roll + " already exists"));
                return;
            }
            sendJson(exchange, 201, added.toMap());

        } catch (Exception e) {
            sendJson(exchange, 400, errorMap("Invalid request data"));
        }
    }

    private static void handleDeleteStudent(HttpExchange exchange, String path) throws IOException {
        String rollStr = path.substring("/api/students/".length());
        try {
            int roll = Integer.parseInt(rollStr);
            boolean removed = store.delete(roll);
            if (removed) {
                sendJson(exchange, 200, Map.of("deleted", true));
            } else {
                sendJson(exchange, 404, errorMap("Student not found"));
            }
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, errorMap("Invalid roll number"));
        }
    }

    // ---------- /api/summary ----------

    private static void handleSummary(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        sendJson(exchange, 200, store.summary());
    }

    // ---------- Static frontend files ----------

    private static void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        File file = new File("frontend" + path);
        if (!file.exists() || file.isDirectory()) {
            byte[] notFound = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound);
            }
            return;
        }

        String contentType = "text/plain";
        if (path.endsWith(".html")) contentType = "text/html; charset=utf-8";
        else if (path.endsWith(".css")) contentType = "text/css";
        else if (path.endsWith(".js")) contentType = "application/javascript";

        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------- Helpers ----------

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        InputStream input = exchange.getRequestBody();
        byte[] chunk = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = Json.stringify(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, Object> errorMap(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", message);
        return map;
    }
}