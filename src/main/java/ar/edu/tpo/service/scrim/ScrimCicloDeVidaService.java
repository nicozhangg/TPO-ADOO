package ar.edu.tpo.service.scrim;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.notification.NotificationService;
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
    private final NotificationService notificaciones;

    public ScrimCicloDeVidaService(ScrimRepository repo, UsuarioService usuarios, NotificationService notificaciones) {
        this.repo = Objects.requireNonNull(repo);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.notificaciones = notificaciones;
    }

    public Scrim crearScrim(String juego, String emailCreador,
                            int rangoMin, int rangoMax, int cupo,
                            String formato, String region, int latenciaMaxMs,
                            String modalidad,
                            LocalDateTime inicio, LocalDateTime fin) {
        String juegoNormalizado = normalizarJuego(juego);
        String formatoNormalizado = textoObligatorio(formato, "Formato");
        String regionNormalizada = textoObligatorio(region, "Región");
        String modalidadNormalizada = textoObligatorio(modalidad, "Modalidad");
        validarLatencia(latenciaMaxMs);
        usuarios.buscar(emailCreador);

        Scrim scrim = new Scrim(
                juegoNormalizado, emailCreador,
                rangoMin, rangoMax, cupo,
                formatoNormalizado, regionNormalizada, latenciaMaxMs,
                modalidadNormalizada
        );

        if (inicio != null && fin != null) {
            scrim.programar(inicio, fin);
        }

        repo.guardar(scrim);
        System.out.println("[evento] ScrimCreado " + scrim.getId() + " (formato=" + formato + ", región=" + region + ", modalidad=" + modalidad + ")");
        notificarCoincidencias(scrim);
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
        Scrim scrim = obtenerScrim(idScrim);
        scrim.programar(inicio, fin);
        repo.guardar(scrim);
        System.out.println("[evento] ScrimProgramado " + idScrim + " " + inicio + "→" + fin);
        notificarProgramacion(scrim);
    }

    public void limpiarAgenda(String idScrim) {
        Scrim scrim = obtenerScrim(idScrim);
        scrim.limpiarAgenda();
        repo.guardar(scrim);
        System.out.println("[evento] ScrimAgendaLimpia " + idScrim);
    }

    public void iniciarScrim(String idScrim) {
        Scrim scrim = obtenerScrim(idScrim);
        scrim.iniciar();
        repo.guardar(scrim);
        System.out.println("[evento] ScrimEnJuego " + idScrim);
        notificarEstado(scrim, "EN_JUEGO");
    }

    public void finalizarScrim(String idScrim) {
        Scrim scrim = obtenerScrim(idScrim);
        scrim.finalizar();
        repo.guardar(scrim);
        System.out.println("[evento] ScrimFinalizado " + idScrim);
        notificarEstado(scrim, "FINALIZADO");
    }

    public void cancelarScrim(String idScrim) {
        Scrim scrim = obtenerScrim(idScrim);
        scrim.cancelar();
        repo.guardar(scrim);
        System.out.println("[evento] ScrimCancelado " + idScrim);
        notificarEstado(scrim, "CANCELADO");
    }

    private void notificarCoincidencias(Scrim scrim) {
        if (notificaciones == null) {
            return;
        }
        for (Usuario usuario : usuarios.listar()) {
            if (!(usuario instanceof Jugador jugador)) {
                continue;
            }
            if (jugador.getAlertasScrim().isEmpty()) {
                continue;
            }
            if (jugador.getLatenciaMs() > scrim.getLatenciaMaxMs()) {
                continue;
            }
            if (!jugador.getRegionNombre().equalsIgnoreCase(scrim.getRegion())) {
                continue;
            }
            int mmrJugador = jugador.getMmr();
            if (mmrJugador < scrim.getRangoMin() || mmrJugador > scrim.getRangoMax()) {
                continue;
            }

            boolean coincide = jugador.getAlertasScrim().stream().anyMatch(alerta -> coincideCon(scrim, jugador, alerta));
            if (coincide) {
                notificaciones.notificarScrimRecomendada(scrim, jugador);
            }
        }
    }

    private boolean coincideCon(Scrim scrim, Jugador jugador, ar.edu.tpo.domain.alerta.ScrimAlerta alerta) {
        if (alerta.getJuego() != null && !alerta.getJuego().isBlank()
                && !scrim.getJuego().equalsIgnoreCase(alerta.getJuego())) {
            return false;
        }
        if (alerta.getRegion() != null && !alerta.getRegion().isBlank()
                && !scrim.getRegion().equalsIgnoreCase(alerta.getRegion())) {
            return false;
        }
        if (alerta.getFormato() != null && !alerta.getFormato().isBlank()
                && !scrim.getFormato().equalsIgnoreCase(alerta.getFormato())) {
            return false;
        }
        if (alerta.getRangoMin() != null && scrim.getRangoMax() < alerta.getRangoMin()) {
            return false;
        }
        if (alerta.getRangoMax() != null && scrim.getRangoMin() > alerta.getRangoMax()) {
            return false;
        }
        if (alerta.getLatenciaMax() != null && scrim.getLatenciaMaxMs() > alerta.getLatenciaMax()) {
            return false;
        }
        // Validación final: el jugador realmente puede participar
        return jugador.getLatenciaMs() <= scrim.getLatenciaMaxMs()
                && jugador.getMmr() >= scrim.getRangoMin()
                && jugador.getMmr() <= scrim.getRangoMax();
    }

    private Scrim obtenerScrim(String idScrim) {
        return repo.buscarPorId(idScrim);
    }

    private String normalizarJuego(String juego) {
        String valor = juego != null ? juego.trim() : "";
        if (!"valorant".equalsIgnoreCase(valor)) {
            throw new IllegalArgumentException("Por ahora solo se permiten scrims de Valorant.");
        }
        return "Valorant";
    }

    private String textoObligatorio(String valor, String nombreCampo) {
        String texto = valor != null ? valor.trim() : "";
        if (texto.isEmpty()) {
            throw new IllegalArgumentException(nombreCampo + " requerido");
        }
        return texto;
    }

    private void validarLatencia(int latenciaMaxMs) {
        if (latenciaMaxMs <= 0) {
            throw new IllegalArgumentException("Latencia máxima debe ser mayor a 0");
        }
    }

    private void notificarProgramacion(Scrim scrim) {
        if (notificaciones != null) {
            notificaciones.notificarScrimProgramado(scrim);
        }
    }

    private void notificarEstado(Scrim scrim, String estado) {
        if (notificaciones != null) {
            notificaciones.notificarScrimEstado(scrim, estado);
        }
    }
}
