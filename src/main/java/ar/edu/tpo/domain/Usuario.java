package ar.edu.tpo.domain;

import java.time.Duration;
import java.time.LocalDateTime;
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
    private final List<SancionHistorica> sancionesHistoricas;
    private Double kdaHistorico; // opcional

    protected Usuario(String email, String passwordHash, int mmr, int latenciaMs) {
        this(null, email, passwordHash, mmr, latenciaMs, null, null);
    }

    protected Usuario(String email, String passwordHash, int mmr, int latenciaMs, List<SancionActiva> sancionesActivas) {
        this(null, email, passwordHash, mmr, latenciaMs, sancionesActivas, null);
    }

    protected Usuario(String email, String passwordHash, List<SancionActiva> sancionesActivas) {
        this(null, email, passwordHash, 0, 0, sancionesActivas, null);
    }

    protected Usuario(String email, String passwordHash, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
        this(null, email, passwordHash, 0, 0, sancionesActivas, sancionesHistoricas);
    }

    protected Usuario(String id, String email, String passwordHash, List<SancionActiva> sancionesActivas) {
        this(id, email, passwordHash, 0, 0, sancionesActivas, null);
    }

    protected Usuario(String id, String email, String passwordHash, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
        this(id, email, passwordHash, 0, 0, sancionesActivas, sancionesHistoricas);
    }

    protected Usuario(String id, String email, String passwordHash, int mmr, int latenciaMs, List<SancionActiva> sancionesActivas) {
        this(id, email, passwordHash, mmr, latenciaMs, sancionesActivas, null);
    }

    protected Usuario(String id, String email, String passwordHash, int mmr, int latenciaMs, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
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
        this.sancionesHistoricas = new ArrayList<>();
        if (sancionesHistoricas != null) {
            this.sancionesHistoricas.addAll(sancionesHistoricas);
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

    public List<SancionHistorica> getSancionesHistoricas() {
        return Collections.unmodifiableList(sancionesHistoricas);
    }

    public boolean tieneSancionesActivas() {
        depurarSancionesVencidas();
        return !sancionesActivas.isEmpty();
    }

    public SancionActiva agregarSancion(String motivo, Duration duracion) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        SancionActiva sancion = SancionActiva.porDuracion(motivo.trim(), duracion);
        sancionesActivas.add(sancion);
        depurarSancionesVencidas();
        return sancion;
    }

    public void limpiarSanciones() {
        if (!sancionesActivas.isEmpty()) {
            LocalDateTime levantadaEn = LocalDateTime.now();
            sancionesActivas.forEach(s -> sancionesHistoricas.add(s.aHistorica(levantadaEn)));
        }
        sancionesActivas.clear();
    }

    public List<SancionHistorica> removerSancionesVencidas() {
        return depurarSancionesVencidas();
    }

    private List<SancionHistorica> depurarSancionesVencidas() {
        List<SancionHistorica> removidas = new ArrayList<>();
        Iterator<SancionActiva> it = sancionesActivas.iterator();
        LocalDateTime levantadaEn = LocalDateTime.now();
        while (it.hasNext()) {
            SancionActiva s = it.next();
            if (!s.estaActiva()) {
                it.remove();
                SancionHistorica historica = s.aHistorica(levantadaEn);
                sancionesHistoricas.add(historica);
                removidas.add(historica);
            }
        }
        return removidas;
    }

    public List<SancionActiva> getSancionesActivasSinDepurar() {
        return Collections.unmodifiableList(sancionesActivas);
    }

    public SancionHistorica levantarSancionPorIndice(int indice) {
        depurarSancionesVencidas();
        if (indice < 0 || indice >= sancionesActivas.size()) {
            throw new IndexOutOfBoundsException("Índice de sanción inválido");
        }
        SancionActiva sancion = sancionesActivas.remove(indice);
        SancionHistorica historica = sancion.aHistorica(LocalDateTime.now());
        sancionesHistoricas.add(historica);
        return historica;
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
