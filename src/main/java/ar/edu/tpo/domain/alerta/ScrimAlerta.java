package ar.edu.tpo.domain.alerta;

import java.util.Objects;

public class ScrimAlerta {
    private final String juego;
    private final String region;
    private final Integer rangoMin;
    private final Integer rangoMax;
    private final Integer latenciaMax;
    private final String formato;

    public ScrimAlerta(String juego, String region, Integer rangoMin, Integer rangoMax, Integer latenciaMax, String formato) {
        this.juego = juego != null ? juego.trim() : null;
        this.region = region != null ? region.trim() : null;
        this.rangoMin = rangoMin;
        this.rangoMax = rangoMax;
        this.latenciaMax = latenciaMax;
        this.formato = formato != null ? formato.trim() : null;
    }

    public String getJuego() {
        return juego;
    }

    public String getRegion() {
        return region;
    }

    public Integer getRangoMin() {
        return rangoMin;
    }

    public Integer getRangoMax() {
        return rangoMax;
    }

    public Integer getLatenciaMax() {
        return latenciaMax;
    }

    public String getFormato() {
        return formato;
    }

    @Override
    public String toString() {
        return "ScrimAlerta{" +
                "juego='" + juego + '\'' +
                ", region='" + region + '\'' +
                ", rangoMin=" + rangoMin +
                ", rangoMax=" + rangoMax +
                ", latenciaMax=" + latenciaMax +
                ", formato='" + formato + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScrimAlerta that = (ScrimAlerta) o;
        return Objects.equals(juego, that.juego) &&
                Objects.equals(region, that.region) &&
                Objects.equals(rangoMin, that.rangoMin) &&
                Objects.equals(rangoMax, that.rangoMax) &&
                Objects.equals(latenciaMax, that.latenciaMax) &&
                Objects.equals(formato, that.formato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(juego, region, rangoMin, rangoMax, latenciaMax, formato);
    }
}


