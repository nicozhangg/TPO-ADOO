package ar.edu.tpo.domain.estado;

import ar.edu.tpo.domain.EstadoScrim;
import ar.edu.tpo.domain.Estadistica;
import ar.edu.tpo.domain.Scrim;

/**
 * Estado: EN_JUEGO - en curso
 */
public class EnJuegoState extends EstadoScrim {
    public static final EnJuegoState INSTANCIA = new EnJuegoState();
    
    private EnJuegoState() {}
    
    @Override
    public void finalizar(Scrim scrim) {
        scrim.cambiarEstado(FinalizadoState.INSTANCIA);
    }
    
    @Override
    public void registrarEstadistica(Scrim scrim, Estadistica estadistica) {
        scrim.agregarEstadisticaDirecta(estadistica);
    }
    
    @Override
    public String getNombre() {
        return "EN_JUEGO";
    }
}



