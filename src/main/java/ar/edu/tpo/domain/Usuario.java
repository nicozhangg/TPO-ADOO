package ar.edu.tpo.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
public abstract class Usuario {
    private String id;
    private final String nombre;
    private final String email;
    private final String passwordHash;
    protected final List<SancionActiva> sancionesActivas;
    protected final List<SancionHistorica> sancionesHistoricas;
    private int strikeCount;
    private boolean suspendido;

    protected Usuario(String nombre, String email, String passwordHash) {
        this(null, nombre, email, passwordHash, null, null, null, null);
    }

    protected Usuario(String nombre, String email, String passwordHash, List<SancionActiva> sancionesActivas) {
        this(null, nombre, email, passwordHash, sancionesActivas, null, null, null);
    }

    protected Usuario(String nombre, String email, String passwordHash, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
        this(null, nombre, email, passwordHash, sancionesActivas, sancionesHistoricas, null, null);
    }

    protected Usuario(String id, String nombre, String email, String passwordHash, List<SancionActiva> sancionesActivas) {
        this(id, nombre, email, passwordHash, sancionesActivas, null, null, null);
    }

    protected Usuario(String id, String nombre, String email, String passwordHash, List<SancionActiva> sancionesActivas, List<SancionHistorica> sancionesHistoricas) {
        this(id, nombre, email, passwordHash, sancionesActivas, sancionesHistoricas, null, null);
    }

    protected Usuario(String id,
                      String nombre,
                      String email,
                      String passwordHash,
                      List<SancionActiva> sancionesActivas,
                      List<SancionHistorica> sancionesHistoricas,
                      Integer strikeCount,
                      Boolean suspendido) {
        this.id = (id != null && !id.isBlank()) ? id : null;
        this.nombre = Objects.requireNonNull(nombre, "nombre requerido");
        this.email = Objects.requireNonNull(email, "email requerido");
        this.passwordHash = Objects.requireNonNull(passwordHash, "password requerido");
        this.sancionesActivas = new ArrayList<>();
        if (sancionesActivas != null) {
            this.sancionesActivas.addAll(sancionesActivas);
        }
        this.sancionesHistoricas = new ArrayList<>();
        if (sancionesHistoricas != null) {
            this.sancionesHistoricas.addAll(sancionesHistoricas);
        }
        this.strikeCount = strikeCount != null ? strikeCount : 0;
        this.suspendido = suspendido != null ? suspendido : false;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public int getStrikeCount() { return strikeCount; }
    public boolean estaSuspendido() { return suspendido; }

    public List<SancionActiva> getSancionesActivas() {
        depurarSancionesVencidas();
        return Collections.unmodifiableList(sancionesActivas);
    }

    public List<SancionHistorica> getSancionesHistoricas() {
        return Collections.unmodifiableList(sancionesHistoricas);
    }

    public boolean tieneSancionesActivas() {
        depurarSancionesVencidas();
        return !sancionesActivas.isEmpty();
    }

    public int incrementarStrike() {
        strikeCount++;
        return strikeCount;
    }

    public void suspenderCuenta() {
        suspendido = true;
    }

    public void limpiarSanciones() {
        if (!sancionesActivas.isEmpty()) {
            LocalDateTime levantadaEn = LocalDateTime.now();
            sancionesActivas.forEach(s -> sancionesHistoricas.add(s.aHistorica(levantadaEn)));
        }
        sancionesActivas.clear();
    }

    public List<SancionHistorica> removerSancionesVencidas() {
        return depurarSancionesVencidas();
    }

    private List<SancionHistorica> depurarSancionesVencidas() {
        List<SancionHistorica> removidas = new ArrayList<>();
        Iterator<SancionActiva> it = sancionesActivas.iterator();
        LocalDateTime levantadaEn = LocalDateTime.now();
        while (it.hasNext()) {
            SancionActiva s = it.next();
            if (!s.estaActiva()) {
                it.remove();
                SancionHistorica historica = s.aHistorica(levantadaEn);
                sancionesHistoricas.add(historica);
                removidas.add(historica);
            }
        }
        return removidas;
    }

    public List<SancionActiva> getSancionesActivasSinDepurar() {
        return Collections.unmodifiableList(sancionesActivas);
    }

    public SancionHistorica levantarSancionPorIndice(int indice) {
        depurarSancionesVencidas();
        if (indice < 0 || indice >= sancionesActivas.size()) {
            throw new IndexOutOfBoundsException("Indice de sancion invalido");
        }
        SancionActiva sancion = sancionesActivas.remove(indice);
        SancionHistorica historica = sancion.aHistorica(LocalDateTime.now());
        sancionesHistoricas.add(historica);
        return historica;
    }

    public abstract String getTipo();
    public abstract SancionActiva agregarSancion(String motivo, Duration duracion);

    public void asignarId(String nuevoId) {
        if (nuevoId == null || nuevoId.isBlank()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser vacío.");
        }
        if (this.id != null && !this.id.equals(nuevoId)) {
            throw new IllegalStateException("El usuario ya tiene un ID asignado.");
        }
        this.id = nuevoId;
    }

    @Override
    public String toString() {
        depurarSancionesVencidas();
        String sanciones = sancionesActivas.isEmpty() ? "" : ", sanciones=" + sancionesActivas;
        return "%s{id='%s', nombre='%s', email='%s'%s}"
                .formatted(
                        getTipo(),
                        id,
                        nombre,
                        email,
                        sanciones
                );
    }
}
