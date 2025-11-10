package ar.edu.tpo.repository;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Organizador;
import ar.edu.tpo.domain.SancionActiva;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.domain.SancionHistorica;
import ar.edu.tpo.domain.alerta.ScrimAlerta;
import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.regiones.StateRegion;
import ar.edu.tpo.domain.roles.StateRoles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class JsonUsuarioRepository implements UsuarioRepository {

    private static final String TIPO_ORGANIZADOR = "Organizador";
    private static final String TIPO_JUGADOR = "Jugador";

    private final String ruta;
    private final Gson gson;
    private final Map<String, Usuario> cache;
    private long nextId;
    private boolean requierePersistencia = false;

    public JsonUsuarioRepository(String rutaArchivo){
        this.ruta = rutaArchivo;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = cargarDesdeDisco();
        this.nextId = calcularSiguienteId(this.cache.values());
        if (requierePersistencia) {
            persistir();
        }
    }

    @Override
    public void guardar(Usuario u) {
        if (cache.containsKey(u.getEmail())) throw new IllegalArgumentException("Email ya registrado");
        asignarIdSiNecesario(u);
        cache.put(u.getEmail(), u);
        persistir();
    }

    @Override
    public Usuario buscar(String email) {
        return cache.get(email);
    }

    @Override
    public Collection<Usuario> listar() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public void actualizar(Usuario u) {
        if (!cache.containsKey(u.getEmail())) {
            throw new IllegalArgumentException("Usuario no registrado: " + u.getEmail());
        }
        asignarIdSiNecesario(u);
        cache.put(u.getEmail(), u);
        persistir();
    }

    private void persistir(){
        try {
            File f = new File(ruta);
            File parent = f.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            JsonObject root = new JsonObject();
            for (Usuario usuario : cache.values()) {
                JsonObject data = new JsonObject();
                data.addProperty("tipo", usuario.getTipo());
                data.addProperty("nombre", usuario.getNombre());
                data.addProperty("id", usuario.getId());
                data.addProperty("email", usuario.getEmail());
                data.addProperty("passwordHash", usuario.getPasswordHash());
                if (usuario instanceof Jugador jugador) {
                    data.addProperty("mmr", usuario.getMmr());
                    data.addProperty("latenciaMs", usuario.getLatenciaMs());
                    if (usuario.getKdaHistorico() != null) {
                        data.addProperty("kdaHistorico", usuario.getKdaHistorico());
                    }
                    data.addProperty("rango", jugador.getRangoNombre());
                    data.addProperty("rolPreferido", jugador.getRolNombre());
                    data.addProperty("region", jugador.getRegionNombre());
                } else {
                    // Solo persistir KDA si existe para otros tipos
                    if (usuario.getKdaHistorico() != null) {
                        data.addProperty("kdaHistorico", usuario.getKdaHistorico());
                    }
                }
                JsonArray sanciones = new JsonArray();
                usuario.getSancionesActivasSinDepurar().forEach(sancion -> {
                    JsonObject sancionJson = new JsonObject();
                    sancionJson.addProperty("motivo", sancion.getMotivo());
                    if (sancion.getExpiraEn() != null) {
                        sancionJson.addProperty("expiraEn", sancion.getExpiraEn().toString());
                    }
                    sanciones.add(sancionJson);
                });
                data.add("sancionesActivas", sanciones);

                JsonArray sancionesHistoricas = new JsonArray();
                usuario.getSancionesHistoricas().forEach(sancion -> {
                    JsonObject sancionJson = new JsonObject();
                    sancionJson.addProperty("motivo", sancion.getMotivo());
                    if (sancion.getExpiraEn() != null) {
                        sancionJson.addProperty("expiraEn", sancion.getExpiraEn().toString());
                    }
                    sancionJson.addProperty("levantadaEn", sancion.getLevantadaEn().toString());
                    sancionesHistoricas.add(sancionJson);
                });
                if (!sancionesHistoricas.isEmpty()) {
                    data.add("sancionesHistoricas", sancionesHistoricas);
                }
                JsonArray favoritas = new JsonArray();
                usuario.getScrimsFavoritas().forEach(favoritas::add);
                data.add("scrimsFavoritas", favoritas);

                JsonArray alertas = new JsonArray();
                usuario.getAlertasScrim().forEach(alerta -> {
                    JsonObject alertaJson = new JsonObject();
                    if (alerta.getJuego() != null && !alerta.getJuego().isBlank()) {
                        alertaJson.addProperty("juego", alerta.getJuego());
                    }
                    if (alerta.getRegion() != null && !alerta.getRegion().isBlank()) {
                        alertaJson.addProperty("region", alerta.getRegion());
                    }
                    if (alerta.getRangoMin() != null) {
                        alertaJson.addProperty("rangoMin", alerta.getRangoMin());
                    }
                    if (alerta.getRangoMax() != null) {
                        alertaJson.addProperty("rangoMax", alerta.getRangoMax());
                    }
                    if (alerta.getLatenciaMax() != null) {
                        alertaJson.addProperty("latenciaMax", alerta.getLatenciaMax());
                    }
                    if (alerta.getFormato() != null && !alerta.getFormato().isBlank()) {
                        alertaJson.addProperty("formato", alerta.getFormato());
                    }
                    alertas.add(alertaJson);
                });
                data.add("alertasScrim", alertas);
                data.addProperty("strikeCount", usuario.getStrikeCount());
                data.addProperty("suspendido", usuario.estaSuspendido());
                root.add(usuario.getEmail(), data);
            }

            try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                gson.toJson(root, w);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar JSON: " + e.getMessage(), e);
        }
    }

    private Map<String, Usuario> cargarDesdeDisco(){
        try {
            File f = new File(ruta);
            if (!f.exists()) return new LinkedHashMap<>();
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(r);
                if (element == null || element.isJsonNull()) {
                    return new LinkedHashMap<>();
                }
                JsonObject root = element.getAsJsonObject();
                Map<String, Usuario> usuarios = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    JsonObject data = entry.getValue().getAsJsonObject();
                    Usuario usuario = mapearUsuario(data);
                    usuarios.put(usuario.getEmail(), usuario);
                }
                return usuarios;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer JSON: " + e.getMessage(), e);
        }
    }

    private Usuario mapearUsuario(JsonObject data) {
        String email = stringOrNull(data, "email");
        String nombre = stringOrNull(data, "nombre");
        String id = stringOrNull(data, "id");
        String password = stringOrNull(data, "passwordHash");
        if (password == null) {
            password = "";
        }
        if (nombre == null || nombre.isBlank()) {
            nombre = email != null ? email : "Usuario";
        }
        int mmr = intOrDefault(data, "mmr", 0);
        int latencia = intOrDefault(data, "latenciaMs", 0);
        Double kda = data.has("kdaHistorico") && !data.get("kdaHistorico").isJsonNull()
                ? data.get("kdaHistorico").getAsDouble() : null;

        String tipo = stringOrNull(data, "tipo");
        if (tipo == null) {
            // Compatibilidad con formato legacy basado en ENUM Rol
            String rolLegacy = stringOrNull(data, "rol");
            if ("PLAYER".equalsIgnoreCase(rolLegacy)) {
                tipo = TIPO_JUGADOR;
            } else {
                tipo = TIPO_ORGANIZADOR;
            }
        }

        Usuario usuario;
        List<SancionActiva> sanciones = leerSanciones(data);
        List<SancionHistorica> sancionesHistoricas = leerSancionesHistoricas(data);
        List<String> favoritas = leerScrimsFavoritas(data);
        List<ScrimAlerta> alertas = leerAlertasScrim(data);

        int strikes = intOrDefault(data, "strikeCount", 0);
        boolean suspendido = data.has("suspendido") && !data.get("suspendido").isJsonNull() && data.get("suspendido").getAsBoolean();

        if (TIPO_JUGADOR.equalsIgnoreCase(tipo)) {
            String rango = stringOrNull(data, "rango");
            String rolPreferido = stringOrNull(data, "rolPreferido");
            String region = stringOrNull(data, "region");

            if (rango == null) {
                rango = determinarRangoPorMmr(mmr);
            }
            if (rolPreferido == null) {
                rolPreferido = StateRoles.disponibles().get(0).getNombre();
            }
            if (region == null) {
                region = StateRegion.disponibles().get(0).getNombre();
            }

            usuario = new Jugador(id, nombre, email, password, mmr, latencia, rango, rolPreferido, region, sanciones, sancionesHistoricas, strikes, suspendido, favoritas, alertas);
        } else {
            usuario = new Organizador(id, nombre, email, password, sanciones, sancionesHistoricas, strikes, suspendido);
        }

        if (id == null || id.isBlank()) {
            requierePersistencia = true;
        }

        if (kda != null) {
            usuario.setKdaHistorico(kda);
        }
        return usuario;
    }

    private String stringOrNull(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }

    private int intOrDefault(JsonObject obj, String key, int defaultValue) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String determinarRangoPorMmr(int mmr) {
        for (StateRangos rango : StateRangos.disponibles()) {
            if (mmr >= rango.getMinimo() && mmr <= rango.getMaximo()) {
                return rango.getNombre();
            }
        }
        return StateRangos.disponibles().get(0).getNombre();
    }

    private List<SancionActiva> leerSanciones(JsonObject data) {
        List<SancionActiva> sanciones = new ArrayList<>();
        if (data.has("sancionesActivas") && data.get("sancionesActivas").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("sancionesActivas")) {
                if (element == null || element.isJsonNull()) continue;
                if (element.isJsonObject()) {
                    JsonObject o = element.getAsJsonObject();
                    String motivo = stringOrNull(o, "motivo");
                    LocalDateTime expira = null;
                    if (o.has("expiraEn") && !o.get("expiraEn").isJsonNull()) {
                        expira = LocalDateTime.parse(o.get("expiraEn").getAsString());
                    }
                    if (motivo != null) {
                        sanciones.add(new SancionActiva(motivo, expira));
                    }
                } else if (element.isJsonPrimitive()) {
                    sanciones.add(new SancionActiva(element.getAsString(), null));
                }
            }
        }
        return sanciones;
    }

    private List<SancionHistorica> leerSancionesHistoricas(JsonObject data) {
        List<SancionHistorica> sanciones = new ArrayList<>();
        if (data.has("sancionesHistoricas") && data.get("sancionesHistoricas").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("sancionesHistoricas")) {
                if (element == null || element.isJsonNull()) continue;
                if (element.isJsonObject()) {
                    JsonObject o = element.getAsJsonObject();
                    String motivo = stringOrNull(o, "motivo");
                    LocalDateTime expira = null;
                    if (o.has("expiraEn") && !o.get("expiraEn").isJsonNull()) {
                        expira = LocalDateTime.parse(o.get("expiraEn").getAsString());
                    }
                    LocalDateTime levantada = null;
                    if (o.has("levantadaEn") && !o.get("levantadaEn").isJsonNull()) {
                        levantada = LocalDateTime.parse(o.get("levantadaEn").getAsString());
                    }
                    if (motivo != null && levantada != null) {
                        sanciones.add(new SancionHistorica(motivo, expira, levantada));
                    }
                }
            }
        }
        return sanciones;
    }

    private List<String> leerScrimsFavoritas(JsonObject data) {
        List<String> favoritas = new ArrayList<>();
        if (data.has("scrimsFavoritas") && data.get("scrimsFavoritas").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("scrimsFavoritas")) {
                if (element != null && element.isJsonPrimitive()) {
                    favoritas.add(element.getAsString());
                }
            }
        }
        return favoritas;
    }

    private List<ScrimAlerta> leerAlertasScrim(JsonObject data) {
        List<ScrimAlerta> alertas = new ArrayList<>();
        if (data.has("alertasScrim") && data.get("alertasScrim").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("alertasScrim")) {
                if (element == null || element.isJsonNull() || !element.isJsonObject()) {
                    continue;
                }
                JsonObject alertaJson = element.getAsJsonObject();
                String juego = stringOrNull(alertaJson, "juego");
                String region = stringOrNull(alertaJson, "region");
                Integer rangoMin = alertaJson.has("rangoMin") && !alertaJson.get("rangoMin").isJsonNull()
                        ? alertaJson.get("rangoMin").getAsInt() : null;
                Integer rangoMax = alertaJson.has("rangoMax") && !alertaJson.get("rangoMax").isJsonNull()
                        ? alertaJson.get("rangoMax").getAsInt() : null;
                Integer latenciaMax = alertaJson.has("latenciaMax") && !alertaJson.get("latenciaMax").isJsonNull()
                        ? alertaJson.get("latenciaMax").getAsInt() : null;
                String formato = stringOrNull(alertaJson, "formato");
                alertas.add(new ScrimAlerta(juego, region, rangoMin, rangoMax, latenciaMax, formato));
            }
        }
        return alertas;
    }

    private long calcularSiguienteId(Collection<Usuario> usuarios) {
        long max = 0L;
        for (Usuario usuario : usuarios) {
            String id = usuario.getId();
            if (id == null) {
                continue;
            }
            try {
                long value = Long.parseLong(id);
                if (value > max) {
                    max = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return max + 1;
    }

    private void actualizarSecuencia(String id) {
        if (id == null) {
            return;
        }
        try {
            long value = Long.parseLong(id);
            if (value >= nextId) {
                nextId = value + 1;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void asignarIdSiNecesario(Usuario usuario) {
        if (usuario.getId() == null || usuario.getId().isBlank()) {
            usuario.asignarId(String.valueOf(nextId++));
        } else {
            actualizarSecuencia(usuario.getId());
        }
    }
}
