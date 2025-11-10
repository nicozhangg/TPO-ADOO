package ar.edu.tpo.service;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Organizador;
import ar.edu.tpo.domain.SancionActiva;
import ar.edu.tpo.domain.SancionHistorica;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.domain.alerta.ScrimAlerta;
import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.regiones.StateRegion;
import ar.edu.tpo.domain.roles.StateRoles;
import ar.edu.tpo.repository.UsuarioRepository;
import ar.edu.tpo.notification.NotificationService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class UsuarioService {
    private final UsuarioRepository repo;
    private final NotificationService notificaciones;

    public UsuarioService(UsuarioRepository repo){
        this(repo, null);
    }

    public UsuarioService(UsuarioRepository repo, NotificationService notificaciones){
        this.repo = repo;
        this.notificaciones = notificaciones;
    }

    public void registrar(Usuario usuario){
        repo.guardar(usuario);
        if (notificaciones != null) {
            notificaciones.notificarRegistro(usuario);
        }
    }

    public void registrarOrganizador(String nombre, String email, String password){
        registrar(new Organizador(nombre, email, password));
    }

    public void registrarJugador(String nombre,
                                 String email,
                                 String password,
                                 int mmr,
                                 int latenciaMs,
                                 StateRangos rango,
                                 StateRoles rolPreferido,
                                 StateRegion region){
        registrar(new Jugador(nombre, email, password, mmr, latenciaMs, rango, rolPreferido, region));
    }

    public List<Usuario> listar(){
        return new ArrayList<>(repo.listar());
    }

    public Usuario buscar(String email){
        Usuario u = repo.buscar(email);
        if (u == null) throw new IllegalArgumentException("Usuario no encontrado");
        return u;
    }

    public void actualizar(Usuario usuario) {
        repo.actualizar(usuario);
    }

    public void agregarSancion(String email, String motivo, Duration duracion) {
        Usuario usuario = buscar(email);
        if (usuario.estaSuspendido()) {
            throw new IllegalStateException("La cuenta está suspendida. No se pueden aplicar nuevas sanciones.");
        }
        SancionActiva sancion = usuario.agregarSancion(motivo, duracion);
        repo.actualizar(usuario);
        if (notificaciones != null && sancion != null) {
            notificaciones.notificarSancionAplicada(usuario, sancion);
        }
    }

    public void aplicarCooldown(String email, String motivoBase, Duration duracion) {
        Usuario usuario = buscar(email);
        if (!(usuario instanceof Jugador)) {
            throw new IllegalArgumentException("Solo los jugadores pueden recibir cooldowns.");
        }
        if (usuario.estaSuspendido()) {
            throw new IllegalStateException("La cuenta está suspendida. No se pueden aplicar nuevas sanciones.");
        }
        SancionActiva sancion = usuario.agregarSancion(motivoBase + " - Cooldown", duracion);
        repo.actualizar(usuario);
        if (notificaciones != null && sancion != null) {
            notificaciones.notificarSancionAplicada(usuario, sancion);
        }
    }

    public int aplicarStrike(String email, String motivoBase) {
        Usuario usuario = buscar(email);
        if (!(usuario instanceof Jugador)) {
            throw new IllegalArgumentException("Solo los jugadores pueden recibir strikes.");
        }
        if (usuario.estaSuspendido()) {
            throw new IllegalStateException("La cuenta ya se encuentra suspendida.");
        }
        int strikes = usuario.incrementarStrike();
        if (strikes >= 3) {
            usuario.suspenderCuenta();
            SancionActiva suspension = usuario.agregarSancion("Suspensión de cuenta (" + motivoBase + ")", null);
            repo.actualizar(usuario);
            if (notificaciones != null && suspension != null) {
                notificaciones.notificarSancionAplicada(usuario, suspension);
            }
            return strikes;
        }
        Duration duracion = strikes == 1 ? Duration.ofHours(24) : Duration.ofDays(7);
        SancionActiva sancion = usuario.agregarSancion("Strike " + strikes + " (" + motivoBase + ")", duracion);
        repo.actualizar(usuario);
        if (notificaciones != null && sancion != null) {
            notificaciones.notificarSancionAplicada(usuario, sancion);
        }
        return strikes;
    }

    public List<SancionActiva> obtenerSancionesActivas(String email) {
        return new ArrayList<>(buscar(email).getSancionesActivas());
    }

    public List<SancionHistorica> obtenerSancionesHistoricas(String email) {
        return new ArrayList<>(buscar(email).getSancionesHistoricas());
    }

    public boolean agregarScrimFavorita(String email, String idScrim) {
        Usuario usuario = buscar(email);
        if (!(usuario instanceof Jugador jugador)) {
            throw new IllegalArgumentException("Solo los jugadores pueden guardar scrims favoritas.");
        }
        boolean agregada = jugador.agregarScrimFavorita(idScrim);
        if (agregada) {
            repo.actualizar(jugador);
        }
        return agregada;
    }

    public List<String> obtenerScrimsFavoritas(String email) {
        Usuario usuario = buscar(email);
        if (!(usuario instanceof Jugador jugador)) {
            return List.of();
        }
        return new ArrayList<>(jugador.getScrimsFavoritas());
    }

    public void agregarAlertaScrim(String email, ScrimAlerta alerta) {
        Usuario usuario = buscar(email);
        if (!(usuario instanceof Jugador jugador)) {
            throw new IllegalArgumentException("Solo los jugadores pueden configurar alertas.");
        }
        jugador.agregarAlertaScrim(alerta);
        repo.actualizar(jugador);
    }

    public ScrimAlerta eliminarAlertaScrim(String email, int indice) {
        Usuario usuario = buscar(email);
        if (!(usuario instanceof Jugador jugador)) {
            throw new IllegalArgumentException("Solo los jugadores pueden eliminar alertas.");
        }
        ScrimAlerta eliminada = jugador.eliminarAlertaScrim(indice);
        repo.actualizar(jugador);
        return eliminada;
    }

    public List<ScrimAlerta> obtenerAlertasScrim(String email) {
        Usuario usuario = buscar(email);
        if (!(usuario instanceof Jugador jugador)) {
            return List.of();
        }
        return new ArrayList<>(jugador.getAlertasScrim());
    }

    public SancionHistorica levantarSancion(String email, int indice) {
        Usuario usuario = buscar(email);
        SancionHistorica sancion = usuario.levantarSancionPorIndice(indice);
        repo.actualizar(usuario);
        if (notificaciones != null) {
            notificaciones.notificarSancionLevantada(usuario, sancion);
        }
        return sancion;
    }

    public List<SancionRemovida> limpiarSancionesVencidas() {
        List<SancionRemovida> removidas = new ArrayList<>();
        for (Usuario usuario : repo.listar()) {
            List<SancionActiva> sanciones = usuario.getSancionesActivasSinDepurar();
            if (sanciones.isEmpty()) {
                continue;
            }
            List<SancionHistorica> expiradas = usuario.removerSancionesVencidas();
            if (!expiradas.isEmpty()) {
                repo.actualizar(usuario);
                for (SancionHistorica hist : expiradas) {
                    removidas.add(new SancionRemovida(usuario.getEmail(), hist));
                    if (notificaciones != null) {
                        notificaciones.notificarSancionLevantada(usuario, hist);
                    }
                }
            }
        }
        return removidas;
    }

    public Usuario login(String email, String password){
        Usuario usuario = repo.buscar(email);
        if (usuario == null || !usuario.getPasswordHash().equals(password)) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        if (usuario.estaSuspendido()) {
            throw new IllegalArgumentException("Cuenta suspendida. Contacta al soporte.");
        }
        if (notificaciones != null) {
            notificaciones.notificarInicioSesion(usuario);
        }
        return usuario;
    }

    public record SancionRemovida(String email, SancionHistorica sancion) {}
}
