package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** Fila de trec_codigoRecuperacion. Sin @ToString a propósito: el hash del código no va a los logs. */
@Getter
@Setter
@NoArgsConstructor
public class CodigoRecuperacion implements Serializable {

    private long idCodigo;
    private long codUsuario;
    private String correo;
    private String codigoHash;
    private Date fechaSolicitud;
    private Date fechaExpira;
    private int intentos;
    private String estado;
    private String ip;
    private Long audUsuario;
    private Date audFecha;

    // Apoyo: solo los devuelve p_list_trec_CodigoRecuperacion 'L'.
    private int minutosVigencia;
    private int segundosParaReenviar;
}
