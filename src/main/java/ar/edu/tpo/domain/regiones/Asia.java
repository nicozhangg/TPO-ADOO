package ar.edu.tpo.domain.regiones;

public class Asia implements StateRegion {
    private final String nombre = "Asia";
    private final int ping = 180;

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public int getPing() {
        return ping;
    }
}

