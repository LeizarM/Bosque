package bo.bosque.com.impexpap.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <b>Las rutas que el servidor expone, contra las rutas del contrato.</b> Congeladas como texto.
 *
 * <h3>El agujero que este test tapa</h3>
 * Las rutas viven en dos repos: acá como {@code @PostMapping} y del lado de Flutter como constantes
 * de {@code app_constants.dart}. Nada las ataba. Cuando se escribieron las escrituras, <b>6 de las
 * 10 rutas de escritura no coincidían</b> ({@code /abonos/*} contra {@code /abono-dias/*},
 * {@code /colectivas/*} contra {@code /colectivo/*}) y <b>ningún test se cayó</b>: los del backend
 * piden la URL correcta porque la copian del controlador, y los del cliente mockean el HTTP. El
 * síntoma en producción habría sido un 404 por cada escritura.
 *
 * <p>Este test no puede leer el {@code app_constants.dart} del otro repo, así que hace lo único que
 * sirve desde acá: fija la lista <b>literal</b>. Si alguien renombra un {@code @PostMapping}, se
 * cae y el mensaje dice que hay que tocar también el cliente. Copiar la lista de abajo al otro
 * repo es la mitad que falta.
 *
 * <p>Se lee por reflexión y no con {@code MockMvc} a propósito: sin contexto de Spring corre en
 * milisegundos y no depende de que el slice levante.
 *
 * <p><b>Todas POST</b>, y no es estilo: {@code SecurityFilter} revisa el query string buscando
 * patrones de SQLi y devuelve 403 ante un apóstrofo. Un {@code GET ?nombre=D'Angelo} daría un 403
 * que nadie sabría explicar. Un {@code @GetMapping} nuevo no aparecería en {@link #rutasDe} y este
 * test se caería por ruta faltante.
 */
class RutasPermisoRrhhTest {

    /**
     * <b>EL CONTRATO.</b> Veintiuna rutas, todas POST, todas kebab-case bajo {@code /permiso-rrhh}.
     * Este bloque y el {@code app_constants.dart} del cliente tienen que decir lo mismo.
     */
    private static final Set<String> CONTRATO = new TreeSet<>(Arrays.asList(
            "/permiso-rrhh/saldo/ficha",
            "/permiso-rrhh/saldo/desglose",
            "/permiso-rrhh/saldo/detalle-tramo",
            "/permiso-rrhh/herramientas/calculo-antiguedad",

            "/permiso-rrhh/vacacion-asignada/historial",
            "/permiso-rrhh/vacacion-asignada/registrar",
            "/permiso-rrhh/vacacion-asignada/eliminar",

            "/permiso-rrhh/abono-dias/historial",
            "/permiso-rrhh/abono-dias/registrar",
            "/permiso-rrhh/abono-dias/eliminar",

            "/permiso-rrhh/colectivo/empleados",
            "/permiso-rrhh/colectivo/abono-dias/simular",
            "/permiso-rrhh/colectivo/abono-dias/aplicar",
            "/permiso-rrhh/colectivo/vacacion/simular",
            "/permiso-rrhh/colectivo/vacacion/aplicar",

            // Nómina de permisos y las tres altas individuales. `calcular` es la única de este
            // bloque que NO escribe: alimenta los campos en vivo del modal.
            "/permiso-rrhh/permisos/kardex",
            "/permiso-rrhh/permisos/vacaciones-ganadas",
            "/permiso-rrhh/permisos/tipos",
            "/permiso-rrhh/permisos/calcular",
            "/permiso-rrhh/permisos/dias-no-habiles",
            "/permiso-rrhh/permisos/quien-esta-fuera",
            "/permiso-rrhh/permisos/boletas",
            "/permiso-rrhh/permisos/registrar",
            "/permiso-rrhh/vacacion/registrar",
            "/permiso-rrhh/vacacion/pagar",

            "/permiso-rrhh/reportes/estado-cuenta",
            "/permiso-rrhh/reportes/estado-cuenta-fiscal"));

    @Test
    @DisplayName("CONTRATO · las rutas del controlador son EXACTAMENTE las del contrato")
    void lasRutasSonLasDelContrato() {
        assertEquals(CONTRATO, rutasDe(PermisoRrhhController.class),
                "Cambió alguna ruta de /permiso-rrhh. Si el cambio es a propósito, hay que "
              + "actualizar TAMBIÉN app_constants.dart en el repo de Flutter: si no, esa "
              + "escritura devuelve 404 en producción y ningún otro test lo va a notar.");
    }

    private static Set<String> rutasDe(Class<?> controlador) {
        RequestMapping base = controlador.getAnnotation(RequestMapping.class);
        String prefijo = (base == null || base.value().length == 0) ? "" : base.value()[0];

        Set<String> rutas = new TreeSet<>();
        for (Method m : controlador.getDeclaredMethods()) {
            PostMapping post = m.getAnnotation(PostMapping.class);
            if (post == null) continue;
            for (String ruta : post.value()) {
                rutas.add(prefijo + ruta);
            }
        }
        return rutas;
    }
}
