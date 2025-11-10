package ar.edu.tpo.service.estrategias;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.Usuario;

import java.util.List;
import java.util.stream.Collectors;

public class EstrategiaPorLatencia implements EstrategiaEmparejamiento {
    @Override
    public List<Usuario> seleccionar(List<Usuario> candidatos, Scrim scrim) {
        return candidatos.stream()
                .filter(u -> u instanceof Jugador jugador && jugador.getLatenciaMs() <= 80)
                .collect(Collectors.toList());
    }
}
