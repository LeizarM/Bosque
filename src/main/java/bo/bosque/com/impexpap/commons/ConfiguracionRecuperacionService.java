package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IConfiguracionRecuperacion;
import bo.bosque.com.impexpap.model.ConfiguracionRecuperacion;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parámetros de la recuperación de acceso. Se leen en cada uso, así un cambio desde la pantalla rige en el
 * siguiente pedido, sin reiniciar. Los secretos se cifran al guardar y nunca vuelven a la app.
 */
@Service
public class ConfiguracionRecuperacionService {

    static final String TIPO_SECRETO = "secreto";
    static final String MSG_SIN_VALOR = "Escribe un valor.";
    static final String MSG_NO_EXISTE = "Ese parámetro no existe.";
    static final String MSG_SECRETO_LARGO =
            "La contraseña es demasiado larga para guardarla cifrada; usa una de menos de 340 caracteres.";

    // El largo de trec_configuracion.valor: más que eso SQL Server lo recortaría y el cifrado no se podría leer.
    private static final int LARGO_VALOR = 500;

    private final IConfiguracionRecuperacion dao;
    private final CifradoConfiguracion cifrado;

    public ConfiguracionRecuperacionService(IConfiguracionRecuperacion dao, CifradoConfiguracion cifrado) {
        this.dao = dao;
        this.cifrado = cifrado;
    }

    /** Para la pantalla: de los secretos solo se dice si están cargados. */
    public List<ConfiguracionRecuperacion> paraPantalla() {
        List<ConfiguracionRecuperacion> filas = dao.listar();
        for (ConfiguracionRecuperacion f : filas) {
            f.setConfigurado(f.getValor() != null && !f.getValor().trim().isEmpty());
            if (TIPO_SECRETO.equals(f.getTipo())) f.setValor(null);
        }
        return filas;
    }

    /** Los valores con los que se envía, con los secretos ya descifrados. Un parámetro vacío no aparece. */
    public Map<String, String> vigente() {
        Map<String, String> valores = new HashMap<>();
        for (ConfiguracionRecuperacion f : dao.listar()) {
            if (f.getValor() == null || f.getValor().trim().isEmpty()) continue;
            valores.put(f.getClave(), TIPO_SECRETO.equals(f.getTipo()) ? cifrado.descifrar(f.getValor()) : f.getValor());
        }
        return valores;
    }

    /** El SP valida según el tipo; aquí solo se cifra el secreto (sin recortarlo: una clave puede tener espacios). */
    public void guardar(String clave, String valor, long audUsuario) {
        ConfiguracionRecuperacion fila = dao.obtener(clave);
        if (fila == null) throw new SpBusinessException(MSG_NO_EXISTE);
        if (valor == null || valor.isEmpty()) throw new SpBusinessException(MSG_SIN_VALOR);
        if (!TIPO_SECRETO.equals(fila.getTipo())) {
            dao.actualizar(clave, valor, audUsuario);
            return;
        }
        String cifradoBase64 = cifrado.cifrar(valor);
        if (cifradoBase64.length() > LARGO_VALOR) throw new SpBusinessException(MSG_SECRETO_LARGO);
        dao.actualizar(clave, cifradoBase64, audUsuario);
    }
}
