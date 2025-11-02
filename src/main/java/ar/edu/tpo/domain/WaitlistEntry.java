package ar.edu.tpo.domain;
import java.time.LocalDateTime;
// Composicion: solo existe dentro del Scrim
public record WaitlistEntry(String emailJugador, LocalDateTime fechaSolicitud, int orden) {}
