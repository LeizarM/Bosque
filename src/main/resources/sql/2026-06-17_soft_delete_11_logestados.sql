/* SOFT DELETE - Entidad 9/9: LOG ESTADOS (auditoria de cambios)
   Fase 2 (ABM): accion D -> UPDATE eliminado=1 (mantiene guardas).
   Fase 3 (p_list_tpex_LogEstados): filtra l.eliminado=0 en L, B y F.
   REQUISITO: 01_schema. Ejecutar con usuario de ESCRITURA. */
ALTER PROCEDURE [dbo].[p_abm_tpex_LogEstados]

    -- ── Parámetros de entrada ─────────────────────────────────────────
@ACCION VARCHAR(1)
, @idLog BIGINT = NULL -- requerido en D
, @idSolicitud BIGINT = NULL -- exclusivo
, @idCotizacion BIGINT = NULL -- exclusivo
, @idTransaccion BIGINT = NULL -- exclusivo
, @estadoAnterior VARCHAR(20) = NULL
, @estadoNuevo VARCHAR(20) = NULL -- requerido en I
, @observaciones TEXT = NULL
, @audUsuario INT = NULL

    -- ── Parámetros de salida (patrón SAP B1) ─────────────────────────
, @error INT = 0 OUTPUT
, @errormsg NVARCHAR(500) = '' OUTPUT
, @idGenerado BIGINT = 0 OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    SET @error = 0;
    SET @errormsg = '';
    SET @idGenerado = 0;

    -- ================================================================
    -- INSERT  — único caso de uso real (el log nunca se edita)
    -- ================================================================
    IF @ACCION = 'I'
        BEGIN

            -- V1: Exclusividad FK — exactamente una entidad padre
            DECLARE @fksConValor INT = 0;
            IF ISNULL(@idSolicitud, 0) != 0 SET @fksConValor = @fksConValor + 1;
            IF ISNULL(@idCotizacion, 0) != 0 SET @fksConValor = @fksConValor + 1;
            IF ISNULL(@idTransaccion, 0) != 0 SET @fksConValor = @fksConValor + 1;

            IF @fksConValor != 1
                BEGIN
                    SET @error = 20;
                    SET @errormsg = 'Debe especificar exactamente uno: idSolicitud, idCotizacion o idTransaccion.';
                    RETURN;
                END

            -- V2: estadoNuevo requerido
            --IF ISNULL(@estadoNuevo, '') = ''
            --BEGIN
            --    SET @error    = 21;
            --    SET @errormsg = 'El campo Estado Nuevo es obligatorio.';
            --    RETURN;
            --END

            -- V3: Verificar existencia del padre y obtener estadoAnterior automáticamente
            IF ISNULL(@idSolicitud, 0) != 0
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud)
                        BEGIN
                            SET @error = 22;
                            SET @errormsg =
                                    'No existe la solicitud con idSolicitud = ' + CAST(@idSolicitud AS VARCHAR) + '.';
                            RETURN;
                        END
                    -- Si no viene estadoAnterior, lo tomamos automáticamente de la tabla
                    IF ISNULL(@estadoAnterior, '') = ''
                        SELECT @estadoAnterior = estado FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud;
                END

            IF ISNULL(@idCotizacion, 0) != 0
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM tpex_Cotizaciones WHERE idCotizacion = @idCotizacion)
                        BEGIN
                            SET @error = 23;
                            SET @errormsg =
                                    'No existe la cotización con idCotizacion = ' + CAST(@idCotizacion AS VARCHAR) +
                                    '.';
                            RETURN;
                        END
                    IF ISNULL(@estadoAnterior, '') = ''
                        SELECT @estadoAnterior = estado FROM tpex_Cotizaciones WHERE idCotizacion = @idCotizacion;
                END

            IF ISNULL(@idTransaccion, 0) != 0
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion)
                        BEGIN
                            SET @error = 24;
                            SET @errormsg =
                                    'No existe la transacción con idTransaccion = ' + CAST(@idTransaccion AS VARCHAR) +
                                    '.';
                            RETURN;
                        END
                    IF ISNULL(@estadoAnterior, '') = ''
                        SELECT @estadoAnterior = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;
                END

            -- V4: estadoAnterior != estadoNuevo (no tiene sentido loguear el mismo estado)
            IF @estadoAnterior = @estadoNuevo
                BEGIN
                    SET @error = 25;
                    SET @errormsg =
                            'El Estado Nuevo (' + @estadoNuevo + ') es igual al Estado Anterior: ' + @estadoAnterior +
                            ' .No hay cambio que registrar.';
                    RETURN;
                END

            BEGIN TRY
                BEGIN TRANSACTION;

                INSERT INTO tpex_LogEstados (idSolicitud, idCotizacion, idTransaccion,
                                             estadoAnterior, estadoNuevo,
                                             observaciones, audUsuario, audFecha)
                VALUES (NULLIF(@idSolicitud, 0),
                        NULLIF(@idCotizacion, 0),
                        NULLIF(@idTransaccion, 0),
                        @estadoAnterior, @estadoNuevo,
                        @observaciones, @audUsuario, GETDATE());

                SET @idGenerado = SCOPE_IDENTITY();

                COMMIT TRANSACTION;
                SET @error = 0;
                SET @errormsg =
                        'Cambio de estado registrado: ' + ISNULL(@estadoAnterior, 'NULL') + ' → ' + @estadoNuevo + '.';
            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
                SET @error = 99;
                SET @errormsg = 'Error al insertar: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
                SET @idGenerado = 0;
            END CATCH;

            RETURN;
        END

    -- ================================================================
    -- DELETE  — solo para corrección de errores (rol admin)
    -- ================================================================
    IF @ACCION = 'D'
        BEGIN

            IF ISNULL(@idLog, 0) = 0
                BEGIN
                    SET @error = 30;
                    SET @errormsg = 'El campo idLog es obligatorio para eliminar.';
                    RETURN;
                END

            IF NOT EXISTS (SELECT 1 FROM tpex_LogEstados WHERE idLog = @idLog)
                BEGIN
                    SET @error = 31;
                    SET @errormsg = 'No existe el log con idLog = ' + CAST(@idLog AS VARCHAR) + '.';
                    RETURN;
                END

            BEGIN TRY
                BEGIN TRANSACTION;
                UPDATE tpex_LogEstados SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idLog = @idLog;
                COMMIT TRANSACTION;
                SET @error = 0;
                SET @errormsg = 'Registro de log eliminado.';
            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
                SET @error = 99;
                SET @errormsg = 'Error al eliminar: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            END CATCH;

            RETURN;
        END

    -- UPDATE no existe — el log es inmutable por diseño
    IF @ACCION = 'U'
        BEGIN
            SET @error = 90;
            SET @errormsg = 'El log de estados es inmutable. No se permite Actualizar.';
            RETURN;
        END

END

GO

/* ============================================================================
   Author: Marcelo Jaimes
   ============================================================================ */

ALTER PROCEDURE [dbo].[p_list_tpex_LogEstados]

      @ACCION          VARCHAR(1)   = NULL
    , @idLog           BIGINT       = NULL
    , @idSolicitud     BIGINT       = NULL
    , @idCotizacion    BIGINT       = NULL
    , @idTransaccion   BIGINT       = NULL
    , @estadoAnterior  VARCHAR(50)  = NULL
    , @estadoNuevo     VARCHAR(50)  = NULL
    , @observaciones   VARCHAR(255) = NULL
    , @audUsuario      BIGINT       = NULL

    , @fechaInicio     DATE         = NULL
    , @fechaFin        DATE         = NULL

AS
BEGIN
    SET NOCOUNT ON;

    -- ================================================================
    -- L: Historial completo de una entidad (timeline de estados)
    -- ================================================================
    IF @ACCION = 'L'
    BEGIN
        SELECT
             l.idLog
            ,l.idSolicitud
            ,l.idCotizacion
            ,l.idTransaccion
            ,CASE
                WHEN l.idSolicitud    IS NOT NULL THEN 'SOLICITUD'
                WHEN l.idCotizacion   IS NOT NULL THEN 'COTIZACION'
                WHEN l.idTransaccion  IS NOT NULL THEN 'TRANSACCION'
             END                    AS tipoEntidad
            ,l.estadoAnterior
            ,l.estadoNuevo
            ,l.observaciones
            ,l.audUsuario
            ,COALESCE(
                NULLIF(LTRIM(RTRIM(
                    ISNULL(per.nombres,'')   + ' ' +
                    ISNULL(per.apPaterno,'') + ' ' +
                    ISNULL(per.apMaterno,'')
                )), ''),
                us.login,
                'Usuario #' + CAST(l.audUsuario AS VARCHAR(20))
             )                      AS nombreUsuario
            ,l.audFecha
        FROM tpex_LogEstados l
        LEFT JOIN tb_usuario  us  ON us.codUsuario   = l.audUsuario
        LEFT JOIN tb_empleado emp ON emp.codEmpleado = us.codEmpleado
        LEFT JOIN trh_persona per ON per.codPersona  = emp.codPersona
        WHERE l.eliminado = 0 AND (@idSolicitud   IS NULL OR l.idSolicitud   = @idSolicitud)
          AND (@idCotizacion  IS NULL OR l.idCotizacion  = @idCotizacion)
          AND (@idTransaccion IS NULL OR l.idTransaccion = @idTransaccion)
        ORDER BY l.audFecha ASC  -- cronológico: primer cambio arriba
    END

    -- ================================================================
    -- B: Reporte entre fechas de todos los cambios de estado
    -- ================================================================
    IF @ACCION = 'B'
    BEGIN
        SELECT
             l.idLog
            ,CASE
                WHEN l.idSolicitud   IS NOT NULL THEN 'SOLICITUD'
                WHEN l.idCotizacion  IS NOT NULL THEN 'COTIZACION'
                WHEN l.idTransaccion IS NOT NULL THEN 'TRANSACCION'
             END                    AS tipoEntidad
            ,COALESCE(
                CAST(l.idSolicitud   AS VARCHAR),
                CAST(l.idCotizacion  AS VARCHAR),
                CAST(l.idTransaccion AS VARCHAR)
             )                      AS idEntidad
            ,l.estadoAnterior
            ,l.estadoNuevo
            ,l.observaciones
            ,l.audUsuario
            ,COALESCE(
                NULLIF(LTRIM(RTRIM(
                    ISNULL(per.nombres,'')   + ' ' +
                    ISNULL(per.apPaterno,'') + ' ' +
                    ISNULL(per.apMaterno,'')
                )), ''),
                us.login,
                'Usuario #' + CAST(l.audUsuario AS VARCHAR(20))
             )                      AS nombreUsuario
            ,l.audFecha
        FROM tpex_LogEstados l
        LEFT JOIN tb_usuario  us  ON us.codUsuario   = l.audUsuario
        LEFT JOIN tb_empleado emp ON emp.codEmpleado = us.codEmpleado
        LEFT JOIN trh_persona per ON per.codPersona  = emp.codPersona
        WHERE l.eliminado = 0 AND (@fechaInicio IS NULL OR CAST(l.audFecha AS DATE) >= @fechaInicio)
          AND (@fechaFin    IS NULL OR CAST(l.audFecha AS DATE) <= @fechaFin)
        ORDER BY l.audFecha DESC
    END

    -- ================================================================
    -- F: Timeline completo de una solicitud (solicitud + cotiz + trx)
    --    Todos los cambios relacionados en orden cronológico
    -- ================================================================
    IF @ACCION = 'F'
    BEGIN
        SELECT
             l.idLog
            ,CASE
                WHEN l.idSolicitud   IS NOT NULL THEN 'SOLICITUD'
                WHEN l.idCotizacion  IS NOT NULL THEN 'COTIZACION'
                WHEN l.idTransaccion IS NOT NULL THEN 'TRANSACCION'
             END                    AS tipoEntidad
            ,COALESCE(
                CAST(l.idSolicitud   AS VARCHAR),
                CAST(l.idCotizacion  AS VARCHAR),
                CAST(l.idTransaccion AS VARCHAR)
             )                      AS idEntidad
            ,l.estadoAnterior
            ,l.estadoNuevo
            ,l.observaciones
            ,l.audUsuario
            ,COALESCE(
                NULLIF(LTRIM(RTRIM(
                    ISNULL(per.nombres,'')   + ' ' +
                    ISNULL(per.apPaterno,'') + ' ' +
                    ISNULL(per.apMaterno,'')
                )), ''),
                us.login,
                'Usuario #' + CAST(l.audUsuario AS VARCHAR(20))
             )                      AS nombreUsuario
            ,l.audFecha
        FROM tpex_LogEstados l
        LEFT JOIN tb_usuario  us  ON us.codUsuario   = l.audUsuario
        LEFT JOIN tb_empleado emp ON emp.codEmpleado = us.codEmpleado
        LEFT JOIN trh_persona per ON per.codPersona  = emp.codPersona
        WHERE l.eliminado = 0 AND l.idSolicitud = @idSolicitud  -- cambios de la solicitud
           OR l.idCotizacion IN (           -- cambios de sus cotizaciones
                SELECT idCotizacion FROM tpex_Cotizaciones WHERE idSolicitud = @idSolicitud
              )
           OR l.idTransaccion IN (          -- cambios de sus transacciones
                SELECT idTransaccion FROM tpex_Transacciones WHERE idSolicitud = @idSolicitud
              )
        ORDER BY l.audFecha ASC
    END

END

GO

SELECT
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_LogEstados')  AND definition LIKE '%UPDATE tpex_LogEstados SET eliminado = 1%') AS abm_soft_ok,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_LogEstados')  AND definition LIKE '%DELETE FROM tpex_LogEstados%')                AS abm_delete_residual,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_list_tpex_LogEstados') AND definition LIKE '%l.eliminado = 0%')                            AS list_filtra_ok;
