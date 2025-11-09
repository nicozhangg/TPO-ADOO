package ar.edu.tpo.domain.roles;

public class Duelista implements StateRoles {
    private final String nombre = "Duelista";

    @Override
    public String getNombre() {
        return nombre;
    }
}

