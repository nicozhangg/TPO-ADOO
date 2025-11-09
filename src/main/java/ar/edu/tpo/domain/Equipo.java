package ar.edu.tpo.domain;

import java.util.*;

/**
 * Representa un equipo en un scrim.
 */
public class Equipo {
    private final String nombre;
    private final Set<String> jugadores;

    public Equipo(String nombre) {
        this.nombre = Objects.requireNonNull(nombre);
        this.jugadores = new LinkedHashSet<>();
    }

    public String getNombre() {
        return nombre;
    }

    public Set<String> getJugadores() {
        return Collections.unmodifiableSet(jugadores);
    }

    public void agregarJugador(String email) {
        jugadores.add(email);
    }

    public void quitarJugador(String email) {
        jugadores.remove(email);
    }

    public int getCantidadJugadores() {
        return jugadores.size();
    }

    public boolean contieneJugador(String email) {
        return jugadores.contains(email);
    }

    @Override
    public String toString() {
        return "Equipo{" +
                "nombre='" + nombre + '\'' +
                ", jugadores=" + jugadores.size() +
                '}';
    }
}



