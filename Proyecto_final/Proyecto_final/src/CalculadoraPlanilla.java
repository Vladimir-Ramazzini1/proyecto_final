import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.io.*;

public class CalculadoraPlanilla {
    private double pagoBasePorEntrenamiento = 100.0;
    private double bonoExtranjero = 50.0;
    private double bonoMejorMarca = 200.0;
    private static final String CONFIG_FILE = "planilla.properties";

    public CalculadoraPlanilla() {
        // Intentar cargar parámetros desde archivo
        loadConfig();
    }

    public double getPagoBasePorEntrenamiento() { return pagoBasePorEntrenamiento; }
    public double getBonoExtranjero() { return bonoExtranjero; }
    public double getBonoMejorMarca() { return bonoMejorMarca; }

    public void setPagoBasePorEntrenamiento(double v) { this.pagoBasePorEntrenamiento = v; saveConfig(); }
    public void setBonoExtranjero(double v) { this.bonoExtranjero = v; saveConfig(); }
    public void setBonoMejorMarca(double v) { this.bonoMejorMarca = v; saveConfig(); }

    private void loadConfig() {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            p.load(fis);
            pagoBasePorEntrenamiento = Double.parseDouble(p.getProperty("pagoBasePorEntrenamiento", String.valueOf(pagoBasePorEntrenamiento)));
            bonoExtranjero = Double.parseDouble(p.getProperty("bonoExtranjero", String.valueOf(bonoExtranjero)));
            bonoMejorMarca = Double.parseDouble(p.getProperty("bonoMejorMarca", String.valueOf(bonoMejorMarca)));
        } catch (FileNotFoundException fnf) {
            // no existe: se usan valores por defecto
        } catch (Exception e) {
            System.out.println("Error cargando configuración de planilla: " + e.getMessage());
        }
    }

    private void saveConfig() {
        Properties p = new Properties();
        p.setProperty("pagoBasePorEntrenamiento", String.valueOf(pagoBasePorEntrenamiento));
        p.setProperty("bonoExtranjero", String.valueOf(bonoExtranjero));
        p.setProperty("bonoMejorMarca", String.valueOf(bonoMejorMarca));
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            p.store(fos, "Parámetros de la planilla");
        } catch (Exception e) {
            System.out.println("Error guardando configuración de planilla: " + e.getMessage());
        }
    }

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

        double pagoBase = countEntrenamientosMes * pagoBasePorEntrenamiento;
        double bonoExtranjeroTotal = countExtranjeros * bonoExtranjero;
        double bonoMejor = calcularBonoMejorMarca(atleta, mes, año);

        return pagoBase + bonoExtranjeroTotal + bonoMejor;
    }

    private double calcularBonoMejorMarca(Atleta atleta, int mes, int año) {
        double mejorMarcaHistorica = 0;
        boolean superoMejorMarca = false;

        for (Entrenamiento ent : atleta.getEntrenamientos()) {
            if (ent.getFecha().getYear() < año ||
                (ent.getFecha().getYear() == año && ent.getFecha().getMonthValue() < mes)) {
                if (ent.getValorRendimiento() > mejorMarcaHistorica) {
                    mejorMarcaHistorica = ent.getValorRendimiento();
                }
            }
        }

        for (Entrenamiento ent : atleta.getEntrenamientos()) {
            if (ent.getFecha().getMonthValue() == mes && ent.getFecha().getYear() == año) {
                if (ent.getValorRendimiento() > mejorMarcaHistorica) {
                    superoMejorMarca = true;
                    break;
                }
            }
        }

        return superoMejorMarca ? bonoMejorMarca : 0.0;
    }
}