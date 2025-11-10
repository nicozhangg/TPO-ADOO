package ar.edu.tpo.repository;

import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.repository.json.ScrimJsonAdapter;
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
    private long nextId;

    public JsonScrimRepository(String rutaArchivo){
        this.ruta = rutaArchivo;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Scrim.class, new ScrimJsonAdapter())
                .create();
        this.cache = cargarDesdeDisco();
        this.nextId = calcularSiguienteId(this.cache.keySet());
    }

    @Override
    public void guardar(Scrim scrim) {
        if (scrim.getId() == null || scrim.getId().isBlank()) {
            scrim.asignarId(String.valueOf(nextId++));
        } else {
            actualizarSecuencia(scrim.getId());
        }
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

    private long calcularSiguienteId(Set<String> ids) {
        long max = 0L;
        for (String id : ids) {
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

    private void actualizarSecuencia(String idExistente) {
        try {
            long valor = Long.parseLong(idExistente);
            if (valor >= nextId) {
                nextId = valor + 1;
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
