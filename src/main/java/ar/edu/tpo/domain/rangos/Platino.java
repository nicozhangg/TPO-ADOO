package ar.edu.tpo.domain.rangos;

public class Platino implements StateRangos {
    private final String nombre = "Platino";
    private final int minimo = 1201;
    private final int maximo = 1500;

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

