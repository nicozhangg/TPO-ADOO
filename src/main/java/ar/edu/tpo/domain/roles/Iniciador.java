package ar.edu.tpo.domain.roles;

public class Iniciador implements StateRoles {
    private final String nombre = "Iniciador";

    @Override
    public String getNombre() {
        return nombre;
    }
}

