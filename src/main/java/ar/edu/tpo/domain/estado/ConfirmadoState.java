package ar.edu.tpo.domain.estado;

import ar.edu.tpo.domain.Equipo;
import ar.edu.tpo.domain.EstadoScrim;
import ar.edu.tpo.domain.Scrim;

/**
 * Estado: CONFIRMADO - todos confirmaron, listo para iniciar
 */
public class ConfirmadoState extends EstadoScrim {
    public static final ConfirmadoState INSTANCIA = new ConfirmadoState();
    
    private ConfirmadoState() {}
    
    @Override
    public void quitarJugador(Scrim scrim, String email) {
        scrim.quitarJugadorDirecto(email);
        recalcularEstado(scrim);
    }
    
    @Override
    public void confirmarEquipo(Scrim scrim, String nombreEquipo) {
        Equipo equipo = scrim.obtenerEquipoPorNombre(nombreEquipo);
        scrim.establecerConfirmacionEquipo(equipo.getNombre(), true);
        // Ya está confirmado, no necesita recalcular
    }
    
    @Override
    public void confirmarJugador(Scrim scrim, String email) {
        scrim.confirmarJugador(email);
    }
    
    @Override
    public void iniciar(Scrim scrim) {
        scrim.cambiarEstado(EnJuegoState.INSTANCIA);
    }
    
    @Override
    public void cancelar(Scrim scrim) {
        scrim.cambiarEstado(CanceladoState.INSTANCIA);
    }
    
    @Override
    public void recalcularEstado(Scrim scrim) {
        if (!scrim.ambosEquiposCompletos()) {
            // Volver a BUSCANDO_JUGADORES
            scrim.reiniciarConfirmacionesDeEquipos();
            scrim.cambiarEstado(BuscandoJugadoresState.INSTANCIA);
            return;
        }
        
        // Verificar si ambos equipos siguen confirmados
        if (!scrim.ambosEquiposConfirmados()) {
            scrim.cambiarEstado(LobbyArmadoState.INSTANCIA);
        }
    }
    
    @Override
    public String getNombre() {
        return "CONFIRMADO";
    }
}

