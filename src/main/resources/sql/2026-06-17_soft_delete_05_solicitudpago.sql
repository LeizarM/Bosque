/* SOFT DELETE - Entidad 3/9: SOLICITUD PAGO
   Fase 2 (ABM): accion D -> UPDATE eliminado=1 (mantiene guardas).
   Fase 3 (p_list_tpex_SolicitudPago): filtra eliminado=0 en L (sin alias) y B (t1).
   Acciones A/C son OPENQUERY a SAP (no son filas tpex), no se filtran.
   REQUISITO: 01_schema. Ejecutar con usuario de ESCRITURA. */
ALTER PROCEDURE [dbo].[p_abm_tpex_SolicitudPago]
    @ACCION VARCHAR(1)
    , @idSolicitud BIGINT = NULL
    , @codEmpresa INT = NULL
    , @fechaSolicitud DATETIME = NULL
    , @montoTotalSolicitud DECIMAL(18, 2) = NULL
    , @estado VARCHAR(20) = NULL
    , @project VARCHAR(150) = NULL
    , @audUsuario INT = NULL
    , @error INT = 0 OUTPUT
    , @errormsg NVARCHAR(500) = '' OUTPUT
    , @idGenerado BIGINT = 0 OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0; SET @errormsg = ''; SET @idGenerado = 0;

    DECLARE @estadoActual VARCHAR(20);
    DECLARE @countProvAprobados INT;

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@codEmpresa, 0) = 0
        BEGIN SET @error = 20; SET @errormsg = 'Empresa es obligatorio.'; RETURN; END

        SET @estado = 'PENDIENTE';

        BEGIN TRY
            BEGIN TRANSACTION;
            INSERT INTO tpex_SolicitudPago (codEmpresa, fechaSolicitud, montoTotalSolicitud,
                                            estado, project, audUsuario, audFecha)
            VALUES (@codEmpresa, @fechaSolicitud, ISNULL(@montoTotalSolicitud, 0),
                    @estado, @project, @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();

            INSERT INTO dbo.tpex_LogEstados (idSolicitud, idCotizacion, idTransaccion, idSolicitudProveedor, idDetalle,
                                             estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idGenerado, NULL, NULL, NULL, NULL, NULL, 'PENDIENTE', 'Solicitud creada.', @audUsuario, GETDATE());

            COMMIT TRANSACTION;
            SET @errormsg = 'Solicitud creada correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al insertar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
            SET @idGenerado = 0;
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- UPDATE
    -- ================================================================
    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idSolicitud, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'idSolicitud es obligatorio.'; RETURN; END

        SELECT @estadoActual = estado FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud;
        IF @estadoActual IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'No existe la solicitud con idSolicitud = ' + CAST(@idSolicitud AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual IN ('PAGADA', 'RECHAZADA')
        BEGIN SET @error = 32; SET @errormsg = 'No se puede modificar una solicitud con estado ' + @estadoActual + '.'; RETURN; END

        IF ISNULL(@estado, '') != '' AND @estado NOT IN ('PENDIENTE', 'APROBADA', 'PAGADA', 'RECHAZADA')
        BEGIN SET @error = 33; SET @errormsg = 'Estado invalido: ' + @estado + '. Valores permitidos: PENDIENTE, APROBADA, PAGADA, RECHAZADA.'; RETURN; END

        IF ISNULL(@estado, '') != '' AND @estado != @estadoActual
        BEGIN
            IF NOT ((@estadoActual = 'PENDIENTE' AND @estado IN ('APROBADA','RECHAZADA'))
                 OR (@estadoActual = 'APROBADA'  AND @estado IN ('PAGADA','RECHAZADA')))
            BEGIN
                SET @error = 34;
                SET @errormsg = 'Transicion no permitida: ' + @estadoActual + ' -> ' + @estado +
                                '. Permitidas: PENDIENTE->APROBADA, PENDIENTE->RECHAZADA, APROBADA->PAGADA, APROBADA->RECHAZADA.';
                RETURN;
            END

            -- VALIDACION CLAVE: Para pasar a APROBADA, debe haber al menos 1 proveedor APROBADO
            IF @estado = 'APROBADA'
            BEGIN
                SELECT @countProvAprobados = COUNT(*)
                FROM tpex_SolicitudProveedor
                WHERE idSolicitud = @idSolicitud AND estado = 'APROBADO';

                IF @countProvAprobados = 0
                BEGIN
                    SET @error = 35;
                    SET @errormsg = 'No se puede aprobar la solicitud: debe haber al menos 1 proveedor con estado APROBADO.';
                    RETURN;
                END
            END
        END

        IF ISNULL(@codEmpresa, 0) != 0
           AND @codEmpresa != (SELECT codEmpresa FROM dbo.tpex_SolicitudPago WHERE idSolicitud = @idSolicitud)
           AND EXISTS (SELECT 1 FROM dbo.tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitud)
        BEGIN SET @error = 36; SET @errormsg = 'No se puede cambiar la empresa porque la solicitud ya tiene proveedores asociados.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_SolicitudPago
            SET codEmpresa          = ISNULL(@codEmpresa, codEmpresa),
                fechaSolicitud      = ISNULL(@fechaSolicitud, fechaSolicitud),
                montoTotalSolicitud = ISNULL(@montoTotalSolicitud, montoTotalSolicitud),
                estado              = ISNULL(@estado, estado),
                project             = ISNULL(@project, project),
                audUsuario          = @audUsuario,
                audFecha            = GETDATE()
            WHERE idSolicitud = @idSolicitud;

            SET @idGenerado = @idSolicitud;

            IF ISNULL(@estado, '') != '' AND @estado != @estadoActual
            BEGIN
                INSERT INTO dbo.tpex_LogEstados (idSolicitud, idCotizacion, idTransaccion, idSolicitudProveedor, idDetalle,
                                                 estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                VALUES (@idSolicitud, NULL, NULL, NULL, NULL, @estadoActual, @estado, NULL, @audUsuario, GETDATE());
            END

            COMMIT TRANSACTION;
            SET @errormsg = 'Solicitud actualizada correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al actualizar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- DELETE
    -- ================================================================
    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idSolicitud, 0) = 0
        BEGIN SET @error = 40; SET @errormsg = 'idSolicitud es obligatorio.'; RETURN; END

        SELECT @estadoActual = estado FROM dbo.tpex_SolicitudPago WHERE idSolicitud = @idSolicitud;
        IF @estadoActual IS NULL
        BEGIN SET @error = 41; SET @errormsg = 'No existe la solicitud con idSolicitud = ' + CAST(@idSolicitud AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual != 'PENDIENTE'
        BEGIN SET @error = 42; SET @errormsg = 'Solo se pueden eliminar solicitudes en estado PENDIENTE. Estado actual: ' + @estadoActual + '.'; RETURN; END

        IF EXISTS (SELECT 1 FROM dbo.tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitud)
        BEGIN SET @error = 43; SET @errormsg = 'La solicitud tiene proveedores asociados. Elimine primero los proveedores y sus facturas.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE dbo.tpex_SolicitudPago SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idSolicitud = @idSolicitud;
            COMMIT TRANSACTION;
            SET @errormsg = 'Solicitud eliminada correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al eliminar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END
END

GO

ALTER PROCEDURE [dbo].[p_list_tpex_SolicitudPago]
    @idSolicitud bigint = NULL, @codEmpresa int = NULL, @fechaSolicitud datetime = NULL,
    @montoTotalSolicitud decimal(18, 2) = NULL, @estado varchar(20) = NULL,
    @audUsuario int = NULL, @ACCION VARCHAR(1) = NULL,
    @fechaInicio date = NULL, @fechaFin date = NULL,
    @project NVARCHAR(150) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    -- L: Listado basico
    IF @ACCION = 'L'
    BEGIN
        SELECT idSolicitud, codEmpresa, fechaSolicitud, montoTotalSolicitud, estado, project, audUsuario
        FROM tpex_SolicitudPago
        WHERE eliminado = 0 AND (@idSolicitud IS NULL OR @idSolicitud = idSolicitud)
          AND (@codEmpresa IS NULL OR @codEmpresa = codEmpresa)
          AND (@fechaSolicitud IS NULL OR @fechaSolicitud = fechaSolicitud)
          AND (@montoTotalSolicitud IS NULL OR @montoTotalSolicitud = montoTotalSolicitud)
          AND (@estado IS NULL OR @estado = estado)
          AND (@audUsuario IS NULL OR @audUsuario = audUsuario)
          AND (@project IS NULL OR project = @project);
    END

    -- A: Documentos abiertos SAP (con montoTotal)
    IF @ACCION = 'A'
    BEGIN
        DECLARE @sql NVARCHAR(MAX);
        DECLARE @temp TABLE (
            codEmpresa INT, empresa VARCHAR(15), descripcion VARCHAR(50),
            docNum INT, moneda VARCHAR(10), montoTotal DECIMAL(18,2)
        );

        SET @sql = N'SELECT * FROM OPENQUERY(SRV_2022,
            ''EXEC [CONEXION].[dbo].[p_list_PagosExtranjerosDocAbiertos]
                @codEmpresa = ' + CONVERT(VARCHAR(5), @codEmpresa) + ',
                @ACCION = ''''A'''''')';

        BEGIN TRY
            INSERT INTO @temp EXEC sp_executesql @sql;
        END TRY
        BEGIN CATCH
            -- Si SP en SRV_2022 aun no devuelve montoTotal, fallback sin esa columna
            DECLARE @tempLegacy TABLE (codEmpresa INT, empresa VARCHAR(15), descripcion VARCHAR(50), docNum INT, moneda VARCHAR(10));
            INSERT INTO @tempLegacy EXEC sp_executesql @sql;
            INSERT INTO @temp (codEmpresa, empresa, descripcion, docNum, moneda, montoTotal)
            SELECT codEmpresa, empresa, descripcion, docNum, moneda, NULL FROM @tempLegacy;
        END CATCH;

        SELECT * FROM @temp;
    END

    -- B: Listar solicitudes entre fechas (con join completo)
    IF @ACCION = 'B'
    BEGIN
        SELECT t1.idSolicitud, t4.nombre, t1.fechaSolicitud, t1.montoTotalSolicitud, t1.estado, t1.project,
               t2.idSolicitudProveedor, t2.cardCode, t2.cardName, t2.totalFacturasUsd, t2.totalAmortizadoUsd,
               t2.totalAPagarUsd, t2.estado AS estadoProveedor,
               t3.idDetalle, t3.tipoDocumento, t3.numeroDocumento, t3.facturaProvSap, t3.codigoImportacion,
               t3.numeroCuota, t3.montoFacturaUsd, t3.montoAmortizadoUsd, t3.montoAPagarUsd,
               t3.montoTotalDocumento, t3.fechaFactura, t3.fechaVencimiento, t3.concepto, t3.obs,
               t3.esAprobado, t1.codEmpresa
        FROM tpex_SolicitudPago t1
        JOIN tpex_SolicitudProveedor t2 ON t1.idSolicitud = t2.idSolicitud
        JOIN tpex_DetalleSolicitud t3 ON t2.idSolicitudProveedor = t3.idSolicitudProveedor
        JOIN tb_empresa t4 ON t4.codEmpresa = t1.codEmpresa
        WHERE t1.eliminado = 0 AND CAST(t1.fechaSolicitud as date) >= @fechaInicio
          AND CAST(t1.fechaSolicitud as date) <= @fechaFin;
    END

    -- C: Proveedores por proyecto (preserva logica existente)
    IF @ACCION = 'C'
    BEGIN
        DECLARE @sqlProvb NVARCHAR(MAX);
        DECLARE @projectParam NVARCHAR(300);
        DECLARE @tempProvb TABLE (
            codEmpresa INT, empresa VARCHAR(300), descripcion VARCHAR(80),
            docNum INT, moneda VARCHAR(5), project VARCHAR(150), montoTotal DECIMAL(18,2)
        );

        SET @projectParam = CASE WHEN @project IS NULL THEN N'NULL' ELSE N'''''' + REPLACE(@project, N'''', N'''''') + N'''''' END;

        SET @sqlProvb = N'SELECT * FROM OPENQUERY(SRV_2022, ''SET FMTONLY OFF; SET NOCOUNT ON; EXEC [CONEXION].[dbo].[p_list_PagosExtranjerosDocAbiertos] @codEmpresa = '
            + CASE WHEN @codEmpresa IS NULL THEN N'NULL' ELSE CONVERT(VARCHAR(5), @codEmpresa) END
            + N', @project = ' + @projectParam
            + N', @ACCION = ''''B'''''')';

        BEGIN TRY
            INSERT INTO @tempProvb EXEC sp_executesql @sqlProvb;
        END TRY
        BEGIN CATCH
            DECLARE @tempProvbLegacy TABLE (codEmpresa INT, empresa VARCHAR(300), descripcion VARCHAR(80), docNum INT, moneda VARCHAR(5), project VARCHAR(150));
            INSERT INTO @tempProvbLegacy EXEC sp_executesql @sqlProvb;
            INSERT INTO @tempProvb SELECT codEmpresa, empresa, descripcion, docNum, moneda, project, NULL FROM @tempProvbLegacy;
        END CATCH;

        SELECT * FROM @tempProvb ORDER BY project;
    END
END

GO

SELECT
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_SolicitudPago')  AND definition LIKE '%UPDATE dbo.tpex_SolicitudPago SET eliminado = 1%') AS abm_soft_ok,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_SolicitudPago')  AND definition LIKE '%DELETE FROM dbo.tpex_SolicitudPago%')                AS abm_delete_residual,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_list_tpex_SolicitudPago') AND definition LIKE '%eliminado = 0 AND%')                                 AS list_filtra_ok;
