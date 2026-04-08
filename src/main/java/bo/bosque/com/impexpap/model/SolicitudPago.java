package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudPago implements Serializable {

    private long idSolicitud;
    private int codEmpresa;
    private Date fechaSolicitud;
    private double montoTotalSolicitud;  // decimal(18,2) en BD — float pierde precisión
    private String estado;
    private int audUsuario;


    // --- FILTROS DE RANGO DE FECHA (acción B del SP, no mapean a BD) ---
    private Date fechaInicio;
    private Date fechaFin;

    // --- ATRIBUTOS PARA EL JSON ANIDADO (No mapean directo a BD) ---
    private List<SolicitudProveedor> proveedores;
    private List<Long> proveedoresAEliminar; // IDs que el usuario borró en el frontend



}