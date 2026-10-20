package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.TcpeRolExamen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DTO del módulo (contratos 6.1 y 7): sin {@code claveEsCi} ni {@code usarCi}, y solo validación de tipos y nulos;
 * rangos, límites y modos los decide el SP.
 */
class TcpeDtosV3Test {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void abrir() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    @Test
    @DisplayName("generar no valida rangos ni límites en Java: nota 150, 500 exámenes, 0 preguntas y modalidad X pasan al SP")
    void sinReglasDeNegocioEnElDto() {
        TcpeLoteGuardarDto d = new TcpeLoteGuardarDto();
        d.setNotaAprobacion(new BigDecimal("150"));
        d.setCantidad(500);
        d.setCantidadPreg(0);
        d.setDuracionMinutos(99999);
        d.setModalidad("X");
        List<Long> muchos = new ArrayList<>();
        for (long i = 1; i <= 600; i++) muchos.add(i);
        d.setCodEmpleados(muchos);
        assertTrue(validador.validate(d).isEmpty());

        TcpeRolExamen r = new TcpeRolExamen();
        r.setNotaAprobacion(new BigDecimal("100.5"));
        r.setModalidad("X");
        assertTrue(validador.validate(r).isEmpty());
    }

    @Test
    @DisplayName("lo estructural sí se valida: un elemento nulo en la lista de personas o de empleados")
    void elementosNulos() {
        TcpeLoteAgregarDto d = new TcpeLoteAgregarDto();
        d.setCodRolExamen(3L);
        d.setCodEmpleados(Arrays.asList(1L, null));
        assertEquals(1, validador.validate(d).size());

        d.setCodEmpleados(null);
        d.setPersonas(Collections.<TcpePersonaExamenDto>singletonList(null));
        assertEquals(1, validador.validate(d).size());
    }

    @Test
    @DisplayName("sin claveEsCi en generar, agregar y las credenciales, ni usarCi en regenerar-clave")
    void sinClaveEsCi() {
        List<Class<?>> conClave = Arrays.<Class<?>>asList(TcpeLoteGuardarDto.class, TcpeLoteAgregarDto.class,
                TcpeCredencialDto.class, TcpeClaveGeneradaDto.class, TcpeUsuarioClaveDto.class);
        for (Class<?> c : conClave) {
            for (Field f : c.getDeclaredFields()) {
                assertFalse(f.getName().equalsIgnoreCase("claveEsCi") || f.getName().equalsIgnoreCase("usarCi"),
                        c.getSimpleName() + " todavía tiene " + f.getName());
            }
        }
    }
}
