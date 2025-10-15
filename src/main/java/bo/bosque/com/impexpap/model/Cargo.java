package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class Cargo implements Serializable {

    private int codCargo;
    private int codCargoPadre;
    private String descripcion; //cual es el cargo
    private int codEmpresa;
    private int codNivel;
    private int posicion;
    private int estado; //estado del cargo activo o inactivo 1 = activo, 0 = inactivo
    private int audUsuario;

    private String sucursal;
    private String sucursalPlanilla;
    private String nombreEmpresa;
    private String nombreEmpresaPlanilla;
    private int codEmpresaPlanilla;
    private int codCargoPlanilla;
    private String descripcionPlanilla;


    /**
     * Variables de Apoyo para la exportación de datos
     */

    private int nivel;
    private int tieneEmpleadosActivos;
    private int tieneEmpleadosTotales;
    private int estaAsignadoSucursal;
    private int canDeactivate; // que puede ser desactivado o no :::  1 = puede desactivar, 0 = no puede desactivar
    private int numDependientes; //Total de registros en las 3 tablas dependientes ::: rux_cargo + cargo_sucursal + cargo_funcion
    private int numDependenciasTotales; //Total de dependencias críticas (solo empleados activos) ::: rux_cargo + cargo_sucursal + cargo_funcion + numEmpleadosActivos + numSucursalesAsignadas
    private int numDependenciasCompletas; // Total de todas las dependencias (incluye empleados inactivos) ::: rux_cargo + cargo_sucursal + cargo_funcion + numEmpleadosTotales + numSucursalesAsignadas
    private int numDeDependencias; // Solo las 2 primeras tablas (tac_tarRuXCargo + tb_cargo_sucursal) ::: rux_cargo + cargo_sucursal
    private int numHijosActivos; // Número de hijos directos activos ::: Conteo de trh_cargo donde codCargoPadre = codCargo y estado = 1
    private int numHijosTotal; // Número total de hijos (activos + inactivos) ::: Conteo de trh_cargo donde codCargoPadre = codCargo
    private String resumenCompleto; // Una sola columna: Estado, conteos, advertencias y recomendaciones.
    private String estadoPadre; // Ej: Padre activo con dependencias - EJECUTIVO COMERCIAL SUCURSAL

    private int esVisible;
    private List<Cargo> items = new ArrayList<Cargo>();


}
