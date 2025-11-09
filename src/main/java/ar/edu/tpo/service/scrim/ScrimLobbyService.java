package ar.edu.tpo.service.scrim;

import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.estado.BuscandoJugadoresState;
import ar.edu.tpo.domain.estado.ConfirmadoState;
import ar.edu.tpo.domain.estado.LobbyArmadoState;
import ar.edu.tpo.repository.ScrimRepository;
import ar.edu.tpo.service.ConductaService;
import ar.edu.tpo.service.UsuarioService;

import java.util.Objects;

/**
 * Servicio dedicado a las operaciones de lobby: altas/bajas y confirmaciones.
 */
public class ScrimLobbyService {

    private final ScrimRepository repo;
    private final UsuarioService usuarios;
    private final ConductaService conductaService;

    public ScrimLobbyService(ScrimRepository repo, UsuarioService usuarios, ConductaService conductaService) {
        this.repo = Objects.requireNonNull(repo);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.conductaService = Objects.requireNonNull(conductaService);
    }

    public void unirse(String idScrim, String emailJugador) {
        var usuario = usuarios.buscar(emailJugador);
        if (usuario.tieneSancionesActivas()) {
            String motivos = String.join("@", usuario.getSancionesActivas());
            throw new SecurityException("No puedes unirte a scrims: sanciones activas " + motivos);
        }
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.agregarJugador(emailJugador);
        repo.guardar(scrim);
        System.out.println("[evento] JugadorUnido scrim=" + idScrim + " jugador=" + emailJugador);
    }

    public void unirseAEquipo(String idScrim, String emailJugador, String nombreEquipo) {
        var usuario = usuarios.buscar(emailJugador);
        if (usuario.tieneSancionesActivas()) {
            String motivos = String.join("@", usuario.getSancionesActivas());
            throw new SecurityException("No puedes unirte a scrims: sanciones activas " + motivos);
        }
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.agregarJugador(emailJugador, nombreEquipo);
        repo.guardar(scrim);
        System.out.println("[evento] JugadorUnido scrim=" + idScrim + " jugador=" + emailJugador + " equipo=" + nombreEquipo);
    }

    public void salir(String idScrim, String emailJugador) {
        Scrim scrim = repo.buscarPorId(idScrim);

        boolean estaEnScrim = scrim.getEquipo1().contieneJugador(emailJugador) || scrim.getEquipo2().contieneJugador(emailJugador);
        if (!estaEnScrim) {
            throw new IllegalArgumentException("El jugador no participa de la scrim");
        }

        var estado = scrim.getEstado();
        boolean esBuscando = estado == BuscandoJugadoresState.INSTANCIA;
        boolean esLobby = estado == LobbyArmadoState.INSTANCIA;
        boolean esConfirmado = estado == ConfirmadoState.INSTANCIA;

        if (!esBuscando && !esLobby && !esConfirmado) {
            throw new IllegalStateException("No se puede salir del scrim en estado " + estado.getNombre());
        }

        if (esLobby || esConfirmado) {
            conductaService.registrarAbandono(emailJugador);
            System.out.println("[sancion] MotivoAbandono scrim=" + idScrim + " jugador=" + emailJugador);
        }

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

