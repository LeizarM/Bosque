package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoChofer implements Serializable {

    private int idPrestamo;
    private int idCoche;
    private int idSolicitud;
    private int codSucursal;
    private Date fechaEntrega;
    private int codEmpEntregadoPor;
    private float kilometrajeEntrega;
    private float kilometrajeRecepcion;
    private int nivelCombustibleEntrega;
    private int nivelCombustibleRecepcion;
    private int estadoLateralesEntrega;
    private int estadoInteriorEntrega;
    private int estadoDelanteraEntrega;
    private int estadoTraseraEntrega;
    private int estadoCapoteEntrega;
    private int estadoLateralRecepcion;
    private int estadoInteriorRecepcion;
    private int estadoDelanteraRecepcion;
    private int estadoTraseraRecepcion;
    private int estadoCapoteRecepcion;
    private int audUsuario;


    //Variables de apoyo

    private Date fechaSolicitud;
    private String motivo;
    private String solicitante;
    private String cargo;
    private String coche;
    private String estadoDisponibilidad;

    private String estadoLateralesEntregaAux;
    private String estadoInteriorEntregaAux;
    private String estadoDelanteraEntregaAux;
    private String estadoTraseraEntregaAux;
    private String estadoCapoteEntregaAux;
    private String estadoLateralRecepcionAux;
    private String estadoInteriorRecepcionAux;
    private String estadoDelanteraRecepcionAux;
    private String estadoTraseraRecepcionAux;
    private String estadoCapoteRecepcionAux;


}