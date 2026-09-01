package bo.bosque.com.impexpap.dto;

import java.util.Date;

/**
 * Una fila del reporte "Horario Vigente por Empleado" — qué {@code BioHrSemanal}
 * tiene cada empleado enlazado HOY (el vigente según {@code horarioVigente},
 * la misma regla día-correlacionada que ya usa {@link AsistenciaDiaDto} —
 * no el bug conocido de {@code p_abm_BioHrXEmplExpandido ACCION='A'}), más
 * el detalle de horas por día de semana (de {@code BioHrSemanalDetalle}/
 * {@code BioHrs}), "—" cuando ese día no tiene turno asignado.
 */
public class HorarioVigenteEmpleadoDto {

    private String nombreEmpleado;
    private String nombreHorarioSemanal;
    private Date vigenteDesde;
    private String lunes;
    private String martes;
    private String miercoles;
    private String jueves;
    private String viernes;
    private String sabado;
    private String domingo;

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }

    public void setNombreEmpleado(String nombreEmpleado) {
        this.nombreEmpleado = nombreEmpleado;
    }

    public String getNombreHorarioSemanal() {
        return nombreHorarioSemanal;
    }

    public void setNombreHorarioSemanal(String nombreHorarioSemanal) {
        this.nombreHorarioSemanal = nombreHorarioSemanal;
    }

    public Date getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(Date vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }

    public String getLunes() {
        return lunes;
    }

    public void setLunes(String lunes) {
        this.lunes = lunes;
    }

    public String getMartes() {
        return martes;
    }

    public void setMartes(String martes) {
        this.martes = martes;
    }

    public String getMiercoles() {
        return miercoles;
    }

    public void setMiercoles(String miercoles) {
        this.miercoles = miercoles;
    }

    public String getJueves() {
        return jueves;
    }

    public void setJueves(String jueves) {
        this.jueves = jueves;
    }

    public String getViernes() {
        return viernes;
    }

    public void setViernes(String viernes) {
        this.viernes = viernes;
    }

    public String getSabado() {
        return sabado;
    }

    public void setSabado(String sabado) {
        this.sabado = sabado;
    }

    public String getDomingo() {
        return domingo;
    }

    public void setDomingo(String domingo) {
        this.domingo = domingo;
    }
}
