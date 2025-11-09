package ar.edu.tpo.domain;

import java.time.LocalDateTime;
import java.util.*;

public class Scrim {
    private final String id;
    private final String juego;
    private final String emailCreador;
    private final int rangoMin;
    private final int rangoMax;
    private final String formato;
    private final String region;
    private final int latenciaMaxMs;
    private final String modalidad;

    // Agenda dentro del scrim
    private LocalDateTime inicio;
    private LocalDateTime fin;

    // EQUIPO VS EQUIPO: cupo = jugadores por equipo (ej. 5 para 5v5)
    private final int cupo;                                    // jugadores por equipo
    private final Equipo equipo1;                             // equipo del creador
    private final Equipo equipo2;                              // equipo del rival
    private final LinkedHashMap<String, Boolean> confirmacionesEquipos = new LinkedHashMap<>(); // confirmación por equipo (nombre)

    private EstadoScrim estado = ar.edu.tpo.domain.estado.BuscandoJugadoresState.INSTANCIA;

    private Resultado resultado;
    private final List<Estadistica> estadisticas = new ArrayList<>();
    private final List<WaitlistEntry> listaEspera = new ArrayList<>();

    public Scrim(String juego, String emailCreador,
                 int rangoMin, int rangoMax, int cupo,
                 String formato, String region, int latenciaMaxMs,
                 String modalidad) {
        if (cupo <= 0) throw new IllegalArgumentException("Cupo debe ser >= 1");
        if (rangoMin < 0 || rangoMax < rangoMin) {
            throw new IllegalArgumentException("Rangos inválidos");
        }
        if (latenciaMaxMs <= 0) {
            throw new IllegalArgumentException("Latencia máxima debe ser > 0 ms");
        }
        this.id = UUID.randomUUID().toString();
        this.juego = Objects.requireNonNull(juego);
        this.emailCreador = Objects.requireNonNull(emailCreador);
        this.rangoMin = rangoMin;
        this.rangoMax = rangoMax;
        this.cupo = cupo;
        this.formato = Objects.requireNonNull(formato, "Formato requerido").trim();
        if (this.formato.isEmpty()) throw new IllegalArgumentException("Formato requerido");
        this.region = Objects.requireNonNull(region, "Región requerida").trim();
        if (this.region.isEmpty()) throw new IllegalArgumentException("Región requerida");
        this.latenciaMaxMs = latenciaMaxMs;
        this.modalidad = Objects.requireNonNull(modalidad, "Modalidad requerida").trim();
        if (this.modalidad.isEmpty()) throw new IllegalArgumentException("Modalidad requerida");

        // Crear equipos: equipo1 (creador) y equipo2 (rival)
        this.equipo1 = new Equipo("Equipo A");
        this.equipo2 = new Equipo("Equipo B");
        this.confirmacionesEquipos.put(equipo1.getNombre(), Boolean.FALSE);
        this.confirmacionesEquipos.put(equipo2.getNombre(), Boolean.FALSE);
    }

    // ===== Agenda =====
    public void programar(LocalDateTime ini, LocalDateTime fin){
        if (ini == null || fin == null) throw new IllegalArgumentException("Fechas requeridas");
        if (!fin.isAfter(ini)) throw new IllegalArgumentException("Fin debe ser posterior al inicio");
        this.inicio = ini; this.fin = fin;
    }
    public void limpiarAgenda(){ this.inicio = null; this.fin = null; }

    // ===== Métodos públicos que delegan al estado =====
    // Mantener compatibilidad: agregar jugador a un equipo
    public void agregarJugador(String email, String nombreEquipo){
        estado.agregarJugadorAEquipo(this, email, nombreEquipo);
    }
    
    // Método legacy: agrega al equipo del creador si es el creador, sino al rival
    public void agregarJugador(String email){
        estado.agregarJugadorAEquipo(this, email, equipo1.getNombre());
    }

    public void quitarJugador(String email){
        estado.quitarJugador(this, email);
    }

    public void confirmarEquipo(String nombreEquipo){
        estado.confirmarEquipo(this, nombreEquipo);
    }
    
    // Método legacy: confirma el equipo del jugador
    public void confirmarJugador(String email){
        if (equipo1.contieneJugador(email)) {
            estado.confirmarEquipo(this, equipo1.getNombre());
        } else if (equipo2.contieneJugador(email)) {
            estado.confirmarEquipo(this, equipo2.getNombre());
        } else {
            throw new IllegalArgumentException("El jugador no pertenece a ningún equipo");
        }
    }

    public void iniciar(){
        estado.iniciar(this);
    }

    public void finalizar(){
        estado.finalizar(this);
    }

    public void cancelar(){
        estado.cancelar(this);
    }

    public void registrarEstadistica(Estadistica e){
        estado.registrarEstadistica(this, e);
    }

    // ===== Métodos públicos para que los estados modifiquen el scrim =====
    // (públicos porque los estados están en un subpaquete)
    public void agregarJugadorAEquipoDirecto(String email, String nombreEquipo){
        Equipo equipo = obtenerEquipoPorNombre(nombreEquipo);
        if (equipo.getCantidadJugadores() >= cupo) {
            throw new IllegalStateException("El equipo " + nombreEquipo + " tiene el cupo completo (" + cupo + " jugadores)");
        }
        equipo.agregarJugador(email);
    }

    public void quitarJugadorDirecto(String email){
        if (equipo1.contieneJugador(email)) {
            equipo1.quitarJugador(email);
        } else if (equipo2.contieneJugador(email)) {
            equipo2.quitarJugador(email);
        }
    }
    
    public Equipo obtenerEquipoPorNombre(String nombreEquipo) {
        if (equipo1.getNombre().equals(nombreEquipo)) return equipo1;
        if (equipo2.getNombre().equals(nombreEquipo)) return equipo2;
        throw new IllegalArgumentException("Equipo no encontrado: " + nombreEquipo);
    }

    public void cambiarEstado(EstadoScrim nuevoEstado){
        this.estado = nuevoEstado;
    }

    public void agregarEstadisticaDirecta(Estadistica e){
        estadisticas.add(e);
    }
    public void agregarAListaEspera(String emailJugador){
        listaEspera.add(new WaitlistEntry(emailJugador, LocalDateTime.now(), listaEspera.size()+1));
    }

    // ===== Getters =====
    public String getId(){ return id; }
    public String getJuego(){ return juego; }
    public String getEmailCreador(){ return emailCreador; }
    public int getRangoMin(){ return rangoMin; }
    public int getRangoMax(){ return rangoMax; }
    public String getFormato(){ return formato; }
    public String getRegion(){ return region; }
    public int getLatenciaMaxMs(){ return latenciaMaxMs; }
    public String getModalidad(){ return modalidad; }
    public EstadoScrim getEstado(){ return estado; }
    public LocalDateTime getInicio(){ return inicio; }
    public LocalDateTime getFin(){ return fin; }
    public int getCupo(){ return cupo; }
    public Equipo getEquipo1(){ return equipo1; }
    public Equipo getEquipo2(){ return equipo2; }
    
    // Métodos legacy para compatibilidad
    public Set<String> getJugadores(){ 
        Set<String> todos = new LinkedHashSet<>();
        todos.addAll(equipo1.getJugadores());
        todos.addAll(equipo2.getJugadores());
        return Collections.unmodifiableSet(todos);
    }
    
    public Map<String, Boolean> getConfirmaciones(){ 
        // Convertir confirmaciones de equipos a formato legacy (por jugador)
        Map<String, Boolean> conf = new LinkedHashMap<>();
        for (String jugador : equipo1.getJugadores()) {
            conf.put(jugador, confirmacionesEquipos.getOrDefault(equipo1.getNombre(), Boolean.FALSE));
        }
        for (String jugador : equipo2.getJugadores()) {
            conf.put(jugador, confirmacionesEquipos.getOrDefault(equipo2.getNombre(), Boolean.FALSE));
        }
        return conf;
    }
    
    public Map<String, Boolean> getConfirmacionesEquipos(){ 
        return confirmacionesEquipos; 
    }
    
    public int getTotalJugadores() {
        return equipo1.getCantidadJugadores() + equipo2.getCantidadJugadores();
    }
    
    public boolean ambosEquiposCompletos() {
        return equipo1.getCantidadJugadores() >= cupo && equipo2.getCantidadJugadores() >= cupo;
    }
    
    public boolean ambosEquiposConfirmados() {
        return Boolean.TRUE.equals(confirmacionesEquipos.get(equipo1.getNombre())) &&
               Boolean.TRUE.equals(confirmacionesEquipos.get(equipo2.getNombre()));
    }

    public Resultado getResultado(){ return resultado; }
    public void setResultado(Resultado r){ this.resultado = r; }
    public List<WaitlistEntry> getListaEspera(){ return Collections.unmodifiableList(listaEspera); }
    public List<Estadistica> getEstadisticas(){ return Collections.unmodifiableList(estadisticas); }

    @Override public String toString(){
        String ventana = (inicio != null && fin != null) ? (" " + inicio + "→" + fin) : " (sin agenda)";
        return "Scrim{id='%s', juego='%s', formato='%s', region='%s', cupo=%d/equipo, latenciaMax=%dms, modalidad='%s', equipo1=%d/%d, equipo2=%d/%d, estado=%s%s}"
                .formatted(id, juego, formato, region, cupo,
                    latenciaMaxMs, modalidad,
                    equipo1.getCantidadJugadores(), cupo,
                    equipo2.getCantidadJugadores(), cupo,
                    estado.getNombre(), ventana);
    }

    // Serialización JSON delegada a ar.edu.tpo.repository.json.ScrimJsonAdapter
}
