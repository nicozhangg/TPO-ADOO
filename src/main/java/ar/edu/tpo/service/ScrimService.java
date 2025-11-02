package ar.edu.tpo.service;

import ar.edu.tpo.domain.*;
import ar.edu.tpo.repository.ScrimRepository;
import ar.edu.tpo.service.estrategias.EstrategiaEmparejamiento;

import java.time.LocalDateTime;
import java.util.List;

public class ScrimService {
    private final ScrimRepository repo;
    private final UsuarioService usuarios;
    private EstrategiaEmparejamiento estrategia;
    private final ConductaService conducta;

    public ScrimService(ScrimRepository repo, UsuarioService usuarios,
                        EstrategiaEmparejamiento estrategia, ConductaService conducta){
        this.repo = repo;
        this.usuarios = usuarios;
        this.estrategia = estrategia;
        this.conducta = conducta;
    }

    public void cambiarEstrategia(EstrategiaEmparejamiento e){ this.estrategia = e; }

    // =========================================================
    // ================  CREACIÓN DEL SCRIM  ===================
    // =========================================================

    /** Crear con CUP0 y fechas opcionales (recomendado para el nuevo modelo) */
    public Scrim crearScrim(String juego, String emailCreador, String emailRival,
                            int rangoMin, int rangoMax, int cupo,
                            LocalDateTime inicio, LocalDateTime fin) {
        usuarios.buscar(emailCreador);
        usuarios.buscar(emailRival);
        Scrim s = new Scrim(juego, emailCreador, emailRival, rangoMin, rangoMax, cupo);
        if (inicio != null && fin != null) s.programar(inicio, fin);
        repo.guardar(s);
        System.out.println("[evento] ScrimCreado " + s.getId());
        return s;
    }

    /** Compat: crear SIN fechas y con cupo por defecto (=2) */
    public Scrim crearScrim(String juego, String emailCreador, String emailRival,
                            int rangoMin, int rangoMax){
        return crearScrim(juego, emailCreador, emailRival, rangoMin, rangoMax, 2, null, null);
    }

    // =========================================================
    // =====================  QUERIES  =========================
    // =========================================================
    public List<Scrim> listarScrims(){ return repo.listar(); }

    public Scrim buscar(String id){ return repo.buscarPorId(id); }

    // =========================================================
    // ==============  LOBBY: JUGADORES/CUPOS  =================
    // =========================================================
    public void unirse(String idScrim, String emailJugador){
        usuarios.buscar(emailJugador);
        Scrim s = repo.buscarPorId(idScrim);
        s.agregarJugador(emailJugador);
        repo.guardar(s);
        System.out.println("[evento] JugadorUnido scrim=" + idScrim + " jugador=" + emailJugador);
    }

    public void salir(String idScrim, String emailJugador){
        Scrim s = repo.buscarPorId(idScrim);
        s.quitarJugador(emailJugador);
        repo.guardar(s);
        System.out.println("[evento] JugadorQuitado scrim=" + idScrim + " jugador=" + emailJugador);
    }

    public void confirmar(String idScrim, String emailJugador){
        usuarios.buscar(emailJugador);
        Scrim s = repo.buscarPorId(idScrim);
        s.confirmarJugador(emailJugador);
        repo.guardar(s);
        System.out.println("[evento] JugadorConfirmo scrim=" + idScrim + " jugador=" + emailJugador);
    }

    // =========================================================
    // ====================  AGENDA/FLUJO  =====================
    // =========================================================
    public void programar(String idScrim, LocalDateTime inicio, LocalDateTime fin) {
        Scrim s = repo.buscarPorId(idScrim);
        s.programar(inicio, fin);
        repo.guardar(s);
        System.out.println("[evento] ScrimProgramado " + idScrim + " " + inicio + "→" + fin);
    }

    public void limpiarAgenda(String idScrim) {
        Scrim s = repo.buscarPorId(idScrim);
        s.limpiarAgenda();
        repo.guardar(s);
        System.out.println("[evento] ScrimAgendaLimpia " + idScrim);
    }

    /** CONFIRMADO → EN_JUEGO */
    public void iniciarScrim(String idScrim){
        Scrim s = repo.buscarPorId(idScrim);
        s.iniciar();
        repo.guardar(s);
        System.out.println("[evento] ScrimEnJuego " + idScrim);
    }

    /** EN_JUEGO → FINALIZADO */
    public void finalizarScrim(String idScrim){
        Scrim s = repo.buscarPorId(idScrim);
        s.finalizar();
        repo.guardar(s);
        System.out.println("[evento] ScrimFinalizado " + idScrim);
    }

    /** * → CANCELADO (no permitir si ya FINALIZADO) */
    public void cancelarScrim(String idScrim){
        Scrim s = repo.buscarPorId(idScrim);
        s.cancelar();
        repo.guardar(s);
        System.out.println("[evento] ScrimCancelado " + idScrim);
    }

    // =========================================================
    // ===================  RESULTADOS/STATS  ==================
    // =========================================================
    public void cargarResultado(String idScrim, String emailJugador,
                                int kills, int assists, int deaths, double rating){
        usuarios.buscar(emailJugador); // opcional validar
        Scrim s = repo.buscarPorId(idScrim);
        KDA kda = new KDA(kills, assists, deaths);
        Estadistica e = new Estadistica(emailJugador, kda, rating, LocalDateTime.now());
        s.registrarEstadistica(e);
        repo.guardar(s);
        System.out.println("[evento] ResultadoReportado scrim=" + idScrim +
                " jugador=" + emailJugador + " kda=" + kda.valor());
    }

    public void agregarSuplente(String idScrim, String emailJugador){
        Scrim s = repo.buscarPorId(idScrim);
        s.agregarAListaEspera(emailJugador);
        repo.guardar(s);
        System.out.println("[evento] SuplenteAgregado scrim=" + idScrim + " jugador=" + emailJugador);
    }
}
