package ar.edu.tpo.domain.rangos;

public class Bronce implements StateRangos {
    private final String nombre = "Bronce";
    private final int minimo = 301;
    private final int maximo = 600;

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

