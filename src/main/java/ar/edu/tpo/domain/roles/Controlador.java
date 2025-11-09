package ar.edu.tpo.domain.roles;

public class Controlador implements StateRoles {
    private final String nombre = "Controlador";

    @Override
    public String getNombre() {
        return nombre;
    }
}

