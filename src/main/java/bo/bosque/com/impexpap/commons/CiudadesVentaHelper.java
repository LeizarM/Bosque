package bo.bosque.com.impexpap.commons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dao.IUsuarioCiudad;
import bo.bosque.com.impexpap.dto.CiudadesPermitidasDto;
import bo.bosque.com.impexpap.model.ArticuloPrecioDisponible;
import bo.bosque.com.impexpap.model.CiudadVenta;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * Que ciudades del catalogo de Ventas puede ver el que llama.
 *
 * <ul>
 *   <li>{@code ROLE_ADM}: todas las ciudades de venta, y "Todas" es el pais
 *       entero ({@code p_list_articuloPrecioDisponible @codCiudad = 0}).</li>
 *   <li>Con filas en {@code tven_UsuarioCiudad}: solo esas; "Todas" es la suma
 *       de esas, no el pais.</li>
 *   <li>Sin filas: su ciudad del login ({@code p_list_Usuario @ACCION='C'}).</li>
 * </ul>
 *
 * <p><b>Se decide aca y no en la app.</b> Antes {@code /articulosX} devolvia la
 * ciudad que viniera en el body: cualquiera veia cualquier ciudad cambiando un
 * numero. Ahora el body solo elige entre las permitidas; lo demas es 403.
 *
 * <p>La ciudad 3 (El Alto) cuenta como la 1 (La Paz): mismo departamento,
 * mismo catalogo. Es la misma regla que ya usan los SP de entregas.
 */
@Component
public class CiudadesVentaHelper {

    /** "Todas" en el selector y en {@code p_list_articuloPrecioDisponible}. */
    public static final int TODAS = 0;

    /**
     * Boton del ACL ({@code tb_vistaBtn.nombreBtn}) que habilita la pantalla
     * "Ciudades por usuario". Es el mismo que chequea el {@code PermissionWidget}
     * de la app: por nombre, sin vista. El nombre es nuevo y unico.
     */
    public static final String BTN_GESTION = "btnCiudadesUsuario";

    /** La ciudad del login todavia no se consulto (no hizo falta). */
    private static final int SIN_CONSULTAR = -1;

    private final IUsuarioCiudad usuarioCiudadDao;
    private final ILoginDao loginDao;
    private final AccesoModuloHelper acceso;
    private final IUsuarioBtn usuarioBtnDao;

    public CiudadesVentaHelper(IUsuarioCiudad usuarioCiudadDao, ILoginDao loginDao,
                               AccesoModuloHelper acceso, IUsuarioBtn usuarioBtnDao) {
        this.usuarioCiudadDao = usuarioCiudadDao;
        this.loginDao = loginDao;
        this.acceso = acceso;
        this.usuarioBtnDao = usuarioBtnDao;
    }

    /**
     * 403 si no puede gestionar ciudades por usuario. Misma regla que
     * {@code tienePermisoDeBoton} de la app: {@code ROLE_ADM} siempre; el resto
     * necesita {@link #BTN_GESTION} con {@code nivelAcceso != 0} en
     * {@code tb_usuarioBtn}. Esconder el boton en la app no autoriza nada: el
     * que decide es este metodo.
     */
    public void exigirGestion(Authentication auth) {
        if (acceso.esAdmin(auth)) return;
        int codUsuario = DatosToken.codUsuarioDe(auth);
        boolean tiene = usuarioBtnDao.botonesXUsuario(codUsuario).stream()
                .anyMatch(b -> BTN_GESTION.equals(b.getBoton()) && b.getPermiso() != 0);
        if (!tiene) {
            throw new AccessDeniedException(
                    "No tiene habilitado '" + BTN_GESTION + "' para asignar ciudades.");
        }
    }

    /** El Alto se trata como La Paz. */
    public static int normalizar(int codCiudad) {
        return codCiudad == 3 ? 1 : codCiudad;
    }

    /** Lo que necesita el selector de la pantalla. */
    public CiudadesPermitidasDto permitidas(Authentication auth) {
        Permiso permiso = resolver(auth);

        CiudadesPermitidasDto dto = new CiudadesPermitidasDto();
        dto.setCiudades(permiso.ciudades);
        dto.setEsAdmin(permiso.esAdmin);
        dto.setTodas(permiso.esAdmin || permiso.ciudades.size() > 1);

        // La ciudad del login solo hace falta para elegir con cual arrancar.
        int inicial = permiso.ciudadLogin != SIN_CONSULTAR
                ? permiso.ciudadLogin
                : ciudadDelLogin(auth).getCodCiudad();
        if (inicial <= 0 || !permiso.incluye(inicial)) {
            inicial = permiso.ciudades.isEmpty() ? 0 : permiso.ciudades.get(0).getCodCiudad();
        }
        dto.setCodCiudadInicial(inicial);
        return dto;
    }

    /**
     * Las filas del catalogo para la ciudad pedida, o 403 si no la puede ver.
     *
     * @param consultar llama a {@code p_list_articuloPrecioDisponible} para una ciudad
     */
    public List<ArticuloPrecioDisponible> catalogo(Authentication auth, int codCiudadPedida,
                                                    IntFunction<List<ArticuloPrecioDisponible>> consultar) {
        Permiso permiso = resolver(auth);
        int cod = normalizar(codCiudadPedida);

        if (cod != TODAS) {
            exigir(permiso, cod);
            return consultar.apply(cod);
        }
        if (permiso.esAdmin) {
            return consultar.apply(TODAS);
        }
        // "Todas" de un usuario con excepciones: una consulta por ciudad, y el
        // total se rehace con solo esas ciudades (el SP con 0 sumaria el pais).
        return unirCiudades(permiso.codigos(), consultar);
    }

    /** El detalle por almacen de un articulo; "Todas" siempre ciudad por ciudad. */
    public List<ArticuloPrecioDisponible> almacenes(Authentication auth, int codCiudadPedida,
                                                     IntFunction<List<ArticuloPrecioDisponible>> consultar) {
        Permiso permiso = resolver(auth);
        int cod = normalizar(codCiudadPedida);

        if (cod != TODAS) {
            exigir(permiso, cod);
            return consultar.apply(cod);
        }
        List<ArticuloPrecioDisponible> filas = new ArrayList<>();
        for (int c : permiso.codigos()) {
            filas.addAll(consultar.apply(c));
        }
        return filas;
    }

    /**
     * Junta el catalogo de varias ciudades. {@code disponibleTotal} viene por
     * ciudad (IPX + ESP de esa ciudad, igual en todas sus filas); el de "Todas"
     * es la suma de esos totales, uno por ciudad.
     */
    static List<ArticuloPrecioDisponible> unirCiudades(List<Integer> ciudades,
                                                      IntFunction<List<ArticuloPrecioDisponible>> consultar) {
        List<ArticuloPrecioDisponible> filas = new ArrayList<>();
        Map<String, Integer> total = new HashMap<>();

        for (int c : ciudades) {
            List<ArticuloPrecioDisponible> deLaCiudad = consultar.apply(c);
            Set<String> contados = new HashSet<>();
            for (ArticuloPrecioDisponible a : deLaCiudad) {
                if (contados.add(a.getCodArticulo())) {
                    total.merge(a.getCodArticulo(), a.getDisponibleTotal(), Integer::sum);
                }
            }
            filas.addAll(deLaCiudad);
        }
        for (ArticuloPrecioDisponible a : filas) {
            a.setDisponibleTotal(total.getOrDefault(a.getCodArticulo(), 0));
        }
        return filas;
    }

    private static void exigir(Permiso permiso, int cod) {
        if (!permiso.incluye(cod)) {
            throw new AccessDeniedException(
                    "No tiene permiso para ver los precios y el stock de esa ciudad.");
        }
    }

    private Permiso resolver(Authentication auth) {
        DatosToken datos = DatosToken.de(auth);

        if (acceso.esAdmin(auth)) {
            return new Permiso(true, usuarioCiudadDao.ciudadesVenta(), SIN_CONSULTAR);
        }

        List<CiudadVenta> asignadas = usuarioCiudadDao.asignadasA(datos.getCodUsuario());
        if (!asignadas.isEmpty()) {
            return new Permiso(false, asignadas, SIN_CONSULTAR);
        }

        // Sin excepciones: su ciudad, como siempre.
        Login login = ciudadDelLogin(auth);
        int propia = login.getCodCiudad();
        if (propia <= 0) {
            return new Permiso(false, Collections.<CiudadVenta>emptyList(), 0);
        }
        String nombre = login.getNombreCiudad();
        for (CiudadVenta v : usuarioCiudadDao.ciudadesVenta()) {
            if (v.getCodCiudad() == propia) nombre = v.getCiudad();
        }
        List<CiudadVenta> una = new ArrayList<>();
        una.add(new CiudadVenta(datos.getCodUsuario(), propia, nombre));
        return new Permiso(false, una, propia);
    }

    /** La ciudad del login, ya normalizada. {@code verifyUser} es solo lectura. */
    private Login ciudadDelLogin(Authentication auth) {
        Login login = loginDao.verifyUser(acceso.loginDelToken(auth), null);
        if (login == null) login = new Login();
        login.setCodCiudad(normalizar(login.getCodCiudad()));
        return login;
    }

    private static final class Permiso {
        final boolean esAdmin;
        final List<CiudadVenta> ciudades;
        final int ciudadLogin;

        Permiso(boolean esAdmin, List<CiudadVenta> ciudades, int ciudadLogin) {
            this.esAdmin = esAdmin;
            this.ciudades = ciudades;
            this.ciudadLogin = ciudadLogin;
        }

        boolean incluye(int cod) {
            for (CiudadVenta c : ciudades) {
                if (c.getCodCiudad() == cod) return true;
            }
            return false;
        }

        List<Integer> codigos() {
            List<Integer> cods = new ArrayList<>();
            for (CiudadVenta c : ciudades) cods.add(c.getCodCiudad());
            return cods;
        }
    }
}
