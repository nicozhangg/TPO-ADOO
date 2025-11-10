package ar.edu.tpo.service.scrim;

import ar.edu.tpo.domain.Estadistica;
import ar.edu.tpo.domain.KDA;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.estado.FinalizadoState;
import ar.edu.tpo.repository.ScrimRepository;
import ar.edu.tpo.service.UsuarioService;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Servicio responsable de las estadísticas y resultados asociados a un scrim.
 */
public class ScrimStatsService {

    private final ScrimRepository repo;
    private final UsuarioService usuarios;

    public ScrimStatsService(ScrimRepository repo, UsuarioService usuarios) {
        this.repo = Objects.requireNonNull(repo);
        this.usuarios = Objects.requireNonNull(usuarios);
    }

    public void cargarResultado(String idScrim, String emailJugador,
                                int kills, int assists, int deaths, double rating) {
        usuarios.buscar(emailJugador);
        Scrim scrim = obtenerScrim(idScrim);
        verificarFinalizado(scrim);
        KDA kda = new KDA(kills, assists, deaths);
        Estadistica estadistica = new Estadistica(emailJugador, kda, rating, LocalDateTime.now());
        scrim.registrarEstadistica(estadistica);
        repo.guardar(scrim);
        System.out.println("[evento] ResultadoReportado scrim=" + idScrim +
                " jugador=" + emailJugador + " kda=" + kda.valor());
    }

    private Scrim obtenerScrim(String idScrim) {
        return repo.buscarPorId(idScrim);
    }

    private void verificarFinalizado(Scrim scrim) {
        if (scrim.getEstado() != FinalizadoState.INSTANCIA) {
            throw new IllegalStateException("Solo se pueden cargar resultados cuando el scrim está en estado FINALIZADO.");
        }
    }
}


