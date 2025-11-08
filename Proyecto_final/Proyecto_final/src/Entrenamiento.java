import java.time.LocalDate;

public class Entrenamiento {
    private String id;
    private String idAtleta;
    private LocalDate fecha;
    private TipoEntrenamiento tipo;
    private double valorRendimiento;
    private String ubicacion;
    private String pais;
    
    public Entrenamiento(String id, String idAtleta, LocalDate fecha, TipoEntrenamiento tipo,
                        double valorRendimiento, String ubicacion, String pais) {
        this.id = id;
        this.idAtleta = idAtleta;
        this.fecha = fecha;
        this.tipo = tipo;
        this.valorRendimiento = valorRendimiento;
        this.ubicacion = ubicacion;
        this.pais = pais;
    }
    
    public String getId() { return id; }
    public String getIdAtleta() { return idAtleta; }
    public LocalDate getFecha() { return fecha; }
    public TipoEntrenamiento getTipo() { return tipo; }
    public double getValorRendimiento() { return valorRendimiento; }
    public String getUbicacion() { return ubicacion; }
    public String getPais() { return pais; }
    
    public boolean esInternacional() {
        return "internacional".equalsIgnoreCase(ubicacion);
    }
    
    @Override
    public String toString() {
        return String.format("ID: %s | %s | %s | %.2f | %s", 
                           id, fecha, tipo, valorRendimiento, ubicacion);
    }
}