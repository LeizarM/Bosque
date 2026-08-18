/* ============================================================================
   Limpieza de transacciones de PRUEBA (QA 2026-06-16)
     #10  Traspaso Mercury    (tesorería, sin solicitud/cotización/banco)
     #11  Fondeo Mercury       (tesorería, fuente BISA)
     #12  Devolución de Txn #4 (idTransaccionOrigen = 4)

   Las 3 están en estado PENDIENTE. Único dependiente real: 3 filas en
   tpex_LogEstados (log de creación). cargos/asientos/participantes = 0.
   El borrado 'D' del SP NO limpia tpex_LogEstados, por eso se borra a mano.

   Ejecutar con un usuario de ESCRITURA (claude_ro es de solo lectura).
   La transacción #4 (origen de la devolución) NO se toca.
   ============================================================================ */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF EXISTS (SELECT 1 FROM tpex_Transacciones
           WHERE idTransaccion IN (10, 11, 12) AND estado <> 'PENDIENTE')
BEGIN
    ROLLBACK TRANSACTION;
    RAISERROR('Abortado: alguna de 10/11/12 no esta PENDIENTE. No se borro nada.', 16, 1);
END
ELSE
BEGIN
    -- Hijos primero (FK). Defensivo: cargos/asientos/participantes hoy = 0.
    DELETE FROM tpex_LogEstados               WHERE idTransaccion IN (10, 11, 12);
    DELETE FROM tpex_Cargos                   WHERE idTransaccion IN (10, 11, 12);
    DELETE FROM tpex_Asientos                 WHERE idTransaccion IN (10, 11, 12);
    DELETE FROM tpex_TransaccionParticipantes WHERE idTransaccion IN (10, 11, 12);

    -- Padre
    DELETE FROM tpex_Transacciones            WHERE idTransaccion IN (10, 11, 12);

    COMMIT TRANSACTION;
    PRINT 'OK: transacciones 10, 11 y 12 (y sus logs) eliminadas.';
END

-- Verificación: NO debe devolver filas.
SELECT idTransaccion, estado
FROM tpex_Transacciones
WHERE idTransaccion IN (10, 11, 12);
