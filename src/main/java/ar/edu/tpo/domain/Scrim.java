package ar.edu.tpo.domain;

import ar.edu.tpo.domain.rangos.StateRangos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Scrim {
    private String id;
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
    private final ScrimEquipo equipos;

    private EstadoScrim estado = ar.edu.tpo.domain.estado.BuscandoJugadoresState.INSTANCIA;

    private Resultado resultado;
    private final List<Estadistica> estadisticas = new ArrayList<>();
    private final Waitlist waitlist = new Waitlist();

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
        this.id = null;
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

        this.equipos = new ScrimEquipo(cupo);
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
        estado.agregarJugadorAEquipo(this, email, equipos.getEquipo1().getNombre());
    }

    public void quitarJugador(String email){
        estado.quitarJugador(this, email);
    }

    public void confirmarEquipo(String nombreEquipo){
        estado.confirmarEquipo(this, nombreEquipo);
    }
    
    // Método legacy: confirma el equipo del jugador
    public void confirmarJugador(String email){
        String equipo = equipos.equipoDe(email);
        estado.confirmarEquipo(this, equipo);
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
        equipos.agregarJugadorAEquipo(email, nombreEquipo);
    }

    public void quitarJugadorDirecto(String email){
        equipos.quitarJugador(email);
    }
    
    public Equipo obtenerEquipoPorNombre(String nombreEquipo) {
        return equipos.obtenerEquipoPorNombre(nombreEquipo);
    }

    public void cambiarEstado(EstadoScrim nuevoEstado){
        this.estado = nuevoEstado;
    }

    public void agregarEstadisticaDirecta(Estadistica e){
        estadisticas.add(e);
    }
    public void agregarWaitlistEntryDirecto(WaitlistEntry entry) {
        waitlist.agregarDesdePersistencia(entry);
    }
    public boolean agregarAListaEspera(String emailJugador){
        return waitlist.agregar(emailJugador);
    }

    public boolean quitarDeListaEspera(String emailJugador) {
        return waitlist.quitar(emailJugador);
    }

    public boolean estaEnListaEspera(String emailJugador) {
        return waitlist.esta(emailJugador);
    }

    public boolean hayLugarEnEquipo(String nombreEquipo) {
        return equipos.hayLugarEnEquipo(nombreEquipo);
    }

    public boolean hayCupoDisponible() {
        return equipos.hayCupoDisponible();
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
    public Equipo getEquipo1(){ return equipos.getEquipo1(); }
    public Equipo getEquipo2(){ return equipos.getEquipo2(); }
    
    // Métodos legacy para compatibilidad
    public Set<String> getJugadores(){ 
        return Collections.unmodifiableSet(equipos.getJugadores());
    }
    
    public Map<String, Boolean> getConfirmaciones(){ 
        return equipos.getConfirmacionesPorJugador();
    }
    
    public Map<String, Boolean> getConfirmacionesEquipos(){ 
        return equipos.getConfirmacionesPorEquipo();
    }
    
    public int getTotalJugadores() {
        return equipos.getTotalJugadores();
    }
    
    public boolean ambosEquiposCompletos() {
        return equipos.ambosEquiposCompletos();
    }
    
    public boolean ambosEquiposConfirmados() {
        return equipos.ambosEquiposConfirmados();
    }

    public Resultado getResultado(){ return resultado; }
    public void setResultado(Resultado r){ this.resultado = r; }
    public List<WaitlistEntry> getListaEspera(){ return waitlist.comoListaInmutable(); }
    public List<Estadistica> getEstadisticas(){ return Collections.unmodifiableList(estadisticas); }

    @Override public String toString(){
        String ventana = (inicio != null && fin != null) ? (" " + inicio + "→" + fin) : " (sin agenda)";
        return "Scrim{id='%s', juego='%s', formato='%s', region='%s', rango=%s-%s, cupo=%d/equipo, latenciaMax=%dms, modalidad='%s', equipo1=%d/%d, equipo2=%d/%d, estado=%s%s}"
                .formatted(id, juego, formato, region, nombreRangoPara(rangoMin), nombreRangoPara(rangoMax), cupo,
                    latenciaMaxMs, modalidad,
                    equipos.getEquipo1().getCantidadJugadores(), cupo,
                    equipos.getEquipo2().getCantidadJugadores(), cupo,
                    estado.getNombre(), ventana);
    }

    // Serialización JSON delegada a ar.edu.tpo.repository.json.ScrimJsonAdapter

    private String nombreRangoPara(int puntos) {
        return StateRangos.disponibles().stream()
                .filter(r -> puntos >= r.getMinimo() && puntos <= r.getMaximo())
                .map(StateRangos::getNombre)
                .findFirst()
                .orElse(puntos + " MMR");
    }

    public void asignarId(String nuevoId) {
        if (nuevoId == null || nuevoId.isBlank()) {
            throw new IllegalArgumentException("El ID del scrim no puede ser vacío.");
        }
        if (this.id != null && !this.id.equals(nuevoId)) {
            throw new IllegalStateException("El scrim ya tiene un ID asignado.");
        }
        this.id = nuevoId;
    }

    public void establecerConfirmacionEquipo(String nombreEquipo, boolean confirmado) {
        equipos.establecerConfirmacion(nombreEquipo, confirmado);
    }

    public void reiniciarConfirmacionesDeEquipos() {
        equipos.reiniciarConfirmaciones();
    }

    public ScrimEquipo getEquipos() {
        return equipos;
    }
}
