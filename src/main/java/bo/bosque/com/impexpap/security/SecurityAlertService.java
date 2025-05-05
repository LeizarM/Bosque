package bo.bosque.com.impexpap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SecurityAlertService {

    private final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);

    public void sendAlert(String message, String ip, Map<String, Object> details) {
        logger.error("SECURITY ALERT: {} from IP: {}", message, ip);

        // Aquí puedes implementar la lógica para enviar alertas por correo, SMS, etc.
        // Por ejemplo:
        // emailService.sendSecurityAlert(message, ip, details);
    }
}