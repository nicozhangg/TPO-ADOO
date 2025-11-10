package ar.edu.tpo.domain;

import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.regiones.StateRegion;
import ar.edu.tpo.domain.roles.StateRoles;

import java.util.List;
import java.util.Objects;

public class Jugador extends Usuario {
    private final String rangoNombre;
    private final String rolNombre;
    private final String regionNombre;

    public Jugador(String email,
                   String password,
                   int mmr,
                   int latenciaMs,
                   StateRangos rango,
                   StateRoles rolJuego,
                   StateRegion region) {
        this(null,
                email,
                password,
                mmr,
                latenciaMs,
                rango != null ? rango.getNombre() : null,
                rolJuego != null ? rolJuego.getNombre() : null,
                region != null ? region.getNombre() : null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public Jugador(String email,
                   String password,
                   int mmr,
                   int latenciaMs,
                   String rangoNombre,
                   String rolNombre,
                   String regionNombre) {
        this(null, email, password, mmr, latenciaMs, rangoNombre, rolNombre, regionNombre, null, null, null, null, null, null);
    }

    public Jugador(String id,
                   String email,
                   String password,
                   int mmr,
                   int latenciaMs,
                   String rangoNombre,
                   String rolNombre,
                   String regionNombre,
                   List<SancionActiva> sancionesActivas,
                   List<SancionHistorica> sancionesHistoricas) {
        this(id, email, password, mmr, latenciaMs, rangoNombre, rolNombre, regionNombre, sancionesActivas, sancionesHistoricas, null, null, null, null);
    }

    public Jugador(String id,
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
                   List<ar.edu.tpo.domain.alerta.ScrimAlerta> alertasScrim) {
        super(id, email, password, mmr, latenciaMs, sancionesActivas, sancionesHistoricas, strikes, suspendido, scrimsFavoritas, alertasScrim);
        this.rangoNombre = Objects.requireNonNull(rangoNombre, "rango requerido");
        this.rolNombre = Objects.requireNonNull(rolNombre, "rol requerido");
        this.regionNombre = Objects.requireNonNull(regionNombre, "región requerida");
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

    @Override
    public String getTipo() {
        return "Jugador";
    }

    @Override
    public String toString() {
        return super.toString() +
                ", rango='" + rangoNombre + '\'' +
                ", rolPreferido='" + rolNombre + '\'' +
                ", region='" + regionNombre + '\'';
    }
}

