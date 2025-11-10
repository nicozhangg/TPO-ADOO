package ar.edu.tpo.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsula la lógica relacionada a los equipos de un scrim:
 * cupos, confirmaciones y utilidades de consulta.
 */
public class ScrimEquipo {

    private final int cupo;
    private final Equipo equipo1;
    private final Equipo equipo2;
    private final LinkedHashMap<String, Boolean> confirmaciones = new LinkedHashMap<>();

    public ScrimEquipo(int cupo) {
        if (cupo <= 0) {
            throw new IllegalArgumentException("Cupo debe ser >= 1");
        }
        this.cupo = cupo;
        this.equipo1 = new Equipo("Equipo 1");
        this.equipo2 = new Equipo("Equipo 2");
        confirmaciones.put(equipo1.getNombre(), Boolean.FALSE);
        confirmaciones.put(equipo2.getNombre(), Boolean.FALSE);
    }

    public Equipo getEquipo1() {
        return equipo1;
    }

    public Equipo getEquipo2() {
        return equipo2;
    }

    public Equipo obtenerEquipoPorNombre(String nombreEquipo) {
        Objects.requireNonNull(nombreEquipo, "nombreEquipo requerido");
        if (equipo1.getNombre().equals(nombreEquipo)) {
            return equipo1;
        }
        if (equipo2.getNombre().equals(nombreEquipo)) {
            return equipo2;
        }
        throw new IllegalArgumentException("Equipo no encontrado: " + nombreEquipo);
    }

    public void agregarJugadorAEquipo(String email, String nombreEquipo) {
        Equipo equipo = obtenerEquipoPorNombre(nombreEquipo);
        if (equipo.getCantidadJugadores() >= cupo) {
            throw new IllegalStateException("El equipo " + nombreEquipo + " tiene el cupo completo (" + cupo + " jugadores)");
        }
        equipo.agregarJugador(email);
        confirmaciones.put(equipo.getNombre(), Boolean.FALSE);
    }

    public void quitarJugador(String email) {
        boolean removido = false;
        if (equipo1.contieneJugador(email)) {
            equipo1.quitarJugador(email);
            confirmaciones.put(equipo1.getNombre(), Boolean.FALSE);
            removido = true;
        } else if (equipo2.contieneJugador(email)) {
            equipo2.quitarJugador(email);
            confirmaciones.put(equipo2.getNombre(), Boolean.FALSE);
            removido = true;
        }
        if (!removido) {
            throw new IllegalArgumentException("El jugador no pertenece a ningún equipo");
        }
    }

    public boolean contieneJugador(String email) {
        return equipo1.contieneJugador(email) || equipo2.contieneJugador(email);
    }

    public void establecerConfirmacion(String nombreEquipo, boolean valor) {
        Equipo equipo = obtenerEquipoPorNombre(nombreEquipo);
        if (valor && equipo.getCantidadJugadores() == 0) {
            throw new IllegalStateException("El equipo " + nombreEquipo + " no tiene jugadores para confirmar");
        }
        confirmaciones.put(nombreEquipo, valor);
    }

    public void confirmarEquipo(String nombreEquipo) {
        establecerConfirmacion(nombreEquipo, true);
    }

    public void confirmarPorJugador(String email) {
        confirmarEquipo(equipoDe(email));
    }

    public String equipoDe(String email) {
        if (equipo1.contieneJugador(email)) {
            return equipo1.getNombre();
        }
        if (equipo2.contieneJugador(email)) {
            return equipo2.getNombre();
        }
        throw new IllegalArgumentException("El jugador no pertenece a ningún equipo");
    }

    public void reiniciarConfirmaciones() {
        confirmaciones.replaceAll((k, v) -> Boolean.FALSE);
    }

    public boolean hayLugarEnEquipo(String nombreEquipo) {
        return obtenerEquipoPorNombre(nombreEquipo).getCantidadJugadores() < cupo;
    }

    public boolean hayCupoDisponible() {
        return getTotalJugadores() < cupo * 2;
    }

    public int getTotalJugadores() {
        return equipo1.getCantidadJugadores() + equipo2.getCantidadJugadores();
    }

    public boolean ambosEquiposCompletos() {
        return equipo1.getCantidadJugadores() >= cupo && equipo2.getCantidadJugadores() >= cupo;
    }

    public boolean ambosEquiposConfirmados() {
        return Boolean.TRUE.equals(confirmaciones.get(equipo1.getNombre()))
                && Boolean.TRUE.equals(confirmaciones.get(equipo2.getNombre()));
    }

    public Set<String> getJugadores() {
        Set<String> jugadores = new LinkedHashSet<>();
        jugadores.addAll(equipo1.getJugadores());
        jugadores.addAll(equipo2.getJugadores());
        return jugadores;
    }

    public Map<String, Boolean> getConfirmacionesPorEquipo() {
        return Collections.unmodifiableMap(confirmaciones);
    }

    public Map<String, Boolean> getConfirmacionesPorJugador() {
        LinkedHashMap<String, Boolean> resultado = new LinkedHashMap<>();
        equipo1.getJugadores().forEach(j -> resultado.put(j, confirmaciones.getOrDefault(equipo1.getNombre(), Boolean.FALSE)));
        equipo2.getJugadores().forEach(j -> resultado.put(j, confirmaciones.getOrDefault(equipo2.getNombre(), Boolean.FALSE)));
        return Collections.unmodifiableMap(resultado);
    }
}

