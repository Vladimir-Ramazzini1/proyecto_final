import java.time.LocalDate;
import java.util.List;

public class CalculadoraPlanilla {
    private static final double PAGO_BASE_POR_ENTRENAMIENTO = 100.0;
    private static final double BONO_EXTRANJERO = 50.0;
    private static final double BONO_MEJOR_MARCA = 200.0;
    
    public double calcularPagoMensual(Atleta atleta, int mes, int año) {
        List<Entrenamiento> entrenamientosMes = atleta.getEntrenamientos();
        int countEntrenamientosMes = 0;
        int countExtranjeros = 0;
        
        for (Entrenamiento ent : entrenamientosMes) {
            if (ent.getFecha().getMonthValue() == mes && ent.getFecha().getYear() == año) {
                countEntrenamientosMes++;
                if (ent.esInternacional()) {
                    countExtranjeros++;
                }
            }
        }
        
        if (countEntrenamientosMes == 0) {
            return 0.0;
        }
        
        double pagoBase = countEntrenamientosMes * PAGO_BASE_POR_ENTRENAMIENTO;
        double bonoExtranjero = countExtranjeros * BONO_EXTRANJERO;
        double bonoMejorMarca = calcularBonoMejorMarca(atleta, mes, año);
        
        return pagoBase + bonoExtranjero + bonoMejorMarca;
    }
    
    private double calcularBonoMejorMarca(Atleta atleta, int mes, int año) {
        double mejorMarcaHistorica = 0;
        boolean superoMejorMarca = false;
        
        // Encontrar la mejor marca histórica (antes del mes actual)
        for (Entrenamiento ent : atleta.getEntrenamientos()) {
            if (ent.getFecha().getYear() < año || 
                (ent.getFecha().getYear() == año && ent.getFecha().getMonthValue() < mes)) {
                if (ent.getValorRendimiento() > mejorMarcaHistorica) {
                    mejorMarcaHistorica = ent.getValorRendimiento();
                }
            }
        }
        
        // Verificar si en el mes actual superó la mejor marca
        for (Entrenamiento ent : atleta.getEntrenamientos()) {
            if (ent.getFecha().getMonthValue() == mes && ent.getFecha().getYear() == año) {
                if (ent.getValorRendimiento() > mejorMarcaHistorica) {
                    superoMejorMarca = true;
                    break;
                }
            }
        }
        
        return superoMejorMarca ? BONO_MEJOR_MARCA : 0.0;
    }
}