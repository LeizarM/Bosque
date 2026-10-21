package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.TcpeCrudDao;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Los tres controladores de catálogo se reparten las 40 entidades sin huecos ni repetidas, y el {@code {entidad}} de
 * cada ruta es exactamente el {@code entidad()} de su DAO, el mismo nombre que arma el frontend: una entidad fuera
 * de todo regex da 404 sin que nada lo avise antes.
 */
class TcpeCatalogoRutasTest {

    @Test
    @DisplayName("los tres regex ENTIDADES cubren los 40 DAO tcpe_, sin repetir y con su mismo nombre")
    void regexCubreLosCuarenta() throws Exception {
        List<String> enRutas = new ArrayList<>();
        for (String regex : new String[] { TcpeCatalogoBancoController.ENTIDADES,
                TcpeCatalogoExamenController.ENTIDADES, TcpeCatalogoPostulacionController.ENTIDADES }) {
            enRutas.addAll(Arrays.asList(regex.split("\\|")));
        }
        Set<String> sinRepetir = new TreeSet<>(enRutas);
        assertEquals(enRutas.size(), sinRepetir.size(), "una entidad no puede tener dos controladores");

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(TcpeCrudDao.class));
        SpHelper sp = mock(SpHelper.class);
        Set<String> daos = new TreeSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("bo.bosque.com.impexpap.dao")) {
            TcpeCrudDao<?> dao = (TcpeCrudDao<?>) Class.forName(bd.getBeanClassName())
                    .getConstructor(SpHelper.class).newInstance(sp);
            daos.add(dao.entidad());
        }
        assertEquals(40, daos.size());
        assertEquals(daos, sinRepetir);
    }

    @Test
    @DisplayName("v11: preguntaSemana y rsptaSemana van por el catálogo del banco, sin pisar a pregunta ni rsptaOpcion")
    void preguntasDeLaSemana() {
        Pattern banco = Pattern.compile(TcpeCatalogoBancoController.ENTIDADES);
        assertEquals(true, banco.matcher("preguntaSemana").matches());
        assertEquals(true, banco.matcher("rsptaSemana").matches());
        assertEquals(true, banco.matcher("pregunta").matches());
        assertEquals(false, banco.matcher("preguntaSemanas").matches());
    }

    @Test
    @DisplayName("el regex reconoce el segmento entero y distingue mayúsculas, como lo usa Spring")
    void segmentoExacto() {
        Pattern postulacion = Pattern.compile(TcpeCatalogoPostulacionController.ENTIDADES);
        Pattern examen = Pattern.compile(TcpeCatalogoExamenController.ENTIDADES);
        assertEquals(true, postulacion.matcher("conocimientosPost").matches());
        assertEquals(true, postulacion.matcher("conocimientos").matches());
        assertEquals(true, examen.matcher("rsptaOpcionH").matches());
        assertEquals(false, examen.matcher("RsptaOpcionH").matches(), "PascalCase no es una ruta");
        assertEquals(false, examen.matcher("rsptaopcionh").matches());
    }
}
