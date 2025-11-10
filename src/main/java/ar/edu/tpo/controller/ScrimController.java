package ar.edu.tpo.controller;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Organizador;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.service.ArgentinaTimeZone;
import ar.edu.tpo.service.UsuarioActualPort;
import ar.edu.tpo.service.scrim.ScrimCicloDeVidaService;
import ar.edu.tpo.service.scrim.ScrimLobbyService;
import ar.edu.tpo.service.scrim.ScrimStatsService;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class ScrimController {
    private final ScrimCicloDeVidaService lifecycleService;
    private final ScrimLobbyService lobbyService;
    private final ScrimStatsService statsService;
    private final UsuarioActualPort usuarioActual;

    public ScrimController(ScrimCicloDeVidaService lifecycleService,
                           ScrimLobbyService lobbyService,
                           ScrimStatsService statsService,
                           UsuarioActualPort usuarioActual){
        this.lifecycleService = lifecycleService;
        this.lobbyService = lobbyService;
        this.statsService = statsService;
        this.usuarioActual = usuarioActual;
    }

    // ================== VALIDACIÓN DE PERMISOS ==================
    private Usuario requerirUsuarioActual() {
        Usuario usuario = usuarioActual.obtenerUsuarioActual();
        if (usuario == null) {
            throw new IllegalStateException("No hay usuario logueado");
        }
        return usuario;
    }

    private void validarPermisoOrganizer() {
        Usuario usuario = requerirUsuarioActual();
        if (!(usuario instanceof Organizador)) {
            throw new SecurityException("Operación no permitida. Se requiere usuario organizador");
        }
    }

    // ================== CREAR ==================

    /** Compat (cupo=2, sin fechas, valores por defecto) */
    public void crear(String juego, String emailCreador, int rangoMin, int rangoMax){
        validarPermisoOrganizer();
        var s = lifecycleService.crearScrim(juego, emailCreador, rangoMin, rangoMax,
                2, "2v2", "REGION_DESCONOCIDA", 100, "casual", null, null);
        System.out.println("Scrim creado: " + s.getId());
    }

    /** Nuevo: crear con parámetros completos y fechas opcionales */
    public void crear(String juego, String emailCreador, int rangoMin, int rangoMax,
                      int cupo, String formato, String region, int latenciaMaxMs,
                      String modalidad,
                      String inicioStr, String finStr) {
        validarPermisoOrganizer();
        // Parsear fechas interpretándolas como hora de Argentina
        ZonedDateTime iniZoned = ArgentinaTimeZone.parsear(inicioStr);
        ZonedDateTime finZoned = ArgentinaTimeZone.parsear(finStr);
        LocalDateTime ini = (iniZoned != null) ? ArgentinaTimeZone.aLocalDateTime(iniZoned) : null;
        LocalDateTime fin = (finZoned != null) ? ArgentinaTimeZone.aLocalDateTime(finZoned) : null;
        var s = lifecycleService.crearScrim(juego, emailCreador,
                rangoMin, rangoMax, cupo,
                formato, region, latenciaMaxMs,
                modalidad,
                ini, fin);
        System.out.println("Scrim creado: " + s.getId());
    }

    // ================== LISTAR ==================
    public void listar(){ 
        // Permitido para todos los tipos de usuarios
        lifecycleService.listarScrims().forEach(scrim -> System.out.println(formatearResumen(scrim)));
    }

    // ================== LOBBY ===================
    public void unirse(String idScrim, String emailJugador){
        Usuario usuario = requerirUsuarioActual();
        if (usuario instanceof Jugador jugador) {
            if (!jugador.getEmail().equals(emailJugador)) {
                throw new SecurityException("Solo puedes unirte a un scrim con tu propio email");
            }
        } else if (!(usuario instanceof Organizador)) {
            throw new SecurityException("Operación no permitida para el usuario actual");
        }

        lobbyService.unirse(idScrim, emailJugador); // Determina equipo automáticamente
        System.out.println("Jugador unido.");
    }
    
    public void unirseAEquipo(String idScrim, String emailJugador, String nombreEquipo){
        Usuario usuario = requerirUsuarioActual();
        if (usuario instanceof Jugador jugador) {
            if (!jugador.getEmail().equals(emailJugador)) {
                throw new SecurityException("Solo puedes unirte a un scrim con tu propio email");
            }
        } else if (!(usuario instanceof Organizador)) {
            throw new SecurityException("Operación no permitida para el usuario actual");
        }

        lobbyService.unirseAEquipo(idScrim, emailJugador, nombreEquipo);
        System.out.println("Jugador unido al equipo.");
    }
    public void salir(String idScrim, String emailJugador){
        Usuario usuario = requerirUsuarioActual();
        if (usuario instanceof Jugador jugador) {
            if (!jugador.getEmail().equals(emailJugador)) {
                throw new SecurityException("Solo puedes salir con tu propio email");
            }
        } else if (!(usuario instanceof Organizador)) {
            throw new SecurityException("Operación no permitida para el usuario actual");
        }

        lobbyService.salir(idScrim, emailJugador);
        System.out.println("Salida registrada.");
    }
    public void confirmar(String idScrim, String emailJugador){
        Usuario usuario = requerirUsuarioActual();
        if (usuario instanceof Jugador jugador) {
            if (!jugador.getEmail().equals(emailJugador)) {
                throw new SecurityException("Solo puedes confirmar tu propia participación");
            }
        } else if (!(usuario instanceof Organizador)) {
            throw new SecurityException("Operación no permitida para el usuario actual");
        }

        lobbyService.confirmarJugador(idScrim, emailJugador);
        System.out.println("Confirmación registrada.");
    }
    
    public void confirmarEquipo(String idScrim, String nombreEquipo){
        validarPermisoOrganizer();
        lobbyService.confirmarEquipo(idScrim, nombreEquipo);
        System.out.println("Equipo confirmado.");
    }

    // ================== AGENDA/FLUJO ==================
    public void programar(String idScrim, String inicioStr, String finStr){
        validarPermisoOrganizer();
        // Parsear fechas interpretándolas como hora de Argentina
        ZonedDateTime iniZoned = ArgentinaTimeZone.parsear(inicioStr);
        ZonedDateTime finZoned = ArgentinaTimeZone.parsear(finStr);
        LocalDateTime ini = ArgentinaTimeZone.aLocalDateTime(iniZoned);
        LocalDateTime fin = ArgentinaTimeZone.aLocalDateTime(finZoned);
        lifecycleService.programar(idScrim, ini, fin);
        System.out.println("Scrim programado/reprogramado.");
    }
    public void limpiarAgenda(String idScrim){
        validarPermisoOrganizer();
        lifecycleService.limpiarAgenda(idScrim);
        System.out.println("Agenda limpiada.");
    }
    public void iniciar(String idScrim){
        validarPermisoOrganizer();
        lifecycleService.iniciarScrim(idScrim);
        System.out.println("Scrim en juego.");
    }
    public void finalizar(String idScrim){
        validarPermisoOrganizer();
        lifecycleService.finalizarScrim(idScrim);
        System.out.println("Scrim finalizado.");
    }
    public void cancelar(String idScrim){
        validarPermisoOrganizer();
        lifecycleService.cancelarScrim(idScrim);
        System.out.println("Scrim cancelado.");
    }

    // ================== RESULTADOS / SUPLENTES ==================
    public void cargarResultado(String idScrim, String emailJugador, int kills, int assists, int deaths, double rating){
        validarPermisoOrganizer();
        statsService.cargarResultado(idScrim, emailJugador, kills, assists, deaths, rating);
        System.out.println("Resultado cargado.");
    }
    public Scrim buscar(String idScrim) {
        return lifecycleService.buscar(idScrim);
    }

    public void mostrarSuplentes(String idScrim) {
        validarPermisoOrganizer();
        Scrim scrim = lifecycleService.buscar(idScrim);
        var suplentes = scrim.getListaEspera();
        if (suplentes.isEmpty()) {
            System.out.println("No hay suplentes registrados para este scrim.");
            return;
        }
        System.out.println("\n=== LISTA DE SUPLENTES (" + scrim.getId() + ") ===");
        suplentes.forEach(entry -> System.out.println(
                "%d. %s (desde %s)".formatted(
                        entry.orden(),
                        entry.emailJugador(),
                        entry.fechaSolicitud()
                )));
    }

    public String formatearResumen(Scrim scrim) {
        return "%s | %s | %s | %s | Rango %d-%d | Latencia máx %d ms"
                .formatted(
                        scrim.getId(),
                        scrim.getJuego(),
                        scrim.getRegion(),
                        scrim.getFormato(),
                        scrim.getRangoMin(),
                        scrim.getRangoMax(),
                        scrim.getLatenciaMaxMs()
                );
    }
}
