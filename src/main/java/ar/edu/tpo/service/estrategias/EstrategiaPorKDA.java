package ar.edu.tpo.service.estrategias;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.Usuario;

import java.util.List;
import java.util.stream.Collectors;

public class EstrategiaPorKDA implements EstrategiaEmparejamiento {
    @Override
    public List<Usuario> seleccionar(List<Usuario> candidatos, Scrim scrim) {
        return candidatos.stream()
                .filter(u -> {
                    if (!(u instanceof Jugador jugador)) {
                        return true;
                    }
                    Double kda = jugador.getKdaHistorico();
                    return kda == null || kda >= 1.5;
                })
                .collect(Collectors.toList());
    }
}
