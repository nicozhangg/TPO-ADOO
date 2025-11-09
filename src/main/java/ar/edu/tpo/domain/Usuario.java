package ar.edu.tpo.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class Usuario {
    private final String id;
    private final String email;
    private final String passwordHash;
    private final int mmr;
    private final int latenciaMs;
    private final List<SancionActiva> sancionesActivas;
    private Double kdaHistorico; // opcional

    protected Usuario(String email, String passwordHash, int mmr, int latenciaMs) {
        this(null, email, passwordHash, mmr, latenciaMs, null);
    }

    protected Usuario(String email, String passwordHash, int mmr, int latenciaMs, List<SancionActiva> sancionesActivas) {
        this(null, email, passwordHash, mmr, latenciaMs, sancionesActivas);
    }

    protected Usuario(String email, String passwordHash, List<SancionActiva> sancionesActivas) {
        this(null, email, passwordHash, 0, 0, sancionesActivas);
    }

    protected Usuario(String id, String email, String passwordHash, List<SancionActiva> sancionesActivas) {
        this(id, email, passwordHash, 0, 0, sancionesActivas);
    }

    protected Usuario(String id, String email, String passwordHash, int mmr, int latenciaMs, List<SancionActiva> sancionesActivas) {
        String effectiveId = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.id = effectiveId;
        this.email = Objects.requireNonNull(email, "email requerido");
        this.passwordHash = Objects.requireNonNull(passwordHash, "password requerido");
        this.mmr = mmr;
        this.latenciaMs = latenciaMs;
        this.sancionesActivas = new ArrayList<>();
        if (sancionesActivas != null) {
            this.sancionesActivas.addAll(sancionesActivas);
        }
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public int getMmr() { return mmr; }
    public int getLatenciaMs() { return latenciaMs; }
    public Double getKdaHistorico() { return kdaHistorico; }
    public void setKdaHistorico(Double kdaHistorico) { this.kdaHistorico = kdaHistorico; }

    public List<SancionActiva> getSancionesActivas() {
        depurarSancionesVencidas();
        return Collections.unmodifiableList(sancionesActivas);
    }

    public boolean tieneSancionesActivas() {
        depurarSancionesVencidas();
        return !sancionesActivas.isEmpty();
    }

    public void agregarSancion(String motivo, Duration duracion) {
        if (motivo == null || motivo.isBlank()) {
            return;
        }
        sancionesActivas.add(SancionActiva.porDuracion(motivo.trim(), duracion));
        depurarSancionesVencidas();
    }

    public void limpiarSanciones() {
        sancionesActivas.clear();
    }

    private void depurarSancionesVencidas() {
        Iterator<SancionActiva> it = sancionesActivas.iterator();
        while (it.hasNext()) {
            SancionActiva s = it.next();
            if (!s.estaActiva()) {
                it.remove();
            }
        }
    }

    public List<SancionActiva> getSancionesActivasSinDepurar() {
        return Collections.unmodifiableList(sancionesActivas);
    }

    public abstract String getTipo();

    @Override
    public String toString() {
        depurarSancionesVencidas();
        String sanciones = sancionesActivas.isEmpty() ? "" : ", sanciones=" + sancionesActivas;
        return "%s{id='%s', email='%s', mmr=%d, latMs=%d%s%s}"
                .formatted(
                        getTipo(),
                        id,
                        email,
                        mmr,
                        latenciaMs,
                        kdaHistorico != null ? ", kdaHist=" + kdaHistorico : "",
                        sanciones
                );
    }
}
