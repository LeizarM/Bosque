/* ============================================================================
   TPEX · Aprobación PARCIAL implícita (sin botón dedicado)
   Fecha: 2026-06-23 (B) — EJECUTAR CON USUARIO DE ESCRITURA
   Requiere haber corrido antes: 2026-06-23_tpex_aprobacion_parcial_proveedor.sql

   Cambio
   ------
   El estado APROBADO_PARCIAL ahora se asigna SOLO al aprobar cuotas, sin
   necesidad de una acción "aprobar parcial" explícita:

       Proveedor PENDIENTE + se aprueba 1 cuota (faltan otras) => APROBADO_PARCIAL
       Proveedor PENDIENTE/PARCIAL + se aprueba la última cuota => APROBADO

   Las cuotas no aprobadas pueden quedarse pendientes indefinidamente (estilo
   SAP); el proveedor permanece en APROBADO_PARCIAL y solo paga lo aprobado.

   Solo se re-altera p_abm_tpex_DetalleSolicitud (ACCION 'A'). El SP
   p_abm_tpex_SolicitudProveedor mantiene su ACCION 'P' (queda como override
   manual, ya no la usa el frontend).
   ============================================================================ */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

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
    DECLARE @countAprobRest    INT;
    DECLARE @estadoProvFinal   VARCHAR(20);
    DECLARE @nuevoEstadoR      VARCHAR(20);

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

        IF @estadoProveedor IN ('APROBADO','APROBADO_PARCIAL','RECHAZADO')
        BEGIN SET @error = 23; SET @errormsg = 'El proveedor esta ' + @estadoProveedor + '. No se pueden agregar mas cuotas.'; RETURN; END

        IF ISNULL(@montoFacturaUsd, 0) <= 0
        BEGIN SET @error = 24; SET @errormsg = 'El Monto Factura USD es obligatorio y debe ser mayor a cero.'; RETURN; END

        SET @montoAmortizadoUsd = ISNULL(@montoAmortizadoUsd, 0);
        IF @montoAmortizadoUsd > @montoFacturaUsd
        BEGIN SET @error = 25; SET @errormsg = 'El Monto Amortizado USD no puede ser mayor al Monto Factura USD.'; RETURN; END

        IF @fechaFactura IS NOT NULL AND @fechaVencimiento IS NOT NULL AND @fechaVencimiento < @fechaFactura
        BEGIN SET @error = 26; SET @errormsg = 'La Fecha Vencimiento no puede ser anterior a la Fecha Factura.'; RETURN; END

        SET @montoAPagarUsd = ISNULL(@montoAPagarUsd, @montoFacturaUsd - @montoAmortizadoUsd);

        IF @numeroCuota IS NULL OR @numeroCuota = 0
        BEGIN
            SELECT @maxCuota = ISNULL(MAX(numeroCuota), 0)
            FROM tpex_DetalleSolicitud
            WHERE idSolicitudProveedor = @idSolicitudProveedor
              AND ISNULL(facturaProvSap, 0) = ISNULL(@facturaProvSap, 0);
            SET @numeroCuota = @maxCuota + 1;
        END

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

            UPDATE tpex_SolicitudProveedor
            SET totalFacturasUsd   = (SELECT ISNULL(SUM(montoFacturaUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor AND eliminado = 0),
                totalAmortizadoUsd = (SELECT ISNULL(SUM(montoAmortizadoUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor AND eliminado = 0),
                totalAPagarUsd     = (SELECT ISNULL(SUM(montoAPagarUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor AND eliminado = 0),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd), 0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudActual AND eliminado = 0),
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

        IF @estadoProveedor IN ('APROBADO','APROBADO_PARCIAL','RECHAZADO')
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

            UPDATE tpex_SolicitudProveedor
            SET totalFacturasUsd   = (SELECT ISNULL(SUM(montoFacturaUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0),
                totalAmortizadoUsd = (SELECT ISNULL(SUM(montoAmortizadoUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0),
                totalAPagarUsd     = (SELECT ISNULL(SUM(montoAPagarUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProvActual;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd), 0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudActual AND eliminado = 0),
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

        IF @estadoProveedor IN ('APROBADO','APROBADO_PARCIAL')
        BEGIN SET @error = 44; SET @errormsg = 'El proveedor esta ' + @estadoProveedor + '. Revierta la aprobacion antes de eliminar cuotas.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_DetalleSolicitud SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idDetalle = @idDetalle;

            UPDATE tpex_SolicitudProveedor
            SET totalFacturasUsd   = (SELECT ISNULL(SUM(montoFacturaUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0),
                totalAmortizadoUsd = (SELECT ISNULL(SUM(montoAmortizadoUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0),
                totalAPagarUsd     = (SELECT ISNULL(SUM(montoAPagarUsd), 0) FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProvActual;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd), 0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudActual AND eliminado = 0),
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
    -- ACCION = 'A' : Aprobar cuota individual + propagacion auto del estado
    --   PENDIENTE + faltan cuotas        => APROBADO_PARCIAL  (parcial implicito)
    --   PENDIENTE / PARCIAL + 0 faltan   => APROBADO
    --   totalAPagar = solo aprobadas cuando el proveedor esta APROBADO/PARCIAL.
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

            -- Cuantas cuotas quedan sin aprobar
            SELECT @countNoAprobadas = COUNT(*) FROM tpex_DetalleSolicitud
            WHERE idSolicitudProveedor = @idSolicitudProvActual AND esAprobado = 0 AND eliminado = 0;

            -- Propagacion automatica del estado del proveedor
            IF @countNoAprobadas = 0 AND @estadoProveedor IN ('PENDIENTE','APROBADO_PARCIAL')
            BEGIN
                UPDATE tpex_SolicitudProveedor
                SET estado = 'APROBADO',
                    fechaAprobacion = GETDATE(),
                    usuarioAprobador = @audUsuario,
                    obsAprobacion = 'Aprobacion automatica: todas las cuotas aprobadas.',
                    audUsuario = @audUsuario, audFecha = GETDATE()
                WHERE idSolicitudProveedor = @idSolicitudProvActual;

                INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                VALUES (@idSolicitudActual, @idSolicitudProvActual, @estadoProveedor, 'APROBADO',
                        'Aprobacion automatica del proveedor: todas las cuotas aprobadas.', @audUsuario, GETDATE());
            END
            ELSE IF @countNoAprobadas > 0 AND @estadoProveedor = 'PENDIENTE'
            BEGIN
                -- Primera cuota aprobada (faltan otras) => parcial implicito
                UPDATE tpex_SolicitudProveedor
                SET estado = 'APROBADO_PARCIAL',
                    fechaAprobacion = GETDATE(),
                    usuarioAprobador = @audUsuario,
                    obsAprobacion = 'Aprobacion parcial automatica: al menos una cuota aprobada.',
                    audUsuario = @audUsuario, audFecha = GETDATE()
                WHERE idSolicitudProveedor = @idSolicitudProvActual;

                INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                VALUES (@idSolicitudActual, @idSolicitudProvActual, 'PENDIENTE', 'APROBADO_PARCIAL',
                        'Aprobacion parcial del proveedor (al aprobar una cuota).', @audUsuario, GETDATE());
            END

            -- Recalcular totalAPagar segun estado resultante
            SELECT @estadoProvFinal = estado FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProvActual;
            UPDATE tpex_SolicitudProveedor
            SET totalAPagarUsd = (SELECT ISNULL(SUM(montoAPagarUsd), 0) FROM tpex_DetalleSolicitud
                                  WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0
                                    AND (esAprobado = 1 OR @estadoProvFinal NOT IN ('APROBADO','APROBADO_PARCIAL'))),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProvActual;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd), 0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudActual AND eliminado = 0),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitud = @idSolicitudActual;

            SET @idGenerado = @idDetalle;
            COMMIT TRANSACTION;
            SET @errormsg = CASE WHEN @countNoAprobadas = 0
                THEN 'Cuota aprobada. Proveedor APROBADO automaticamente.'
                ELSE 'Cuota aprobada. Proveedor APROBADO_PARCIAL. Faltan ' + CAST(@countNoAprobadas AS VARCHAR) + ' cuota(s) (pueden quedar pendientes).' END;
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
    --   APROBADO/APROBADO_PARCIAL => 0 aprobadas: PENDIENTE ; >=1: APROBADO_PARCIAL
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

            IF @estadoProveedor IN ('APROBADO','APROBADO_PARCIAL')
            BEGIN
                SELECT @countAprobRest = SUM(CASE WHEN esAprobado = 1 THEN 1 ELSE 0 END)
                FROM tpex_DetalleSolicitud
                WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0;

                SET @nuevoEstadoR = CASE WHEN ISNULL(@countAprobRest, 0) = 0 THEN 'PENDIENTE' ELSE 'APROBADO_PARCIAL' END;

                IF @nuevoEstadoR <> @estadoProveedor
                BEGIN
                    UPDATE tpex_SolicitudProveedor
                    SET estado = @nuevoEstadoR,
                        fechaAprobacion  = CASE WHEN @nuevoEstadoR = 'PENDIENTE' THEN NULL ELSE fechaAprobacion END,
                        usuarioAprobador = CASE WHEN @nuevoEstadoR = 'PENDIENTE' THEN NULL ELSE usuarioAprobador END,
                        obsAprobacion = 'Cuota revertida: proveedor pasa a ' + @nuevoEstadoR + '.',
                        audUsuario = @audUsuario, audFecha = GETDATE()
                    WHERE idSolicitudProveedor = @idSolicitudProvActual;

                    INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, idDetalle, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                    VALUES (@idSolicitudActual, @idSolicitudProvActual, @idDetalle, @estadoProveedor, @nuevoEstadoR,
                            'Recalculo de estado del proveedor por reversion de cuota.', @audUsuario, GETDATE());
                END
            END

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, idDetalle, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitudActual, @idSolicitudProvActual, @idDetalle, '1', '0', 'Aprobacion de cuota revertida.', @audUsuario, GETDATE());

            SELECT @estadoProvFinal = estado FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProvActual;
            UPDATE tpex_SolicitudProveedor
            SET totalAPagarUsd = (SELECT ISNULL(SUM(montoAPagarUsd), 0) FROM tpex_DetalleSolicitud
                                  WHERE idSolicitudProveedor = @idSolicitudProvActual AND eliminado = 0
                                    AND (esAprobado = 1 OR @estadoProvFinal NOT IN ('APROBADO','APROBADO_PARCIAL'))),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProvActual;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd), 0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudActual AND eliminado = 0),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitud = @idSolicitudActual;

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
