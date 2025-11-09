package ar.edu.tpo.notification;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Objects;
import java.util.Properties;

public class MailStrategy implements NotificacionStrategy {
    private final boolean smtpHabilitado;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsuario;
    private final String smtpPassword;
    private final boolean usarStartTls;
    private final String remitente;

    private MailStrategy(boolean smtpHabilitado,
                         String remitente,
                         String smtpHost,
                         int smtpPort,
                         String smtpUsuario,
                         String smtpPassword,
                         boolean usarStartTls) {
        this.smtpHabilitado = smtpHabilitado;
        this.remitente = Objects.requireNonNull(remitente, "remitente requerido");
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsuario = smtpUsuario;
        this.smtpPassword = smtpPassword;
        this.usarStartTls = usarStartTls;
    }

    public static MailStrategy smtp(String remitente,
                                    String host,
                                    int port,
                                    String usuario,
                                    String password,
                                    boolean usarStartTls) {
        if (host == null || usuario == null || password == null) {
            throw new IllegalArgumentException("Configuración SMTP inválida");
        }
        return new MailStrategy(true, remitente != null ? remitente : usuario, host, port, usuario, password, usarStartTls);
    }

    public static MailStrategy consola(String remitente) {
        return new MailStrategy(false, remitente, null, 0, null, null, false);
    }

    @Override
    public void enviar(Notificacion notificacion) {
        if (!smtpHabilitado) {
            enviarPorConsola(notificacion);
            return;
        }
        try {
            Session session = Session.getInstance(crearPropiedades(), new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsuario, smtpPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(remitente));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(notificacion.getDestinatario()));
            message.setSubject("eScrims - " + notificacion.getTipo());
            message.setText(notificacion.getPayload());

            Transport.send(message);
            notificacion.marcarEnviado();
            System.out.println("[mail] Email enviado a " + notificacion.getDestinatario());
        } catch (MessagingException e) {
            notificacion.marcarError();
            System.err.println("[mail] Error al enviar email: " + e.getMessage());
            throw new RuntimeException("Error al enviar email", e);
        }
    }

    private Properties crearPropiedades() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        boolean usarSsl = smtpPort == 465;
        if (usarSsl) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", usarStartTls ? "true" : "false");
        }
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", Integer.toString(smtpPort));
        // Ajuste para proveedores que requieren TLS explícito en el puerto 587
        props.put("mail.smtp.socketFactory.port", Integer.toString(smtpPort));
        return props;
    }

    private void enviarPorConsola(Notificacion notificacion) {
        System.out.println("[mail-simulado] Remitente: " + remitente);
        System.out.println("[mail-simulado] Destinatario: " + notificacion.getDestinatario());
        System.out.println("[mail-simulado] Asunto: " + notificacion.getTipo());
        System.out.println("[mail-simulado] Mensaje: " + notificacion.getPayload());
        notificacion.marcarEnviado();
    }

    @Override
    public String getTipo() {
        return "EMAIL";
    }
}


