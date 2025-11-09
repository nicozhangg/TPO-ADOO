package ar.edu.tpo.domain.rangos;

public class Ascendente implements StateRangos {
    private final String nombre = "Ascendente";
    private final int minimo = 1801;
    private final int maximo = 2100;

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

