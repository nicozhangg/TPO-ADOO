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
        notificarRegistro(usuario);
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
        return List.copyOf(repo.listar());
    }

    public Usuario buscar(String email){
        Usuario usuario = repo.buscar(email);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        return usuario;
    }

    public void actualizar(Usuario usuario) {
        repo.actualizar(usuario);
    }

    public void agregarSancion(String email, String motivo, Duration duracion) {
        Usuario usuario = asegurarUsuarioVigente(email);
        aplicarSancion(usuario, motivo, duracion);
    }

    public void aplicarCooldown(String email, String motivoBase, Duration duracion) {
        Jugador jugador = asegurarJugadorActivo(email, "Solo los jugadores pueden recibir cooldowns.");
        aplicarSancion(jugador, motivoBase + " - Cooldown", duracion);
    }

    public int aplicarStrike(String email, String motivoBase) {
        Jugador jugador = asegurarJugadorActivo(email, "Solo los jugadores pueden recibir strikes.");
        int strikes = jugador.incrementarStrike();
        if (strikes >= 3) {
            jugador.suspenderCuenta();
            aplicarSancion(jugador, "Suspensión de cuenta (" + motivoBase + ")", null);
            return strikes;
        }
        Duration duracion = strikes == 1 ? Duration.ofHours(24) : Duration.ofDays(7);
        aplicarSancion(jugador, "Strike " + strikes + " (" + motivoBase + ")", duracion);
        return strikes;
    }

    public List<SancionActiva> obtenerSancionesActivas(String email) {
        return List.copyOf(buscar(email).getSancionesActivas());
    }

    public List<SancionHistorica> obtenerSancionesHistoricas(String email) {
        return List.copyOf(buscar(email).getSancionesHistoricas());
    }

    public boolean agregarScrimFavorita(String email, String idScrim) {
        Jugador jugador = obtenerJugador(email, "Solo los jugadores pueden guardar scrims favoritas.");
        boolean agregada = jugador.agregarScrimFavorita(idScrim);
        if (agregada) {
            repo.actualizar(jugador);
        }
        return agregada;
    }

    public List<String> obtenerScrimsFavoritas(String email) {
        Jugador jugador = obtenerJugadorOPorDefecto(email);
        return jugador == null ? List.of() : List.copyOf(jugador.getScrimsFavoritas());
    }

    public void agregarAlertaScrim(String email, ScrimAlerta alerta) {
        Jugador jugador = obtenerJugador(email, "Solo los jugadores pueden configurar alertas.");
        jugador.agregarAlertaScrim(alerta);
        repo.actualizar(jugador);
    }

    public ScrimAlerta eliminarAlertaScrim(String email, int indice) {
        Jugador jugador = obtenerJugador(email, "Solo los jugadores pueden eliminar alertas.");
        ScrimAlerta eliminada = jugador.eliminarAlertaScrim(indice);
        repo.actualizar(jugador);
        return eliminada;
    }

    public List<ScrimAlerta> obtenerAlertasScrim(String email) {
        Jugador jugador = obtenerJugadorOPorDefecto(email);
        return jugador == null ? List.of() : List.copyOf(jugador.getAlertasScrim());
    }

    public SancionHistorica levantarSancion(String email, int indice) {
        Usuario usuario = buscar(email);
        SancionHistorica sancion = usuario.levantarSancionPorIndice(indice);
        repo.actualizar(usuario);
        notificarSancionLevantada(usuario, sancion);
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
                expiradas.forEach(hist -> {
                    removidas.add(new SancionRemovida(usuario.getEmail(), hist));
                    notificarSancionLevantada(usuario, hist);
                });
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
        notificarInicioSesion(usuario);
        return usuario;
    }

    private Usuario asegurarUsuarioVigente(String email) {
        Usuario usuario = buscar(email);
        if (usuario.estaSuspendido()) {
            throw new IllegalStateException("La cuenta está suspendida. No se pueden aplicar nuevas sanciones.");
        }
        return usuario;
    }

    private Jugador obtenerJugador(String email, String mensajeError) {
        Usuario usuario = buscar(email);
        if (!(usuario instanceof Jugador jugador)) {
            throw new IllegalArgumentException(mensajeError);
        }
        return jugador;
    }

    private Jugador asegurarJugadorActivo(String email, String mensajeError) {
        Jugador jugador = obtenerJugador(email, mensajeError);
        if (jugador.estaSuspendido()) {
            throw new IllegalStateException("La cuenta ya se encuentra suspendida.");
        }
        return jugador;
    }

    private Jugador obtenerJugadorOPorDefecto(String email) {
        Usuario usuario = repo.buscar(email);
        return usuario instanceof Jugador jugador ? jugador : null;
    }

    private void persistirYNotificarSancion(Usuario usuario, SancionActiva sancion) {
        repo.actualizar(usuario);
        if (notificaciones != null && sancion != null) {
            notificaciones.notificarSancionAplicada(usuario, sancion);
        }
    }

    private void notificarSancionLevantada(Usuario usuario, SancionHistorica sancion) {
        if (notificaciones != null) {
            notificaciones.notificarSancionLevantada(usuario, sancion);
        }
    }

    private void notificarRegistro(Usuario usuario) {
        if (notificaciones != null) {
            notificaciones.notificarRegistro(usuario);
        }
    }

    private void notificarInicioSesion(Usuario usuario) {
        if (notificaciones != null) {
            notificaciones.notificarInicioSesion(usuario);
        }
    }

    private void aplicarSancion(Usuario usuario, String motivo, Duration duracion) {
        SancionActiva sancion = usuario.agregarSancion(motivo, duracion);
        persistirYNotificarSancion(usuario, sancion);
    }

    public record SancionRemovida(String email, SancionHistorica sancion) {}
}
