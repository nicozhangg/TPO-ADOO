package ar.edu.tpo.domain;

import java.util.List;

public class Organizador extends Usuario {

    public Organizador(String email, String password) {
        super(email, password, (List<String>) null);
    }

    public Organizador(String email, String password, List<String> sancionesActivas) {
        super(email, password, sancionesActivas);
    }

    public Organizador(String id, String email, String password, List<String> sancionesActivas) {
        super(id, email, password, sancionesActivas);
    }

    @Override
    public String getTipo() {
        return "Organizador";
    }
}

