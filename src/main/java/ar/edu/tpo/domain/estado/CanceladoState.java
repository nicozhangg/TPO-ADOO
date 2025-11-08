package ar.edu.tpo.domain.estado;

import ar.edu.tpo.domain.EstadoScrim;

/**
 * Estado: CANCELADO - cancelado antes de iniciar
 */
public class CanceladoState extends EstadoScrim {
    public static final CanceladoState INSTANCIA = new CanceladoState();
    
    private CanceladoState() {}
    
    @Override
    public String getNombre() {
        return "CANCELADO";
    }
}



