package ar.edu.tpo.domain.rangos;

public class Plata implements StateRangos {
    private final String nombre = "Plata";
    private final int minimo = 601;
    private final int maximo = 900;

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

