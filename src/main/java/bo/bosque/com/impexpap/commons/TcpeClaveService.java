package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.TcpeSeguridadConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Claves de {@code tcpe_usuario.password2}: {@link SecureRandom} sin los caracteres que se confunden al dictar, BCrypt
 * de costo 14 calculado en paralelo por lote ({@link #generarConHash}) porque cada hash tarda cerca de un segundo, y
 * {@link #coincide} acepta también el texto plano del legacy para la migración perezosa del login.
 */
@Service
public class TcpeClaveService {

    public static final int LARGO = 10;

    /** A-Z sin I ni O, a-z sin l, 2-9 (sin 0 ni 1): 57 símbolos, unos 58 bits por clave. */
    static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ" + "abcdefghijkmnopqrstuvwxyz" + "23456789";

    /** Lo que el SP acepta como hash ({@code LIKE '$2_$%'}), con el largo real de BCrypt. */
    private static final Pattern HASH_BCRYPT = Pattern.compile("^\\$2[abxy]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private static final SecureRandom AZAR = new SecureRandom();

    private final PasswordEncoder encoder;
    private final ExecutorService hilos;

    public TcpeClaveService(@Qualifier(TcpeSeguridadConfig.ENCODER) PasswordEncoder encoder) {
        this.encoder = encoder;
        int n = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 8));
        AtomicInteger contador = new AtomicInteger();
        this.hilos = Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "tcpe-bcrypt-" + contador.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void cerrar() {
        hilos.shutdownNow();
    }

    /** Una clave nueva de {@value #LARGO} caracteres. */
    public String generar() {
        char[] c = new char[LARGO];
        for (int i = 0; i < LARGO; i++) {
            c[i] = ALFABETO.charAt(AZAR.nextInt(ALFABETO.length()));
        }
        return new String(c);
    }

    public String hash(String plano) {
        return encoder.encode(plano);
    }

    /** Si lo guardado ya es un hash BCrypt (y no la clave en texto plano del legacy). */
    public static boolean esHash(String guardado) {
        return guardado != null && HASH_BCRYPT.matcher(guardado).matches();
    }

    /**
     * Compara lo que escribió el usuario con lo guardado: contra el hash si lo es, y si no en tiempo constante
     * contra el texto plano heredado. Nada guardado nunca coincide.
     */
    public boolean coincide(String plano, String guardado) {
        if (plano == null || guardado == null || guardado.isEmpty()) return false;
        if (esHash(guardado)) return encoder.matches(plano, guardado);
        return MessageDigest.isEqual(plano.getBytes(StandardCharsets.UTF_8), guardado.getBytes(StandardCharsets.UTF_8));
    }

    /** {@code n} claves nuevas con su hash, calculado en paralelo. El orden de la lista es estable. */
    public List<Clave> generarConHash(int n) {
        List<String> planos = new ArrayList<>(n);
        for (int i = 0; i < n; i++) planos.add(generar());
        List<String> hashes = hashEnParalelo(planos);
        List<Clave> claves = new ArrayList<>(n);
        for (int i = 0; i < n; i++) claves.add(new Clave(planos.get(i), hashes.get(i)));
        return claves;
    }

    /** El hash de cada texto, en el mismo orden, repartiendo el trabajo entre los núcleos. */
    public List<String> hashEnParalelo(List<String> planos) {
        if (planos == null || planos.isEmpty()) return Collections.emptyList();
        List<CompletableFuture<String>> tareas = new ArrayList<>(planos.size());
        for (String p : planos) {
            tareas.add(CompletableFuture.supplyAsync(() -> encoder.encode(p), hilos));
        }
        List<String> hashes = new ArrayList<>(planos.size());
        try {
            for (CompletableFuture<String> t : tareas) hashes.add(t.join());
        } catch (CompletionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("No se pudieron calcular las claves: " + causa.getMessage(), causa);
        }
        return hashes;
    }

    /** Una clave recién generada: el texto que se muestra una sola vez y el hash que se guarda. */
    public static final class Clave {
        private final String plano;
        private final String hash;

        public Clave(String plano, String hash) {
            this.plano = plano;
            this.hash = hash;
        }

        public String getPlano() { return plano; }

        public String getHash() { return hash; }

        @Override
        public String toString() {
            return "Clave{****}";
        }
    }
}
