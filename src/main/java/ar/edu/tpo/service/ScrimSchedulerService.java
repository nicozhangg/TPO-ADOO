package ar.edu.tpo.service;

import ar.edu.tpo.domain.EstadoScrim;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.repository.ScrimRepository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio que ejecuta transiciones automáticas de estados de scrims
 * basadas en fecha/hora programada.
 */
public class ScrimSchedulerService {
    private final ScrimRepository repo;
    private final ScrimService scrimService;
    private ScheduledExecutorService scheduler;
    private boolean activo = false;

    public ScrimSchedulerService(ScrimRepository repo, ScrimService scrimService) {
        this.repo = repo;
        this.scrimService = scrimService;
    }

    /**
     * Inicia el scheduler que revisa periódicamente las transiciones automáticas.
     * @param intervaloSegundos Intervalo en segundos entre cada revisión
     */
    public void iniciar(int intervaloSegundos) {
        if (activo) {
            System.out.println("[scheduler] Ya está activo");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ScrimScheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
            this::procesarTransiciones,
            0,
            intervaloSegundos,
            TimeUnit.SECONDS
        );

        activo = true;
        System.out.println("[scheduler] Iniciado - revisando cada " + intervaloSegundos + " segundos");
    }

    /**
     * Detiene el scheduler.
     */
    public void detener() {
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
        }

        activo = false;
        System.out.println("[scheduler] Detenido");
    }

    /**
     * Procesa las transiciones automáticas:
     * - CONFIRMADO → EN_JUEGO (cuando llega la fecha/hora de inicio)
     * NOTA: EN_JUEGO → FINALIZADO debe ser manual (no automático por scheduler)
     */
    private void procesarTransiciones() {
        // Usar hora actual de Argentina
        ZonedDateTime ahoraArgentina = ArgentinaTimeZone.ahora();

        try {
            for (Scrim scrim : repo.listar()) {
                EstadoScrim estado = scrim.getEstado();
                LocalDateTime inicio = scrim.getInicio();

                // CONFIRMADO → EN_JUEGO: cuando la fecha de inicio ya pasó (comparando en zona Argentina)
                if (estado == EstadoScrim.CONFIRMADO && inicio != null) {
                    ZonedDateTime inicioArgentina = ArgentinaTimeZone.aZonaArgentina(inicio);
                    if (inicioArgentina != null && !inicioArgentina.isAfter(ahoraArgentina)) {
                        try {
                            scrimService.iniciarScrim(scrim.getId());
                            System.out.println("[scheduler] Scrim iniciado automáticamente: " + scrim.getId());
                        } catch (Exception e) {
                            System.err.println("[scheduler] Error al iniciar scrim " + scrim.getId() + ": " + e.getMessage());
                        }
                    }
                }

                // EN_JUEGO → FINALIZADO: Solo manual (removido del scheduler automático)
            }
        } catch (Exception e) {
            System.err.println("[scheduler] Error en procesamiento de transiciones: " + e.getMessage());
        }
    }

    public boolean estaActivo() {
        return activo;
    }
}

