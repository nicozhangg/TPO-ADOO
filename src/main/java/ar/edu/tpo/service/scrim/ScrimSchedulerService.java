package ar.edu.tpo.service.scrim;

import ar.edu.tpo.domain.EstadoScrim;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.repository.ScrimRepository;
import ar.edu.tpo.service.ArgentinaTimeZone;

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
    private static volatile ScrimSchedulerService instancia;

    private final ScrimRepository repo;
    private final ScrimCicloDeVidaService lifecycleService;
    private ScheduledExecutorService scheduler;
    private boolean activo = false;

    private ScrimSchedulerService(ScrimRepository repo, ScrimCicloDeVidaService lifecycleService) {
        this.repo = repo;
        this.lifecycleService = lifecycleService;
    }

    /**
     * Obtiene la instancia única del scheduler. Si aún no existe, la crea.
     * @param repo repositorio de scrims
     * @param lifecycleService servicio de ciclo de vida de scrims
     * @return instancia única del servicio de scheduler
     */
    public static ScrimSchedulerService getInstance(ScrimRepository repo, ScrimCicloDeVidaService lifecycleService) {
        if (instancia == null) {
            synchronized (ScrimSchedulerService.class) {
                if (instancia == null) {
                    instancia = new ScrimSchedulerService(repo, lifecycleService);
                }
            }
        }
        return instancia;
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
        scheduler = null;
        System.out.println("[scheduler] Detenido");
    }

    /**
     * Procesa las transiciones automáticas:
     * - CONFIRMADO → EN_JUEGO (cuando llega la fecha/hora de inicio)
     * - EN_JUEGO   → FINALIZADO (cuando llega la fecha/hora de fin)
     */
    private void procesarTransiciones() {
        // Usar hora actual de Argentina
        ZonedDateTime ahoraArgentina = ArgentinaTimeZone.ahora();
        ZonedDateTime ahoraTruncado = ahoraArgentina.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);

        try {
            for (Scrim scrim : repo.listar()) {
                EstadoScrim estado = scrim.getEstado();
                LocalDateTime inicio = scrim.getInicio();

                // CONFIRMADO → EN_JUEGO: cuando la fecha de inicio ya pasó completamente (comparando en zona Argentina)
                if (estado instanceof ar.edu.tpo.domain.estado.ConfirmadoState && inicio != null) {
                    ZonedDateTime inicioArgentina = ArgentinaTimeZone.aZonaArgentina(inicio);
                    if (inicioArgentina != null) {
                        ZonedDateTime inicioTruncado = inicioArgentina.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
                        
                        if (ahoraTruncado.isAfter(inicioTruncado) || ahoraTruncado.isEqual(inicioTruncado)) {
                            try {
                                lifecycleService.iniciarScrim(scrim.getId());
                                System.out.println("[scheduler] Scrim iniciado automáticamente: " + scrim.getId() + 
                                    " (inicio programado: " + inicioTruncado + ", hora actual: " + ahoraTruncado + ")");
                            } catch (Exception e) {
                                System.err.println("[scheduler] Error al iniciar scrim " + scrim.getId() + ": " + e.getMessage());
                            }
                        } else {
                            // Debug: mostrar cuando aún no es hora
                            long minutosRestantes = java.time.temporal.ChronoUnit.MINUTES.between(ahoraTruncado, inicioTruncado);
                            if (minutosRestantes <= 5 && minutosRestantes > 0) {
                                System.out.println("[scheduler] Scrim " + scrim.getId() + " iniciará en " + minutosRestantes + " minutos");
                            }
                        }
                    }
                }

                // EN_JUEGO → FINALIZADO: cuando la hora de fin ya pasó
                LocalDateTime fin = scrim.getFin();
                if (estado instanceof ar.edu.tpo.domain.estado.EnJuegoState && fin != null) {
                    ZonedDateTime finArgentina = ArgentinaTimeZone.aZonaArgentina(fin);
                    if (finArgentina != null) {
                        ZonedDateTime finTruncado = finArgentina.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
                        if (ahoraTruncado.isAfter(finTruncado) || ahoraTruncado.isEqual(finTruncado)) {
                            try {
                                lifecycleService.finalizarScrim(scrim.getId());
                                System.out.println("[scheduler] Scrim finalizado automáticamente: " + scrim.getId() +
                                        " (fin programado: " + finTruncado + ", hora actual: " + ahoraTruncado + ")");
                            } catch (Exception e) {
                                System.err.println("[scheduler] Error al finalizar scrim " + scrim.getId() + ": " + e.getMessage());
                            }
                        } else {
                            long minutosRestantes = java.time.temporal.ChronoUnit.MINUTES.between(ahoraTruncado, finTruncado);
                            if (minutosRestantes <= 5 && minutosRestantes > 0) {
                                System.out.println("[scheduler] Scrim " + scrim.getId() + " finalizará en " + minutosRestantes + " minutos");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[scheduler] Error en procesamiento de transiciones: " + e.getMessage());
        }
    }

    public boolean estaActivo() {
        return activo;
    }
}

