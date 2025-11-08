package ar.edu.tpo.domain.estado;

import ar.edu.tpo.domain.EstadoScrim;
import ar.edu.tpo.domain.Estadistica;
import ar.edu.tpo.domain.Scrim;

/**
 * Estado: FINALIZADO - terminó, se pueden cargar stats
 */
public class FinalizadoState extends EstadoScrim {
    public static final FinalizadoState INSTANCIA = new FinalizadoState();
    
    private FinalizadoState() {}
    
    @Override
    public void registrarEstadistica(Scrim scrim, Estadistica estadistica) {
        scrim.agregarEstadisticaDirecta(estadistica);
    }
    
    @Override
    public String getNombre() {
        return "FINALIZADO";
    }
}



