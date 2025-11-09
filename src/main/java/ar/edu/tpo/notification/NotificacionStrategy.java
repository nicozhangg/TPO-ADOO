package ar.edu.tpo.notification;

public interface NotificacionStrategy {
    void enviar(Notificacion notificacion);
    String getTipo();
}


