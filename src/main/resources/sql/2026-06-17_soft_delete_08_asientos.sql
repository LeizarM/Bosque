/* SOFT DELETE - Entidad 6/9: ASIENTOS
   Fase 2 (ABM): accion D -> UPDATE eliminado=1 (mantiene guardas).
   Fase 3 (p_list_tpex_Asientos): filtra a.eliminado=0 en L, T y V.
   REQUISITO: 01_schema. Ejecutar con usuario de ESCRITURA. */

ALTER PROCEDURE [dbo].[p_abm_tpex_Asientos]
      @ACCION        VARCHAR(1)
    , @idAsiento     BIGINT          = NULL
    , @idTransaccion BIGINT          = NULL
    , @numero        TINYINT         = NULL
    , @tipoAsiento   CHAR(2)         = NULL    -- PR / PE
    , @codBancoRef   INT             = NULL
    , @cuentaDebe    NVARCHAR(200)   = NULL
    , @cuentaHaber   NVARCHAR(200)   = NULL
    , @descripcion   NVARCHAR(500)   = NULL
    , @debitoUs      DECIMAL(18,4)   = NULL
    , @creditoUs     DECIMAL(18,4)   = NULL
    , @debitoBs      DECIMAL(18,4)   = NULL
    , @creditoBs     DECIMAL(18,4)   = NULL
    , @tcAplicado    DECIMAL(10,6)   = NULL
    , @audUsuario    BIGINT          = NULL
    , @error         INT             = 0   OUTPUT
    , @errormsg      NVARCHAR(500)   = ''  OUTPUT
    , @idGenerado    BIGINT          = 0   OUTPUT
AS
BEGIN
    SET NOCOUNT ON

    SET @error      = 0
    SET @errormsg   = ''
    SET @idGenerado = 0

    DECLARE @estadoTrans   VARCHAR(20)
    DECLARE @tcTrans       DECIMAL(10,6)
    DECLARE @tcFinal       DECIMAL(10,6)
    DECLARE @totalDebBs    DECIMAL(18,4)
    DECLARE @totalCreBs    DECIMAL(18,4)
    DECLARE @diferencia    DECIMAL(18,4)

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idTransaccion, 0) = 0
        BEGIN SET @error = 10; SET @errormsg = 'idTransaccion es obligatorio.'; RETURN; END

        SELECT @estadoTrans = estado,
               @tcTrans     = tipoCambioAplicado
        FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;

        IF @estadoTrans IS NULL
        BEGIN SET @error = 11; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END

        -- ► FIX: CONFIRMADO ya no bloquea (solo CANCELADO)
        IF @estadoTrans IN ('CANCELADO')
        BEGIN SET @error = 12; SET @errormsg = 'No se pueden agregar asientos a una transacción en estado ' + @estadoTrans + '.'; RETURN; END

        IF ISNULL(@numero, 0) = 0
        BEGIN SET @error = 13; SET @errormsg = 'numero de asiento es obligatorio (1, 2, 3…).'; RETURN; END

        -- ► FIX: MP eliminado como tipo válido
        IF ISNULL(@tipoAsiento, '') NOT IN ('PR','PE')
        BEGIN SET @error = 14; SET @errormsg = 'tipoAsiento debe ser PR o PE. Recibido: ' + ISNULL(@tipoAsiento,'NULL') + '.'; RETURN; END

        IF ISNULL(@debitoUs, 0) = 0 AND ISNULL(@debitoBs, 0) = 0
           AND ISNULL(@creditoUs, 0) = 0 AND ISNULL(@creditoBs, 0) = 0
        BEGIN SET @error = 15; SET @errormsg = 'Debe proporcionar al menos debitoUs, debitoBs, creditoUs o creditoBs.'; RETURN; END

        IF @codBancoRef IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tch_banco WHERE codBanco = @codBancoRef)
        BEGIN SET @error = 16; SET @errormsg = 'No existe el banco con codBanco = ' + CAST(@codBancoRef AS VARCHAR) + '.'; RETURN; END

        SET @tcFinal = ISNULL(@tcAplicado, @tcTrans);
        IF ISNULL(@tcFinal, 0) = 0
        BEGIN SET @error = 17; SET @errormsg = 'No se pudo determinar el tipo de cambio. Envíe @tcAplicado.'; RETURN; END

        IF @debitoUs  IS NOT NULL AND @debitoBs  IS NULL SET @debitoBs  = ROUND(@debitoUs  * @tcFinal, 4);
        IF @creditoUs IS NOT NULL AND @creditoBs IS NULL SET @creditoBs = ROUND(@creditoUs * @tcFinal, 4);
        IF @debitoBs  IS NOT NULL AND @debitoUs  IS NULL SET @debitoUs  = ROUND(@debitoBs  / @tcFinal, 4);
        IF @creditoBs IS NOT NULL AND @creditoUs IS NULL SET @creditoUs = ROUND(@creditoBs / @tcFinal, 4);

        BEGIN TRY
            BEGIN TRANSACTION;
            INSERT INTO tpex_Asientos
                (idTransaccion, numero, tipoAsiento, codBancoRef,
                 cuentaDebe, cuentaHaber, descripcion,
                 debitoUs, creditoUs, debitoBs, creditoBs,
                 tcAplicado, audUsuario, audFecha)
            VALUES
                (@idTransaccion, @numero, @tipoAsiento, @codBancoRef,
                 @cuentaDebe, @cuentaHaber, @descripcion,
                 @debitoUs, @creditoUs, @debitoBs, @creditoBs,
                 @tcFinal, @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Asiento registrado. debitoBs=' + CAST(ISNULL(@debitoBs,0) AS VARCHAR(30)) + ' | creditoBs=' + CAST(ISNULL(@creditoBs,0) AS VARCHAR(30));
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al insertar asiento: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            SET @idGenerado = 0;
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- UPDATE
    -- ================================================================
    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idAsiento, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'idAsiento es obligatorio para actualizar.'; RETURN; END

        DECLARE @idTransActual BIGINT;
        SELECT @idTransActual = idTransaccion FROM tpex_Asientos WHERE idAsiento = @idAsiento;
        IF @idTransActual IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'No existe el asiento ' + CAST(@idAsiento AS VARCHAR) + '.'; RETURN; END

        SELECT @estadoTrans = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransActual;

        -- ► FIX: CONFIRMADO ya no bloquea (solo CANCELADO)
        IF @estadoTrans IN ('CANCELADO')
        BEGIN SET @error = 32; SET @errormsg = 'No se puede modificar un asiento de una transacción en estado ' + @estadoTrans + '.'; RETURN; END

        IF @codBancoRef IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tch_banco WHERE codBanco = @codBancoRef)
        BEGIN SET @error = 33; SET @errormsg = 'No existe el banco con codBanco = ' + CAST(@codBancoRef AS VARCHAR) + '.'; RETURN; END

        DECLARE @tcActual DECIMAL(10,6);
        SELECT @tcActual = tcAplicado FROM tpex_Asientos WHERE idAsiento = @idAsiento;
        SET @tcFinal = ISNULL(@tcAplicado, @tcActual);

        IF @debitoUs  IS NOT NULL SET @debitoBs  = ROUND(@debitoUs  * @tcFinal, 4);
        IF @creditoUs IS NOT NULL SET @creditoBs = ROUND(@creditoUs * @tcFinal, 4);
        IF @debitoBs  IS NOT NULL AND @debitoUs  IS NULL SET @debitoUs  = ROUND(@debitoBs  / @tcFinal, 4);
        IF @creditoBs IS NOT NULL AND @creditoUs IS NULL SET @creditoUs = ROUND(@creditoBs / @tcFinal, 4);

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_Asientos
            SET numero      = ISNULL(@numero,      numero),
                tipoAsiento = ISNULL(@tipoAsiento, tipoAsiento),
                codBancoRef = ISNULL(@codBancoRef, codBancoRef),
                cuentaDebe  = ISNULL(@cuentaDebe,  cuentaDebe),
                cuentaHaber = ISNULL(@cuentaHaber, cuentaHaber),
                descripcion = ISNULL(@descripcion, descripcion),
                debitoUs    = ISNULL(@debitoUs,    debitoUs),
                creditoUs   = ISNULL(@creditoUs,   creditoUs),
                debitoBs    = ISNULL(@debitoBs,    debitoBs),
                creditoBs   = ISNULL(@creditoBs,   creditoBs),
                tcAplicado  = ISNULL(@tcAplicado,  tcAplicado),
                audUsuario  = @audUsuario,
                audFecha    = GETDATE()
            WHERE idAsiento = @idAsiento;
            SET @idGenerado = @idAsiento;
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Asiento actualizado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al actualizar asiento: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- DELETE
    -- ================================================================
    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idAsiento, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'idAsiento es obligatorio para eliminar.'; RETURN; END

        DECLARE @idTransDel BIGINT;
        SELECT @idTransDel = idTransaccion FROM tpex_Asientos WHERE idAsiento = @idAsiento;
        IF @idTransDel IS NULL
        BEGIN SET @error = 51; SET @errormsg = 'No existe el asiento ' + CAST(@idAsiento AS VARCHAR) + '.'; RETURN; END

        SELECT @estadoTrans = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransDel;

        -- ► FIX: CONFIRMADO ya no bloquea (solo CANCELADO)
        IF @estadoTrans IN ('CANCELADO')
        BEGIN SET @error = 52; SET @errormsg = 'No se puede eliminar un asiento de una transacción en estado ' + @estadoTrans + '.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_Asientos SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idAsiento = @idAsiento;
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Asiento eliminado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al eliminar asiento: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- VALIDAR CUADRE
    -- ================================================================
    IF @ACCION = 'V'
    BEGIN
        IF ISNULL(@idTransaccion, 0) = 0
        BEGIN SET @error = 70; SET @errormsg = 'idTransaccion es obligatorio para validar cuadre.'; RETURN; END

        IF NOT EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion)
        BEGIN SET @error = 71; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END

        SELECT @totalDebBs = ISNULL(SUM(debitoBs),  0),
               @totalCreBs = ISNULL(SUM(creditoBs), 0)
        FROM tpex_Asientos WHERE idTransaccion = @idTransaccion;

        SET @diferencia = @totalDebBs - @totalCreBs;

        IF ABS(@diferencia) < 0.01
        BEGIN
            SET @error    = 0;
            SET @errormsg = 'Asientos cuadrados OK. TotalDébito=' + CAST(@totalDebBs AS VARCHAR(30))
                          + ' | TotalCrédito=' + CAST(@totalCreBs AS VARCHAR(30)) + ' Bs.';
        END
        ELSE
        BEGIN
            SET @error    = 80;
            SET @errormsg = 'Asientos DESCUADRADOS. Diferencia=' + CAST(@diferencia AS VARCHAR(30))
                          + ' Bs. (Débito=' + CAST(@totalDebBs AS VARCHAR(30))
                          + ' | Crédito=' + CAST(@totalCreBs AS VARCHAR(30)) + ')';
        END
        RETURN;
    END

    SET @error    = 99;
    SET @errormsg = 'ACCION no válida.';
END

GO

 
ALTER PROCEDURE [dbo].[p_list_tpex_Asientos]
      @ACCION          VARCHAR(1)    = NULL
    , @idAsiento       BIGINT        = NULL
    , @idTransaccion   BIGINT        = NULL
    , @tipoAsiento     CHAR(2)       = NULL   -- filtro opcional: PR / PE / MP
    , @audUsuario      BIGINT        = NULL
AS
BEGIN
    SET NOCOUNT ON;
 
    -- ================================================================
    -- L: Registro individual (o todos si idAsiento = 0)
    -- ================================================================
    IF @ACCION = 'L'
    BEGIN
        SELECT
             a.idAsiento
            ,a.idTransaccion
            ,a.numero
            ,a.tipoAsiento
            ,a.codBancoRef
            ,b.nombre               AS banco
            ,a.cuentaDebe
            ,a.cuentaHaber
            ,a.descripcion
            ,a.debitoUs
            ,a.creditoUs
            ,a.debitoBs
            ,a.creditoBs
            ,a.tcAplicado
            ,t.estado               AS estadoTransaccion
            ,t.fechaTransaccion
            ,a.audUsuario
            ,a.audFecha
        FROM tpex_Asientos          a
        JOIN tpex_Transacciones     t  ON t.idTransaccion = a.idTransaccion
        LEFT JOIN tch_banco         b  ON b.codBanco      = a.codBancoRef
        WHERE a.eliminado = 0 AND (ISNULL(@idAsiento, 0)     = 0 OR a.idAsiento     = @idAsiento)
          AND (ISNULL(@idTransaccion, 0) = 0 OR a.idTransaccion = @idTransaccion)
          AND (@tipoAsiento IS NULL OR a.tipoAsiento = @tipoAsiento)
        ORDER BY a.idTransaccion, a.numero;
        RETURN;
    END
 
    -- ================================================================
    -- T: Todos los asientos de una transacción (vista contable completa)
    -- ================================================================
    IF @ACCION = 'T'
    BEGIN
        IF ISNULL(@idTransaccion, 0) = 0
        BEGIN
            -- Retorna vacío si no se pasa idTransaccion
            SELECT TOP 0
                 CAST(0 AS BIGINT)       AS idAsiento
                ,CAST(0 AS BIGINT)       AS idTransaccion
                ,CAST(0 AS TINYINT)      AS numero
                ,CAST('' AS CHAR(2))     AS tipoAsiento
                ,CAST(NULL AS INT)       AS codBancoRef
                ,CAST('' AS VARCHAR(100))AS banco
                ,CAST('' AS NVARCHAR(200)) AS cuentaDebe
                ,CAST('' AS NVARCHAR(200)) AS cuentaHaber
                ,CAST('' AS NVARCHAR(500)) AS descripcion
                ,CAST(0 AS DECIMAL(18,4))  AS debitoUs
                ,CAST(0 AS DECIMAL(18,4))  AS creditoUs
                ,CAST(0 AS DECIMAL(18,4))  AS debitoBs
                ,CAST(0 AS DECIMAL(18,4))  AS creditoBs
                ,CAST(0 AS DECIMAL(10,6))  AS tcAplicado
                ,CAST('' AS VARCHAR(20))   AS estadoTransaccion
                ,CAST(NULL AS DATE)        AS fechaTransaccion
                ,CAST(0 AS BIGINT)         AS audUsuario
                ,CAST(NULL AS DATETIME)    AS audFecha;
            RETURN;
        END
 
        SELECT
             a.idAsiento
            ,a.idTransaccion
            ,a.numero
            ,a.tipoAsiento
            ,a.codBancoRef
            ,b.nombre               AS banco
            ,a.cuentaDebe
            ,a.cuentaHaber
            ,a.descripcion
            ,a.debitoUs
            ,a.creditoUs
            ,a.debitoBs
            ,a.creditoBs
            ,a.tcAplicado
            ,t.estado               AS estadoTransaccion
            ,t.fechaTransaccion
            ,a.audUsuario
            ,a.audFecha
        FROM tpex_Asientos          a
        JOIN tpex_Transacciones     t  ON t.idTransaccion = a.idTransaccion
        LEFT JOIN tch_banco         b  ON b.codBanco      = a.codBancoRef
        WHERE a.eliminado = 0 AND a.idTransaccion = @idTransaccion
          AND (@tipoAsiento IS NULL OR a.tipoAsiento = @tipoAsiento)
        ORDER BY a.numero;
        RETURN;
    END
 
    -- ================================================================
    -- V: Resumen de cuadre de asientos de una transacción
    --    Muestra totales y si están cuadrados
    -- ================================================================
    IF @ACCION = 'V'
    BEGIN
        SELECT
             a.idTransaccion
            ,t.fechaTransaccion
            ,t.estado                                       AS estadoTransaccion
            ,COUNT(*)                                       AS cantidadAsientos
            ,ISNULL(SUM(a.debitoUs),  0)                   AS totalDebitoUs
            ,ISNULL(SUM(a.creditoUs), 0)                   AS totalCreditoUs
            ,ISNULL(SUM(a.debitoBs),  0)                   AS totalDebitoBs
            ,ISNULL(SUM(a.creditoBs), 0)                   AS totalCreditoBs
            ,ISNULL(SUM(a.debitoBs),  0)
             - ISNULL(SUM(a.creditoBs), 0)                 AS diferenciaBs
            ,CASE
                WHEN ABS(ISNULL(SUM(a.debitoBs), 0)
                       - ISNULL(SUM(a.creditoBs), 0)) < 0.01
                THEN 'CUADRADO'
                ELSE 'DESCUADRADO'
             END                                            AS estadoCuadre
            ,MAX(a.tcAplicado)                              AS tcAplicado
        FROM tpex_Asientos          a
        JOIN tpex_Transacciones     t ON t.idTransaccion = a.idTransaccion
        WHERE a.eliminado = 0 AND a.idTransaccion = @idTransaccion
        GROUP BY a.idTransaccion, t.fechaTransaccion, t.estado;
        RETURN;
    END
 
    -- ACCION no reconocida — retorna vacío sin error para mantener
    -- consistencia con el patrón del resto de SPs de listado
    SELECT TOP 0
         CAST(0 AS BIGINT)   AS idAsiento
        ,CAST('' AS CHAR(2)) AS tipoAsiento;
 
END

GO

SELECT
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_Asientos')  AND definition LIKE '%UPDATE tpex_Asientos SET eliminado = 1%') AS abm_soft_ok,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_Asientos')  AND definition LIKE '%DELETE FROM tpex_Asientos%')                AS abm_delete_residual,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_list_tpex_Asientos') AND definition LIKE '%a.eliminado = 0%')                          AS list_filtra_ok;
