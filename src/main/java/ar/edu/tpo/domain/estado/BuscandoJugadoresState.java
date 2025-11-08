package ar.edu.tpo.domain.estado;

import ar.edu.tpo.domain.EstadoScrim;
import ar.edu.tpo.domain.Scrim;

/**
 * Estado: BUSCANDO_JUGADORES - faltan plazas
 */
public class BuscandoJugadoresState extends EstadoScrim {
    public static final BuscandoJugadoresState INSTANCIA = new BuscandoJugadoresState();
    
    private BuscandoJugadoresState() {}
    
    @Override
    public void agregarJugadorAEquipo(Scrim scrim, String email, String nombreEquipo) {
        scrim.agregarJugadorAEquipoDirecto(email, nombreEquipo);
        recalcularEstado(scrim);
    }
    
    @Override
    public void agregarJugador(Scrim scrim, String email) {
        // Legacy: determinar equipo automáticamente
        if (scrim.getEmailCreador().equals(email)) {
            agregarJugadorAEquipo(scrim, email, scrim.getEquipo1().getNombre());
        } else {
            agregarJugadorAEquipo(scrim, email, scrim.getEquipo2().getNombre());
        }
    }
    
    @Override
    public void quitarJugador(Scrim scrim, String email) {
        scrim.quitarJugadorDirecto(email);
        recalcularEstado(scrim);
    }
    
    @Override
    public void cancelar(Scrim scrim) {
        scrim.cambiarEstado(CanceladoState.INSTANCIA);
    }
    
    @Override
    public void recalcularEstado(Scrim scrim) {
        if (!scrim.ambosEquiposCompletos()) {
            // Sigue en BUSCANDO_JUGADORES
            return;
        }
        
        // Ambos equipos completos, verificar confirmaciones
        if (scrim.ambosEquiposConfirmados()) {
            scrim.cambiarEstado(ConfirmadoState.INSTANCIA);
        } else {
            scrim.cambiarEstado(LobbyArmadoState.INSTANCIA);
        }
    }
    
    @Override
    public String getNombre() {
        return "BUSCANDO_JUGADORES";
    }
}

