package ar.edu.tpo.service.scrim;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.SancionActiva;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.estado.BuscandoJugadoresState;
import ar.edu.tpo.domain.estado.ConfirmadoState;
import ar.edu.tpo.domain.estado.LobbyArmadoState;
import ar.edu.tpo.repository.ScrimRepository;
import ar.edu.tpo.service.ConductaService;
import ar.edu.tpo.service.UsuarioService;
import ar.edu.tpo.notification.NotificationService;

import java.util.Objects;

/**
 * Servicio dedicado a las operaciones de lobby: altas/bajas y confirmaciones.
 */
public class ScrimLobbyService {

    private final ScrimRepository repo;
    private final UsuarioService usuarios;
    private final ConductaService conductaService;
    private final NotificationService notificaciones;

    public ScrimLobbyService(ScrimRepository repo, UsuarioService usuarios, ConductaService conductaService, NotificationService notificaciones) {
        this.repo = Objects.requireNonNull(repo);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.conductaService = Objects.requireNonNull(conductaService);
        this.notificaciones = notificaciones;
    }

    public void unirse(String idScrim, String emailJugador) {
        var usuario = usuarios.buscar(emailJugador);
        Scrim scrim = repo.buscarPorId(idScrim);
        validarPuedeUnirse(usuario, scrim);
        if (!scrim.hayCupoDisponible()) {
            registrarSuplente(scrim, emailJugador);
            return;
        }

        scrim.quitarDeListaEspera(emailJugador);
        scrim.agregarJugador(emailJugador);
        repo.guardar(scrim);
        System.out.println("[evento] JugadorUnido scrim=" + idScrim + " jugador=" + emailJugador);
        notificarUnion(scrim, emailJugador);
    }

    public void unirseAEquipo(String idScrim, String emailJugador, String nombreEquipo) {
        var usuario = usuarios.buscar(emailJugador);
        Scrim scrim = repo.buscarPorId(idScrim);
        validarPuedeUnirse(usuario, scrim);
        if (!scrim.hayLugarEnEquipo(nombreEquipo)) {
            if (scrim.hayCupoDisponible()) {
                System.out.println("El equipo seleccionado ya está completo. Probá con el otro equipo.");
                return;
            }
            registrarSuplente(scrim, emailJugador);
            return;
        }

        scrim.quitarDeListaEspera(emailJugador);
        scrim.agregarJugador(emailJugador, nombreEquipo);
        repo.guardar(scrim);
        System.out.println("[evento] JugadorUnido scrim=" + idScrim + " jugador=" + emailJugador + " equipo=" + nombreEquipo);
        notificarUnion(scrim, emailJugador);
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
        notificarCupoLiberado(scrim);
    }

    public void confirmarJugador(String idScrim, String emailJugador) {
        usuarios.buscar(emailJugador);
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.confirmarJugador(emailJugador);
        boolean todosConfirmados = scrim.ambosEquiposConfirmados();
        repo.guardar(scrim);
        System.out.println("[evento] EquipoConfirmado scrim=" + idScrim + " jugador=" + emailJugador);
        if (notificaciones != null && todosConfirmados && scrim.getEstado() == ConfirmadoState.INSTANCIA) {
            notificaciones.notificarScrimEstado(scrim, scrim.getEstado().getNombre());
        }
    }

    public void confirmarEquipo(String idScrim, String nombreEquipo) {
        Scrim scrim = repo.buscarPorId(idScrim);
        scrim.confirmarEquipo(nombreEquipo);
        boolean todosConfirmados = scrim.ambosEquiposConfirmados();
        repo.guardar(scrim);
        System.out.println("[evento] EquipoConfirmado scrim=" + idScrim + " equipo=" + nombreEquipo);
        if (notificaciones != null && todosConfirmados && scrim.getEstado() == ConfirmadoState.INSTANCIA) {
            notificaciones.notificarScrimEstado(scrim, scrim.getEstado().getNombre());
        }
    }

    private void validarPuedeUnirse(Usuario usuario, Scrim scrim) {
        if (usuario.tieneSancionesActivas()) {
            String motivos = usuario.getSancionesActivas()
                    .stream()
                    .map(SancionActiva::toString)
                    .reduce((a, b) -> a + " @ " + b)
                    .orElse("Desconocido");
            throw new SecurityException("No puedes unirte a scrims: sanciones activas " + motivos);
        }

        if (!(usuario instanceof Jugador jugador)) {
            throw new SecurityException("Solo los jugadores pueden unirse a scrims");
        }

        if (scrim.getEquipo1().contieneJugador(jugador.getEmail()) || scrim.getEquipo2().contieneJugador(jugador.getEmail())) {
            String equipoActual = scrim.getEquipo1().contieneJugador(jugador.getEmail())
                    ? scrim.getEquipo1().getNombre()
                    : scrim.getEquipo2().getNombre();
            throw new IllegalStateException("Ya formas parte de esta scrim en " + equipoActual + ".");
        }

        if (jugador.getLatenciaMs() > scrim.getLatenciaMaxMs()) {
            throw new IllegalStateException("Tu latencia (" + jugador.getLatenciaMs() + "ms) supera el máximo permitido (" + scrim.getLatenciaMaxMs() + "ms)");
        }

        if (!jugador.getRegionNombre().equalsIgnoreCase(scrim.getRegion())) {
            throw new IllegalStateException("Tu región (" + jugador.getRegionNombre() + ") no coincide con la región de la scrim (" + scrim.getRegion() + ")");
        }

        int mmr = jugador.getMmr();
        if (mmr < scrim.getRangoMin() || mmr > scrim.getRangoMax()) {
            throw new IllegalStateException("Tu rango (" + nombreRango(mmr) + ") está fuera del rango permitido (" +
                    nombreRango(scrim.getRangoMin()) + " - " + nombreRango(scrim.getRangoMax()) + ")");
        }
    }

    private String nombreRango(int puntos) {
        return StateRangos.disponibles().stream()
                .filter(r -> puntos >= r.getMinimo() && puntos <= r.getMaximo())
                .map(StateRangos::getNombre)
                .findFirst()
                .orElse(puntos + " MMR");
    }

    private void registrarSuplente(Scrim scrim, String emailJugador) {
        boolean agregado = scrim.agregarAListaEspera(emailJugador);
        repo.guardar(scrim);
        if (agregado) {
            System.out.println("[evento] SuplenteAgregado scrim=" + scrim.getId() + " jugador=" + emailJugador);
            if (notificaciones != null) {
                notificaciones.notificarIngresoListaEspera(scrim, emailJugador);
            }
        } else {
            System.out.println("[evento] SuplenteExistente scrim=" + scrim.getId() + " jugador=" + emailJugador);
        }
        System.out.println("El cupo está completo. Quedaste en la lista de suplentes. Te avisaremos si se libera un lugar.");
    }

    private void notificarUnion(Scrim scrim, String emailJugador) {
        if (notificaciones != null) {
            notificaciones.notificarUnionScrim(scrim, emailJugador);
            if (scrim.getEstado() == LobbyArmadoState.INSTANCIA || scrim.getEstado() == ConfirmadoState.INSTANCIA) {
                notificaciones.notificarScrimEstado(scrim, scrim.getEstado().getNombre());
            }
        }
    }

    private void notificarCupoLiberado(Scrim scrim) {
        if (notificaciones != null && !scrim.getListaEspera().isEmpty()) {
            notificaciones.notificarCupoDisponible(scrim);
        }
    }
}

