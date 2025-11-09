package ar.edu.tpo.domain.rangos;

public class Hierro implements StateRangos {
    private final String nombre = "Hierro";
    private final int minimo = 0;
    private final int maximo = 300;

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

