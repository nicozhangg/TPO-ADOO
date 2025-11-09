package ar.edu.tpo.domain.regiones;

public class Europa implements StateRegion {
    private final String nombre = "Europa";
    private final int ping = 120;

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public int getPing() {
        return ping;
    }
}

