package ar.edu.tpo.domain;

import java.util.List;

public class Organizador extends Usuario {

    public Organizador(String email) {
        super(email, (List<String>) null);
    }

    public Organizador(String email, List<String> sancionesActivas) {
        super(email, sancionesActivas);
    }

    public Organizador(String id, String email, List<String> sancionesActivas) {
        super(id, email, sancionesActivas);
    }

    public Organizador(String id, String email, int mmr, int latenciaMs, List<String> sancionesActivas) {
        super(id, email, mmr, latenciaMs, sancionesActivas);
    }

    @Override
    public String getTipo() {
        return "Organizador";
    }
}

