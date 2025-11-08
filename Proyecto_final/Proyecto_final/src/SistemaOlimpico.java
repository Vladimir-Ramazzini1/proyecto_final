import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SistemaOlimpico {
    private List<Atleta> atletas;
    private Database db;
    private static final String ARCHIVO_ATLETAS_JSON = "atletas.json";
    private static final String ARCHIVO_ENTRENAMIENTOS_JSON = "entrenamientos.json";
    
    public SistemaOlimpico() {
        this.atletas = new ArrayList<>();
        // Intentar leer configuración de base de datos desde db.properties
        Properties cfg = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            cfg.load(fis);
            String url = cfg.getProperty("db.url");
            String user = cfg.getProperty("db.user");
            String pass = cfg.getProperty("db.password");
            if (url != null && !url.isEmpty()) {
                db = new Database(url, user, pass);
                db.init();
                System.out.println("Conectando a la base de datos configurada en db.properties...");
            } else {
                System.out.println("db.properties encontrado pero db.url no está definido. No se inicializa BD.");
                db = null;
            }
        } catch (FileNotFoundException fnf) {
            System.out.println("No se encontró db.properties. Para usar MySQL cree el archivo db.properties con las credenciales.");
            db = null;
        } catch (IOException ioe) {
            System.out.println("Error leyendo db.properties: " + ioe.getMessage());
            db = null;
        }

        cargarDatos();
    }
    
    public static void main(String[] args) {
        SistemaOlimpico sistema = new SistemaOlimpico();
        sistema.mostrarMenuPrincipal();
    }
    
    private void mostrarMenuPrincipal() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n=== SISTEMA OLÍMPICO GUATEMALTECO ===");
            System.out.println("1. Gestión de Atletas");
            System.out.println("2. Gestión de Entrenamientos");
            System.out.println("3. Estadísticas y Reportes");
            System.out.println("4. Gestión Financiera (Planilla)");
            System.out.println("5. Exportar Reportes");
            System.out.println("6. Guardar Datos");
            System.out.println("7. Salir");
            System.out.print("Seleccione una opción: ");
            
            int opcion = scanner.nextInt();
            scanner.nextLine();
            
            switch (opcion) {
                case 1:
                    menuGestionAtletas(scanner);
                    break;
                case 2:
                    menuGestionEntrenamientos(scanner);
                    break;
                case 3:
                    menuEstadisticas(scanner);
                    break;
                case 4:
                    menuPlanilla(scanner);
                    break;
                case 5:
                    menuExportarReportes(scanner);
                    break;
                case 6:
                    guardarDatos();
                    break;
                case 7:
                    guardarDatos();
                    System.out.println("¡Hasta pronto!");
                    return;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }
    
    private void menuGestionAtletas(Scanner scanner) {
        while (true) {
            System.out.println("\n=== GESTIÓN DE ATLETAS ===");
            System.out.println("1. Registrar nuevo atleta");
            System.out.println("2. Listar atletas");
            System.out.println("3. Buscar atleta");
            System.out.println("4. Editar atleta");
            System.out.println("5. Eliminar atleta");
            System.out.println("6. Volver al menú principal");
            System.out.print("Seleccione una opción: ");
            
            int opcion = scanner.nextInt();
            scanner.nextLine();
            
            switch (opcion) {
                case 1:
                    registrarAtleta(scanner);
                    break;
                case 2:
                    listarAtletas();
                    break;
                case 3:
                    buscarAtleta(scanner);
                    break;
                case 4:
                    editarAtleta(scanner);
                    break;
                case 5:
                    eliminarAtleta(scanner);
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }

    private void editarAtleta(Scanner scanner) {
        System.out.print("Ingrese ID del atleta a editar: ");
        String id = scanner.nextLine();
        Atleta atleta = buscarAtletaPorId(id);
        if (atleta == null) {
            System.out.println("Atleta no encontrado.");
            return;
        }

        System.out.println("Deje en blanco para mantener el valor actual.");
        System.out.println("Nombre actual: " + atleta.getNombreCompleto());
        System.out.print("Nuevo nombre: ");
        String nombre = scanner.nextLine();
        if (!nombre.trim().isEmpty()) atleta.setNombreCompleto(nombre.trim());

        System.out.println("Edad actual: " + atleta.getEdad());
        System.out.print("Nueva edad: ");
        String edadStr = scanner.nextLine();
        if (!edadStr.trim().isEmpty()) {
            try { atleta.setEdad(Integer.parseInt(edadStr.trim())); } catch (NumberFormatException e) { System.out.println("Edad inválida, se mantiene la anterior."); }
        }

        System.out.println("Disciplina actual: " + atleta.getDisciplina());
        System.out.print("Nueva disciplina: ");
        String disc = scanner.nextLine(); if (!disc.trim().isEmpty()) atleta.setDisciplina(disc.trim());

        System.out.println("Departamento actual: " + atleta.getDepartamento());
        System.out.print("Nuevo departamento: ");
        String dep = scanner.nextLine(); if (!dep.trim().isEmpty()) atleta.setDepartamento(dep.trim());

        System.out.println("Nacionalidad actual: " + atleta.getNacionalidad());
        System.out.print("Nueva nacionalidad: ");
        String nat = scanner.nextLine(); if (!nat.trim().isEmpty()) atleta.setNacionalidad(nat.trim());

        System.out.println("Fecha de ingreso actual: " + atleta.getFechaIngreso());
        System.out.print("Nueva fecha de ingreso (YYYY-MM-DD) o Enter: ");
        String fechaStr = scanner.nextLine();
        if (!fechaStr.trim().isEmpty()) {
            try { atleta.setFechaIngreso(LocalDate.parse(fechaStr.trim())); } catch (Exception e) { System.out.println("Fecha inválida, se mantiene la anterior."); }
        }

        if (db != null) db.guardarAtleta(atleta);
        System.out.println("Atleta actualizado: " + atleta);
    }

    private void eliminarAtleta(Scanner scanner) {
        System.out.print("Ingrese ID del atleta a eliminar: ");
        String id = scanner.nextLine();
        Atleta atleta = buscarAtletaPorId(id);
        if (atleta == null) {
            System.out.println("Atleta no encontrado.");
            return;
        }
        System.out.print("Confirma eliminación del atleta '" + atleta.getNombreCompleto() + "'? (s/N): ");
        String conf = scanner.nextLine();
        if (!"s".equalsIgnoreCase(conf)) { System.out.println("Eliminación cancelada."); return; }

        // Eliminar de la lista en memoria
        atletas.remove(atleta);
        // Eliminar de BD si aplica (cascade borrará entrenamientos)
        if (db != null) db.deleteAtleta(atleta.getId());
        System.out.println("Atleta eliminado.");
    }
    
    private void registrarAtleta(Scanner scanner) {
        System.out.println("\n=== REGISTRAR NUEVO ATLETA ===");
        
        String id = "ATL" + (atletas.size() + 1);
        System.out.print("Nombre completo: ");
        String nombre = scanner.nextLine();
        
        System.out.print("Edad: ");
        int edad = scanner.nextInt();
        scanner.nextLine();
        
        System.out.print("Disciplina (atletismo/natacion): ");
        String disciplina = scanner.nextLine();
        
        System.out.print("Departamento: ");
        String departamento = scanner.nextLine();
        
        System.out.print("Nacionalidad: ");
        String nacionalidad = scanner.nextLine();
        
        LocalDate fechaIngreso = LocalDate.now();
        
        Atleta atleta = new Atleta(id, nombre, edad, disciplina, departamento, nacionalidad, fechaIngreso);
        atletas.add(atleta);
        // Guardar inmediatamente en la base de datos
        if (db != null) {
            db.guardarAtleta(atleta);
        }
        
        System.out.println("Atleta registrado exitosamente: " + atleta);
    }
    
    private void listarAtletas() {
        System.out.println("\n=== LISTA DE ATLETAS ===");
        if (atletas.isEmpty()) {
            System.out.println("No hay atletas registrados.");
            return;
        }
        
        for (int i = 0; i < atletas.size(); i++) {
            System.out.println((i + 1) + ". " + atletas.get(i));
        }
    }
    
    private void buscarAtleta(Scanner scanner) {
        System.out.print("Ingrese nombre o ID del atleta: ");
        String criterio = scanner.nextLine().toLowerCase();
        
        List<Atleta> resultados = new ArrayList<>();
        for (Atleta atleta : atletas) {
            if (atleta.getNombreCompleto().toLowerCase().contains(criterio) || 
                atleta.getId().toLowerCase().contains(criterio)) {
                resultados.add(atleta);
            }
        }
        
        if (resultados.isEmpty()) {
            System.out.println("No se encontraron atletas.");
        } else {
            System.out.println("=== RESULTADOS DE BÚSQUEDA ===");
            for (Atleta atleta : resultados) {
                System.out.println(atleta);
            }
        }
    }
    
    private void menuGestionEntrenamientos(Scanner scanner) {
        while (true) {
            System.out.println("\n=== GESTIÓN DE ENTRENAMIENTOS ===");
            System.out.println("1. Registrar entrenamiento");
            System.out.println("2. Ver entrenamientos de atleta");
            System.out.println("3. Editar entrenamiento");
            System.out.println("4. Eliminar entrenamiento");
            System.out.println("5. Volver al menú principal");
            System.out.print("Seleccione una opción: ");
            
            int opcion = scanner.nextInt();
            scanner.nextLine();
            
            switch (opcion) {
                case 1:
                    registrarEntrenamiento(scanner);
                    break;
                case 2:
                    verEntrenamientosAtleta(scanner);
                    break;
                case 3:
                    editarEntrenamiento(scanner);
                    break;
                case 4:
                    eliminarEntrenamiento(scanner);
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }

    private void editarEntrenamiento(Scanner scanner) {
        System.out.print("Ingrese ID del entrenamiento a editar: ");
        String id = scanner.nextLine();
        Atleta atletaEncontrado = null;
        Entrenamiento entrenamientoEncontrado = null;
        for (Atleta a : atletas) {
            for (Entrenamiento e : a.getEntrenamientos()) {
                if (e.getId().equalsIgnoreCase(id)) {
                    atletaEncontrado = a;
                    entrenamientoEncontrado = e;
                    break;
                }
            }
            if (entrenamientoEncontrado != null) break;
        }
        if (entrenamientoEncontrado == null) { System.out.println("Entrenamiento no encontrado."); return; }

        System.out.println("Deje en blanco para mantener el valor actual.");
        System.out.println("Fecha actual: " + entrenamientoEncontrado.getFecha());
        System.out.print("Nueva fecha (YYYY-MM-DD) o Enter: ");
        String fechaStr = scanner.nextLine();
        LocalDate fecha = fechaStr.trim().isEmpty() ? entrenamientoEncontrado.getFecha() : LocalDate.parse(fechaStr.trim());

        System.out.println("Tipo actual: " + entrenamientoEncontrado.getTipo());
        System.out.print("Nuevo tipo (RESISTENCIA/TECNICA/FUERZA) o Enter: ");
        String tipoStr = scanner.nextLine();
        TipoEntrenamiento tipo = tipoStr.trim().isEmpty() ? entrenamientoEncontrado.getTipo() : TipoEntrenamiento.valueOf(tipoStr.trim().toUpperCase());

        System.out.println("Valor actual: " + entrenamientoEncontrado.getValorRendimiento());
        System.out.print("Nuevo valor (numero) o Enter: ");
        String valorStr = scanner.nextLine();
        double valor = valorStr.trim().isEmpty() ? entrenamientoEncontrado.getValorRendimiento() : Double.parseDouble(valorStr.trim());

        System.out.println("Ubicación actual: " + entrenamientoEncontrado.getUbicacion());
        System.out.print("Nueva ubicación (nacional/internacional) o Enter: ");
        String ubic = scanner.nextLine(); if (ubic.trim().isEmpty()) ubic = entrenamientoEncontrado.getUbicacion();

        System.out.println("País actual: " + entrenamientoEncontrado.getPais());
        System.out.print("Nuevo país o Enter: ");
        String pais = scanner.nextLine(); if (pais.trim().isEmpty()) pais = entrenamientoEncontrado.getPais();

        // Reemplazar entrenamiento
        Entrenamiento nuevo = new Entrenamiento(entrenamientoEncontrado.getId(), atletaEncontrado.getId(), fecha, tipo, valor, ubic, pais);
        List<Entrenamiento> lista = atletaEncontrado.getEntrenamientos();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equalsIgnoreCase(entrenamientoEncontrado.getId())) {
                lista.set(i, nuevo);
                break;
            }
        }
        if (db != null) db.guardarEntrenamiento(nuevo);
        System.out.println("Entrenamiento actualizado.");
    }

    private void eliminarEntrenamiento(Scanner scanner) {
        System.out.print("Ingrese ID del entrenamiento a eliminar: ");
        String id = scanner.nextLine();
        for (Atleta a : atletas) {
            Iterator<Entrenamiento> it = a.getEntrenamientos().iterator();
            while (it.hasNext()) {
                Entrenamiento e = it.next();
                if (e.getId().equalsIgnoreCase(id)) {
                    System.out.print("Confirma eliminación del entrenamiento '" + e.getId() + "'? (s/N): ");
                    String conf = scanner.nextLine();
                    if (!"s".equalsIgnoreCase(conf)) { System.out.println("Eliminación cancelada."); return; }
                    it.remove();
                    if (db != null) db.deleteEntrenamiento(id);
                    System.out.println("Entrenamiento eliminado.");
                    return;
                }
            }
        }
        System.out.println("Entrenamiento no encontrado.");
    }
    
    private void registrarEntrenamiento(Scanner scanner) {
        System.out.println("\n=== REGISTRAR ENTRENAMIENTO ===");
        
        if (atletas.isEmpty()) {
            System.out.println("No hay atletas registrados. Registre un atleta primero.");
            return;
        }
        
        listarAtletas();
        System.out.print("Ingrese ID del atleta: ");
        String idAtleta = scanner.nextLine();
        
        Atleta atleta = buscarAtletaPorId(idAtleta);
        if (atleta == null) {
            System.out.println("Atleta no encontrado.");
            return;
        }
        
        String idEntrenamiento = "ENT" + System.currentTimeMillis();
        
        System.out.print("Fecha (YYYY-MM-DD) o Enter para hoy: ");
        String fechaStr = scanner.nextLine();
        LocalDate fecha = fechaStr.isEmpty() ? LocalDate.now() : LocalDate.parse(fechaStr);
        
        System.out.print("Tipo (RESISTENCIA/TECNICA/FUERZA): ");
        String tipoStr = scanner.nextLine().toUpperCase();
        TipoEntrenamiento tipo = TipoEntrenamiento.valueOf(tipoStr);
        
        System.out.print("Puntuacion de rendimiento: ");
        double valor = scanner.nextDouble();
        scanner.nextLine();
        
        System.out.print("Ubicación (nacional/internacional): ");
        String ubicacion = scanner.nextLine();
        
        String pais = "Guatemala";
        if ("internacional".equalsIgnoreCase(ubicacion)) {
            System.out.print("País: ");
            pais = scanner.nextLine();
        }
        
        Entrenamiento entrenamiento = new Entrenamiento(idEntrenamiento, idAtleta, fecha, 
                                                       tipo, valor, ubicacion, pais);
        atleta.agregarEntrenamiento(entrenamiento);
        // Guardar entrenamiento en la base de datos (asegurar que el atleta exista primero)
        if (db != null) {
            db.guardarAtleta(atleta); // guarda o actualiza
            db.guardarEntrenamiento(entrenamiento);
        }
        
        System.out.println("Entrenamiento registrado exitosamente: " + entrenamiento);
    }
    
    private Atleta buscarAtletaPorId(String id) {
        for (Atleta atleta : atletas) {
            if (atleta.getId().equalsIgnoreCase(id)) {
                return atleta;
            }
        }
        return null;
    }
    
    private void verEntrenamientosAtleta(Scanner scanner) {
        System.out.print("Ingrese ID del atleta: ");
        String idAtleta = scanner.nextLine();
        
        Atleta atleta = buscarAtletaPorId(idAtleta);
        if (atleta == null) {
            System.out.println("Atleta no encontrado.");
            return;
        }
        
        System.out.println("\n=== ENTRENAMIENTOS DE " + atleta.getNombreCompleto().toUpperCase() + " ===");
        if (atleta.getEntrenamientos().isEmpty()) {
            System.out.println("No hay entrenamientos registrados.");
            return;
        }
        
        for (Entrenamiento ent : atleta.getEntrenamientos()) {
            System.out.println(ent);
        }
    }
    
    private void menuEstadisticas(Scanner scanner) {
        while (true) {
            System.out.println("\n=== ESTADÍSTICAS Y REPORTES ===");
            System.out.println("1. Historial completo de atleta");
            System.out.println("2. Estadísticas de rendimiento");
            System.out.println("3. Comparación nacional vs internacional");
            System.out.println("4. Volver al menú principal");
            System.out.print("Seleccione una opción: ");
            
            int opcion = scanner.nextInt();
            scanner.nextLine();
            
            switch (opcion) {
                case 1:
                    mostrarHistorialCompleto(scanner);
                    break;
                case 2:
                    mostrarEstadisticasRendimiento(scanner);
                    break;
                case 3:
                    compararRendimientoNacionalInternacional(scanner);
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }
    
    private void mostrarHistorialCompleto(Scanner scanner) {
        System.out.print("Ingrese ID del atleta: ");
        String idAtleta = scanner.nextLine();
        
        Atleta atleta = buscarAtletaPorId(idAtleta);
        if (atleta == null) {
            System.out.println("Atleta no encontrado.");
            return;
        }
        
        System.out.println("\n=== HISTORIAL COMPLETO DE " + atleta.getNombreCompleto().toUpperCase() + " ===");
        if (atleta.getEntrenamientos().isEmpty()) {
            System.out.println("No hay entrenamientos registrados.");
            return;
        }
        
        // Ordenar por fecha
        List<Entrenamiento> entrenamientosOrdenados = new ArrayList<>(atleta.getEntrenamientos());
        Collections.sort(entrenamientosOrdenados, (e1, e2) -> e1.getFecha().compareTo(e2.getFecha()));
        
        for (Entrenamiento ent : entrenamientosOrdenados) {
            System.out.printf("%s | %s | %.2f | %s | %s\n", 
                             ent.getFecha(), ent.getTipo(), ent.getValorRendimiento(), 
                             ent.getUbicacion(), ent.getPais());
        }
    }
    
    private void mostrarEstadisticasRendimiento(Scanner scanner) {
        System.out.print("Ingrese ID del atleta: ");
        String idAtleta = scanner.nextLine();
        
        Atleta atleta = buscarAtletaPorId(idAtleta);
        if (atleta == null) {
            System.out.println("Atleta no encontrado.");
            return;
        }
        
        List<Entrenamiento> entrenamientos = atleta.getEntrenamientos();
        if (entrenamientos.isEmpty()) {
            System.out.println("No hay entrenamientos para calcular estadísticas.");
            return;
        }
        
        // Calcular estadísticas
        double suma = 0;
        double mejorMarca = Double.MIN_VALUE;
        double peorMarca = Double.MAX_VALUE;
        
        for (Entrenamiento ent : entrenamientos) {
            double valor = ent.getValorRendimiento();
            suma += valor;
            if (valor > mejorMarca) mejorMarca = valor;
            if (valor < peorMarca) peorMarca = valor;
        }
        
        double promedio = suma / entrenamientos.size();
        
        System.out.println("\n=== ESTADÍSTICAS DE RENDIMIENTO ===");
        System.out.println("Atleta: " + atleta.getNombreCompleto());
        System.out.println("Total entrenamientos: " + entrenamientos.size());
        System.out.printf("Promedio de rendimiento: %.2f\n", promedio);
        System.out.printf("Mejor marca: %.2f\n", mejorMarca);
        System.out.printf("Peor marca: %.2f\n", peorMarca);
        
        // Evolución en el tiempo
        System.out.println("\nEvolución (últimos 5 entrenamientos):");
        List<Entrenamiento> ultimos = new ArrayList<>(entrenamientos);
        Collections.sort(ultimos, (e1, e2) -> e2.getFecha().compareTo(e1.getFecha()));
        
        int limit = Math.min(5, ultimos.size());
        for (int i = 0; i < limit; i++) {
            Entrenamiento ent = ultimos.get(i);
            System.out.printf("%s: %.2f\n", ent.getFecha(), ent.getValorRendimiento());
        }
    }
    
    private void compararRendimientoNacionalInternacional(Scanner scanner) {
        System.out.print("Ingrese ID del atleta: ");
        String idAtleta = scanner.nextLine();
        
        Atleta atleta = buscarAtletaPorId(idAtleta);
        if (atleta == null) {
            System.out.println("Atleta no encontrado.");
            return;
        }
        
        List<Entrenamiento> entrenamientos = atleta.getEntrenamientos();
        if (entrenamientos.isEmpty()) {
            System.out.println("No hay entrenamientos para comparar.");
            return;
        }
        
        List<Entrenamiento> nacionales = new ArrayList<>();
        List<Entrenamiento> internacionales = new ArrayList<>();
        
        for (Entrenamiento ent : entrenamientos) {
            if (ent.esInternacional()) {
                internacionales.add(ent);
            } else {
                nacionales.add(ent);
            }
        }
        
        System.out.println("\n=== COMPARACIÓN NACIONAL VS INTERNACIONAL ===");
        System.out.println("ENTRENAMIENTOS NACIONALES: " + nacionales.size());
        if (!nacionales.isEmpty()) {
            double sumaNacional = 0;
            for (Entrenamiento ent : nacionales) {
                sumaNacional += ent.getValorRendimiento();
            }
            double promNacional = sumaNacional / nacionales.size();
            System.out.printf("Promedio nacional: %.2f\n", promNacional);
        }
        
        System.out.println("ENTRENAMIENTOS INTERNACIONALES: " + internacionales.size());
        if (!internacionales.isEmpty()) {
            double sumaInternacional = 0;
            for (Entrenamiento ent : internacionales) {
                sumaInternacional += ent.getValorRendimiento();
            }
            double promInternacional = sumaInternacional / internacionales.size();
            System.out.printf("Promedio internacional: %.2f\n", promInternacional);
        }
    }
    
    private void menuPlanilla(Scanner scanner) {
        while (true) {
            System.out.println("\n=== GESTIÓN FINANCIERA (PLANILLA) ===");
            System.out.println("1. Calcular pago mensual de atleta");
            System.out.println("2. Reporte de planilla completa");
            System.out.println("3. Editar parámetros de planilla");
            System.out.println("4. Volver al menú principal");
            System.out.print("Seleccione una opción: ");
            
            int opcion = scanner.nextInt();
            scanner.nextLine();
            
            switch (opcion) {
                case 1:
                    calcularPagoMensual(scanner);
                    break;
                case 2:
                    generarReportePlanillaCompleta();
                    break;
                case 3:
                    editarParametrosPlanilla(scanner);
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }

    private void editarParametrosPlanilla(Scanner scanner) {
        CalculadoraPlanilla calc = new CalculadoraPlanilla();
        System.out.println("Parámetros actuales:");
        System.out.printf("1. Pago base por entrenamiento: %.2f\n", calc.getPagoBasePorEntrenamiento());
        System.out.printf("2. Bono por entreno internacional: %.2f\n", calc.getBonoExtranjero());
        System.out.printf("3. Bono por superar mejor marca: %.2f\n", calc.getBonoMejorMarca());

        System.out.print("Ingrese nueva tarifa pago base (Enter para mantener): ");
        String s1 = scanner.nextLine();
        if (!s1.trim().isEmpty()) {
            try { calc.setPagoBasePorEntrenamiento(Double.parseDouble(s1.trim())); } catch (Exception e) { System.out.println("Valor inválido"); }
        }

        System.out.print("Ingrese nuevo bono por entrenamiento internacional (Enter para mantener): ");
        String s2 = scanner.nextLine();
        if (!s2.trim().isEmpty()) {
            try { calc.setBonoExtranjero(Double.parseDouble(s2.trim())); } catch (Exception e) { System.out.println("Valor inválido"); }
        }

        System.out.print("Ingrese nuevo bono por mejor marca (Enter para mantener): ");
        String s3 = scanner.nextLine();
        if (!s3.trim().isEmpty()) {
            try { calc.setBonoMejorMarca(Double.parseDouble(s3.trim())); } catch (Exception e) { System.out.println("Valor inválido"); }
        }

        System.out.println("Parámetros actualizados y guardados en planilla.properties");
    }
    
    private void calcularPagoMensual(Scanner scanner) {
        System.out.print("Ingrese ID del atleta: ");
        String idAtleta = scanner.nextLine();
        
        Atleta atleta = buscarAtletaPorId(idAtleta);
        if (atleta == null) {
            System.out.println("Atleta no encontrado.");
            return;
        }
        
        System.out.print("Ingrese el mes (1-12): ");
        int mes = scanner.nextInt();
        System.out.print("Ingrese el año: ");
        int año = scanner.nextInt();
        scanner.nextLine();
        
        CalculadoraPlanilla calculadora = new CalculadoraPlanilla();
        double pago = calculadora.calcularPagoMensual(atleta, mes, año);
        
        System.out.println("\n=== CALCULO DE PAGO MENSUAL ===");
        System.out.println("Atleta: " + atleta.getNombreCompleto());
        System.out.printf("Pago mensual: Q%.2f\n", pago);
    }
    
    private void generarReportePlanillaCompleta() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el mes (1-12): ");
        int mes = scanner.nextInt();
        System.out.print("Ingrese el año: ");
        int año = scanner.nextInt();
        
        CalculadoraPlanilla calculadora = new CalculadoraPlanilla();
        
        System.out.println("\n=== REPORTE DE PLANILLA COMPLETA ===");
        System.out.println("Mes: " + mes + " Año: " + año);
        System.out.println("=====================================");
        
        double totalPlanilla = 0;
        
        for (Atleta atleta : atletas) {
            double pago = calculadora.calcularPagoMensual(atleta, mes, año);
            totalPlanilla += pago;
            System.out.printf("%s: Q%.2f\n", atleta.getNombreCompleto(), pago);
        }
        
        System.out.println("=====================================");
        System.out.printf("TOTAL PLANILLA: Q%.2f\n", totalPlanilla);
    }
    
    private void menuExportarReportes(Scanner scanner) {
        while (true) {
            System.out.println("\n=== EXPORTAR REPORTES ===");
            System.out.println("1. Exportar reporte de atletas (CSV)");
            System.out.println("2. Exportar reporte de entrenamientos (CSV)");
            System.out.println("3. Exportar estadísticas (TXT)");
            System.out.println("4. Volver al menú principal");
            System.out.print("Seleccione una opción: ");
            
            int opcion = scanner.nextInt();
            scanner.nextLine();
            
            switch (opcion) {
                case 1:
                    exportarAtletasCSV();
                    break;
                case 2:
                    exportarEntrenamientosCSV();
                    break;
                case 3:
                    exportarEstadisticasTXT();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }
    
    private void exportarAtletasCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("reporte_atletas.csv"))) {
            writer.println("ID,Nombre,Edad,Disciplina,Departamento,Nacionalidad,FechaIngreso");
            
            for (Atleta atleta : atletas) {
                writer.printf("%s,\"%s\",%d,%s,%s,%s,%s\n",
                             atleta.getId(), atleta.getNombreCompleto(), atleta.getEdad(),
                             atleta.getDisciplina(), atleta.getDepartamento(),
                             atleta.getNacionalidad(), atleta.getFechaIngreso());
            }
            
            System.out.println("Reporte de atletas exportado a: reporte_atletas.csv");
        } catch (IOException e) {
            System.out.println("Error exportando reporte: " + e.getMessage());
        }
    }
    
    private void exportarEntrenamientosCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("reporte_entrenamientos.csv"))) {
            writer.println("ID,Atleta,Fecha,Tipo,Valor,Ubicacion,Pais");
            
            for (Atleta atleta : atletas) {
                for (Entrenamiento ent : atleta.getEntrenamientos()) {
                    writer.printf("%s,\"%s\",%s,%s,%.2f,%s,%s\n",
                                 ent.getId(), atleta.getNombreCompleto(), ent.getFecha(),
                                 ent.getTipo(), ent.getValorRendimiento(), 
                                 ent.getUbicacion(), ent.getPais());
                }
            }
            
            System.out.println("Reporte de entrenamientos exportado a: reporte_entrenamientos.csv");
        } catch (IOException e) {
            System.out.println("Error exportando reporte: " + e.getMessage());
        }
    }
    
    private void exportarEstadisticasTXT() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("reporte_estadisticas.txt"))) {
            writer.println("=== REPORTE DE ESTADÍSTICAS - COMITÉ OLÍMPICO GUATEMALTECO ===");
            writer.println("Fecha: " + LocalDate.now());
            writer.println("Total atletas: " + atletas.size());
            writer.println("\n" + "=".repeat(80));
            
            for (Atleta atleta : atletas) {
                writer.println("\nATLETA: " + atleta.getNombreCompleto());
                writer.println("Disciplina: " + atleta.getDisciplina());
                writer.println("Total entrenamientos: " + atleta.getEntrenamientos().size());
                
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
                    
                    writer.printf("Promedio rendimiento: %.2f\n", promedio);
                    writer.printf("Mejor marca: %.2f\n", mejorMarca);
                    writer.println("Entrenamientos internacionales: " + internacionales);
                }
                writer.println("-".repeat(40));
            }
            
            System.out.println("Reporte de estadísticas exportado a: reporte_estadisticas.txt");
        } catch (IOException e) {
            System.out.println("Error exportando reporte: " + e.getMessage());
        }
    }
    
    private void guardarDatos() {
        guardarAtletasJSON();
        guardarEntrenamientosJSON();

        // Además guardar en la base de datos SQLite
        if (db != null) {
            for (Atleta atleta : atletas) {
                db.guardarAtleta(atleta);
                for (Entrenamiento ent : atleta.getEntrenamientos()) {
                    db.guardarEntrenamiento(ent);
                }
            }
        }

        System.out.println("Datos guardados exitosamente.");
    }
    
    private void cargarDatos() {
        // Intentar cargar desde la base de datos si está inicializada
        if (db != null) {
            List<Atleta> atletasBD = db.cargarAtletas();
            List<Entrenamiento> entrenamientosBD = db.cargarEntrenamientos();

            // Mapear atletas por ID
            Map<String, Atleta> mapa = new HashMap<>();
            for (Atleta a : atletasBD) {
                mapa.put(a.getId(), a);
            }

            // Asociar entrenamientos a atletas
            for (Entrenamiento e : entrenamientosBD) {
                Atleta a = mapa.get(e.getIdAtleta());
                if (a != null) {
                    a.agregarEntrenamiento(e);
                }
            }

            // Reemplazar lista en memoria
            if (!atletasBD.isEmpty()) {
                this.atletas = new ArrayList<>(mapa.values());
                System.out.println("Datos cargados desde la base de datos.");
                return;
            }
        }

        // Fallback: el sistema seguirá iniciando vacio y guardará con la opción 6
        System.out.println("Sistema iniciado. No se encontraron datos en la BD; los datos se guardarán al salir o con la opción 6.");
    }

    // Métodos públicos para uso por un servidor web o API
    public synchronized List<Atleta> getAtletasApi() {
        return new ArrayList<>(this.atletas);
    }

    public synchronized void agregarAtletaDesdeApi(Atleta atleta) {
        this.atletas.add(atleta);
        if (db != null) {
            db.guardarAtleta(atleta);
        }
    }

    public synchronized boolean agregarEntrenamientoDesdeApi(Entrenamiento ent) {
        Atleta atleta = buscarAtletaPorId(ent.getIdAtleta());
        if (atleta == null) return false;
        atleta.agregarEntrenamiento(ent);
        if (db != null) {
            db.guardarEntrenamiento(ent);
        }
        return true;
    }

    // API: editar atleta por id con datos planos en map (claves: nombre,edad,disciplina,departamento,nacionalidad,fechaIngreso)
    public synchronized boolean editarAtletaDesdeApi(String id, Map<String,String> data) {
        Atleta atleta = buscarAtletaPorId(id);
        if (atleta == null) return false;
        if (data.containsKey("nombre")) {
            atleta.setNombreCompleto(data.get("nombre"));
        }
        if (data.containsKey("edad")) {
            try { atleta.setEdad(Integer.parseInt(data.get("edad"))); } catch (Exception e) { /* ignore */ }
        }
        if (data.containsKey("disciplina")) atleta.setDisciplina(data.get("disciplina"));
        if (data.containsKey("departamento")) atleta.setDepartamento(data.get("departamento"));
        if (data.containsKey("nacionalidad")) atleta.setNacionalidad(data.get("nacionalidad"));
        if (data.containsKey("fechaIngreso")) {
            try { atleta.setFechaIngreso(LocalDate.parse(data.get("fechaIngreso"))); } catch (Exception e) { /* ignore */ }
        }
        if (db != null) db.guardarAtleta(atleta);
        return true;
    }

    public synchronized boolean eliminarAtletaDesdeApi(String id) {
        Atleta atleta = buscarAtletaPorId(id);
        if (atleta == null) return false;
        atletas.remove(atleta);
        if (db != null) db.deleteAtleta(id);
        return true;
    }

    // API: editar entrenamiento por id con campos posibles: idAtleta, fecha, tipo, valor, ubicacion, pais
    public synchronized boolean editarEntrenamientoDesdeApi(String id, Map<String,String> data) {
        Atleta atletaEncontrado = null;
        Entrenamiento entEncontrado = null;
        for (Atleta a : atletas) {
            for (Entrenamiento e : a.getEntrenamientos()) {
                if (e.getId().equalsIgnoreCase(id)) { atletaEncontrado = a; entEncontrado = e; break; }
            }
            if (entEncontrado != null) break;
        }
        if (entEncontrado == null) return false;

        String idAtleta = data.getOrDefault("idAtleta", entEncontrado.getIdAtleta());
        LocalDate fecha = entEncontrado.getFecha();
        if (data.containsKey("fecha") && !data.get("fecha").isEmpty()) {
            try { fecha = LocalDate.parse(data.get("fecha")); } catch (Exception ex) { }
        }
        TipoEntrenamiento tipo = entEncontrado.getTipo();
        if (data.containsKey("tipo") && !data.get("tipo").isEmpty()) {
            try { tipo = TipoEntrenamiento.valueOf(data.get("tipo").toUpperCase()); } catch (Exception ex) { }
        }
        double valor = entEncontrado.getValorRendimiento();
        if (data.containsKey("valor") && !data.get("valor").isEmpty()) {
            try { valor = Double.parseDouble(data.get("valor")); } catch (Exception ex) { }
        }
        String ubic = data.getOrDefault("ubicacion", entEncontrado.getUbicacion());
        String pais = data.getOrDefault("pais", entEncontrado.getPais());

        Entrenamiento nuevo = new Entrenamiento(entEncontrado.getId(), idAtleta, fecha, tipo, valor, ubic, pais);
        List<Entrenamiento> lista = atletaEncontrado.getEntrenamientos();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equalsIgnoreCase(entEncontrado.getId())) { lista.set(i, nuevo); break; }
        }
        if (db != null) db.guardarEntrenamiento(nuevo);
        return true;
    }

    public synchronized boolean eliminarEntrenamientoDesdeApi(String id) {
        for (Atleta a : atletas) {
            Iterator<Entrenamiento> it = a.getEntrenamientos().iterator();
            while (it.hasNext()) {
                Entrenamiento e = it.next();
                if (e.getId().equalsIgnoreCase(id)) {
                    it.remove();
                    if (db != null) db.deleteEntrenamiento(id);
                    return true;
                }
            }
        }
        return false;
    }
    
    private void guardarAtletasJSON() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ARCHIVO_ATLETAS_JSON))) {
            writer.println("[");
            for (int i = 0; i < atletas.size(); i++) {
                Atleta atleta = atletas.get(i);
                writer.printf("  {\n" +
                             "    \"id\": \"%s\",\n" +
                             "    \"nombre\": \"%s\",\n" +
                             "    \"edad\": %d,\n" +
                             "    \"disciplina\": \"%s\",\n" +
                             "    \"departamento\": \"%s\",\n" +
                             "    \"nacionalidad\": \"%s\",\n" +
                             "    \"fechaIngreso\": \"%s\"\n" +
                             "  }%s\n",
                             atleta.getId(), atleta.getNombreCompleto(), atleta.getEdad(),
                             atleta.getDisciplina(), atleta.getDepartamento(), 
                             atleta.getNacionalidad(), atleta.getFechaIngreso(),
                             (i < atletas.size() - 1 ? "," : ""));
            }
            writer.println("]");
        } catch (IOException e) {
            System.out.println("Error guardando atletas: " + e.getMessage());
        }
    }
    
    private void guardarEntrenamientosJSON() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ARCHIVO_ENTRENAMIENTOS_JSON))) {
            writer.println("[");
            
            // Contar total de entrenamientos
            int totalCount = 0;
            for (Atleta atleta : atletas) {
                totalCount += atleta.getEntrenamientos().size();
            }
            
            int currentCount = 0;
            for (Atleta atleta : atletas) {
                for (Entrenamiento ent : atleta.getEntrenamientos()) {
                    writer.printf("  {\n" +
                                 "    \"id\": \"%s\",\n" +
                                 "    \"idAtleta\": \"%s\",\n" +
                                 "    \"fecha\": \"%s\",\n" +
                                 "    \"tipo\": \"%s\",\n" +
                                 "    \"valor\": %.2f,\n" +
                                 "    \"ubicacion\": \"%s\",\n" +
                                 "    \"pais\": \"%s\"\n" +
                                 "  }%s\n",
                                 ent.getId(), ent.getIdAtleta(), ent.getFecha(),
                                 ent.getTipo(), ent.getValorRendimiento(), 
                                 ent.getUbicacion(), ent.getPais(),
                                 (++currentCount < totalCount ? "," : ""));
                }
            }
            writer.println("]");
        } catch (IOException e) {
            System.out.println("Error guardando entrenamientos: " + e.getMessage());
        }
    }
}