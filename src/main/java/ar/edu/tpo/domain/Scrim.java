package ar.edu.tpo.domain;

import ar.edu.tpo.service.ArgentinaTimeZone;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

public class Scrim {
    private final String id;
    private final String juego;
    private final String emailCreador;
    private final String emailRival;
    private final int rangoMin;
    private final int rangoMax;

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

    public Scrim(String juego, String emailCreador, String emailRival, int rangoMin, int rangoMax, int cupo) {
        if (cupo <= 0) throw new IllegalArgumentException("Cupo debe ser >= 1");
        this.id = UUID.randomUUID().toString();
        this.juego = Objects.requireNonNull(juego);
        this.emailCreador = Objects.requireNonNull(emailCreador);
        this.emailRival = Objects.requireNonNull(emailRival);
        this.rangoMin = rangoMin;
        this.rangoMax = rangoMax;
        this.cupo = cupo;

        // Crear equipos: equipo1 (creador) y equipo2 (rival)
        this.equipo1 = new Equipo("Equipo " + emailCreador, emailCreador);
        this.equipo2 = new Equipo("Equipo " + emailRival, emailRival);
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
        if (emailCreador.equals(email)) {
            estado.agregarJugadorAEquipo(this, email, equipo1.getNombre());
        } else if (emailRival.equals(email)) {
            estado.agregarJugadorAEquipo(this, email, equipo2.getNombre());
        } else {
            // Por defecto, agregar al equipo1
            estado.agregarJugadorAEquipo(this, email, equipo1.getNombre());
        }
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
    public String getEmailRival(){ return emailRival; }
    public int getRangoMin(){ return rangoMin; }
    public int getRangoMax(){ return rangoMax; }
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
        return "Scrim{id='%s', juego='%s', cupo=%d/equipo, equipo1=%d/%d, equipo2=%d/%d, estado=%s%s}"
                .formatted(id, juego, cupo, 
                    equipo1.getCantidadJugadores(), cupo,
                    equipo2.getCantidadJugadores(), cupo,
                    estado.getNombre(), ventana);
    }

    // ==== JSON Adapter ====
    public static class JsonAdapter implements JsonSerializer<Scrim>, JsonDeserializer<Scrim> {
        @Override public JsonElement serialize(Scrim src, Type t, JsonSerializationContext ctx) {
            JsonObject o = new JsonObject();
            o.addProperty("id", src.id);
            o.addProperty("juego", src.juego);
            o.addProperty("emailCreador", src.emailCreador);
            o.addProperty("emailRival", src.emailRival);
            o.addProperty("rangoMin", src.rangoMin);
            o.addProperty("rangoMax", src.rangoMax);
            o.addProperty("cupo", src.cupo);
            o.addProperty("estado", src.estado.getNombre());
            if (src.inicio != null) o.addProperty("inicio", src.inicio.toString());
            if (src.fin != null)    o.addProperty("fin",    src.fin.toString());

            // Serializar equipos
            JsonObject equipo1Json = new JsonObject();
            equipo1Json.addProperty("nombre", src.equipo1.getNombre());
            equipo1Json.addProperty("capitan", src.equipo1.getEmailCapitan());
            JsonArray jugadores1 = new JsonArray();
            for (String j : src.equipo1.getJugadores()) jugadores1.add(j);
            equipo1Json.add("jugadores", jugadores1);
            o.add("equipo1", equipo1Json);
            
            JsonObject equipo2Json = new JsonObject();
            equipo2Json.addProperty("nombre", src.equipo2.getNombre());
            equipo2Json.addProperty("capitan", src.equipo2.getEmailCapitan());
            JsonArray jugadores2 = new JsonArray();
            for (String j : src.equipo2.getJugadores()) jugadores2.add(j);
            equipo2Json.add("jugadores", jugadores2);
            o.add("equipo2", equipo2Json);
            
            // Legacy: mantener jugadores para compatibilidad
            JsonArray arrJ = new JsonArray();
            for (String j : src.getJugadores()) arrJ.add(j);
            o.add("jugadores", arrJ);

            // Confirmaciones de equipos
            JsonObject confEquipos = new JsonObject();
            for (var e : src.confirmacionesEquipos.entrySet()) {
                confEquipos.addProperty(e.getKey(), e.getValue());
            }
            o.add("confirmacionesEquipos", confEquipos);
            
            // Legacy: confirmaciones por jugador (para compatibilidad)
            JsonObject conf = new JsonObject();
            for (var e : src.getConfirmaciones().entrySet()) conf.addProperty(e.getKey(), e.getValue());
            o.add("confirmaciones", conf);

            JsonArray stats = new JsonArray();
            for (Estadistica e : src.estadisticas) {
                JsonObject je = new JsonObject();
                je.addProperty("emailJugador", e.getEmailJugador());
                JsonObject jk = new JsonObject();
                jk.addProperty("kills", e.getKda().getKills());
                jk.addProperty("assists", e.getKda().getAssists());
                jk.addProperty("deaths", e.getKda().getDeaths());
                je.add("kda", jk);
                je.addProperty("rating", e.getRating());
                je.addProperty("fechaCarga", e.getFechaCarga().toString());
                stats.add(je);
            }
            o.add("estadisticas", stats);

            JsonArray wl = new JsonArray();
            for (WaitlistEntry w : src.listaEspera) {
                JsonObject jw = new JsonObject();
                jw.addProperty("emailJugador", w.emailJugador());
                jw.addProperty("fechaSolicitud", w.fechaSolicitud().toString());
                jw.addProperty("orden", w.orden());
                wl.add(jw);
            }
            o.add("listaEspera", wl);

            if (src.resultado != null) {
                JsonObject jr = new JsonObject();
                jr.addProperty("ganadorEmail", src.resultado.getGanadorEmail());
                o.add("resultado", jr);
            }
            return o;
        }

        @Override public Scrim deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject o = json.getAsJsonObject();
            Scrim s = new Scrim(
                    o.get("juego").getAsString(),
                    o.get("emailCreador").getAsString(),
                    o.get("emailRival").getAsString(),
                    o.get("rangoMin").getAsInt(),
                    o.get("rangoMax").getAsInt(),
                    o.get("cupo").getAsInt()
            );
            try {
                var idF = Scrim.class.getDeclaredField("id"); idF.setAccessible(true); idF.set(s, o.get("id").getAsString());
                var esF = Scrim.class.getDeclaredField("estado"); esF.setAccessible(true); esF.set(s, EstadoScrim.desdeNombre(o.get("estado").getAsString()));
            } catch (Exception e) { throw new JsonParseException("Reconstruccion Scrim fallo: " + e.getMessage(), e); }

            // Parsear fechas interpretándolas como hora de Argentina
            if (o.has("inicio") && !o.get("inicio").isJsonNull()) {
                String inicioStr = o.get("inicio").getAsString();
                // Interpretar como hora de Argentina
                ZonedDateTime inicioZoned = ArgentinaTimeZone.parsear(inicioStr);
                s.inicio = (inicioZoned != null) ? ArgentinaTimeZone.aLocalDateTime(inicioZoned) : null;
            }
            if (o.has("fin") && !o.get("fin").isJsonNull()) {
                String finStr = o.get("fin").getAsString();
                // Interpretar como hora de Argentina
                ZonedDateTime finZoned = ArgentinaTimeZone.parsear(finStr);
                s.fin = (finZoned != null) ? ArgentinaTimeZone.aLocalDateTime(finZoned) : null;
            }

            // Deserializar equipos (nuevo formato)
            if (o.has("equipo1") && o.get("equipo1").isJsonObject()) {
                JsonObject eq1 = o.getAsJsonObject("equipo1");
                if (eq1.has("jugadores")) {
                    for (JsonElement je : eq1.getAsJsonArray("jugadores")) {
                        String email = je.getAsString();
                        if (!email.equals(s.equipo1.getEmailCapitan())) {
                            s.equipo1.agregarJugador(email);
                        }
                    }
                }
            }
            if (o.has("equipo2") && o.get("equipo2").isJsonObject()) {
                JsonObject eq2 = o.getAsJsonObject("equipo2");
                if (eq2.has("jugadores")) {
                    for (JsonElement je : eq2.getAsJsonArray("jugadores")) {
                        String email = je.getAsString();
                        if (!email.equals(s.equipo2.getEmailCapitan())) {
                            s.equipo2.agregarJugador(email);
                        }
                    }
                }
            }
            
            // Deserializar confirmaciones de equipos
            if (o.has("confirmacionesEquipos") && o.get("confirmacionesEquipos").isJsonObject()) {
                var confEquipos = o.getAsJsonObject("confirmacionesEquipos");
                for (String k : confEquipos.keySet()) {
                    s.confirmacionesEquipos.put(k, confEquipos.get(k).getAsBoolean());
                }
            }
            
            // Legacy: compatibilidad con formato antiguo (jugadores individuales)
            if (o.has("jugadores") && !o.has("equipo1")) {
                // Si no hay equipos pero hay jugadores, es formato antiguo
                for (JsonElement je : o.getAsJsonArray("jugadores")) {
                    String email = je.getAsString();
                    if (s.emailCreador.equals(email)) {
                        // ya está en equipo1 como capitán
                    } else if (s.emailRival.equals(email)) {
                        // ya está en equipo2 como capitán
                    } else {
                        // Agregar al equipo1 por defecto
                        s.equipo1.agregarJugador(email);
                    }
                }
            }
            
            // Legacy: confirmaciones por jugador (convertir a confirmaciones de equipos)
            if (o.has("confirmaciones") && o.get("confirmaciones").isJsonObject() && !o.has("confirmacionesEquipos")) {
                var conf = o.getAsJsonObject("confirmaciones");
                // Si un jugador del equipo1 está confirmado, confirmar equipo1
                boolean equipo1Confirmado = false;
                boolean equipo2Confirmado = false;
                for (String k : conf.keySet()) {
                    if (conf.get(k).getAsBoolean()) {
                        if (s.equipo1.contieneJugador(k)) {
                            equipo1Confirmado = true;
                        }
                        if (s.equipo2.contieneJugador(k)) {
                            equipo2Confirmado = true;
                        }
                    }
                }
                s.confirmacionesEquipos.put(s.equipo1.getNombre(), equipo1Confirmado);
                s.confirmacionesEquipos.put(s.equipo2.getNombre(), equipo2Confirmado);
            }
            // recalcular por si faltaba algo
            s.estado.recalcularEstado(s);

            if (o.has("resultado") && o.get("resultado").isJsonObject()) {
                s.setResultado(new Resultado(o.getAsJsonObject("resultado").get("ganadorEmail").getAsString()));
            }
            if (o.has("estadisticas")) {
                for (JsonElement el : o.getAsJsonArray("estadisticas")) {
                    JsonObject je = el.getAsJsonObject();
                    JsonObject jk = je.get("kda").getAsJsonObject();
                    KDA k = new KDA(jk.get("kills").getAsInt(), jk.get("assists").getAsInt(), jk.get("deaths").getAsInt());
                    Estadistica e = new Estadistica(je.get("emailJugador").getAsString(), k, je.get("rating").getAsDouble(),
                            LocalDateTime.parse(je.get("fechaCarga").getAsString()));
                    s.estadisticas.add(e);
                }
            }
            if (o.has("listaEspera")) {
                for (JsonElement el : o.getAsJsonArray("listaEspera")) {
                    JsonObject jw = el.getAsJsonObject();
                    s.listaEspera.add(new WaitlistEntry(
                            jw.get("emailJugador").getAsString(),
                            LocalDateTime.parse(jw.get("fechaSolicitud").getAsString()),
                            jw.get("orden").getAsInt()
                    ));
                }
            }
            return s;
        }
    }
}
