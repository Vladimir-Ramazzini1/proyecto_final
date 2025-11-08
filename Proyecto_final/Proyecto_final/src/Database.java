import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    /**
     * Constructora para bases MySQL u otras JDBC. La URL debe ser completa, p.ej:
     * jdbc:mysql://host:3306/nombre_db?useSSL=false&serverTimezone=UTC
     */
    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection connect() throws SQLException {
        if (user == null || user.isEmpty()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, password);
    }

    public void init() {
        boolean isSqlite = url != null && url.startsWith("jdbc:sqlite:");
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Cargar driver opcionalmente para MySQL (compatibilidad)
            if (!isSqlite) {
                try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException ex) { /* no-op */ }
            }

            if (isSqlite) {
                // Habilitar llaves foraneas en SQLite
                try { stmt.execute("PRAGMA foreign_keys = ON;"); } catch (SQLException ignore) {}
            }

            String sqlAtletas;
            String sqlEntrenamientos;

            if (isSqlite) {
                sqlAtletas = "CREATE TABLE IF NOT EXISTS atletas (" +
                        "id TEXT PRIMARY KEY, " +
                        "nombre TEXT, " +
                        "edad INTEGER, " +
                        "disciplina TEXT, " +
                        "departamento TEXT, " +
                        "nacionalidad TEXT, " +
                        "fechaIngreso TEXT" +
                        ")";

                sqlEntrenamientos = "CREATE TABLE IF NOT EXISTS entrenamientos (" +
                        "id TEXT PRIMARY KEY, " +
                        "idAtleta TEXT, " +
                        "fecha TEXT, " +
                        "tipo TEXT, " +
                        "valor REAL, " +
                        "ubicacion TEXT, " +
                        "pais TEXT, " +
                        "FOREIGN KEY(idAtleta) REFERENCES atletas(id) ON DELETE CASCADE" +
                        ")";
            } else {
                // MySQL compatible types
                sqlAtletas = "CREATE TABLE IF NOT EXISTS atletas (" +
                        "id VARCHAR(64) PRIMARY KEY, " +
                        "nombre TEXT, " +
                        "edad INT, " +
                        "disciplina VARCHAR(100), " +
                        "departamento VARCHAR(100), " +
                        "nacionalidad VARCHAR(100), " +
                        "fechaIngreso VARCHAR(20)" +
                        ")";

                sqlEntrenamientos = "CREATE TABLE IF NOT EXISTS entrenamientos (" +
                        "id VARCHAR(64) PRIMARY KEY, " +
                        "idAtleta VARCHAR(64), " +
                        "fecha VARCHAR(20), " +
                        "tipo VARCHAR(32), " +
                        "valor DOUBLE, " +
                        "ubicacion VARCHAR(32), " +
                        "pais VARCHAR(64), " +
                        "FOREIGN KEY(idAtleta) REFERENCES atletas(id) ON DELETE CASCADE" +
                        ")";
            }

            stmt.execute(sqlAtletas);
            stmt.execute(sqlEntrenamientos);
        } catch (SQLException e) {
            System.out.println("Error inicializando la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void guardarAtleta(Atleta a) {
        boolean isSqlite = url != null && url.startsWith("jdbc:sqlite:");
        String sql;
        if (isSqlite) {
            sql = "INSERT OR REPLACE INTO atletas(id,nombre,edad,disciplina,departamento,nacionalidad,fechaIngreso) VALUES(?,?,?,?,?,?,?)";
        } else {
            sql = "INSERT INTO atletas(id,nombre,edad,disciplina,departamento,nacionalidad,fechaIngreso) VALUES(?,?,?,?,?,?,?) " +
                  "ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), edad=VALUES(edad), disciplina=VALUES(disciplina), departamento=VALUES(departamento), nacionalidad=VALUES(nacionalidad), fechaIngreso=VALUES(fechaIngreso)";
        }

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getId());
            ps.setString(2, a.getNombreCompleto());
            ps.setInt(3, a.getEdad());
            ps.setString(4, a.getDisciplina());
            ps.setString(5, a.getDepartamento());
            ps.setString(6, a.getNacionalidad());
            ps.setString(7, a.getFechaIngreso().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error guardando atleta en BD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void guardarEntrenamiento(Entrenamiento e) {
        boolean isSqlite = url != null && url.startsWith("jdbc:sqlite:");
        String sql;
        if (isSqlite) {
            sql = "INSERT OR REPLACE INTO entrenamientos(id,idAtleta,fecha,tipo,valor,ubicacion,pais) VALUES(?,?,?,?,?,?,?)";
        } else {
            sql = "INSERT INTO entrenamientos(id,idAtleta,fecha,tipo,valor,ubicacion,pais) VALUES(?,?,?,?,?,?,?) " +
                  "ON DUPLICATE KEY UPDATE idAtleta=VALUES(idAtleta), fecha=VALUES(fecha), tipo=VALUES(tipo), valor=VALUES(valor), ubicacion=VALUES(ubicacion), pais=VALUES(pais)";
        }

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getIdAtleta());
            ps.setString(3, e.getFecha().toString());
            ps.setString(4, e.getTipo().name());
            ps.setDouble(5, e.getValorRendimiento());
            ps.setString(6, e.getUbicacion());
            ps.setString(7, e.getPais());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Error guardando entrenamiento en BD: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public List<Atleta> cargarAtletas() {
        List<Atleta> lista = new ArrayList<>();
        String sql = "SELECT id,nombre,edad,disciplina,departamento,nacionalidad,fechaIngreso FROM atletas";
    try (Connection conn = connect(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String nombre = rs.getString("nombre");
                int edad = rs.getInt("edad");
                String disciplina = rs.getString("disciplina");
                String departamento = rs.getString("departamento");
                String nacionalidad = rs.getString("nacionalidad");
        String fechaStr = rs.getString("fechaIngreso");
        LocalDate fechaIngreso = (fechaStr == null || fechaStr.isEmpty()) ? LocalDate.now() : LocalDate.parse(fechaStr);

                Atleta a = new Atleta(id, nombre, edad, disciplina, departamento, nacionalidad, fechaIngreso);
                lista.add(a);
            }
        } catch (SQLException e) {
            System.out.println("Error cargando atletas desde BD: " + e.getMessage());
        }
        return lista;
    }

    public List<Entrenamiento> cargarEntrenamientos() {
        List<Entrenamiento> lista = new ArrayList<>();
        String sql = "SELECT id,idAtleta,fecha,tipo,valor,ubicacion,pais FROM entrenamientos";
        try (Connection conn = connect(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String idAtleta = rs.getString("idAtleta");
                LocalDate fecha = LocalDate.parse(rs.getString("fecha"));
                TipoEntrenamiento tipo = TipoEntrenamiento.valueOf(rs.getString("tipo"));
                double valor = rs.getDouble("valor");
                String ubicacion = rs.getString("ubicacion");
                String pais = rs.getString("pais");

                Entrenamiento e = new Entrenamiento(id, idAtleta, fecha, tipo, valor, ubicacion, pais);
                lista.add(e);
            }
        } catch (SQLException e) {
            System.out.println("Error cargando entrenamientos desde BD: " + e.getMessage());
        }
        return lista;
    }
}
