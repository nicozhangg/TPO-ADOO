package ar.edu.tpo.domain.rangos;

public class Radiante implements StateRangos {
    private final String nombre = "Radiante";
    private final int minimo = 2401;
    private final int maximo = 3000;

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

