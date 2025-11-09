package ar.edu.tpo.domain.rangos;

public class Oro implements StateRangos {
    private final String nombre = "Oro";
    private final int minimo = 901;
    private final int maximo = 1200;

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

