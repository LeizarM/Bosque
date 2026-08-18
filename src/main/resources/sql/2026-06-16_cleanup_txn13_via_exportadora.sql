/* ============================================================================
   Limpieza de la transacción de PRUEBA #13 (Vía Exportadora, QA 2026-06-16)
     #13  EXPORTADORA  (solicitud #2 / cotización #2, ROXCEL TRADING, USD->BOB 5.000)

   Está en estado PENDIENTE; único dependiente: 1 fila en tpex_LogEstados.
   Ejecutar con un usuario de ESCRITURA (claude_ro es de solo lectura).

   NOTA: al borrarla, la solicitud #2 queda con su cotización #2 ACEPTADA y SIN
   transacción (la Txn #1 original ya se había eliminado para la prueba). Eso es
   un estado válido: se le puede crear otra transacción cuando se quiera.
   ============================================================================ */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idTransaccion = 13 AND estado = 'PENDIENTE')
BEGIN
    ROLLBACK TRANSACTION;
    RAISERROR('Abortado: la Txn #13 no existe o no esta PENDIENTE. No se borro nada.', 16, 1);
END
ELSE
BEGIN
    DELETE FROM tpex_LogEstados               WHERE idTransaccion = 13;
    DELETE FROM tpex_Cargos                   WHERE idTransaccion = 13;
    DELETE FROM tpex_Asientos                 WHERE idTransaccion = 13;
    DELETE FROM tpex_TransaccionParticipantes WHERE idTransaccion = 13;
    DELETE FROM tpex_Transacciones            WHERE idTransaccion = 13;
    COMMIT TRANSACTION;
    PRINT 'OK: Txn #13 (Via Exportadora) eliminada.';
END

-- Verificacion: #13 no debe existir.
SELECT idTransaccion, estado FROM tpex_Transacciones WHERE idTransaccion = 13;
