package ar.edu.tpo.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Encapsula las operaciones relacionadas a la lista de suplentes del scrim.
 */
public class Waitlist {

    private final List<WaitlistEntry> entradas = new ArrayList<>();

    public boolean agregar(String emailJugador) {
        Objects.requireNonNull(emailJugador, "emailJugador requerido");
        if (esta(emailJugador)) {
            return false;
        }
        entradas.add(new WaitlistEntry(emailJugador, LocalDateTime.now(), entradas.size() + 1));
        return true;
    }

    public boolean quitar(String emailJugador) {
        Objects.requireNonNull(emailJugador, "emailJugador requerido");
        boolean removed = entradas.removeIf(entry -> entry.emailJugador().equalsIgnoreCase(emailJugador));
        if (removed) {
            reordenar();
        }
        return removed;
    }

    public boolean esta(String emailJugador) {
        return entradas.stream().anyMatch(entry -> entry.emailJugador().equalsIgnoreCase(emailJugador));
    }

    public boolean esVacia() {
        return entradas.isEmpty();
    }

    public List<WaitlistEntry> comoListaInmutable() {
        return Collections.unmodifiableList(entradas);
    }

    public Stream<WaitlistEntry> stream() {
        return entradas.stream();
    }

    public void agregarDesdePersistencia(WaitlistEntry entry) {
        Objects.requireNonNull(entry, "entry requerido");
        entradas.add(entry);
    }

    private void reordenar() {
        for (int i = 0; i < entradas.size(); i++) {
            WaitlistEntry entry = entradas.get(i);
            entradas.set(i, new WaitlistEntry(entry.emailJugador(), entry.fechaSolicitud(), i + 1));
        }
    }
}

