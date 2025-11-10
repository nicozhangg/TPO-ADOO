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

    /** Compatibilidad: crea scrim con valores por defecto (sin agenda) */
    public void crear(String juego, String emailCreador, int rangoMin, int rangoMax){
        crear(juego, emailCreador, rangoMin, rangoMax,
                2, "2v2", "REGION_DESCONOCIDA", 100, "casual", null, null);
    }

    /** Nuevo: crear con parámetros completos y fechas opcionales */
    public void crear(String juego, String emailCreador, int rangoMin, int rangoMax,
                      int cupo, String formato, String region, int latenciaMaxMs,
                      String modalidad,
                      String inicioStr, String finStr) {
        validarPermisoOrganizer();
        LocalDateTime ini = parsearFechaOpcional(inicioStr);
        LocalDateTime fin = parsearFechaOpcional(finStr);
        var scrim = lifecycleService.crearScrim(juego, emailCreador,
                rangoMin, rangoMax, cupo,
                formato, region, latenciaMaxMs,
                modalidad,
                ini, fin);
        System.out.println("Scrim creado: " + scrim.getId());
    }

    // ================== LISTAR ==================
    public void listar(){ 
        // Permitido para todos los tipos de usuarios
        lifecycleService.listarScrims().forEach(scrim -> System.out.println(formatearResumen(scrim)));
    }

    // ================== LOBBY ===================
    public void unirse(String idScrim, String emailJugador){
        validarPermisoJugadorOOrganizador(emailJugador, "unirte a un scrim");
        lobbyService.unirse(idScrim, emailJugador); // Determina equipo automáticamente
        System.out.println("Jugador unido.");
    }
    
    public void unirseAEquipo(String idScrim, String emailJugador, String nombreEquipo){
        validarPermisoJugadorOOrganizador(emailJugador, "unirte a un equipo");
        lobbyService.unirseAEquipo(idScrim, emailJugador, nombreEquipo);
        System.out.println("Jugador unido al equipo.");
    }
    public void salir(String idScrim, String emailJugador){
        validarPermisoJugadorOOrganizador(emailJugador, "salir del scrim");
        lobbyService.salir(idScrim, emailJugador);
        System.out.println("Salida registrada.");
    }
    public void confirmar(String idScrim, String emailJugador){
        validarPermisoJugadorOOrganizador(emailJugador, "confirmar tu participación");
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
        LocalDateTime ini = parsearFechaObligatoria(inicioStr, "inicio");
        LocalDateTime fin = parsearFechaObligatoria(finStr, "fin");
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
        suplentes.forEach(entry -> System.out.printf(
                "%d. %s (desde %s)%n",
                entry.orden(),
                entry.emailJugador(),
                entry.fechaSolicitud()
        ));
    }

    public String formatearResumen(Scrim scrim) {
        return String.format(
                "%s | %s | %s | %s | Rango %d-%d | Latencia máx %d ms",
                scrim.getId(),
                scrim.getJuego(),
                scrim.getRegion(),
                scrim.getFormato(),
                scrim.getRangoMin(),
                scrim.getRangoMax(),
                scrim.getLatenciaMaxMs()
        );
    }

    // ================== Helpers ==================
    private void validarPermisoJugadorOOrganizador(String emailJugador, String accion) {
        Usuario usuario = requerirUsuarioActual();
        if (usuario instanceof Organizador) {
            return;
        }
        if (usuario instanceof Jugador jugador) {
            if (!jugador.getEmail().equals(emailJugador)) {
                throw new SecurityException("Solo puedes " + accion + " con tu propio email");
            }
            return;
        }
        throw new SecurityException("Operación no permitida para el usuario actual");
    }

    private LocalDateTime parsearFechaOpcional(String fechaStr) {
        ZonedDateTime zoned = ArgentinaTimeZone.parsear(fechaStr);
        return zoned != null ? ArgentinaTimeZone.aLocalDateTime(zoned) : null;
    }

    private LocalDateTime parsearFechaObligatoria(String fechaStr, String campo) {
        LocalDateTime fecha = parsearFechaOpcional(fechaStr);
        if (fecha == null) {
            throw new IllegalArgumentException("Fecha de " + campo + " requerida");
        }
        return fecha;
    }
}
