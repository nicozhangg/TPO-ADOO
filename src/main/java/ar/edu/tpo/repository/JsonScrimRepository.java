package ar.edu.tpo.repository;

import ar.edu.tpo.domain.Scrim;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JsonScrimRepository implements ScrimRepository {

    private final String ruta;
    private final Gson gson;
    private final Map<String, Scrim> cache;

    public JsonScrimRepository(String rutaArchivo){
        this.ruta = rutaArchivo;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Scrim.class, new ar.edu.tpo.domain.Scrim.JsonAdapter())
                .create();
        this.cache = cargarDesdeDisco();
    }

    @Override
    public void guardar(Scrim scrim) {
        cache.put(scrim.getId(), scrim);
        persistir();
    }

    @Override
    public Scrim buscarPorId(String id) {
        Scrim s = cache.get(id);
        if (s == null) throw new IllegalArgumentException("Scrim no encontrado");
        return s;
    }

    @Override
    public List<Scrim> listar() {
        return new ArrayList<>(cache.values());
    }

    private void persistir(){
        try {
            File f = new File(ruta);
            f.getParentFile().mkdirs();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                gson.toJson(cache, w);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar JSON: " + e.getMessage(), e);
        }
    }

    private Map<String, Scrim> cargarDesdeDisco(){
        try {
            File f = new File(ruta);
            if (!f.exists()) return new LinkedHashMap<>();
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                Type t = new TypeToken<LinkedHashMap<String, Scrim>>(){}.getType();
                Map<String, Scrim> m = gson.fromJson(r, t);
                return (m != null) ? m : new LinkedHashMap<>();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer JSON: " + e.getMessage(), e);
        }
    }
}
