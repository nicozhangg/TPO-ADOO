package ar.edu.tpo.repository.json;

import ar.edu.tpo.domain.*;
import ar.edu.tpo.service.ArgentinaTimeZone;
import com.google.gson.*;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Adaptador Gson externo para serializar y deserializar Scrim.
 * Permite mantener la entidad enfocada en reglas de negocio.
 */
public class ScrimJsonAdapter implements JsonSerializer<Scrim>, JsonDeserializer<Scrim> {

    @Override
    public JsonElement serialize(Scrim src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject o = new JsonObject();
        o.addProperty("id", src.getId());
        o.addProperty("juego", src.getJuego());
        o.addProperty("emailCreador", src.getEmailCreador());
        o.addProperty("rangoMin", src.getRangoMin());
        o.addProperty("rangoMax", src.getRangoMax());
        o.addProperty("cupo", src.getCupo());
        o.addProperty("formato", src.getFormato());
        o.addProperty("region", src.getRegion());
        o.addProperty("latenciaMaxMs", src.getLatenciaMaxMs());
        o.addProperty("modalidad", src.getModalidad());
        o.addProperty("estado", src.getEstado().getNombre());
        if (src.getInicio() != null) {
            o.addProperty("inicio", src.getInicio().toString());
        }
        if (src.getFin() != null) {
            o.addProperty("fin", src.getFin().toString());
        }

        o.add("equipo1", serializarEquipo(src.getEquipo1()));
        o.add("equipo2", serializarEquipo(src.getEquipo2()));

        JsonArray jugadores = new JsonArray();
        for (String jugador : src.getJugadores()) {
            jugadores.add(jugador);
        }
        o.add("jugadores", jugadores);

        JsonObject confEquipos = new JsonObject();
        for (Map.Entry<String, Boolean> entry : src.getConfirmacionesEquipos().entrySet()) {
            confEquipos.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("confirmacionesEquipos", confEquipos);

        JsonObject confJugadores = new JsonObject();
        for (Map.Entry<String, Boolean> entry : src.getConfirmaciones().entrySet()) {
            confJugadores.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("confirmaciones", confJugadores);

        JsonArray stats = new JsonArray();
        for (Estadistica estadistica : src.getEstadisticas()) {
            JsonObject je = new JsonObject();
            je.addProperty("emailJugador", estadistica.getEmailJugador());
            JsonObject jk = new JsonObject();
            jk.addProperty("kills", estadistica.getKda().getKills());
            jk.addProperty("assists", estadistica.getKda().getAssists());
            jk.addProperty("deaths", estadistica.getKda().getDeaths());
            je.add("kda", jk);
            je.addProperty("rating", estadistica.getRating());
            je.addProperty("fechaCarga", estadistica.getFechaCarga().toString());
            stats.add(je);
        }
        o.add("estadisticas", stats);

        JsonArray waitlist = new JsonArray();
        for (WaitlistEntry entry : src.getListaEspera()) {
            JsonObject jw = new JsonObject();
            jw.addProperty("emailJugador", entry.emailJugador());
            jw.addProperty("fechaSolicitud", entry.fechaSolicitud().toString());
            jw.addProperty("orden", entry.orden());
            waitlist.add(jw);
        }
        o.add("listaEspera", waitlist);

        if (src.getResultado() != null) {
            JsonObject jr = new JsonObject();
            jr.addProperty("ganadorEmail", src.getResultado().getGanadorEmail());
            o.add("resultado", jr);
        }

        return o;
    }

    @Override
    public Scrim deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject o = json.getAsJsonObject();

        int cupo = o.get("cupo").getAsInt();
        String formato = o.has("formato") && !o.get("formato").isJsonNull()
                ? o.get("formato").getAsString()
                : "%dv%d".formatted(cupo, cupo);
        String region = o.has("region") && !o.get("region").isJsonNull()
                ? o.get("region").getAsString()
                : "REGION_DESCONOCIDA";
        int latenciaMax = o.has("latenciaMaxMs") && !o.get("latenciaMaxMs").isJsonNull()
                ? o.get("latenciaMaxMs").getAsInt()
                : 100;
        String modalidad = o.has("modalidad") && !o.get("modalidad").isJsonNull()
                ? o.get("modalidad").getAsString()
                : "casual";

        String emailCreador = o.get("emailCreador").getAsString();
        String emailRivalLegacy = o.has("emailRival") && !o.get("emailRival").isJsonNull()
                ? o.get("emailRival").getAsString()
                : null;

        Scrim scrim = new Scrim(
                o.get("juego").getAsString(),
                emailCreador,
                o.get("rangoMin").getAsInt(),
                o.get("rangoMax").getAsInt(),
                cupo,
                formato,
                region,
                latenciaMax,
                modalidad
        );

        try {
            scrim.asignarId(o.get("id").getAsString());
            setField(scrim, "estado", EstadoScrim.desdeNombre(o.get("estado").getAsString()));
        } catch (Exception e) {
            throw new JsonParseException("Reconstruccion Scrim fallo: " + e.getMessage(), e);
        }

        if (o.has("inicio") && !o.get("inicio").isJsonNull()) {
            ZonedDateTime inicioZoned = ArgentinaTimeZone.parsear(o.get("inicio").getAsString());
            if (inicioZoned != null) {
                setSilently(scrim, "inicio", ArgentinaTimeZone.aLocalDateTime(inicioZoned));
            }
        }
        if (o.has("fin") && !o.get("fin").isJsonNull()) {
            ZonedDateTime finZoned = ArgentinaTimeZone.parsear(o.get("fin").getAsString());
            if (finZoned != null) {
                setSilently(scrim, "fin", ArgentinaTimeZone.aLocalDateTime(finZoned));
            }
        }

        if (o.has("equipo1") && o.get("equipo1").isJsonObject()) {
            cargarEquipo(scrim, scrim.getEquipo1(), o.getAsJsonObject("equipo1"));
        }
        if (o.has("equipo2") && o.get("equipo2").isJsonObject()) {
            cargarEquipo(scrim, scrim.getEquipo2(), o.getAsJsonObject("equipo2"));
        }

        if (o.has("confirmacionesEquipos") && o.get("confirmacionesEquipos").isJsonObject()) {
            JsonObject confEquipos = o.getAsJsonObject("confirmacionesEquipos");
            for (String key : confEquipos.keySet()) {
                scrim.getConfirmacionesEquipos().put(key, confEquipos.get(key).getAsBoolean());
            }
        }

        if (o.has("jugadores") && !o.has("equipo1")) {
            for (JsonElement element : o.getAsJsonArray("jugadores")) {
                String email = element.getAsString();
                scrim.getEquipo1().agregarJugador(email);
            }
        }

        if (emailRivalLegacy != null && !emailRivalLegacy.isBlank()) {
            scrim.getEquipo2().agregarJugador(emailRivalLegacy);
        }

        if (o.has("confirmaciones") && o.get("confirmaciones").isJsonObject() && !o.has("confirmacionesEquipos")) {
            JsonObject conf = o.getAsJsonObject("confirmaciones");
            boolean equipo1Confirmado = false;
            boolean equipo2Confirmado = false;
            for (String key : conf.keySet()) {
                if (conf.get(key).getAsBoolean()) {
                    if (scrim.getEquipo1().contieneJugador(key)) {
                        equipo1Confirmado = true;
                    }
                    if (scrim.getEquipo2().contieneJugador(key)) {
                        equipo2Confirmado = true;
                    }
                }
            }
            scrim.getConfirmacionesEquipos().put(scrim.getEquipo1().getNombre(), equipo1Confirmado);
            scrim.getConfirmacionesEquipos().put(scrim.getEquipo2().getNombre(), equipo2Confirmado);
        }

        if (o.has("resultado") && o.get("resultado").isJsonObject()) {
            scrim.setResultado(new Resultado(o.getAsJsonObject("resultado").get("ganadorEmail").getAsString()));
        }

        if (o.has("estadisticas")) {
            for (JsonElement element : o.getAsJsonArray("estadisticas")) {
                JsonObject je = element.getAsJsonObject();
                JsonObject jk = je.getAsJsonObject("kda");
                KDA kda = new KDA(
                        jk.get("kills").getAsInt(),
                        jk.get("assists").getAsInt(),
                        jk.get("deaths").getAsInt()
                );
                Estadistica estadistica = new Estadistica(
                        je.get("emailJugador").getAsString(),
                        kda,
                        je.get("rating").getAsDouble(),
                        LocalDateTime.parse(je.get("fechaCarga").getAsString())
                );
                addToList(scrim, "estadisticas", estadistica);
            }
        }

        if (o.has("listaEspera")) {
            for (JsonElement element : o.getAsJsonArray("listaEspera")) {
                JsonObject jw = element.getAsJsonObject();
                WaitlistEntry entry = new WaitlistEntry(
                        jw.get("emailJugador").getAsString(),
                        LocalDateTime.parse(jw.get("fechaSolicitud").getAsString()),
                        jw.get("orden").getAsInt()
                );
                addToList(scrim, "listaEspera", entry);
            }
        }

        scrim.getEstado().recalcularEstado(scrim);
        return scrim;
    }

    private JsonObject serializarEquipo(Equipo equipo) {
        JsonObject equipoJson = new JsonObject();
        equipoJson.addProperty("nombre", equipo.getNombre());
        JsonArray jugadores = new JsonArray();
        for (String j : equipo.getJugadores()) {
            jugadores.add(j);
        }
        equipoJson.add("jugadores", jugadores);
        return equipoJson;
    }

    private void cargarEquipo(Scrim scrim, Equipo equipo, JsonObject equipoJson) {
        if (!equipoJson.has("jugadores")) {
            return;
        }
        for (JsonElement element : equipoJson.getAsJsonArray("jugadores")) {
            String email = element.getAsString();
            equipo.agregarJugador(email);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void addToList(Scrim scrim, String fieldName, T value) {
        try {
            Field field = Scrim.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            ((List<T>) field.get(scrim)).add(value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new JsonParseException("No se pudo reconstruir lista '" + fieldName + "': " + e.getMessage(), e);
        }
    }

    private void setSilently(Scrim scrim, String fieldName, Object value) {
        try {
            setField(scrim, fieldName, value);
        } catch (Exception e) {
            throw new JsonParseException("No se pudo establecer el campo '" + fieldName + "': " + e.getMessage(), e);
        }
    }

    private void setField(Scrim scrim, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = Scrim.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(scrim, value);
    }
}



