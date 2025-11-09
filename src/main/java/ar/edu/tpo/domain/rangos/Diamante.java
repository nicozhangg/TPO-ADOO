package ar.edu.tpo.domain.rangos;

public class Diamante implements StateRangos {
    private final String nombre = "Diamante";
    private final int minimo = 1501;
    private final int maximo = 1800;

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

