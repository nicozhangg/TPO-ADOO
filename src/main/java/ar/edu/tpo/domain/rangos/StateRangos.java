package ar.edu.tpo.domain.rangos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface StateRangos {
    String getNombre();
    int getMinimo();
    int getMaximo();

    static List<StateRangos> disponibles() {
        List<StateRangos> rangos = new ArrayList<>();
        rangos.add(new Hierro());
        rangos.add(new Bronce());
        rangos.add(new Plata());
        rangos.add(new Oro());
        rangos.add(new Platino());
        rangos.add(new Diamante());
        rangos.add(new Ascendente());
        rangos.add(new Immortal());
        rangos.add(new Radiante());
        return rangos;
    }

    static StateRangos asignarRangoSegunPuntos(int puntosJugador) {
        for (StateRangos rango : disponibles()) {
            if (puntosJugador >= rango.getMinimo() && puntosJugador <= rango.getMaximo()) {
                System.out.println("Rango asignado automáticamente: " + rango.getNombre() +
                        " (" + puntosJugador + " puntos)");
                return rango;
            }
        }
        return new Hierro();
    }

    static StateRangos fromNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        String buscado = nombre.trim().toLowerCase(Locale.ROOT);
        return disponibles().stream()
                .filter(r -> r.getNombre().toLowerCase(Locale.ROOT).equals(buscado))
                .findFirst()
                .orElse(null);
    }
}

