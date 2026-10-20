package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpeCatalogo;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ITcpeCrud;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRUD genérico {@code /examenes/catalogo/{entidad}/listar|guardar|eliminar} (contrato 4.1); cada controlador atiende
 * su grupo por regex, así una entidad no tiene dos dueños. El cuerpo llega como mapa y se valida a mano porque
 * {@code @Valid} no conoce su tipo; PK nula o 0 = alta y {@code audUsuario} sale del token.
 */
public abstract class TcpeCatalogoBase {

    private final TcpeCatalogo catalogo;
    private final TcpePermisos permisos;
    private final ObjectMapper mapper;
    private final Validator validator;

    protected TcpeCatalogoBase(TcpeCatalogo catalogo, TcpePermisos permisos, ObjectMapper mapper, Validator validator) {
        this.catalogo = catalogo;
        this.permisos = permisos;
        this.mapper = mapper;
        this.validator = validator;
    }

    protected ResponseEntity<ApiResponse<?>> listar(String entidad, Map<String, Object> cuerpo, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.EVALUADOR);
        return listarTipado(catalogo.de(entidad), cuerpo);
    }

    protected ResponseEntity<ApiResponse<?>> guardar(String entidad, Map<String, Object> cuerpo, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.EVALUADOR);
        return guardarTipado(catalogo.de(entidad), cuerpo, DatosToken.codUsuarioDe(auth));
    }

    protected ResponseEntity<ApiResponse<?>> eliminar(String entidad, Map<String, Object> cuerpo, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.EVALUADOR);
        ITcpeCrud<?> dao = catalogo.de(entidad);
        Object valor = cuerpo == null ? null : cuerpo.get(dao.pk());
        return TcpeRespuestas.escritura(dao.eliminar(aLargo(valor, dao.pk()), DatosToken.codUsuarioDe(auth)));
    }

    private <T> ResponseEntity<ApiResponse<?>> listarTipado(ITcpeCrud<T> dao, Map<String, Object> cuerpo) {
        T filtro = convertir(dao, cuerpo);
        return TcpeRespuestas.listado(dao.listar(filtro));
    }

    private <T> ResponseEntity<ApiResponse<?>> guardarTipado(ITcpeCrud<T> dao, Map<String, Object> cuerpo, long aud) {
        if (cuerpo == null || cuerpo.isEmpty()) throw new SpBusinessException("No llegaron los datos a guardar.");
        T modelo = convertir(dao, cuerpo);
        Set<ConstraintViolation<T>> errores = validator.validate(modelo);
        if (!errores.isEmpty()) {
            throw new SpBusinessException(errores.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(" ")));
        }
        return TcpeRespuestas.escritura(dao.guardar(modelo, aud));
    }

    private <T> T convertir(ITcpeCrud<T> dao, Map<String, Object> cuerpo) {
        try {
            return mapper.convertValue(cuerpo == null ? Collections.emptyMap() : cuerpo, dao.tipo());
        } catch (IllegalArgumentException e) {
            throw new SpBusinessException("Los datos enviados no tienen el formato de " + dao.entidad() + ".");
        }
    }

    private static Long aLargo(Object valor, String pk) {
        if (valor instanceof Number) return ((Number) valor).longValue();
        if (valor instanceof String && ((String) valor).trim().matches("\\d{1,18}")) {
            return Long.parseLong(((String) valor).trim());
        }
        throw new SpBusinessException("Indique el registro a eliminar (" + pk + ").");
    }
}
