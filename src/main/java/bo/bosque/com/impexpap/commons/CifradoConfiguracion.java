package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra los parámetros secretos de trec_configuracion (la contraseña SMTP) con AES-256-GCM.
 * La clave vive solo en el servidor (CONFIG_CIFRADO_CLAVE): en la base queda Base64(iv + cifrado).
 */
@Component
public class CifradoConfiguracion {

    static final String MSG_SIN_CLAVE =
            "Falta configurar CONFIG_CIFRADO_CLAVE en el servidor; sin ella no se puede guardar ni usar la contraseña del correo.";
    static final String MSG_CLAVE_MAL_FORMADA =
            "CONFIG_CIFRADO_CLAVE tiene que ser el texto Base64 que genera el comando de LEEME-SECRETS.md "
            + "(32 bytes o más al decodificarlo), no una contraseña escrita a mano";

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int LARGO_IV = 12;
    private static final int BITS_ETIQUETA = 128;
    private static final SecureRandom AZAR = new SecureRandom();

    private final byte[] clave;

    public CifradoConfiguracion(@Value("${recuperacion.clave-cifrado:}") String claveBase64) {
        if (claveBase64 == null || claveBase64.trim().isEmpty()) {
            this.clave = null;
            return;
        }
        // Mal escrita no arranca, igual que JWT_SECRET: mejor eso que descubrirlo al primer envío.
        byte[] material;
        try {
            material = Base64.getDecoder().decode(claveBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(MSG_CLAVE_MAL_FORMADA + "; el valor actual no es Base64.", e);
        }
        if (material.length < 32) {
            throw new IllegalStateException(MSG_CLAVE_MAL_FORMADA + "; el valor actual da " + material.length + " bytes.");
        }
        // Sirve cualquier largo desde 32 (el comando de JWT_SECRET da 64): la clave AES-256 es su SHA-256.
        try {
            this.clave = MessageDigest.getInstance("SHA-256").digest(material);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }

    public String cifrar(String texto) {
        byte[] iv = new byte[LARGO_IV];
        AZAR.nextBytes(iv);
        try {
            Cipher c = Cipher.getInstance(ALGORITMO);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(exigirClave(), "AES"), new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el parámetro", e);
        }
    }

    public String descifrar(String guardado) {
        byte[] todo = Base64.getDecoder().decode(guardado);
        try {
            Cipher c = Cipher.getInstance(ALGORITMO);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(exigirClave(), "AES"), new GCMParameterSpec(BITS_ETIQUETA, todo, 0, LARGO_IV));
            return new String(c.doFinal(todo, LARGO_IV, todo.length - LARGO_IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // Típico de una CONFIG_CIFRADO_CLAVE cambiada: el valor guardado ya no se puede leer.
            throw new IllegalStateException("No se pudo descifrar el parámetro; ¿cambió CONFIG_CIFRADO_CLAVE?", e);
        }
    }

    private byte[] exigirClave() {
        if (clave == null) throw new SpBusinessException(MSG_SIN_CLAVE);
        return clave;
    }
}
