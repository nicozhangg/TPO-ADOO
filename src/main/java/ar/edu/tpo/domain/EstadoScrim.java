package ar.edu.tpo.domain;

/**
 * Interfaz State para el patrón State.
 * Cada estado concreto implementa las operaciones permitidas.
 */
public abstract class EstadoScrim {
    
    // Métodos que cada estado puede implementar o lanzar excepción
    
    public void agregarJugador(Scrim scrim, String email) {
        throw new IllegalStateException("No se puede agregar jugador en estado " + getNombre());
    }
    
    public void agregarJugadorAEquipo(Scrim scrim, String email, String nombreEquipo) {
        // Por defecto, delegar al método legacy
        agregarJugador(scrim, email);
    }
    
    public void quitarJugador(Scrim scrim, String email) {
        throw new IllegalStateException("No se puede quitar jugador en estado " + getNombre());
    }
    
    public void confirmarJugador(Scrim scrim, String email) {
        throw new IllegalStateException("No se puede confirmar jugador en estado " + getNombre());
    }
    
    public void confirmarEquipo(Scrim scrim, String nombreEquipo) {
        throw new IllegalStateException("No se puede confirmar equipo en estado " + getNombre());
    }
    
    public void iniciar(Scrim scrim) {
        throw new IllegalStateException("No se puede iniciar en estado " + getNombre());
    }
    
    public void finalizar(Scrim scrim) {
        throw new IllegalStateException("No se puede finalizar en estado " + getNombre());
    }
    
    public void cancelar(Scrim scrim) {
        throw new IllegalStateException("No se puede cancelar en estado " + getNombre());
    }
    
    public void registrarEstadistica(Scrim scrim, Estadistica estadistica) {
        throw new IllegalStateException("No se pueden registrar estadísticas en estado " + getNombre());
    }
    
    /**
     * Recalcula el estado basado en cupo y confirmaciones.
     * Solo algunos estados permiten recalcular.
     */
    public void recalcularEstado(Scrim scrim) {
        // Por defecto no hace nada, solo algunos estados permiten recalcular
    }
    
    /**
     * Obtiene el nombre del estado para serialización.
     */
    public abstract String getNombre();
    
    /**
     * Factory method para crear estados desde string (JSON).
     */
    public static EstadoScrim desdeNombre(String nombre) {
        return switch (nombre) {
            case "BUSCANDO_JUGADORES" -> ar.edu.tpo.domain.estado.BuscandoJugadoresState.INSTANCIA;
            case "LOBBY_ARMADO" -> ar.edu.tpo.domain.estado.LobbyArmadoState.INSTANCIA;
            case "CONFIRMADO" -> ar.edu.tpo.domain.estado.ConfirmadoState.INSTANCIA;
            case "EN_JUEGO" -> ar.edu.tpo.domain.estado.EnJuegoState.INSTANCIA;
            case "FINALIZADO" -> ar.edu.tpo.domain.estado.FinalizadoState.INSTANCIA;
            case "CANCELADO" -> ar.edu.tpo.domain.estado.CanceladoState.INSTANCIA;
            default -> throw new IllegalArgumentException("Estado desconocido: " + nombre);
        };
    }
}
