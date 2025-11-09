package ar.edu.tpo.service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler que depura periódicamente las sanciones vencidas de los usuarios
 * y persiste los cambios en el repositorio.
 */
public class SancionSchedulerService {

    private static volatile SancionSchedulerService instancia;

    private final UsuarioService usuarioService;
    private ScheduledExecutorService scheduler;
    private boolean activo = false;

    private SancionSchedulerService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public static SancionSchedulerService getInstance(UsuarioService usuarioService) {
        if (instancia == null) {
            synchronized (SancionSchedulerService.class) {
                if (instancia == null) {
                    instancia = new SancionSchedulerService(usuarioService);
                }
            }
        }
        return instancia;
    }

    /**
     * Inicia el scheduler con un intervalo fijo en minutos.
     * @param intervaloMinutos intervalo entre ejecuciones
     */
    public synchronized void iniciar(long intervaloMinutos) {
        if (activo) {
            System.out.println("[sanciones] Scheduler ya estaba activo");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SancionScheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
                this::limpiarSancionesVencidas,
                0,
                intervaloMinutos,
                TimeUnit.MINUTES
        );

        activo = true;
        System.out.println("[sanciones] Scheduler iniciado - revisando cada " + intervaloMinutos + " minutos");
    }

    /**
     * Detiene el scheduler si estaba activo.
     */
    public synchronized void detener() {
        if (!activo || scheduler == null) {
            return;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            scheduler = null;
            activo = false;
        }
        System.out.println("[sanciones] Scheduler detenido");
    }

    public boolean estaActivo() {
        return activo;
    }

    private void limpiarSancionesVencidas() {
        try {
            List<UsuarioService.SancionRemovida> removidas = usuarioService.limpiarSancionesVencidas();
            if (!removidas.isEmpty()) {
                removidas.forEach(r -> {
                    String expira = r.sancion().getExpiraEn() != null ? r.sancion().getExpiraEn().toString() : "sin fecha de expiración";
                    System.out.println("[sanciones] Sanción '" + r.sancion().getMotivo() +
                            "' levantada para " + r.email() +
                            " (expiraba " + expira + ", levantada en " + r.sancion().getLevantadaEn() + ")");
                });
            }
        } catch (Exception e) {
            System.err.println("[sanciones] Error en limpieza de sanciones: " + e.getMessage());
        }
    }
}


