package ar.edu.tpo.controller;

import ar.edu.tpo.domain.Rol;
import ar.edu.tpo.service.ArgentinaTimeZone;
import ar.edu.tpo.service.ScrimService;
import ar.edu.tpo.service.UsuarioActualPort;
import ar.edu.tpo.service.UsuarioService;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class ScrimController {
    private final ScrimService service;
    private final UsuarioService usuarios;
    private final UsuarioActualPort usuarioActual;

    public ScrimController(ScrimService s, UsuarioService u, UsuarioActualPort usuarioActual){
        this.service = s;
        this.usuarios = u;
        this.usuarioActual = usuarioActual;
    }

    // ================== VALIDACIÓN DE PERMISOS ==================
    private void validarPermiso(Rol rolRequerido) {
        Rol rolActual = usuarioActual.obtenerRolUsuarioActual();
        if (rolActual == null) {
            throw new IllegalStateException("No hay usuario logueado");
        }
        if (rolActual != rolRequerido) {
            throw new SecurityException("Operación no permitida. Se requiere rol: " + rolRequerido);
        }
    }

    private void validarPermisoOrganizer() {
        validarPermiso(Rol.ORGANIZER);
    }

    // ================== CREAR ==================

    /** Compat (cupo=2, sin fechas) */
    public void crear(String juego, String emailCreador, String emailRival, int rangoMin, int rangoMax){
        validarPermisoOrganizer();
        var s = service.crearScrim(juego, emailCreador, emailRival, rangoMin, rangoMax);
        System.out.println("Scrim creado: " + s.getId());
    }

    /** Nuevo: crear con cupo y fechas opcionales */
    public void crear(String juego, String emailCreador, String emailRival, int rangoMin, int rangoMax,
                      int cupo, String inicioStr, String finStr) {
        validarPermisoOrganizer();
        // Parsear fechas interpretándolas como hora de Argentina
        ZonedDateTime iniZoned = ArgentinaTimeZone.parsear(inicioStr);
        ZonedDateTime finZoned = ArgentinaTimeZone.parsear(finStr);
        LocalDateTime ini = (iniZoned != null) ? ArgentinaTimeZone.aLocalDateTime(iniZoned) : null;
        LocalDateTime fin = (finZoned != null) ? ArgentinaTimeZone.aLocalDateTime(finZoned) : null;
        var s = service.crearScrim(juego, emailCreador, emailRival, rangoMin, rangoMax, cupo, ini, fin);
        System.out.println("Scrim creado: " + s.getId());
    }

    // ================== LISTAR ==================
    public void listar(){ 
        // Permitido para todos los usuarios (PLAYER y ORGANIZER)
        service.listarScrims().forEach(System.out::println); 
    }

    // ================== LOBBY ===================
    public void unirse(String idScrim, String emailJugador){
        // PLAYER solo puede unirse si es el mismo usuario logueado
        Rol rolActual = usuarioActual.obtenerRolUsuarioActual();
        if (rolActual == null) {
            throw new IllegalStateException("No hay usuario logueado");
        }
        
        if (rolActual == Rol.PLAYER) {
            String emailActual = usuarioActual.obtenerEmailUsuarioActual();
            if (emailActual == null || !emailActual.equals(emailJugador)) {
                throw new SecurityException("Solo puedes unirte a un scrim con tu propio email");
            }
        } else {
            validarPermisoOrganizer();
        }
        
        usuarios.buscar(emailJugador); // valida existencia
        service.unirse(idScrim, emailJugador);
        System.out.println("Jugador unido.");
    }
    public void salir(String idScrim, String emailJugador){
        validarPermisoOrganizer();
        service.salir(idScrim, emailJugador);
        System.out.println("Jugador quitado.");
    }
    public void confirmar(String idScrim, String emailJugador){
        validarPermisoOrganizer();
        usuarios.buscar(emailJugador);
        service.confirmar(idScrim, emailJugador);
        System.out.println("Jugador confirmado.");
    }

    // ================== AGENDA/FLUJO ==================
    public void programar(String idScrim, String inicioStr, String finStr){
        validarPermisoOrganizer();
        // Parsear fechas interpretándolas como hora de Argentina
        ZonedDateTime iniZoned = ArgentinaTimeZone.parsear(inicioStr);
        ZonedDateTime finZoned = ArgentinaTimeZone.parsear(finStr);
        LocalDateTime ini = ArgentinaTimeZone.aLocalDateTime(iniZoned);
        LocalDateTime fin = ArgentinaTimeZone.aLocalDateTime(finZoned);
        service.programar(idScrim, ini, fin);
        System.out.println("Scrim programado/reprogramado.");
    }
    public void limpiarAgenda(String idScrim){
        validarPermisoOrganizer();
        service.limpiarAgenda(idScrim);
        System.out.println("Agenda limpiada.");
    }
    public void iniciar(String idScrim){
        validarPermisoOrganizer();
        service.iniciarScrim(idScrim);
        System.out.println("Scrim en juego.");
    }
    public void finalizar(String idScrim){
        validarPermisoOrganizer();
        service.finalizarScrim(idScrim);
        System.out.println("Scrim finalizado.");
    }
    public void cancelar(String idScrim){
        validarPermisoOrganizer();
        service.cancelarScrim(idScrim);
        System.out.println("Scrim cancelado.");
    }

    // ================== RESULTADOS / SUPLENTES ==================
    public void cargarResultado(String idScrim, String emailJugador, int kills, int assists, int deaths, double rating){
        validarPermisoOrganizer();
        usuarios.buscar(emailJugador);
        service.cargarResultado(idScrim, emailJugador, kills, assists, deaths, rating);
        System.out.println("Resultado cargado.");
    }
    public void agregarSuplente(String idScrim, String emailJugador){
        validarPermisoOrganizer();
        usuarios.buscar(emailJugador);
        service.agregarSuplente(idScrim, emailJugador);
        System.out.println("Suplente agregado.");
    }
}
