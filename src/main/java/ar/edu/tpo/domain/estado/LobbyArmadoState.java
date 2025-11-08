package ar.edu.tpo.domain.estado;

import ar.edu.tpo.domain.Equipo;
import ar.edu.tpo.domain.EstadoScrim;
import ar.edu.tpo.domain.Scrim;

/**
 * Estado: LOBBY_ARMADO - cupo completo, falta confirmar
 */
public class LobbyArmadoState extends EstadoScrim {
    public static final LobbyArmadoState INSTANCIA = new LobbyArmadoState();
    
    private LobbyArmadoState() {}
    
    @Override
    public void quitarJugador(Scrim scrim, String email) {
        scrim.quitarJugadorDirecto(email);
        recalcularEstado(scrim);
    }
    
    @Override
    public void confirmarEquipo(Scrim scrim, String nombreEquipo) {
        Equipo equipo = scrim.obtenerEquipoPorNombre(nombreEquipo);
        scrim.getConfirmacionesEquipos().put(equipo.getNombre(), Boolean.TRUE);
        recalcularEstado(scrim);
    }
    
    @Override
    public void confirmarJugador(Scrim scrim, String email) {
        // Legacy: confirmar el equipo del jugador
        if (scrim.getEquipo1().contieneJugador(email)) {
            confirmarEquipo(scrim, scrim.getEquipo1().getNombre());
        } else if (scrim.getEquipo2().contieneJugador(email)) {
            confirmarEquipo(scrim, scrim.getEquipo2().getNombre());
        } else {
            throw new IllegalStateException("El jugador no está en el scrim");
        }
    }
    
    @Override
    public void cancelar(Scrim scrim) {
        scrim.cambiarEstado(CanceladoState.INSTANCIA);
    }
    
    @Override
    public void recalcularEstado(Scrim scrim) {
        if (!scrim.ambosEquiposCompletos()) {
            // Volver a BUSCANDO_JUGADORES
            scrim.getConfirmacionesEquipos().put(scrim.getEquipo1().getNombre(), Boolean.FALSE);
            scrim.getConfirmacionesEquipos().put(scrim.getEquipo2().getNombre(), Boolean.FALSE);
            scrim.cambiarEstado(BuscandoJugadoresState.INSTANCIA);
            return;
        }
        
        // Ambos equipos completos, verificar si ambos confirmaron
        if (scrim.ambosEquiposConfirmados()) {
            scrim.cambiarEstado(ConfirmadoState.INSTANCIA);
        }
        // Si no ambos confirmaron, sigue en LOBBY_ARMADO
    }
    
    @Override
    public String getNombre() {
        return "LOBBY_ARMADO";
    }
}

