package ar.edu.tpo.service;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Organizador;
import ar.edu.tpo.domain.SancionActiva;
import ar.edu.tpo.domain.SancionHistorica;
import ar.edu.tpo.domain.Usuario;
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

    public void registrarOrganizador(String email, String password){
        registrar(new Organizador(email, password));
    }

    public void registrarJugador(String email,
                                 String password,
                                 int mmr,
                                 int latenciaMs,
                                 StateRangos rango,
                                 StateRoles rolPreferido,
                                 StateRegion region){
        registrar(new Jugador(email, password, mmr, latenciaMs, rango, rolPreferido, region));
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
        SancionActiva sancion = usuario.agregarSancion(motivo, duracion);
        repo.actualizar(usuario);
        if (notificaciones != null && sancion != null) {
            notificaciones.notificarSancionAplicada(usuario, sancion);
        }
    }

    public List<SancionActiva> obtenerSancionesActivas(String email) {
        return new ArrayList<>(buscar(email).getSancionesActivas());
    }

    public List<SancionHistorica> obtenerSancionesHistoricas(String email) {
        return new ArrayList<>(buscar(email).getSancionesHistoricas());
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
        if (notificaciones != null) {
            notificaciones.notificarInicioSesion(usuario);
        }
        return usuario;
    }

    public record SancionRemovida(String email, SancionHistorica sancion) {}
}
