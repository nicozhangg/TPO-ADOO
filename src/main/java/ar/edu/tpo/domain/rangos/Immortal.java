package ar.edu.tpo.domain.rangos;

public class Immortal implements StateRangos {
    private final String nombre = "Immortal";
    private final int minimo = 2101;
    private final int maximo = 2400;

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
        return maximo;
    }
}

