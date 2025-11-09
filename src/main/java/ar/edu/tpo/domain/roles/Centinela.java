package ar.edu.tpo.domain.roles;

public class Centinela implements StateRoles {
    private final String nombre = "Centinela";

    @Override
    public String getNombre() {
        return nombre;
    }
}

