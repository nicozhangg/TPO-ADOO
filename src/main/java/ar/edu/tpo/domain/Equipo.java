package ar.edu.tpo.domain;

import java.util.*;

/**
 * Representa un equipo en un scrim.
 */
public class Equipo {
    private final String nombre;
    private final String emailCapitan;
    private final Set<String> jugadores;
    
    public Equipo(String nombre, String emailCapitan) {
        this.nombre = Objects.requireNonNull(nombre);
        this.emailCapitan = Objects.requireNonNull(emailCapitan);
        this.jugadores = new LinkedHashSet<>();
        this.jugadores.add(emailCapitan); // El capitán siempre está en el equipo
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public String getEmailCapitan() {
        return emailCapitan;
    }
    
    public Set<String> getJugadores() {
        return Collections.unmodifiableSet(jugadores);
    }
    
    public void agregarJugador(String email) {
        jugadores.add(email);
    }
    
    public void quitarJugador(String email) {
        if (email.equals(emailCapitan)) {
            throw new IllegalArgumentException("No se puede quitar al capitán del equipo");
        }
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
                ", capitan='" + emailCapitan + '\'' +
                ", jugadores=" + jugadores.size() +
                '}';
    }
}



