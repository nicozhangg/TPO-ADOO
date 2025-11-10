package ar.edu.tpo.domain.rangos;

public class Radiante implements StateRangos {
    private final String nombre = "Radiante";
    private final int minimo = 2401;

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public int getMinimo() {
        return minimo;
    }

    @Override
    public int getMaximo() {
        return Integer.MAX_VALUE;
    }
}

