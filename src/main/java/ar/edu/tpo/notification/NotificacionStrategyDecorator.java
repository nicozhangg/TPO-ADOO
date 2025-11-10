package ar.edu.tpo.notification;

import java.util.Objects;

/**
 * Decorador base para estrategias de notificación. Permite encadenar
 * responsabilidades (logging, métricas, reintentos, fallback, etc.)
 * sin modificar la estrategia concreta (patrón Decorator).
 */
public abstract class NotificacionStrategyDecorator implements NotificacionStrategy {

    protected final NotificacionStrategy delegate;

    protected NotificacionStrategyDecorator(NotificacionStrategy delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate requerido");
    }

    @Override
    public String getTipo() {
        return delegate.getTipo();
    }
}

