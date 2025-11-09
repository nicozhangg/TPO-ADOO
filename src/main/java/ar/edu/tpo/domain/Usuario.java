package ar.edu.tpo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class Usuario {
    private final String id;
    private final String email;
    private final int mmr;
    private final int latenciaMs;
    private final List<String> sancionesActivas;
    private Double kdaHistorico; // opcional

    protected Usuario(String email, int mmr, int latenciaMs) {
        this(null, email, mmr, latenciaMs, null);
    }

    protected Usuario(String email, int mmr, int latenciaMs, List<String> sancionesActivas) {
        this(null, email, mmr, latenciaMs, sancionesActivas);
    }

    protected Usuario(String email, List<String> sancionesActivas) {
        this(null, email, 0, 0, sancionesActivas);
    }

    protected Usuario(String id, String email, List<String> sancionesActivas) {
        this(id, email, 0, 0, sancionesActivas);
    }

    protected Usuario(String id, String email, int mmr, int latenciaMs, List<String> sancionesActivas) {
        String effectiveId = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.id = effectiveId;
        this.email = Objects.requireNonNull(email, "email requerido");
        this.mmr = mmr;
        this.latenciaMs = latenciaMs;
        this.sancionesActivas = new ArrayList<>();
        if (sancionesActivas != null) {
            this.sancionesActivas.addAll(sancionesActivas);
        }
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public int getMmr() { return mmr; }
    public int getLatenciaMs() { return latenciaMs; }
    public Double getKdaHistorico() { return kdaHistorico; }
    public void setKdaHistorico(Double kdaHistorico) { this.kdaHistorico = kdaHistorico; }

    public List<String> getSancionesActivas() {
        return Collections.unmodifiableList(sancionesActivas);
    }

    public boolean tieneSancionesActivas() {
        return !sancionesActivas.isEmpty();
    }

    public void agregarSancion(String motivo) {
        if (motivo != null && !motivo.isBlank()) {
            sancionesActivas.add(motivo.trim());
        }
    }

    public void limpiarSanciones() {
        sancionesActivas.clear();
    }

    public abstract String getTipo();

    @Override
    public String toString() {
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
