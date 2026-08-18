/* SOFT DELETE - Entidad 8/9: TRANSACCION PARTICIPANTES
   Fase 2 (ABM): accion D -> UPDATE eliminado=1 (mantiene guardas).
   Fase 3 (p_list_tpex_TransaccionParticipantes): filtra pa.eliminado=0 en T y V.
   REQUISITO: 01_schema. Ejecutar con usuario de ESCRITURA. */

ALTER PROCEDURE [dbo].[p_abm_tpex_TransaccionParticipantes]

      @ACCION           VARCHAR(1)
    , @idParticipante   BIGINT         = NULL
    , @idTransaccion    BIGINT         = NULL
    , @tipoParticipante VARCHAR(10)    = NULL   -- EMPRESA / TERCERO
    , @nombre           VARCHAR(100)   = NULL
    , @porcentaje       DECIMAL(8, 4)  = NULL
    , @montoUs          DECIMAL(18, 4) = NULL
    , @montoBs          DECIMAL(18, 4) = NULL
    , @itfUs            DECIMAL(18, 4) = NULL
    , @itfBs            DECIMAL(18, 4) = NULL
    , @observaciones    VARCHAR(500)   = NULL
    , @audUsuario       BIGINT         = NULL
    , @error            INT            = 0   OUTPUT
    , @errormsg         NVARCHAR(500)  = ''  OUTPUT
    , @idGenerado       BIGINT         = 0   OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    SET @error      = 0;
    SET @errormsg   = '';
    SET @idGenerado = 0;

    DECLARE @estadoTrans     VARCHAR(20);
    DECLARE @tcTrans         DECIMAL(10, 6);
    DECLARE @montoConvertido DECIMAL(18, 2);

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idTransaccion, 0) = 0
        BEGIN SET @error = 10; SET @errormsg = 'idTransaccion es obligatorio.'; RETURN; END

        SELECT @estadoTrans = estado, @tcTrans = tipoCambioAplicado
        FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;

        IF @estadoTrans IS NULL
        BEGIN SET @error = 11; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END

        IF @estadoTrans = 'CANCELADO'
        BEGIN SET @error = 12; SET @errormsg = 'No se pueden agregar participantes a una transacción CANCELADA.'; RETURN; END

        IF ISNULL(@tipoParticipante, '') NOT IN ('EMPRESA', 'TERCERO')
        BEGIN SET @error = 13; SET @errormsg = 'tipoParticipante debe ser EMPRESA o TERCERO. Recibido: ' + ISNULL(@tipoParticipante, 'NULL') + '.'; RETURN; END

        IF ISNULL(@nombre, '') = ''
        BEGIN SET @error = 14; SET @errormsg = 'nombre del participante es obligatorio.'; RETURN; END

        IF ISNULL(@montoUs, 0) = 0 AND ISNULL(@montoBs, 0) = 0
        BEGIN SET @error = 15; SET @errormsg = 'Debe proporcionar montoUs o montoBs.'; RETURN; END

        IF ISNULL(@porcentaje, 0) < 0 OR ISNULL(@porcentaje, 0) > 100
        BEGIN SET @error = 16; SET @errormsg = 'porcentaje debe estar entre 0 y 100.'; RETURN; END

        -- Derivar el lado faltante con el TC de la transacción (mismo criterio que asientos)
        IF ISNULL(@tcTrans, 0) > 0
        BEGIN
            IF @montoUs IS NOT NULL AND @montoBs IS NULL SET @montoBs = ROUND(@montoUs * @tcTrans, 4);
            IF @montoBs IS NOT NULL AND @montoUs IS NULL SET @montoUs = ROUND(@montoBs / @tcTrans, 4);
            IF @itfUs   IS NOT NULL AND @itfBs   IS NULL SET @itfBs   = ROUND(@itfUs   * @tcTrans, 4);
            IF @itfBs   IS NOT NULL AND @itfUs   IS NULL SET @itfUs   = ROUND(@itfBs   / @tcTrans, 4);
        END

        -- Porcentaje automático respecto al monto convertido (USD) de la transacción
        IF @porcentaje IS NULL
        BEGIN
            SELECT @montoConvertido = montoConvertido FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;
            IF ISNULL(@montoConvertido, 0) > 0 AND ISNULL(@montoUs, 0) > 0
                SET @porcentaje = ROUND((@montoUs / @montoConvertido) * 100, 4);
        END

        BEGIN TRY
            BEGIN TRANSACTION;
            INSERT INTO tpex_TransaccionParticipantes
                (idTransaccion, tipoParticipante, nombre, porcentaje,
                 montoUs, montoBs, itfUs, itfBs,
                 observaciones, audUsuario, audFecha)
            VALUES
                (@idTransaccion, @tipoParticipante, @nombre, @porcentaje,
                 @montoUs, @montoBs, @itfUs, @itfBs,
                 @observaciones, @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Participante registrado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error      = 99;
            SET @errormsg   = 'Error al insertar participante: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            SET @idGenerado = 0;
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- UPDATE
    -- ================================================================
    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idParticipante, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'idParticipante es obligatorio para actualizar.'; RETURN; END

        DECLARE @idTransActual BIGINT;
        SELECT @idTransActual = idTransaccion FROM tpex_TransaccionParticipantes WHERE idParticipante = @idParticipante;
        IF @idTransActual IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'No existe el participante ' + CAST(@idParticipante AS VARCHAR) + '.'; RETURN; END

        SELECT @estadoTrans = estado, @tcTrans = tipoCambioAplicado
        FROM tpex_Transacciones WHERE idTransaccion = @idTransActual;

        IF @estadoTrans = 'CANCELADO'
        BEGIN SET @error = 32; SET @errormsg = 'No se puede modificar un participante de una transacción CANCELADA.'; RETURN; END

        IF @tipoParticipante IS NOT NULL AND @tipoParticipante NOT IN ('EMPRESA', 'TERCERO')
        BEGIN SET @error = 33; SET @errormsg = 'tipoParticipante debe ser EMPRESA o TERCERO.'; RETURN; END

        IF ISNULL(@tcTrans, 0) > 0
        BEGIN
            IF @montoUs IS NOT NULL AND @montoBs IS NULL SET @montoBs = ROUND(@montoUs * @tcTrans, 4);
            IF @montoBs IS NOT NULL AND @montoUs IS NULL SET @montoUs = ROUND(@montoBs / @tcTrans, 4);
            IF @itfUs   IS NOT NULL AND @itfBs   IS NULL SET @itfBs   = ROUND(@itfUs   * @tcTrans, 4);
            IF @itfBs   IS NOT NULL AND @itfUs   IS NULL SET @itfUs   = ROUND(@itfBs   / @tcTrans, 4);
        END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_TransaccionParticipantes
            SET tipoParticipante = ISNULL(@tipoParticipante, tipoParticipante),
                nombre           = ISNULL(@nombre,           nombre),
                porcentaje       = ISNULL(@porcentaje,       porcentaje),
                montoUs          = ISNULL(@montoUs,          montoUs),
                montoBs          = ISNULL(@montoBs,          montoBs),
                itfUs            = ISNULL(@itfUs,            itfUs),
                itfBs            = ISNULL(@itfBs,            itfBs),
                observaciones    = ISNULL(@observaciones,    observaciones),
                audUsuario       = @audUsuario,
                audFecha         = GETDATE()
            WHERE idParticipante = @idParticipante;
            SET @idGenerado = @idParticipante;
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Participante actualizado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al actualizar participante: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- DELETE
    -- ================================================================
    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idParticipante, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'idParticipante es obligatorio para eliminar.'; RETURN; END

        DECLARE @idTransDel BIGINT;
        SELECT @idTransDel = idTransaccion FROM tpex_TransaccionParticipantes WHERE idParticipante = @idParticipante;
        IF @idTransDel IS NULL
        BEGIN SET @error = 51; SET @errormsg = 'No existe el participante ' + CAST(@idParticipante AS VARCHAR) + '.'; RETURN; END

        SELECT @estadoTrans = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransDel;
        IF @estadoTrans = 'CANCELADO'
        BEGIN SET @error = 52; SET @errormsg = 'No se puede eliminar un participante de una transacción CANCELADA.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_TransaccionParticipantes SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idParticipante = @idParticipante;
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Participante eliminado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al eliminar participante: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    SET @error    = 99;
    SET @errormsg = 'ACCION no válida.';
END

GO


ALTER PROCEDURE [dbo].[p_list_tpex_TransaccionParticipantes]

      @ACCION           VARCHAR(1)  = NULL
    , @idParticipante   BIGINT      = NULL
    , @idTransaccion    BIGINT      = NULL
    , @tipoParticipante VARCHAR(10) = NULL
    , @audUsuario       BIGINT      = NULL
AS
BEGIN
    SET NOCOUNT ON;

    -- ================================================================
    -- T: Participantes de una transacción
    -- ================================================================
    IF @ACCION = 'T'
    BEGIN
        SELECT
             pa.idParticipante
            ,pa.idTransaccion
            ,pa.tipoParticipante
            ,pa.nombre
            ,pa.porcentaje
            ,pa.montoUs
            ,pa.montoBs
            ,pa.itfUs
            ,pa.itfBs
            ,pa.observaciones
            ,t.estado               AS estadoTransaccion
            ,t.fechaTransaccion
            ,t.montoOrigen
            ,t.montoConvertido
            ,pa.audUsuario
            ,pa.audFecha
        FROM tpex_TransaccionParticipantes pa
        JOIN tpex_Transacciones            t ON t.idTransaccion = pa.idTransaccion
        WHERE pa.eliminado = 0 AND pa.idTransaccion = ISNULL(@idTransaccion, 0)
          AND (@tipoParticipante IS NULL OR pa.tipoParticipante = @tipoParticipante)
        ORDER BY pa.idParticipante;
        RETURN;
    END

    -- ================================================================
    -- V: Resumen de cuadre del split
    --    CUADRADO si Σ montoUs = montoConvertido de la transacción
    --    (tolerancia 0.01) y Σ porcentaje ≈ 100 cuando hay porcentajes.
    -- ================================================================
    IF @ACCION = 'V'
    BEGIN
        SELECT
             pa.idTransaccion
            ,t.fechaTransaccion
            ,t.estado                                      AS estadoTransaccion
            ,COUNT(*)                                      AS cantidadParticipantes
            ,ISNULL(SUM(pa.porcentaje), 0)                 AS totalPorcentaje
            ,ISNULL(SUM(pa.montoUs), 0)                    AS totalMontoUs
            ,ISNULL(SUM(pa.montoBs), 0)                    AS totalMontoBs
            ,ISNULL(SUM(pa.itfUs),  0)                     AS totalItfUs
            ,ISNULL(SUM(pa.itfBs),  0)                     AS totalItfBs
            ,t.montoConvertido
            ,ISNULL(SUM(pa.montoUs), 0) - t.montoConvertido AS diferenciaUs
            ,CASE
                WHEN ABS(ISNULL(SUM(pa.montoUs), 0) - t.montoConvertido) < 0.01
                THEN 'CUADRADO'
                ELSE 'DESCUADRADO'
             END                                           AS estadoCuadre
        FROM tpex_TransaccionParticipantes pa
        JOIN tpex_Transacciones            t ON t.idTransaccion = pa.idTransaccion
        WHERE pa.eliminado = 0 AND pa.idTransaccion = ISNULL(@idTransaccion, 0)
        GROUP BY pa.idTransaccion, t.fechaTransaccion, t.estado, t.montoConvertido;
        RETURN;
    END

    -- ACCION no reconocida: vacío, consistente con el resto de SPs de listado
    SELECT TOP 0
         CAST(0 AS BIGINT)    AS idParticipante
        ,CAST('' AS VARCHAR(10)) AS tipoParticipante;
END

GO

SELECT
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_TransaccionParticipantes')  AND definition LIKE '%UPDATE tpex_TransaccionParticipantes SET eliminado = 1%') AS abm_soft_ok,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_TransaccionParticipantes')  AND definition LIKE '%DELETE FROM tpex_TransaccionParticipantes%')                AS abm_delete_residual,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_list_tpex_TransaccionParticipantes') AND definition LIKE '%pa.eliminado = 0%')                                          AS list_filtra_ok;
