package bo.bosque.com.impexpap.commons;

import java.util.List;

/**
 * Los textos de error de Verificar Cheques (vista 77), en un solo lugar y sin ninguna regla de negocio: describen lo
 * que {@link ReglasVerificacion} y {@link VerificacionChequeService} deciden, no deciden nada.
 *
 * <p>Va aparte de {@link MensajesCheque} a proposito (es otra pantalla y otro equipo la toca a la vez), pero con el mismo
 * criterio: cada mensaje dice <i>que</i> fallo, <i>por que</i> y <i>que hacer</i>, con los datos concretos cuando se
 * conocen. En el legacy esta pantalla solo decia "Registro no Disponible", "Accion Invalida" o no decia nada (si el
 * guardado fallaba, la ventana temblaba y ya). Espanol neutro, con tuteo.
 */
public final class MensajesVerificacion {

    private MensajesVerificacion() {
    }

    // ===================================================================== //
    //                              CAMPOS                                   //
    // ===================================================================== //

    public static final String SIN_DATOS =
            "No se recibieron los datos de la verificación. Vuelve a intentarlo.";

    public static final String FALTA_FECHA =
            "Falta la fecha de verificación. Elige el día en que el banco verificó el depósito del cheque.";

    public static final String FALTA_BANCO =
            "Falta elegir el banco de la verificación. Elige en la lista el banco donde se verificó el depósito.";

    public static final String FALTA_CHEQUE =
            "Falta indicar de qué cheque es la verificación. Vuelve a la lista de cheques pendientes y elige uno con «Seleccionar».";

    public static final String FALTA_CHEQUE_A_PREPARAR =
            "No se indicó qué cheque regularizar. Vuelve a la lista de cheques pendientes y elige uno con «Seleccionar».";

    public static final String FALTA_VERIFICACION_A_ANULAR =
            "No se indicó qué verificación anular. Elige una verificación de la lista y vuelve a pulsar «Cancelar».";

    // ===================================================================== //
    //                              NEGOCIO                                  //
    // ===================================================================== //

    public static String bancoNoEncontrado(Long codBanco) {
        return "El banco elegido (código " + codBanco + ") no existe. Es posible que lo hayan eliminado: actualiza la pantalla y elige otro banco.";
    }

    /**
     * La lectura de "cheques por regularizar" no devolvio el cheque y tampoco tiene una verificacion valida: no existe, o
     * no aparece en esa lista porque le falta el banco o el estado registrados.
     */
    public static String chequeNoEncontrado(Long codCheque) {
        return "No se encontró el cheque " + codCheque + " entre los cheques por regularizar: no existe, o no tiene un banco o un "
                + "estado registrados. Actualiza la lista de cheques pendientes y elige otro.";
    }

    public static String chequeYaVerificado(Long codCheque) {
        return "El cheque " + codCheque + " ya tiene una verificación válida. Un cheque solo puede tener una: si la anterior está "
                + "equivocada, anúlala con «Cancelar» y registra otra. Actualiza la lista de cheques pendientes.";
    }

    public static String chequeCerrado(String nroCheque, Long codCheque) {
        return "El cheque número " + textoNro(nroCheque) + " (código " + codCheque + ") está CERRADO: un cheque cerrado ya no se "
                + "regulariza, solo se verifican los cheques pendientes. Elige otro cheque de la lista.";
    }

    public static String verificacionNoEncontrada(Long codvd) {
        return "No se encontró la verificación " + codvd + ". Es posible que ya no exista o que su cheque o su banco hayan "
                + "desaparecido: actualiza la lista.";
    }

    public static final String YA_ESTABA_ANULADA =
            "Esta verificación ya estaba anulada: no hay nada que cambiar.";

    public static String estadoDeChequeNoValido(String recibido, List<String> validos) {
        return "El estado de cheque «" + recibido + "» no es válido para buscar. Elige " + MensajesCheque.oLista(validos)
                + ", o deja «Todos».";
    }

    // ===================================================================== //
    //                              APOYO                                    //
    // ===================================================================== //

    /** El numero tal como esta en el cheque, o "sin número" si no vino. */
    private static String textoNro(String nroCheque) {
        return nroCheque == null || nroCheque.trim().isEmpty() ? "(sin número)" : nroCheque.trim();
    }
}
