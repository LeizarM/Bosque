package bo.bosque.com.impexpap.utils;

public class RespuestaSp {


    private int error;
    private String errormsg;
    private long idGenerado;


    // Constructor, Getters y Setters
    public RespuestaSp( int error, String errormsg, long idGenerado ) {
        this.error = error;
        this.errormsg = errormsg;
        this.idGenerado = idGenerado;
    }

    public boolean isExitoso() {
        return this.error == 0;
    }


    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getErrormsg() {
        return errormsg;
    }

    public void setErrormsg(String errormsg) {
        this.errormsg = errormsg;
    }

    public long getIdGenerado() {
        return idGenerado;
    }

    public void setIdGenerado(long idGenerado) {
        this.idGenerado = idGenerado;
    }
}