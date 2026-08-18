/* SOFT DELETE - Entidad 5/9: DETALLE SOLICITUD (facturas)
   Fase 2 (ABM): accion D -> UPDATE eliminado=1 (mantiene guardas).
   Fase 3 (p_list_tpex_DetalleSolicitud): filtra eliminado=0 en L (sin alias).
   Accion A es OPENQUERY a SAP (facturas disponibles), no se filtra.
   REQUISITO: 01_schema. Ejecutar con usuario de ESCRITURA. */
ALTER PROCEDURE [dbo].[p_abm_tpex_DetalleSolicitud]
    @ACCION VARCHAR(1)
    , @idDetalle BIGINT = NULL
    , @idSolicitudProveedor BIGINT = NULL
    , @tipoDocumento VARCHAR(50) = NULL
    , @numeroDocumento VARCHAR(250) = NULL
    , @facturaProvSap INT = NULL
    , @codigoImportacion VARCHAR(50) = NULL
    , @montoFacturaUsd DECIMAL(18, 2) = NULL
    , @montoAmortizadoUsd DECIMAL(18, 2) = NULL
    , @montoAPagarUsd DECIMAL(18, 2) = NULL
    , @montoTotalDocumento DECIMAL(18, 2) = NULL
    , @numeroCuota INT = NULL
    , @fechaFactura DATETIME = NULL
    , @fechaVencimiento DATETIME = NULL
    , @concepto VARCHAR(255) = NULL
    , @obs VARCHAR(255) = NULL
    , @esAprobado BIT = NULL
    , @audUsuario INT = NULL
    , @error INT = 0 OUTPUT
    , @errormsg NVARCHAR(500) = '' OUTPUT
    , @idGenerado BIGINT = 0 OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0; SET @errormsg = ''; SET @idGenerado = 0;

    DECLARE @estadoSolicitud   VARCHAR(20);
    DECLARE @estadoProveedor   VARCHAR(20);
    DECLARE @esAprobadoActual  BIT;
    DECLARE @idSolicitudProvActual BIGINT;
    DECLARE @idSolicitudActual BIGINT;
    DECLARE @facturaSapActual  INT;
    DECLARE @sumOtrasCuotas    DECIMAL(18, 2);
    DECLARE @maxCuota          INT;
    DECLARE @countNoAprobadas  INT;

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 20; SET @errormsg = 'El campo idSolicitudProveedor es obligatorio.'; RETURN; END

        SELECT @estadoProveedor = sp.estado, @estadoSolicitud = s.estado, @idSolicitudActual = s.idSolicitud
        FROM tpex_SolicitudProveedor sp
        JOIN tpex_SolicitudPago s ON s.idSolicitud = sp.idSolicitud
        WHERE sp.idSolicitudProveedor = @idSolicitudProveedor;

        IF @estadoSolicitud IS NULL
        BEGIN SET @error = 21; SET @errormsg = 'No existe el proveedor con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoSolicitud IN ('PAGADA','RECHAZADA')
        BEGIN SET @error = 22; SET @errormsg = 'No se pueden agregar cuotas a una solicitud ' + @estadoSolicitud + '.'; RETURN; END

        IF @estadoProveedor IN ('APROBADO','RECHAZADO')
        BEGIN SET @error = 23; SET @errormsg = 'El proveedor esta ' + @estadoProveedor + '. No se pueden agregar mas cuotas.'; RETURN; END

        IF ISNULL(@montoFacturaUsd, 0) <= 0
        BEGIN SET @error = 24; SET @errormsg = 'El Monto Factura USD es obligatorio y debe ser mayor a cero.'; RETURN; END

        SET @montoAmortizadoUsd = ISNULL(@montoAmortizadoUsd, 0);
        IF @montoAmortizadoUsd > @montoFacturaUsd
        BEGIN SET @error = 25; SET @errormsg = 'El Monto Amortizado USD no puede ser mayor al Monto Factura USD.'; RETURN; END

        IF @fechaFactura IS NOT NULL AND @fechaVencimiento IS NOT NULL AND @fechaVencimiento < @fechaFactura
        BEGIN SET @error = 26; SET @errormsg = 'La Fecha Vencimiento no puede ser anterior a la Fecha Factura.'; RETURN; END

        -- Calcular montoAPagar de esta cuota
        SET @montoAPagarUsd = ISNULL(@montoAPagarUsd, @montoFacturaUsd - @montoAmortizadoUsd);

        -- Auto-asignar numeroCuota si no viene
        IF @numeroCuota IS NULL OR @numeroCuota = 0
        BEGIN
            SELECT @maxCuota = ISNULL(MAX(numeroCuota), 0)
            FROM tpex_DetalleSolicitud
            WHERE idSolicitudProveedor = @idSolicitudProveedor
              AND ISNULL(facturaProvSap, 0) = ISNULL(@facturaProvSap, 0);
            SET @numeroCuota = @maxCuota + 1;
        END

        -- Validar SUM cuotas mismo doc <= montoTotalDocumento (si vino el dato)
        IF ISNULL(@montoTotalDocumento, 0) > 0 AND ISNULL(@facturaProvSap, 0) > 0
        BEGIN
            SELECT @sumOtrasCuotas = ISNULL(SUM(montoAPagarUsd), 0)
            FROM tpex_DetalleSolicitud
            WHERE idSolicitudProveedor = @idSolicitudProveedor
              AND facturaProvSap = @facturaProvSap;

            IF (@sumOtrasCuotas + @montoAPagarUsd) > @montoTotalDocumento
            BEGIN
                SET @error = 27;
                SET @errormsg = 'La suma de cuotas (' + CAST(@sumOtrasCuotas + @montoAPagarUsd AS VARCHAR) +
                                ') supera el monto total del documento (' + CAST(@montoTotalDocumento AS VARCHAR) + ').';
                RETURN;
            END
        END

        -- Validar duplicado de numeroCuota
        IF EXISTS (SELECT 1 FROM tpex_DetalleSolicitud
                   WHERE idSolicitudProveedor = @idSolicitudProveedor
                     AND ISNULL(facturaProvSap, 0) = ISNULL(@facturaProvSap, 0)
                     AND numeroCuota = @numeroCuota)
        BEGIN SET @error = 28; SET @errormsg = 'Ya existe la cuota #' + CAST(@numeroCuota AS VARCHAR) + ' para este documento.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            INSERT INTO tpex_DetalleSolicitud (idSolicitudProveedor, tipoDocumento, numeroDocumento,
                                               facturaProvSap, codigoImportacion, numeroCuota,
                                               montoFacturaUsd, montoAmortizadoUsd, montoAPagarUsd,
                                               montoTotalDocumento, fechaFactura, fechaVencimiento,
                                               concepto, obs, esAprobado, audUsuario, audFecha)
            VALUES (@idSolicitudProveedor, @tipoDocumento, @numeroDocumento,
                    @facturaProvSap, @codigoImportacion, @numeroCuota,
                    @montoFacturaUsd, @montoAmortizadoUsd, @montoAPagarUsd,
                    @montoTotalDocumento, @fechaFactura, @fechaVencimiento,
                    @concepto, @obs, 0, @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();

            -- Recalcular totales en cascada
            UPDATE tpex_SolicitudProveedor
            SET totalFacturasUsd   = (SELECT ISNULL(SUM(montoFacturaUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor),
                totalAmortizadoUsd = (SELECT ISNULL(SUM(montoAmortizadoUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor),
                totalAPagarUsd     = (SELECT ISNULL(SUM(montoAPagarUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd), 0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudActual),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitud = @idSolicitudActual;

            COMMIT TRANSACTION;
            SET @errormsg = 'Cuota #' + CAST(@numeroCuota AS VARCHAR) + ' insertada. montoAPagarUsd = ' + CAST(@montoAPagarUsd AS VARCHAR);
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
        IF ISNULL(@idDetalle, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'El campo idDetalle es obligatorio para actualizar.'; RETURN; END

        SELECT @estadoSolicitud = s.estado, @estadoProveedor = sp.estado, @esAprobadoActual = d.esAprobado,
               @idSolicitudProvActual = d.idSolicitudProveedor, @idSolicitudActual = s.idSolicitud
        FROM tpex_DetalleSolicitud d
        JOIN tpex_SolicitudProveedor sp ON sp.idSolicitudProveedor = d.idSolicitudProveedor
        JOIN tpex_SolicitudPago s ON s.idSolicitud = sp.idSolicitud
        WHERE d.idDetalle = @idDetalle;

        IF @estadoSolicitud IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'No existe la cuota con idDetalle = ' + CAST(@idDetalle AS VARCHAR) + '.'; RETURN; END

        IF @esAprobadoActual = 1
        BEGIN SET @error = 32; SET @errormsg = 'Esta cuota ya esta aprobada. No se puede modificar.'; RETURN; END

        IF @estadoProveedor IN ('APROBADO','RECHAZADO')
        BEGIN SET @error = 33; SET @errormsg = 'El proveedor esta ' + @estadoProveedor + '. No se pueden modificar sus cuotas.'; RETURN; END

        IF ISNULL(@montoFacturaUsd, 0) > 0 AND ISNULL(@montoAmortizadoUsd, 0) > @montoFacturaUsd
        BEGIN SET @error = 34; SET @errormsg = 'El montoAmortizadoUsd no puede ser mayor al montoFacturaUsd.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_DetalleSolicitud
            SET tipoDocumento      = ISNULL(@tipoDocumento, tipoDocumento),
                numeroDocumento    = ISNULL(@numeroDocumento, numeroDocumento),
                facturaProvSap     = ISNULL(@facturaProvSap, facturaProvSap),
                codigoImportacion  = ISNULL(@codigoImportacion, codigoImportacion),
                numeroCuota        = ISNULL(@numeroCuota, numeroCuota),
                montoFacturaUsd    = ISNULL(@montoFacturaUsd, montoFacturaUsd),
                montoAmortizadoUsd = ISNULL(@montoAmortizadoUsd, montoAmortizadoUsd),
                montoAPagarUsd     = ISNULL(@montoAPagarUsd, montoAPagarUsd),
                montoTotalDocumento= ISNULL(@montoTotalDocumento, montoTotalDocumento),
                fechaFactura       = ISNULL(@fechaFactura, fechaFactura),
                fechaVencimiento   = ISNULL(@fechaVencimiento, fechaVencimiento),
                concepto           = ISNULL(@concepto, concepto),
                obs                = ISNULL(@obs, obs),
                audUsuario         = @audUsuario, audFecha = GETDATE()
            WHERE idDetalle = @idDetalle;

            -- Recalcular totales
            UPDATE tpex_SolicitudProveedor
            SET totalFacturasUsd   = (SELECT ISNULL(SUM(montoFacturaUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual),
                totalAmortizadoUsd = (SELECT ISNULL(SUM(montoAmortizadoUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual),
                totalAPagarUsd     = (SELECT ISNULL(SUM(montoAPagarUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProvActual;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd), 0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudActual),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitud = @idSolicitudActual;

            SET @idGenerado = @idDetalle;
            COMMIT TRANSACTION;
            SET @errormsg = 'Cuota actualizada correctamente.';
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
        IF ISNULL(@idDetalle, 0) = 0
        BEGIN SET @error = 40; SET @errormsg = 'El campo idDetalle es obligatorio para eliminar.'; RETURN; END

        SELECT @estadoSolicitud = s.estado, @estadoProveedor = sp.estado, @esAprobadoActual = d.esAprobado,
               @idSolicitudProvActual = d.idSolicitudProveedor, @idSolicitudActual = s.idSolicitud
        FROM tpex_DetalleSolicitud d
        JOIN tpex_SolicitudProveedor sp ON sp.idSolicitudProveedor = d.idSolicitudProveedor
        JOIN tpex_SolicitudPago s ON s.idSolicitud = sp.idSolicitud
        WHERE d.idDetalle = @idDetalle;

        IF @estadoSolicitud IS NULL
        BEGIN SET @error = 41; SET @errormsg = 'No existe la cuota con idDetalle = ' + CAST(@idDetalle AS VARCHAR) + '.'; RETURN; END

        IF @estadoSolicitud IN ('APROBADA','PAGADA','RECHAZADA')
        BEGIN SET @error = 42; SET @errormsg = 'No se puede eliminar una cuota cuya solicitud esta ' + @estadoSolicitud + '.'; RETURN; END

        IF @esAprobadoActual = 1
        BEGIN SET @error = 43; SET @errormsg = 'Esta cuota ya fue aprobada y no puede eliminarse.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_DetalleSolicitud SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idDetalle = @idDetalle;

            UPDATE tpex_SolicitudProveedor
            SET totalFacturasUsd   = (SELECT ISNULL(SUM(montoFacturaUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual),
                totalAmortizadoUsd = (SELECT ISNULL(SUM(montoAmortizadoUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual),
                totalAPagarUsd     = (SELECT ISNULL(SUM(montoAPagarUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProvActual;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd), 0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudActual),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitud = @idSolicitudActual;

            COMMIT TRANSACTION;
            SET @errormsg = 'Cuota eliminada correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al eliminar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- ACCION = 'A' : Aprobar cuota individual + propagacion auto
    -- ================================================================
    IF @ACCION = 'A'
    BEGIN
        IF ISNULL(@idDetalle, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'idDetalle es obligatorio para aprobar.'; RETURN; END

        SELECT @estadoSolicitud = s.estado, @estadoProveedor = sp.estado, @esAprobadoActual = d.esAprobado,
               @idSolicitudProvActual = d.idSolicitudProveedor, @idSolicitudActual = s.idSolicitud
        FROM tpex_DetalleSolicitud d
        JOIN tpex_SolicitudProveedor sp ON sp.idSolicitudProveedor = d.idSolicitudProveedor
        JOIN tpex_SolicitudPago s ON s.idSolicitud = sp.idSolicitud
        WHERE d.idDetalle = @idDetalle;

        IF @estadoSolicitud IS NULL
        BEGIN SET @error = 51; SET @errormsg = 'No existe la cuota con idDetalle = ' + CAST(@idDetalle AS VARCHAR) + '.'; RETURN; END

        IF @esAprobadoActual = 1
        BEGIN SET @error = 52; SET @errormsg = 'La cuota ya esta aprobada.'; RETURN; END

        IF @estadoProveedor = 'RECHAZADO'
        BEGIN SET @error = 53; SET @errormsg = 'No se puede aprobar una cuota de un proveedor RECHAZADO.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_DetalleSolicitud
            SET esAprobado = 1, audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idDetalle = @idDetalle;

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, idDetalle, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitudActual, @idSolicitudProvActual, @idDetalle, '0', '1', 'Cuota aprobada individualmente.', @audUsuario, GETDATE());

            -- Auto-propagacion: si TODAS las cuotas del proveedor estan aprobadas
            SELECT @countNoAprobadas = COUNT(*) FROM tpex_DetalleSolicitud
            WHERE idSolicitudProveedor = @idSolicitudProvActual AND esAprobado = 0;

            IF @countNoAprobadas = 0 AND @estadoProveedor = 'PENDIENTE'
            BEGIN
                UPDATE tpex_SolicitudProveedor
                SET estado = 'APROBADO',
                    fechaAprobacion = GETDATE(),
                    usuarioAprobador = @audUsuario,
                    obsAprobacion = 'Aprobacion automatica: todas las cuotas aprobadas.',
                    audUsuario = @audUsuario, audFecha = GETDATE()
                WHERE idSolicitudProveedor = @idSolicitudProvActual;

                INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                VALUES (@idSolicitudActual, @idSolicitudProvActual, 'PENDIENTE', 'APROBADO',
                        'Aprobacion automatica del proveedor: todas las cuotas aprobadas.', @audUsuario, GETDATE());
            END

            SET @idGenerado = @idDetalle;
            COMMIT TRANSACTION;
            SET @errormsg = CASE WHEN @countNoAprobadas = 0
                THEN 'Cuota aprobada. Proveedor APROBADO automaticamente.'
                ELSE 'Cuota aprobada. Faltan ' + CAST(@countNoAprobadas AS VARCHAR) + ' cuota(s) por aprobar en este proveedor.' END;
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al aprobar cuota: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- ACCION = 'R' : Revertir aprobacion (desmarcar esAprobado)
    -- ================================================================
    IF @ACCION = 'R'
    BEGIN
        IF ISNULL(@idDetalle, 0) = 0
        BEGIN SET @error = 60; SET @errormsg = 'idDetalle es obligatorio para revertir.'; RETURN; END

        SELECT @estadoSolicitud = s.estado, @estadoProveedor = sp.estado, @esAprobadoActual = d.esAprobado,
               @idSolicitudProvActual = d.idSolicitudProveedor, @idSolicitudActual = s.idSolicitud
        FROM tpex_DetalleSolicitud d
        JOIN tpex_SolicitudProveedor sp ON sp.idSolicitudProveedor = d.idSolicitudProveedor
        JOIN tpex_SolicitudPago s ON s.idSolicitud = sp.idSolicitud
        WHERE d.idDetalle = @idDetalle;

        IF @estadoSolicitud IS NULL
        BEGIN SET @error = 61; SET @errormsg = 'No existe la cuota.'; RETURN; END
        IF @esAprobadoActual = 0
        BEGIN SET @error = 62; SET @errormsg = 'La cuota no estaba aprobada.'; RETURN; END
        IF @estadoSolicitud != 'PENDIENTE'
        BEGIN SET @error = 63; SET @errormsg = 'Solo se pueden revertir aprobaciones en solicitudes PENDIENTES.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_DetalleSolicitud SET esAprobado = 0, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idDetalle = @idDetalle;

            -- Si el proveedor estaba APROBADO, volverlo a PENDIENTE
            IF @estadoProveedor = 'APROBADO'
            BEGIN
                UPDATE tpex_SolicitudProveedor
                SET estado = 'PENDIENTE', fechaAprobacion = NULL, usuarioAprobador = NULL,
                    obsAprobacion = 'Revertida aprobacion automatica por cuota no aprobada.',
                    audUsuario = @audUsuario, audFecha = GETDATE()
                WHERE idSolicitudProveedor = @idSolicitudProvActual;

                INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, idDetalle, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                VALUES (@idSolicitudActual, @idSolicitudProvActual, @idDetalle, 'APROBADO', 'PENDIENTE',
                        'Revertida aprobacion automatica.', @audUsuario, GETDATE());
            END

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, idDetalle, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitudActual, @idSolicitudProvActual, @idDetalle, '1', '0', 'Aprobacion de cuota revertida.', @audUsuario, GETDATE());

            SET @idGenerado = @idDetalle;
            COMMIT TRANSACTION;
            SET @errormsg = 'Aprobacion de cuota revertida correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al revertir: ' + ERROR_MESSAGE();
        END CATCH;
        RETURN;
    END
END

GO

ALTER PROCEDURE [dbo].[p_list_tpex_DetalleSolicitud] @idDetalle bigint = NULL, @idSolicitudProveedor bigint = NULL, @tipoDocumento varchar(50) = NULL, @numeroDocumento varchar(250) = NULL, @facturaProvSap int = NULL, @codigoImportacion varchar(50) = NULL, @montoFacturaUsd decimal(18, 2) = NULL, @montoAmortizadoUsd decimal(18, 2) = NULL, @montoAPagarUsd decimal(19, 2) = NULL, @fechaFactura datetime = NULL, @fechaVencimiento datetime = NULL, @concepto varchar(255) = NULL, @obs varchar(255) = NULL, @esAprobado bit = NULL, @audUsuario int = NULL, @ACCION VARCHAR(1) = NULL, @codEmpresa int = NULLASBEGIN	 -- =====LISTAR==================================	 IF(@ACCION = 'L')	 BEGIN		 SELECT			 idDetalle,			 idSolicitudProveedor,			 tipoDocumento,			 numeroDocumento,			 facturaProvSap,			 codigoImportacion,			 montoFacturaUsd,			 montoAmortizadoUsd,			 montoAPagarUsd,			 fechaFactura,			 fechaVencimiento,			 concepto,			 obs,			 esAprobado,			 audUsuario,			 audFecha		 FROM tpex_DetalleSolicitud		 WHERE eliminado = 0 AND (@idDetalle IS NULL OR @idDetalle = idDetalle) 			AND (@idSolicitudProveedor IS NULL OR @idSolicitudProveedor = idSolicitudProveedor) 			AND (@tipoDocumento IS NULL OR @tipoDocumento = tipoDocumento) 			AND (@numeroDocumento IS NULL OR @numeroDocumento = numeroDocumento) 			AND (@facturaProvSap IS NULL OR @facturaProvSap = facturaProvSap) 			AND (@codigoImportacion IS NULL OR @codigoImportacion = codigoImportacion) 			AND (@montoFacturaUsd IS NULL OR @montoFacturaUsd = montoFacturaUsd) 			AND (@montoAmortizadoUsd IS NULL OR @montoAmortizadoUsd = montoAmortizadoUsd) 			AND (@montoAPagarUsd IS NULL OR @montoAPagarUsd = montoAPagarUsd) 			AND (@fechaFactura IS NULL OR @fechaFactura = fechaFactura) 			AND (@fechaVencimiento IS NULL OR @fechaVencimiento = fechaVencimiento) 			AND (@concepto IS NULL OR @concepto = concepto) 			AND (@obs IS NULL OR @obs = obs) 			AND (@esAprobado IS NULL OR @esAprobado = esAprobado) 			AND (@audUsuario IS NULL OR @audUsuario = audUsuario)	END	--- ============== PARA LISTAR LAS FACTURAS PROVEEDOR Y PEDIDOS(Orden de Compra) POR EMPRESA ======================	 IF( @ACCION = 'A' )	 BEGIN		 			DECLARE @sql    NVARCHAR(MAX);  
			DECLARE @temp   TABLE (  
				codEmpresa  INT,  
				empresa     VARCHAR(15),  
				descripcion VARCHAR(50),  
				docNum      INT,  
				moneda      VARCHAR(10)  
			);  
  
			 
			SET @sql = N'SELECT * FROM OPENQUERY(SRV_2022,   
				''EXEC [CONEXION].[dbo].[p_list_PagosExtranjerosDocAbiertos]   
					@codEmpresa = ' + CONVERT(VARCHAR(5), @codEmpresa) + ',   
					@ACCION = ''''A'''''')';  
  
			INSERT INTO @temp  
			EXEC sp_executesql @sql			SELECT 			t1.docNum as facturaProvSap			,t1.descripcion as tipoDocumento			FROM @temp t1	 ENDEND

GO

SELECT
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_DetalleSolicitud')  AND definition LIKE '%UPDATE tpex_DetalleSolicitud SET eliminado = 1%') AS abm_soft_ok,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_DetalleSolicitud')  AND definition LIKE '%DELETE FROM tpex_DetalleSolicitud%')                AS abm_delete_residual,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_list_tpex_DetalleSolicitud') AND definition LIKE '%eliminado = 0 AND%')                                 AS list_filtra_ok;
