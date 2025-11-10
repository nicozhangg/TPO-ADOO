package ar.edu.tpo.domain;

import ar.edu.tpo.domain.alerta.ScrimAlerta;
import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.regiones.StateRegion;
import ar.edu.tpo.domain.roles.StateRoles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Jugador extends Usuario {
    private final int mmr;
    private final int latenciaMs;
    private final String rangoNombre;
    private final String rolNombre;
    private final String regionNombre;
    private final List<String> scrimsFavoritas;
    private final List<ScrimAlerta> alertasScrim;
    private Double kdaHistorico;

    public Jugador(String nombre,
                   String email,
                   String password,
                   int mmr,
                   int latenciaMs,
                   StateRangos rango,
                   StateRoles rolJuego,
                   StateRegion region) {
        this(null,
                nombre,
                email,
                password,
                mmr,
                latenciaMs,
                rango != null ? rango.getNombre() : StateRangos.disponibles().get(0).getNombre(),
                rolJuego != null ? rolJuego.getNombre() : StateRoles.disponibles().get(0).getNombre(),
                region != null ? region.getNombre() : StateRegion.disponibles().get(0).getNombre(),
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public Jugador(String id,
                   String nombre,
                   String email,
                   String password,
                   int mmr,
                   int latenciaMs,
                   String rangoNombre,
                   String rolNombre,
                   String regionNombre,
                   List<SancionActiva> sancionesActivas,
                   List<SancionHistorica> sancionesHistoricas,
                   Integer strikes,
                   Boolean suspendido,
                   List<String> scrimsFavoritas,
                   List<ScrimAlerta> alertasScrim) {
        super(id, nombre, email, password, sancionesActivas, sancionesHistoricas, strikes, suspendido);
        this.mmr = mmr;
        this.latenciaMs = latenciaMs;
        this.rangoNombre = Objects.requireNonNull(rangoNombre, "rango requerido");
        this.rolNombre = Objects.requireNonNull(rolNombre, "rol requerido");
        this.regionNombre = Objects.requireNonNull(regionNombre, "región requerida");
        this.scrimsFavoritas = new ArrayList<>();
        if (scrimsFavoritas != null) {
            this.scrimsFavoritas.addAll(scrimsFavoritas);
        }
        this.alertasScrim = new ArrayList<>();
        if (alertasScrim != null) {
            this.alertasScrim.addAll(alertasScrim);
        }
    }

    public int getMmr() {
        return mmr;
    }

    public int getLatenciaMs() {
        return latenciaMs;
    }

    public Double getKdaHistorico() {
        return kdaHistorico;
    }

    public void setKdaHistorico(Double kdaHistorico) {
        this.kdaHistorico = kdaHistorico;
    }

    public StateRangos getRango() {
        return StateRangos.fromNombre(rangoNombre);
    }

    public StateRoles getRolPreferido() {
        return StateRoles.fromNombre(rolNombre);
    }

    public StateRegion getRegion() {
        return StateRegion.fromNombre(regionNombre);
    }

    public String getRangoNombre() {
        return rangoNombre;
    }

    public String getRolNombre() {
        return rolNombre;
    }

    public String getRegionNombre() {
        return regionNombre;
    }

    public List<String> getScrimsFavoritas() {
        return Collections.unmodifiableList(scrimsFavoritas);
    }

    public List<ScrimAlerta> getAlertasScrim() {
        return Collections.unmodifiableList(alertasScrim);
    }

    @Override
    public SancionActiva agregarSancion(String motivo, Duration duracion) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        SancionActiva sancion = SancionActiva.porDuracion(motivo.trim(), duracion);
        sancionesActivas.add(sancion);
        removerSancionesVencidas();
        return sancion;
    }

    public boolean agregarScrimFavorita(String idScrim) {
        if (scrimsFavoritas.contains(idScrim)) {
            return false;
        }
        scrimsFavoritas.add(idScrim);
        return true;
    }

    public void agregarAlertaScrim(ScrimAlerta alerta) {
        alertasScrim.add(alerta);
    }

    public ScrimAlerta eliminarAlertaScrim(int indice) {
        if (indice < 0 || indice >= alertasScrim.size()) {
            throw new IllegalArgumentException("Índice de alerta inválido.");
        }
        return alertasScrim.remove(indice);
    }

    @Override
    public String getTipo() {
        return "Jugador";
    }

    @Override
    public String toString() {
        String base = super.toString();
        if (base.endsWith("}")) {
            base = base.substring(0, base.length() - 1);
        }
        StringBuilder sb = new StringBuilder(base);
        sb.append(", mmr=").append(mmr)
                .append(", latMs=").append(latenciaMs)
                .append(", rango='").append(rangoNombre).append('\'')
                .append(", rolPreferido='").append(rolNombre).append('\'')
                .append(", region='").append(regionNombre).append('\'');
        if (kdaHistorico != null) {
            sb.append(", kdaHist=").append(kdaHistorico);
        }
        sb.append('}');
        return sb.toString();
    }
}

