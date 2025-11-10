package ar.edu.tpo.domain;

import java.time.Duration;
import java.util.List;

public class Organizador extends Usuario {

    public Organizador(String nombre, String email, String password) {
        this(null, nombre, email, password, null, null, null, null);
    }

    public Organizador(String nombre, String email, String password, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
        this(null, nombre, email, password, sancionesActivas, sancionesHistoricas, null, null);
    }

    public Organizador(String id, String nombre, String email, String password, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas, Integer strikes, Boolean suspendido) {
        super(id, nombre, email, password, sancionesActivas, sancionesHistoricas, strikes, suspendido);
    }

    @Override
    public SancionActiva agregarSancion(String motivo, Duration duracion) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        SancionActiva sancion = SancionActiva.porDuracion(motivo.trim(), duracion);
        sancionesActivas.add(sancion);
        removerSancionesVencidas();
        return sancion;
    }

    @Override
    public String getTipo() {
        return "Organizador";
    }
}

