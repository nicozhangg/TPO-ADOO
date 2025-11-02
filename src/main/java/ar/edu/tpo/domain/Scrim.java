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

    // NUEVO: cupo/jugadores/confirmaciones
    private final int cupo;                                    // total de plazas (ej. 10 para 5v5)
    private final LinkedHashSet<String> jugadores = new LinkedHashSet<>();
    private final LinkedHashMap<String, Boolean> confirmaciones = new LinkedHashMap<>();

    private EstadoScrim estado = EstadoScrim.BUSCANDO_JUGADORES;

    private Resultado resultado;
    private final List<Estadistica> estadisticas = new ArrayList<>();
    private final List<WaitlistEntry> listaEspera = new ArrayList<>();

    public Scrim(String juego, String emailCreador, String emailRival, int rangoMin, int rangoMax, int cupo) {
        if (cupo <= 1) throw new IllegalArgumentException("Cupo debe ser >= 2");
        this.id = UUID.randomUUID().toString();
        this.juego = Objects.requireNonNull(juego);
        this.emailCreador = Objects.requireNonNull(emailCreador);
        this.emailRival = Objects.requireNonNull(emailRival);
        this.rangoMin = rangoMin;
        this.rangoMax = rangoMax;
        this.cupo = cupo;

        // opcional: arrancar con creador/rival ya dentro
        agregarJugador(emailCreador);
        agregarJugador(emailRival);
    }

    // ===== Agenda =====
    public void programar(LocalDateTime ini, LocalDateTime fin){
        if (ini == null || fin == null) throw new IllegalArgumentException("Fechas requeridas");
        if (!fin.isAfter(ini)) throw new IllegalArgumentException("Fin debe ser posterior al inicio");
        this.inicio = ini; this.fin = fin;
    }
    public void limpiarAgenda(){ this.inicio = null; this.fin = null; }

    // ===== Jugadores & Confirmaciones =====
    public void agregarJugador(String email){
        if (estado == EstadoScrim.CANCELADO || estado == EstadoScrim.FINALIZADO || estado == EstadoScrim.EN_JUEGO)
            throw new IllegalStateException("No se puede agregar jugador en estado " + estado);
        if (jugadores.size() >= cupo) throw new IllegalStateException("Cupo completo");
        jugadores.add(email);
        confirmaciones.putIfAbsent(email, Boolean.FALSE);
        recalcEstadoPorCupoYConfirmaciones();
    }

    public void quitarJugador(String email){
        if (estado == EstadoScrim.EN_JUEGO || estado == EstadoScrim.FINALIZADO)
            throw new IllegalStateException("No se puede quitar jugador con el scrim en curso o finalizado");
        jugadores.remove(email);
        confirmaciones.remove(email);
        recalcEstadoPorCupoYConfirmaciones();
    }

    public void confirmarJugador(String email){
        if (!jugadores.contains(email)) throw new IllegalStateException("El jugador no está en el scrim");
        if (estado != EstadoScrim.LOBBY_ARMADO && estado != EstadoScrim.CONFIRMADO)
            throw new IllegalStateException("Solo se puede confirmar con lobby armado");
        confirmaciones.put(email, Boolean.TRUE);
        recalcEstadoPorCupoYConfirmaciones();
    }

    private void recalcEstadoPorCupoYConfirmaciones(){
        if (estado == EstadoScrim.CANCELADO || estado == EstadoScrim.FINALIZADO || estado == EstadoScrim.EN_JUEGO) return;

        if (jugadores.size() < cupo) {
            estado = EstadoScrim.BUSCANDO_JUGADORES;
            // reset parciales de confirmación si se cae del cupo
            for (String j : jugadores) confirmaciones.putIfAbsent(j, Boolean.FALSE);
            return;
        }

        // cupo completo
        boolean todosConfirmaron = jugadores.stream().allMatch(j -> Boolean.TRUE.equals(confirmaciones.get(j)));
        estado = todosConfirmaron ? EstadoScrim.CONFIRMADO : EstadoScrim.LOBBY_ARMADO;
    }

    // ===== Transiciones de alto nivel =====
    public void iniciar(){
        if (estado != EstadoScrim.CONFIRMADO) throw new IllegalStateException("Debe estar CONFIRMADO para iniciar");
        estado = EstadoScrim.EN_JUEGO;
    }

    public void finalizar(){
        if (estado != EstadoScrim.EN_JUEGO) throw new IllegalStateException("Solo desde EN_JUEGO");
        estado = EstadoScrim.FINALIZADO;
    }

    public void cancelar(){
        if (estado == EstadoScrim.FINALIZADO) throw new IllegalStateException("No cancelar finalizado");
        estado = EstadoScrim.CANCELADO;
    }

    // ===== Stats / waitlist legacy =====
    public void registrarEstadistica(Estadistica e){
        if (estado != EstadoScrim.EN_JUEGO && estado != EstadoScrim.FINALIZADO)
            throw new IllegalStateException("Estadísticas solo en EN_JUEGO/FINALIZADO");
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
    public Set<String> getJugadores(){ return Collections.unmodifiableSet(jugadores); }
    public Map<String, Boolean> getConfirmaciones(){ return Collections.unmodifiableMap(confirmaciones); }

    public Resultado getResultado(){ return resultado; }
    public void setResultado(Resultado r){ this.resultado = r; }
    public List<WaitlistEntry> getListaEspera(){ return Collections.unmodifiableList(listaEspera); }
    public List<Estadistica> getEstadisticas(){ return Collections.unmodifiableList(estadisticas); }

    @Override public String toString(){
        String ventana = (inicio != null && fin != null) ? (" " + inicio + "→" + fin) : " (sin agenda)";
        return "Scrim{id='%s', juego='%s', cupo=%d, jugadores=%d/%d, estado=%s%s}"
                .formatted(id, juego, jugadores.size(), cupo, cupo, estado, ventana);
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
            o.addProperty("estado", src.estado.name());
            if (src.inicio != null) o.addProperty("inicio", src.inicio.toString());
            if (src.fin != null)    o.addProperty("fin",    src.fin.toString());

            JsonArray arrJ = new JsonArray();
            for (String j : src.jugadores) arrJ.add(j);
            o.add("jugadores", arrJ);

            JsonObject conf = new JsonObject();
            for (var e : src.confirmaciones.entrySet()) conf.addProperty(e.getKey(), e.getValue());
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
                var esF = Scrim.class.getDeclaredField("estado"); esF.setAccessible(true); esF.set(s, EstadoScrim.valueOf(o.get("estado").getAsString()));
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

            if (o.has("jugadores")) for (JsonElement je : o.getAsJsonArray("jugadores")) s.jugadores.add(je.getAsString());
            if (o.has("confirmaciones") && o.get("confirmaciones").isJsonObject()) {
                var conf = o.getAsJsonObject("confirmaciones");
                for (String k : conf.keySet()) s.confirmaciones.put(k, conf.get(k).getAsBoolean());
            }
            // recalcular por si faltaba algo
            s.recalcEstadoPorCupoYConfirmaciones();

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
