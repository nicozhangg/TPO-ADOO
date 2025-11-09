package ar.edu.tpo.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class SancionActiva {
    private final String motivo;
    private final LocalDateTime expiraEn;

    public SancionActiva(String motivo, LocalDateTime expiraEn) {
        this.motivo = Objects.requireNonNull(motivo, "motivo requerido");
        this.expiraEn = expiraEn;
    }

    public static SancionActiva porDuracion(String motivo, Duration duracion) {
        if (duracion == null) {
            return new SancionActiva(motivo, null);
        }
        return new SancionActiva(motivo, LocalDateTime.now().plus(duracion));
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public boolean estaActiva() {
        return expiraEn == null || LocalDateTime.now().isBefore(expiraEn);
    }

    @Override
    public String toString() {
        if (expiraEn == null) {
            return motivo;
        }
        return motivo + " hasta " + expiraEn;
    }
}

