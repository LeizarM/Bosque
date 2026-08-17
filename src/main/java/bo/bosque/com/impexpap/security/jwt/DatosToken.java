package bo.bosque.com.impexpap.security.jwt;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/**
 * La identidad que viene firmada dentro del JWT, colgada del
 * {@code Authentication} para que la lean los controladores.
 *
 * <h3>Por que existe</h3>
 * {@code loadUserByUsername} devuelve un {@code UserDetails} con <b>login y rol
 * y nada mas</b>. Por eso, hasta ahora, el {@code codUsuario} y el
 * {@code audUsuario} de casi todos los ABM llegaban <b>en el body o en un
 * {@code @RequestParam}</b>: los elegia el cliente. O sea que cualquiera
 * registraba una operacion a nombre de otro, y {@code POST /view/vistaBtn}
 * devolvia el ACL de cualquier usuario con solo cambiar un numero.
 *
 * <p>Los datos ya viajaban firmados en el token ({@code codUsuario} como
 * {@code jti}, {@code codEmpresa} y {@code codEmpleado} como claims); lo unico
 * que faltaba era bajarlos al {@code SecurityContext}. Eso hace
 * {@link JwtTokenFilter}, y esta clase es donde quedan.
 *
 * <h3>Contra la alternativa de consultar la base</h3>
 * {@code AccesoModuloHelper.miPermiso()} tambien resuelve {@code login →
 * codUsuario}, pero cuesta una consulta por llamada y no conoce la empresa.
 * Aca no hay consulta: el token ya esta verificado cuando se lee. La contra es
 * que los datos son de la hora del login y no de ahora — para identidad
 * (quien soy, de que empresa) eso es exactamente lo que se quiere, porque es
 * tambien lo que el usuario vio en pantalla toda la sesion.
 *
 * <h3>Falla cerrado</h3>
 * {@link #de(Authentication)} tira 403 si no hay token, si el token es viejo o
 * si no trae {@code codUsuario}. Nunca devuelve 0 ni {@code null}: un
 * {@code audUsuario} en 0 seria una fila de auditoria anonima, que es
 * justamente lo que se viene a arreglar.
 */
public final class DatosToken {

    private final int codUsuario;
    private final int codEmpleado;
    private final int codEmpresa;
    private final String tipoUsuario;

    public DatosToken(int codUsuario, int codEmpleado, int codEmpresa, String tipoUsuario) {
        this.codUsuario  = codUsuario;
        this.codEmpleado = codEmpleado;
        this.codEmpresa  = codEmpresa;
        this.tipoUsuario = tipoUsuario;
    }

    public int getCodUsuario()     { return codUsuario; }
    public int getCodEmpleado()    { return codEmpleado; }
    public int getCodEmpresa()     { return codEmpresa; }
    public String getTipoUsuario() { return tipoUsuario; }

    /**
     * Los datos del que esta llamando, o 403.
     *
     * <p>El caso "token viejo": los tokens emitidos antes de este cambio siguen
     * siendo validos hasta que expiren, pero pasaron por un filtro que no
     * poblaba los details. Caen aca y reciben un 403 con un mensaje que dice
     * que hay que volver a entrar — que es la accion correcta y no un error
     * generico que nadie sabe interpretar.
     *
     * @throws AccessDeniedException 403, que {@code GlobalExceptionHandler} ya sabe traducir
     */
    public static DatosToken de(Authentication auth) {
        Object detalles = auth != null ? auth.getDetails() : null;
        if (!(detalles instanceof DatosToken)) {
            throw new AccessDeniedException(
                    "No se pudo identificar su usuario. Cierre sesión y vuelva a entrar.");
        }
        DatosToken datos = (DatosToken) detalles;
        if (datos.codUsuario <= 0) {
            throw new AccessDeniedException(
                    "Su token no identifica un usuario válido. Cierre sesión y vuelva a entrar.");
        }
        return datos;
    }

    /** El {@code codUsuario} del que llama, para {@code audUsuario} y para el ACL. */
    public static int codUsuarioDe(Authentication auth) {
        return de(auth).getCodUsuario();
    }

    @Override
    public String toString() {
        return "DatosToken{codUsuario=" + codUsuario
                + ", codEmpleado=" + codEmpleado
                + ", codEmpresa=" + codEmpresa
                + ", tipoUsuario='" + tipoUsuario + "'}";
    }
}
