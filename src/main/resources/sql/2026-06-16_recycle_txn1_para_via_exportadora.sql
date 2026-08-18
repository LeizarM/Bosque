/* ============================================================================
   Reciclar Txn #1 para PROBAR Vía Exportadora en vivo (QA 2026-06-16)

   Borra la transacción #1 (PENDIENTE, solicitud #2, cotización #2 ACEPTADA,
   SU00015 / BISA / 5.000). Al quedar la solicitud #2 con su cotización aceptada
   y SIN transacción, se puede crear sobre ella una transacción Vía Exportadora
   por el flujo normal (camino INSERT real).

   #1 está limpia: 0 cargos / 0 asientos / 0 participantes; solo 1 log de creación.
   Ejecutar con un usuario de ESCRITURA (claude_ro es de solo lectura).
   ============================================================================ */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idTransaccion = 1 AND estado = 'PENDIENTE')
BEGIN
    ROLLBACK TRANSACTION;
    RAISERROR('Abortado: la Txn #1 no existe o no esta PENDIENTE. No se borro nada.', 16, 1);
END
ELSE
BEGIN
    DELETE FROM tpex_LogEstados               WHERE idTransaccion = 1;
    DELETE FROM tpex_Cargos                   WHERE idTransaccion = 1;
    DELETE FROM tpex_Asientos                 WHERE idTransaccion = 1;
    DELETE FROM tpex_TransaccionParticipantes WHERE idTransaccion = 1;
    DELETE FROM tpex_Transacciones            WHERE idTransaccion = 1;
    COMMIT TRANSACTION;
    PRINT 'OK: Txn #1 eliminada. La solicitud #2 / cotizacion #2 quedan listas para una nueva transaccion.';
END

-- Verificacion: la #1 no debe existir; la cotizacion #2 sigue ACEPTADA y libre.
SELECT (SELECT COUNT(*) FROM tpex_Transacciones WHERE idTransaccion = 1)                         AS txn1_existe,
       (SELECT estado    FROM tpex_Cotizaciones  WHERE idCotizacion = 2)                          AS cot2_estado,
       (SELECT COUNT(*)  FROM tpex_Transacciones WHERE idCotizacion = 2)                          AS cot2_tiene_txn;
