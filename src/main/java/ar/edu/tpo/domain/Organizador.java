package ar.edu.tpo.domain;

import java.util.List;

public class Organizador extends Usuario {

    public Organizador(String nombre, String email, String password) {
        this(null, nombre, email, password, null, null, null, null);
    }

    public Organizador(String nombre, String email, String password, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
        this(null, nombre, email, password, sancionesActivas, sancionesHistoricas, null, null);
    }

    public Organizador(String id, String nombre, String email, String password, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas, Integer strikes, Boolean suspendido) {
        super(id, nombre, email, password, 0, 0, sancionesActivas, sancionesHistoricas, strikes, suspendido, null, null);
    }

    @Override
    public String getTipo() {
        return "Organizador";
    }
}

