package ar.edu.tpo.service.scrim;

import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.repository.ScrimRepository;
import ar.edu.tpo.service.UsuarioService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Servicio enfocado en el ciclo de vida del Scrim: creación, agenda y estados.
 */
public class ScrimCicloDeVidaService {

    private final ScrimRepository repo;
    private final UsuarioService usuarios;

    public ScrimCicloDeVidaService(ScrimRepository repo, UsuarioService usuarios) {
        this.repo = Objects.requireNonNull(repo);
        this.usuarios = Objects.requireNonNull(usuarios);
    }

    public Scrim crearScrim(String juego, String emailCreador,
                            int rangoMin, int rangoMax, int cupo,
                            String formato, String region, int latenciaMaxMs,
                            String modalidad,
                            LocalDateTime inicio, LocalDateTime fin) {
        if (formato == null || formato.isBlank()) {
            throw new IllegalArgumentException("Formato requerido");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Región requerida");
        }
        if (modalidad == null || modalidad.isBlank()) {
            throw new IllegalArgumentException("Modalidad requerida");
        }
        if (latenciaMaxMs <= 0) {
            throw new IllegalArgumentException("Latencia máxima debe ser mayor a 0");
        }

        usuarios.buscar(emailCreador);

        Scrim scrim = new Scrim(
                juego, emailCreador,
                rangoMin, rangoMax, cupo,
                formato.trim(), region.trim(), latenciaMaxMs,
                modalidad.trim()
        );

        if (inicio != null && fin != null) {
            scrim.programar(inicio, fin);
        }

        repo.guardar(scrim);
        System.out.println("[evento] ScrimCreado " + scrim.getId() + " (formato=" + formato + ", región=" + region + ", modalidad=" + modalidad + ")");
        return scrim;
    }

    /** Compatibilidad: creación simple con valores por defecto. */
    public Scrim crearScrim(String juego, String emailCreador,
                            int rangoMin, int rangoMax) {
        return crearScrim(juego, emailCreador, rangoMin, rangoMax,
                2, "2v2", "REGION_DESCONOCIDA", 100, "casual",
                null, null);
    }

    public List<Scrim> listarScrims() {
        return repo.listar();
    }

    public Scrim buscar(String id) {
        return repo.buscarPorId(id);
    }

    public void programar(String idScrim, LocalDateTime inicio, LocalDateTime fin) {
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.programar(inicio, fin);
        repo.guardar(scrim);
        System.out.println("[evento] ScrimProgramado " + idScrim + " " + inicio + "→" + fin);
    }

    public void limpiarAgenda(String idScrim) {
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.limpiarAgenda();
        repo.guardar(scrim);
        System.out.println("[evento] ScrimAgendaLimpia " + idScrim);
    }

    public void iniciarScrim(String idScrim) {
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.iniciar();
        repo.guardar(scrim);
        System.out.println("[evento] ScrimEnJuego " + idScrim);
    }

    public void finalizarScrim(String idScrim) {
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.finalizar();
        repo.guardar(scrim);
        System.out.println("[evento] ScrimFinalizado " + idScrim);
    }

    public void cancelarScrim(String idScrim) {
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.cancelar();
        repo.guardar(scrim);
        System.out.println("[evento] ScrimCancelado " + idScrim);
    }
}


