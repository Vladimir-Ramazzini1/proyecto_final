import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class WebServer {
    private static final int PORT = 8000;

    public static void main(String[] args) throws Exception {
        SistemaOlimpico sistema = new SistemaOlimpico();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Static files
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            Path file = Paths.get("web" + path).normalize();
            if (!Files.exists(file) || Files.isDirectory(file)) {
                String notFound = "404 - Archivo no encontrado";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
                exchange.close();
                return;
            }
            String type = guessContentType(file.toString());
            exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
            byte[] bytes = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        // API: listar atletas
        server.createContext("/api/atletas", exchange -> {
            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    List<Atleta> lista = sistema.getAtletasApi();
                    String json = "[" + String.join(",", lista.stream().map(WebServer::atletaToJson).toArray(String[]::new)) + "]";
                    byte[] out = json.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, out.length);
                    exchange.getResponseBody().write(out);
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
                    Map<String,String> data = parseJsonSimple(body);
                    String id = "ATL" + (sistema.getAtletasApi().size() + 1);
                    String nombre = data.getOrDefault("nombre", "");
                    int edad = Integer.parseInt(data.getOrDefault("edad","0"));
                    String disciplina = data.getOrDefault("disciplina","");
                    String departamento = data.getOrDefault("departamento","");
                    String nacionalidad = data.getOrDefault("nacionalidad","");
                    LocalDate fechaIngreso = LocalDate.now();
                    Atleta a = new Atleta(id, nombre, edad, disciplina, departamento, nacionalidad, fechaIngreso);
                    sistema.agregarAtletaDesdeApi(a);
                    byte[] out = atletaToJson(a).getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(201, out.length);
                    exchange.getResponseBody().write(out);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                String msg = "Error API atletas: " + e.getMessage();
                exchange.sendResponseHeaders(500, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
            } finally { exchange.close(); }
        });

        // API: entrenamientos
        server.createContext("/api/entrenamientos", exchange -> {
            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    String atletaId = null;
                    if (query != null) {
                        for (String pair : query.split("&")) {
                            String[] kv = pair.split("="); if (kv.length==2 && kv[0].equals("atletaId")) atletaId = java.net.URLDecoder.decode(kv[1], "UTF-8");
                        }
                    }
                    List<Entrenamiento> resultados = new ArrayList<>();
                    for (Atleta a : sistema.getAtletasApi()) {
                        if (atletaId==null || a.getId().equalsIgnoreCase(atletaId)) {
                            resultados.addAll(a.getEntrenamientos());
                        }
                    }
                    String json = "[" + String.join(",", resultados.stream().map(WebServer::entToJson).toArray(String[]::new)) + "]";
                    byte[] out = json.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, out.length);
                    exchange.getResponseBody().write(out);
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
                    Map<String,String> data = parseJsonSimple(body);
                    String id = "ENT" + System.currentTimeMillis();
                    String idAtleta = data.getOrDefault("idAtleta", "");
                    LocalDate fecha = data.containsKey("fecha") && !data.get("fecha").isEmpty() ? LocalDate.parse(data.get("fecha")) : LocalDate.now();
                    TipoEntrenamiento tipo = TipoEntrenamiento.valueOf(data.getOrDefault("tipo","RESISTENCIA"));
                    double valor = Double.parseDouble(data.getOrDefault("valor","0"));
                    String ubicacion = data.getOrDefault("ubicacion","nacional");
                    String pais = data.getOrDefault("pais","Guatemala");
                    Entrenamiento ent = new Entrenamiento(id, idAtleta, fecha, tipo, valor, ubicacion, pais);
                    boolean ok = sistema.agregarEntrenamientoDesdeApi(ent);
                    if (!ok) {
                        String msg = "Atleta no encontrado";
                        exchange.sendResponseHeaders(404, msg.length());
                        exchange.getResponseBody().write(msg.getBytes());
                    } else {
                        byte[] out = entToJson(ent).getBytes("UTF-8");
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                        exchange.sendResponseHeaders(201, out.length);
                        exchange.getResponseBody().write(out);
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                String msg = "Error API entrenos: " + e.getMessage();
                exchange.sendResponseHeaders(500, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
            } finally { exchange.close(); }
        });

        // API: planilla (JSON) -> calcula pagos por atleta para mes/año
        server.createContext("/api/planilla", exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); exchange.close(); return; }
                Map<String,String> q = parseQuery(exchange.getRequestURI().getQuery());
                int mes = Integer.parseInt(q.getOrDefault("mes", "0"));
                int anio = Integer.parseInt(q.getOrDefault("anio", "0"));
                CalculadoraPlanilla calc = new CalculadoraPlanilla();
                List<Atleta> atletas = sistema.getAtletasApi();
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                double total = 0.0;
                for (int i=0;i<atletas.size();i++){
                    Atleta a = atletas.get(i);
                    double pago = calc.calcularPagoMensual(a, mes, anio);
                    total += pago;
                    sb.append(String.format("{\"id\":\"%s\",\"nombre\":\"%s\",\"pago\":%.2f}", escape(a.getId()), escape(a.getNombreCompleto()), pago));
                    if (i<atletas.size()-1) sb.append(",");
                }
                sb.append("]");
                byte[] out = sb.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.getResponseHeaders().set("X-Planilla-Total", String.valueOf(total));
                exchange.sendResponseHeaders(200, out.length);
                exchange.getResponseBody().write(out);
            } catch (Exception e) {
                String msg = "Error calculando planilla: " + e.getMessage();
                exchange.sendResponseHeaders(500, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
            } finally { exchange.close(); }
        });

        // API: exportar planilla (CSV)
        server.createContext("/api/export/planilla", exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); exchange.close(); return; }
                Map<String,String> q = parseQuery(exchange.getRequestURI().getQuery());
                int mes = Integer.parseInt(q.getOrDefault("mes", "0"));
                int anio = Integer.parseInt(q.getOrDefault("anio", "0"));
                CalculadoraPlanilla calc = new CalculadoraPlanilla();
                List<Atleta> atletas = sistema.getAtletasApi();
                StringBuilder csv = new StringBuilder();
                csv.append("ID,Nombre,Pago\n");
                double total = 0.0;
                for (Atleta a : atletas) {
                    double pago = calc.calcularPagoMensual(a, mes, anio);
                    total += pago;
                    csv.append(String.format("%s,\"%s\",%.2f\n", a.getId(), a.getNombreCompleto().replace("\"","'"), pago));
                }
                csv.append(String.format(",TOTAL,%.2f\n", total));
                byte[] out = csv.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                exchange.getResponseHeaders().set("Content-Disposition", String.format("attachment; filename=planilla_%02d_%d.csv", mes, anio));
                exchange.sendResponseHeaders(200, out.length);
                exchange.getResponseBody().write(out);
            } catch (Exception e) {
                String msg = "Error exportando planilla: " + e.getMessage();
                exchange.sendResponseHeaders(500, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
            } finally { exchange.close(); }
        });

        // API: exportar atletas CSV
        server.createContext("/api/export/atletas", exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); exchange.close(); return; }
                List<Atleta> atletas = sistema.getAtletasApi();
                StringBuilder csv = new StringBuilder();
                csv.append("ID,Nombre,Edad,Disciplina,Departamento,Nacionalidad,FechaIngreso\n");
                for (Atleta a : atletas) {
                    csv.append(String.format("%s,\"%s\",%d,%s,%s,%s,%s\n", a.getId(), a.getNombreCompleto().replace("\"","'"), a.getEdad(), a.getDisciplina(), a.getDepartamento(), a.getNacionalidad(), a.getFechaIngreso()));
                }
                byte[] out = csv.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=atletas.csv");
                exchange.sendResponseHeaders(200, out.length);
                exchange.getResponseBody().write(out);
            } catch (Exception e) {
                String msg = "Error exportando atletas: " + e.getMessage();
                exchange.sendResponseHeaders(500, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
            } finally { exchange.close(); }
        });

        // API: exportar entrenamientos CSV
        server.createContext("/api/export/entrenamientos", exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); exchange.close(); return; }
                StringBuilder csv = new StringBuilder();
                csv.append("ID,Atleta,Fecha,Tipo,Valor,Ubicacion,Pais\n");
                for (Atleta a : sistema.getAtletasApi()) {
                    for (Entrenamiento e : a.getEntrenamientos()) {
                        csv.append(String.format("%s,\"%s\",%s,%s,%.2f,%s,%s\n", e.getId(), a.getNombreCompleto().replace("\"","'"), e.getFecha(), e.getTipo(), e.getValorRendimiento(), e.getUbicacion(), e.getPais()));
                    }
                }
                byte[] out = csv.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=entrenamientos.csv");
                exchange.sendResponseHeaders(200, out.length);
                exchange.getResponseBody().write(out);
            } catch (Exception e) {
                String msg = "Error exportando entrenamientos: " + e.getMessage();
                exchange.sendResponseHeaders(500, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
            } finally { exchange.close(); }
        });

        // API: exportar estadísticas TXT
        server.createContext("/api/export/estadisticas", exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); exchange.close(); return; }
                handleExportEstadisticas(exchange, sistema);
            } catch (Exception e) {
                String msg = "Error exportando estadísticas: " + e.getMessage();
                exchange.sendResponseHeaders(500, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
                exchange.close();
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Servidor web iniciado en http://localhost:" + PORT);
    }

    private static String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        return "text/plain";
    }

    private static String atletaToJson(Atleta a) {
        return String.format("{\"id\":\"%s\",\"nombreCompleto\":\"%s\",\"edad\":%d,\"disciplina\":\"%s\",\"departamento\":\"%s\",\"nacionalidad\":\"%s\",\"fechaIngreso\":\"%s\"}",
            escape(a.getId()), escape(a.getNombreCompleto()), a.getEdad(), escape(a.getDisciplina()), escape(a.getDepartamento()), escape(a.getNacionalidad()), a.getFechaIngreso().toString());
    }

    private static String entToJson(Entrenamiento e) {
        return String.format("{\"id\":\"%s\",\"idAtleta\":\"%s\",\"fecha\":\"%s\",\"tipo\":\"%s\",\"valor\":%.2f,\"ubicacion\":\"%s\",\"pais\":\"%s\"}",
            escape(e.getId()), escape(e.getIdAtleta()), e.getFecha().toString(), escape(e.getTipo().name()), e.getValorRendimiento(), escape(e.getUbicacion()), escape(e.getPais()));
    }

    private static String escape(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }

    // Very simple JSON parser for flat objects with string/number values (not robust but enough for forms)
    private static Map<String,String> parseJsonSimple(String body) {
        Map<String,String> map = new HashMap<>();
        body = body.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length()-1);
        // crude split by , but ignore commas inside quotes
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i=0;i<body.length();i++){
            char c = body.charAt(i);
            if (c=='\"') inQ = !inQ;
            if (c==',' && !inQ) { parts.add(cur.toString()); cur.setLength(0); } else cur.append(c);
        }
        if (cur.length()>0) parts.add(cur.toString());
        for (String p: parts) {
            String[] kv = p.split(":",2);
            if (kv.length<2) continue;
            String key = kv[0].trim().replaceAll("^\"|\"$", "");
            String val = kv[1].trim();
            val = val.replaceAll("^\"|\"$", "");
            map.put(key, val);
        }
        return map;
    }

    private static Map<String,String> parseQuery(String query) {
        Map<String,String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] parts = query.split("&");
        for (String p : parts) {
            String[] kv = p.split("=", 2);
            try {
                String k = java.net.URLDecoder.decode(kv[0], "UTF-8");
                String v = kv.length>1 ? java.net.URLDecoder.decode(kv[1], "UTF-8") : "";
                map.put(k, v);
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }
        return map;
    }

    // Export estadisticas as TXT
    // This builds a plain text report similar to exportarEstadisticasTXT
    private static void handleExportEstadisticas(HttpExchange exchange, SistemaOlimpico sistema) throws IOException {
        StringBuilder writer = new StringBuilder();
        writer.append("=== REPORTE DE ESTADÍSTICAS - COMITÉ OLÍMPICO GUATEMALTECO ===\n");
        writer.append("Fecha: " + java.time.LocalDate.now() + "\n");
        writer.append("Total atletas: " + sistema.getAtletasApi().size() + "\n\n");
        writer.append("" + "=".repeat(80) + "\n");

        for (Atleta atleta : sistema.getAtletasApi()) {
            writer.append("\nATLETA: " + atleta.getNombreCompleto() + "\n");
            writer.append("Disciplina: " + atleta.getDisciplina() + "\n");
            writer.append("Total entrenamientos: " + atleta.getEntrenamientos().size() + "\n");
            if (!atleta.getEntrenamientos().isEmpty()) {
                double suma = 0;
                double mejorMarca = Double.MIN_VALUE;
                int internacionales = 0;
                for (Entrenamiento ent : atleta.getEntrenamientos()) {
                    double valor = ent.getValorRendimiento();
                    suma += valor;
                    if (valor > mejorMarca) mejorMarca = valor;
                    if (ent.esInternacional()) internacionales++;
                }
                double promedio = suma / atleta.getEntrenamientos().size();
                writer.append(String.format("Promedio rendimiento: %.2f\n", promedio));
                writer.append(String.format("Mejor marca: %.2f\n", mejorMarca));
                writer.append("Entrenamientos internacionales: " + internacionales + "\n");
            }
            writer.append("-".repeat(40) + "\n");
        }

        byte[] out = writer.toString().getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=estadisticas.txt");
        exchange.sendResponseHeaders(200, out.length);
        exchange.getResponseBody().write(out);
        exchange.close();
    }
}
