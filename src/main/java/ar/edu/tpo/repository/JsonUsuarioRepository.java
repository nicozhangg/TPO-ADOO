package ar.edu.tpo.repository;

import ar.edu.tpo.domain.Usuario;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JsonUsuarioRepository implements UsuarioRepository {

    private final String ruta;
    private final Gson gson;
    private final Map<String, Usuario> cache;

    public JsonUsuarioRepository(String rutaArchivo){
        this.ruta = rutaArchivo;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = cargarDesdeDisco();
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

    private Map<String, Usuario> cargarDesdeDisco(){
        try {
            File f = new File(ruta);
            if (!f.exists()) return new LinkedHashMap<>();
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                Type t = new TypeToken<LinkedHashMap<String, Usuario>>(){}.getType();
                Map<String, Usuario> m = gson.fromJson(r, t);
                return (m != null) ? m : new LinkedHashMap<>();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer JSON: " + e.getMessage(), e);
        }
    }
}
