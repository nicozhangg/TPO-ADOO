package ar.edu.tpo.domain;

import java.util.List;

public class Organizador extends Usuario {

    public Organizador(String email, String password) {
        super(email, password, (List<SancionActiva>) null);
    }

    public Organizador(String email, String password, List<SancionActiva> sancionesActivas) {
        super(email, password, sancionesActivas);
    }

    public Organizador(String email, String password, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
        super(email, password, sancionesActivas, sancionesHistoricas);
    }

    public Organizador(String id, String email, String password, List<SancionActiva> sancionesActivas) {
        super(id, email, password, sancionesActivas);
    }

    public Organizador(String id, String email, String password, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
        super(id, email, password, sancionesActivas, sancionesHistoricas);
    }

    @Override
    public String getTipo() {
        return "Organizador";
    }
}

