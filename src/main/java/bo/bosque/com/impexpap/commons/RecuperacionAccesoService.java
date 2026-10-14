package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ICodigoRecuperacion;
import bo.bosque.com.impexpap.dto.RecuperacionEnviadaDto;
import bo.bosque.com.impexpap.model.CodigoRecuperacion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Recuperación de acceso con un código de 6 dígitos enviado al correo corporativo.
 * Las reglas (relación laboral, bloqueo, dominios, frecuencia, intentos) viven en p_abm_trec_CodigoRecuperacion
 * y sus valores en trec_configuracion.
 */
@Slf4j
@Service
public class RecuperacionAccesoService {

    static final String MSG_SIN_CODIGO = "No hay un código vigente para este usuario. Pide uno nuevo.";
    static final String MSG_ENVIO_FALLIDO =
            "No pudimos enviar el correo. Inténtalo de nuevo en unos minutos o comunícate con un administrador.";
    static final String MSG_CONTRASENA_CORTA = "La nueva contraseña debe tener al menos 6 caracteres.";
    static final String MSG_CONTRASENA_DEFECTO = "La nueva contraseña no puede ser 123456789.";
    static final String ASUNTO = "Código para recuperar tu acceso a Bosque";

    // Mismas reglas que la pantalla de cambio de contraseña de la app.
    private static final int LARGO_MINIMO = 6;
    private static final String CONTRASENA_POR_DEFECTO = "123456789";

    private static final SecureRandom AZAR = new SecureRandom();

    // El costo 14 de las contraseñas son ~1 s de CPU por pedido anónimo. Un código que vence en minutos y admite
    // pocos intentos no gana nada con eso; con 10 sigue siendo inviable de invertir mientras vale.
    private static final PasswordEncoder CODIGOS = new BCryptPasswordEncoder(10);

    private final ICodigoRecuperacion dao;
    private final PasswordEncoder encoder;
    private final CorreoRecuperacion correo;
    private final SesionService sesiones;

    public RecuperacionAccesoService(ICodigoRecuperacion dao, PasswordEncoder encoder, CorreoRecuperacion correo,
                                     SesionService sesiones) {
        this.dao = dao;
        this.encoder = encoder;
        this.correo = correo;
        this.sesiones = sesiones;
    }

    /** Genera el código, lo registra (el SP rechaza con su mensaje lo que no corresponde) y lo envía. */
    public RecuperacionEnviadaDto solicitar(String login, String ip) {
        String codigo = String.format("%06d", AZAR.nextInt(1000000));
        long idCodigo = dao.solicitar(login.trim(), CODIGOS.encode(codigo), ip).getIdGenerado();
        CodigoRecuperacion registrado = dao.obtenerPorId(idCodigo);
        try {
            correo.enviar(registrado.getCorreo().split(";"), ASUNTO, texto(registrado, codigo));
        } catch (RuntimeException e) {
            // Falla del SMTP o de la configuración (sin contraseña, sin clave de cifrado): para el empleado es lo mismo.
            // Sin la baja, el reintento chocaría con la espera por un correo que nunca llegó.
            log.error("No se pudo enviar el código de recuperación {} del usuario {}", idCodigo, registrado.getCodUsuario(), e);
            try {
                dao.eliminar(idCodigo);
            } catch (RuntimeException bajaFallida) {
                // Que la baja falle no tapa el aviso: el código vence solo en minutos.
                log.error("No se pudo dar de baja el código de recuperación {}", idCodigo, bajaFallida);
            }
            throw new CorreoNoEnviadoException(MSG_ENVIO_FALLIDO);
        }
        return new RecuperacionEnviadaDto(enmascarar(registrado.getCorreo()), registrado.getMinutosVigencia(),
                registrado.getSegundosParaReenviar());
    }

    /** La contraseña se valida antes que el código, para no gastar un intento por una clave corta. */
    public void restablecer(String login, String codigo, String nuevaContrasena, String ip) {
        // Recortada igual que la manda la pantalla de login; si no, nunca coincidiría.
        String clave = nuevaContrasena.trim();
        if (clave.length() < LARGO_MINIMO) throw new SpBusinessException(MSG_CONTRASENA_CORTA);
        if (CONTRASENA_POR_DEFECTO.equals(clave)) throw new SpBusinessException(MSG_CONTRASENA_DEFECTO);

        CodigoRecuperacion vigente = dao.obtenerVigente(login.trim());
        if (vigente == null) throw new SpBusinessException(MSG_SIN_CODIGO);
        // El intento se reserva ANTES de comparar: ni pedidos en paralelo prueban más códigos de los
        // permitidos. Si no quedan, el SP rechaza aquí; si quedan, trae el aviso por si no coincide.
        String avisoSiNoCoincide = dao.reservarIntento(vigente.getIdCodigo()).getErrormsg();
        if (!CODIGOS.matches(codigo.trim(), vigente.getCodigoHash())) {
            throw new SpBusinessException(avisoSiNoCoincide);
        }
        dao.restablecer(vigente.getIdCodigo(), encoder.encode(clave), ip);
        // Si alguien más tenía una sesión abierta con la clave anterior, deja de servirle.
        sesiones.cerrarTodas(vigente.getCodUsuario(), SesionService.MOTIVO_CLAVE, null);
    }

    private static String texto(CodigoRecuperacion registrado, String codigo) {
        return "Hola:\n\n"
                + "Tu código para recuperar el acceso a Bosque es: " + codigo + "\n\n"
                + "Vence en " + registrado.getMinutosVigencia() + " minutos y sirve una sola vez. "
                + "Escríbelo en la pantalla de recuperación junto con tu nueva contraseña.\n\n"
                + "Si no lo pediste, ignora este correo y avisa a un administrador.\n\n"
                + "Este correo se envía automáticamente; no lo respondas.";
    }

    /** j****z@esppapel.com: lo justo para que la persona reconozca su casilla. */
    static String enmascarar(String correos) {
        StringBuilder sb = new StringBuilder();
        for (String c : correos.split(";")) {
            int arroba = c.indexOf('@');
            if (arroba < 1) continue;
            String local = c.substring(0, arroba);
            if (sb.length() > 0) sb.append(", ");
            sb.append(local.charAt(0)).append("****");
            if (local.length() > 2) sb.append(local.charAt(local.length() - 1));
            sb.append(c.substring(arroba));
        }
        return sb.toString();
    }

    /** El correo no salió y el código ya se dio de baja; el controlador responde 503 con el aviso. */
    public static class CorreoNoEnviadoException extends RuntimeException {
        public CorreoNoEnviadoException(String mensaje) {
            super(mensaje);
        }
    }
}
