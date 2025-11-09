package ar.edu.tpo.domain.regiones;

public class America implements StateRegion {
    private final String nombre = "America";
    private final int ping = 60;

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public int getPing() {
        return ping;
    }
}

