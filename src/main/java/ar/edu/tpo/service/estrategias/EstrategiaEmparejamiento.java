package ar.edu.tpo.service.estrategias;

import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.Usuario;
import java.util.List;

public interface EstrategiaEmparejamiento {
    List<Usuario> seleccionar(List<Usuario> candidatos, Scrim scrim);
}
