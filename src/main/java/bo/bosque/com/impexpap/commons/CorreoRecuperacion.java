package bo.bosque.com.impexpap.commons;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

/**
 * Envía los correos de la recuperación con el SMTP que dice trec_configuracion en ese momento.
 * El emisor se arma en cada envío: cambiar servidor, casilla o contraseña no pide reiniciar.
 */
@Component
public class CorreoRecuperacion {

    private final ConfiguracionRecuperacionService configuracion;

    public CorreoRecuperacion(ConfiguracionRecuperacionService configuracion) {
        this.configuracion = configuracion;
    }

    public void enviar(String[] para, String asunto, String texto) {
        Map<String, String> c = configuracion.vigente();
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(c.get("remitente"));
        m.setTo(para);
        m.setSubject(asunto);
        m.setText(texto);
        emisor(c).send(m);
    }

    static JavaMailSenderImpl emisor(Map<String, String> c) {
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(c.get("smtpServidor"));
        s.setPort(Integer.parseInt(c.getOrDefault("smtpPuerto", "587")));
        s.setUsername(c.get("smtpUsuario"));
        s.setPassword(c.get("smtpClave"));
        s.setDefaultEncoding("UTF-8");

        Properties p = s.getJavaMailProperties();
        p.put("mail.smtp.auth", "true");
        // Topes por operación (conectar, cada lectura, cada escritura), no del envío entero. Un SMTP sano
        // responde en milisegundos; uno muy lento puede pasar los 30 s de la app, y el correo sale igual.
        p.put("mail.smtp.connectiontimeout", "5000");
        p.put("mail.smtp.timeout", "8000");
        p.put("mail.smtp.writetimeout", "8000");
        String seguridad = c.getOrDefault("smtpSeguridad", "STARTTLS");
        if ("STARTTLS".equals(seguridad)) {
            p.put("mail.smtp.starttls.enable", "true");
            p.put("mail.smtp.starttls.required", "true");
        } else if ("SSL".equals(seguridad)) {
            p.put("mail.smtp.ssl.enable", "true");
        }
        return s;
    }
}
