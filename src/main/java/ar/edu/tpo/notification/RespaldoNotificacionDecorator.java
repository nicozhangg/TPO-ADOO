package ar.edu.tpo.notification;

import java.util.Objects;

/**
 * Decorador que intenta enviar la notificación con un canal principal y,
 * en caso de fallo, utiliza un canal de respaldo (fallback).
 */
public class RespaldoNotificacionDecorator extends NotificacionStrategyDecorator {

    private final NotificacionStrategy respaldo;

    public RespaldoNotificacionDecorator(NotificacionStrategy principal, NotificacionStrategy respaldo) {
        super(principal);
        this.respaldo = Objects.requireNonNull(respaldo, "respaldo requerido");
    }

    @Override
    public void enviar(Notificacion notificacion) {
        try {
            delegate.enviar(notificacion);
        } catch (RuntimeException e) {
            System.err.println("[notificacion][fallback] Error con canal '" + delegate.getTipo() +
                    "': " + e.getMessage() + ". Intentando respaldo '" + respaldo.getTipo() + "'");
            respaldo.enviar(notificacion);
        }
    }

    @Override
    public String getTipo() {
        return delegate.getTipo() + "+fallback(" + respaldo.getTipo() + ")";
    }
}

