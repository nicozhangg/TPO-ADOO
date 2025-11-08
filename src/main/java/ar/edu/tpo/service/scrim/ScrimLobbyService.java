package ar.edu.tpo.service.scrim;

import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.repository.ScrimRepository;
import ar.edu.tpo.service.UsuarioService;

import java.util.Objects;

/**
 * Servicio dedicado a las operaciones de lobby: altas/bajas y confirmaciones.
 */
public class ScrimLobbyService {

    private final ScrimRepository repo;
    private final UsuarioService usuarios;

    public ScrimLobbyService(ScrimRepository repo, UsuarioService usuarios) {
        this.repo = Objects.requireNonNull(repo);
        this.usuarios = Objects.requireNonNull(usuarios);
    }

    public void unirse(String idScrim, String emailJugador) {
        usuarios.buscar(emailJugador);
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.agregarJugador(emailJugador);
        repo.guardar(scrim);
        System.out.println("[evento] JugadorUnido scrim=" + idScrim + " jugador=" + emailJugador);
    }

    public void unirseAEquipo(String idScrim, String emailJugador, String nombreEquipo) {
        usuarios.buscar(emailJugador);
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.agregarJugador(emailJugador, nombreEquipo);
        repo.guardar(scrim);
        System.out.println("[evento] JugadorUnido scrim=" + idScrim + " jugador=" + emailJugador + " equipo=" + nombreEquipo);
    }

    public void salir(String idScrim, String emailJugador) {
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.quitarJugador(emailJugador);
        repo.guardar(scrim);
        System.out.println("[evento] JugadorQuitado scrim=" + idScrim + " jugador=" + emailJugador);
    }

    public void confirmarJugador(String idScrim, String emailJugador) {
        usuarios.buscar(emailJugador);
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.confirmarJugador(emailJugador);
        repo.guardar(scrim);
        System.out.println("[evento] EquipoConfirmado scrim=" + idScrim + " jugador=" + emailJugador);
    }

    public void confirmarEquipo(String idScrim, String nombreEquipo) {
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.confirmarEquipo(nombreEquipo);
        repo.guardar(scrim);
        System.out.println("[evento] EquipoConfirmado scrim=" + idScrim + " equipo=" + nombreEquipo);
    }

    public void agregarSuplente(String idScrim, String emailJugador) {
        usuarios.buscar(emailJugador);
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.agregarAListaEspera(emailJugador);
        repo.guardar(scrim);
        System.out.println("[evento] SuplenteAgregado scrim=" + idScrim + " jugador=" + emailJugador);
    }
}


