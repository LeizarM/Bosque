package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** Fila de trec_sesion: un inicio de sesión de la app, su última señal y, si terminó, cómo. */
@Getter
@Setter
@NoArgsConstructor
public class Sesion implements Serializable {

    private long idSesion;
    private long codUsuario;
    private String plataforma;
    private String versionApp;
    private String ip;
    private Date fechaInicio;
    private Date fechaVence;
    private Date fechaUltimaSenal;
    private Date fechaCierre;
    /** S salió, A administrador, B bloqueado, I dado de baja, R cambió la contraseña; null si sigue abierta. */
    private String motivoCierre;
    private Long codUsuarioCierre;
    private Long audUsuario;
    private Date audFecha;

    // Apoyo: los devuelve p_list_trec_Sesion 'A' y 'H'.
    private String login;
    private String nombreCompleto;
    private String loginCierre;
    /** C conectada, N sin señal reciente, V vencida, X cerrada. */
    private String estado;
    /** La sesión del administrador que está mirando la lista (no se ofrece cerrarla). */
    private boolean esActual;
}
