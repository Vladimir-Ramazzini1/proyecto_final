public enum TipoEntrenamiento {
    RESISTENCIA("Resistencia"),
    TECNICA("Técnica"),
    FUERZA("Fuerza");
    
    private final String descripcion;
    
    TipoEntrenamiento(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    @Override
    public String toString() {
        return descripcion;
    }
}