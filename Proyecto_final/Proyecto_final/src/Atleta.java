import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Atleta {
    private String id;
    private String nombreCompleto;
    private int edad;
    private String disciplina;
    private String departamento;
    private String nacionalidad;
    private LocalDate fechaIngreso;
    private List<Entrenamiento> entrenamientos;
    
    public Atleta(String id, String nombreCompleto, int edad, String disciplina, 
                  String departamento, String nacionalidad, LocalDate fechaIngreso) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.edad = edad;
        this.disciplina = disciplina;
        this.departamento = departamento;
        this.nacionalidad = nacionalidad;
        this.fechaIngreso = fechaIngreso;
        this.entrenamientos = new ArrayList<>();
    }
    
    public String getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public int getEdad() { return edad; }
    public String getDisciplina() { return disciplina; }
    public String getDepartamento() { return departamento; }
    public String getNacionalidad() { return nacionalidad; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public List<Entrenamiento> getEntrenamientos() { return entrenamientos; }
    
    public void agregarEntrenamiento(Entrenamiento entrenamiento) {
        this.entrenamientos.add(entrenamiento);
    }
    
    @Override
    public String toString() {
        return String.format("ID: %s | %s | %d años | %s | %s", 
                           id, nombreCompleto, edad, disciplina, departamento);
    }
}