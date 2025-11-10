package ar.edu.tpo.notification;

import java.time.LocalDateTime;

/**
 * Decorador que agrega trazas antes y después del envío de la notificación.
 */
public class LoggingNotificacionDecorator extends NotificacionStrategyDecorator {

    public LoggingNotificacionDecorator(NotificacionStrategy delegate) {
        super(delegate);
    }

    @Override
    public void enviar(Notificacion notificacion) {
        System.out.println("[notificacion][log] (" + LocalDateTime.now() + ") Enviando " +
                notificacion.getTipo() + " a " + notificacion.getDestinatario());
        delegate.enviar(notificacion);
        System.out.println("[notificacion][log] (" + LocalDateTime.now() + ") Notificación estado=" +
                notificacion.getEstado());
    }
}

