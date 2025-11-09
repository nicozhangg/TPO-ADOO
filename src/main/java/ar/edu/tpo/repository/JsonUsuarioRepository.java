package ar.edu.tpo.repository;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Organizador;
import ar.edu.tpo.domain.Usuario;
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
import java.util.*;

public class JsonUsuarioRepository implements UsuarioRepository {

    private static final String TIPO_ORGANIZADOR = "Organizador";
    private static final String TIPO_JUGADOR = "Jugador";

    private final String ruta;
    private final Gson gson;
    private final Map<String, Usuario> cache;
    private boolean requierePersistencia = false;

    public JsonUsuarioRepository(String rutaArchivo){
        this.ruta = rutaArchivo;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = cargarDesdeDisco();
        if (requierePersistencia) {
            persistir();
        }
    }

    @Override
    public void guardar(Usuario u) {
        if (cache.containsKey(u.getEmail())) throw new IllegalArgumentException("Email ya registrado");
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
                data.addProperty("id", usuario.getId());
                data.addProperty("email", usuario.getEmail());
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
                for (String s : usuario.getSancionesActivas()) {
                    sanciones.add(s);
                }
                data.add("sancionesActivas", sanciones);
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
        String id = stringOrNull(data, "id");
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
        List<String> sanciones = leerSanciones(data);

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

            usuario = new Jugador(id, email, mmr, latencia, rango, rolPreferido, region, sanciones);
        } else {
            usuario = new Organizador(id, email, sanciones);
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

    private List<String> leerSanciones(JsonObject data) {
        List<String> sanciones = new ArrayList<>();
        if (data.has("sancionesActivas") && data.get("sancionesActivas").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("sancionesActivas")) {
                if (!element.isJsonNull()) {
                    sanciones.add(element.getAsString());
                }
            }
        }
        return sanciones;
    }
}
