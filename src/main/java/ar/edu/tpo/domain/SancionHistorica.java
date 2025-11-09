package ar.edu.tpo.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class SancionHistorica {
    private final String motivo;
    private final LocalDateTime expiraEn;
    private final LocalDateTime levantadaEn;

    public SancionHistorica(String motivo, LocalDateTime expiraEn, LocalDateTime levantadaEn) {
        this.motivo = Objects.requireNonNull(motivo, "motivo requerido");
        this.expiraEn = expiraEn;
        this.levantadaEn = Objects.requireNonNull(levantadaEn, "levantadaEn requerido");
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public LocalDateTime getLevantadaEn() {
        return levantadaEn;
    }

    @Override
    public String toString() {
        String expiraStr = expiraEn == null ? "sin fecha de expiración" : "expiraba " + expiraEn;
        return "%s (%s, levantada %s)".formatted(motivo, expiraStr, levantadaEn);
    }
}


