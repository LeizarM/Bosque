/* ============================================================================
   TPEX · Aprobación PARCIAL por proveedor
   Fecha: 2026-06-23
   Autor: (asistido) — EJECUTAR CON USUARIO DE ESCRITURA

   Objetivo
   --------
   Permitir que un proveedor avance a un estado aprobado aun cuando NO todas
   sus cuotas estén aprobadas. Se introduce el estado APROBADO_PARCIAL:

       PENDIENTE ──(≥1 cuota aprob., faltan)──► APROBADO_PARCIAL ──(todas)──► APROBADO
                 └──(todas las cuotas aprob.)──────────────────────────────► APROBADO

   Reglas
   ------
   • Las cuotas NO aprobadas quedan FUERA del pago: el totalAPagarUsd del
     proveedor pasa a sumar SOLO las cuotas con esAprobado = 1 cuando el
     proveedor está APROBADO o APROBADO_PARCIAL. (Cuando está PENDIENTE se
     mantiene el total proyectado = todas las cuotas, como hasta ahora.)
   • Las cuotas no aprobadas se pueden aprobar después; al completar todas, el
     proveedor pasa automáticamente a APROBADO total.
   • Revertir una cuota de un proveedor APROBADO/APROBADO_PARCIAL recalcula el
     estado: si quedan 0 aprobadas → PENDIENTE; si quedan algunas → APROBADO_PARCIAL.

   Notas
   -----
   • La columna estado es varchar(20); 'APROBADO_PARCIAL' (16) entra holgado.
   • No existen CHECK constraints sobre estado, así que no hay que soltar nada.
   • Solo se modifican 2 SPs (idempotente vía ALTER):
       - p_abm_tpex_SolicitudProveedor : nueva ACCION 'P' (aprobar parcial),
         guardas U/D y recálculo de totales para el nuevo estado.
       - p_abm_tpex_DetalleSolicitud   : auto-propagación y reversión por cuota
         contemplan APROBADO_PARCIAL + recálculo de totalAPagar (solo aprobadas).
   ============================================================================ */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* ===========================================================================
   1/2 · p_abm_tpex_SolicitudProveedor
   =========================================================================== */
ALTER PROCEDURE [dbo].[p_abm_tpex_SolicitudProveedor]
    @ACCION VARCHAR(1)
    , @idSolicitudProveedor BIGINT = NULL
    , @idSolicitud BIGINT = NULL
    , @cardCode VARCHAR(50) = NULL
    , @cardName VARCHAR(255) = NULL
    , @totalFacturasUsd DECIMAL(18, 2) = NULL
    , @totalAmortizadoUsd DECIMAL(18, 2) = NULL
    , @totalAPagarUsd DECIMAL(18, 2) = NULL
    , @obs VARCHAR(255) = NULL
    , @estado VARCHAR(20) = NULL
    , @obsAprobacion VARCHAR(500) = NULL
    , @audUsuario INT = NULL
    , @error INT = 0 OUTPUT
    , @errormsg NVARCHAR(500) = '' OUTPUT
    , @idGenerado BIGINT = 0 OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0; SET @errormsg = ''; SET @idGenerado = 0;

    DECLARE @estadoSolicitud  VARCHAR(20);
    DECLARE @estadoActual     VARCHAR(20);
    DECLARE @idSolicitudProv  BIGINT;
    DECLARE @countCuotasNoAprob INT;
    DECLARE @countAprob       INT;
    DECLARE @countTotal       INT;
    DECLARE @nuevoEstadoP     VARCHAR(20);

    -- ================================================================
    --  INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idSolicitud, 0) = 0
        BEGIN SET @error = 20; SET @errormsg = 'El campo idSolicitud es obligatorio.'; RETURN; END

        IF NOT EXISTS (SELECT 1 FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud)
        BEGIN SET @error = 21; SET @errormsg = 'No existe la solicitud con idSolicitud = ' + CAST(@idSolicitud AS VARCHAR) + '.'; RETURN; END

        IF ISNULL(@cardCode, '') = ''
        BEGIN SET @error = 22; SET @errormsg = 'El campo cardCode es obligatorio.'; RETURN; END

        IF EXISTS (SELECT 1 FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitud AND cardCode = @cardCode)
        BEGIN SET @error = 23; SET @errormsg = 'El proveedor ' + @cardCode + ' ya existe en esta solicitud.'; RETURN; END

        IF ISNULL(@totalAPagarUsd, 0) < 0
        BEGIN SET @error = 24; SET @errormsg = 'El totalAPagarUsd no puede ser negativo.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            INSERT INTO tpex_SolicitudProveedor (idSolicitud, cardCode, cardName,
                                                 totalFacturasUsd, totalAmortizadoUsd, totalAPagarUsd,
                                                 obs, estado, audUsuario, audFecha)
            VALUES (@idSolicitud, @cardCode, @cardName,
                    ISNULL(@totalFacturasUsd, 0),
                    ISNULL(@totalAmortizadoUsd, 0),
                    ISNULL(@totalAPagarUsd, 0),
                    @obs, 'PENDIENTE', @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitud, @idGenerado, NULL, 'PENDIENTE', 'Proveedor agregado a solicitud.', @audUsuario, GETDATE());

            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor insertado correctamente.';
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
    -- UPDATE (datos basicos del proveedor)
    -- ================================================================
    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'El campo idSolicitudProveedor es obligatorio para actualizar.'; RETURN; END

        SELECT @estadoActual = estado, @idSolicitudProv = idSolicitud
        FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;

        IF @estadoActual IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'No existe el registro con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        -- APROBADO_PARCIAL se trata como aprobado a efectos de edicion de datos basicos.
        IF @estadoActual IN ('APROBADO','APROBADO_PARCIAL','RECHAZADO')
        BEGIN SET @error = 32; SET @errormsg = 'El proveedor esta ' + @estadoActual + '. Para modificar use ACCION A/P/R.'; RETURN; END

        IF ISNULL(@cardCode, '') = ''
        BEGIN SET @error = 33; SET @errormsg = 'El campo cardCode es obligatorio para actualizar.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_SolicitudProveedor
            SET cardCode          = @cardCode,
                cardName          = @cardName,
                totalFacturasUsd  = ISNULL(@totalFacturasUsd, totalFacturasUsd),
                totalAmortizadoUsd= ISNULL(@totalAmortizadoUsd, totalAmortizadoUsd),
                totalAPagarUsd    = ISNULL(@totalAPagarUsd, totalAPagarUsd),
                obs               = @obs,
                audUsuario        = @audUsuario,
                audFecha          = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;
            SET @idGenerado = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor actualizado correctamente.';
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
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 40; SET @errormsg = 'El campo idSolicitudProveedor es obligatorio para eliminar.'; RETURN; END

        SELECT @estadoActual = estado FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;
        IF @estadoActual IS NULL
        BEGIN SET @error = 41; SET @errormsg = 'No existe el registro con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual IN ('APROBADO','APROBADO_PARCIAL')
        BEGIN SET @error = 42; SET @errormsg = 'No se puede eliminar un proveedor ' + @estadoActual + '. Rechacelo primero.'; RETURN; END

        IF EXISTS (SELECT 1 FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor)
        BEGIN SET @error = 43; SET @errormsg = 'El proveedor tiene facturas asociadas. Elimine primero las lineas de detalle.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_SolicitudProveedor SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idSolicitudProveedor = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor eliminado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al eliminar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- ACCION = 'A' : Aprobar proveedor TOTAL (aprueba todas las cuotas)
    -- ================================================================
    IF @ACCION = 'A'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'idSolicitudProveedor es obligatorio para aprobar.'; RETURN; END

        SELECT @estadoActual = estado, @idSolicitudProv = idSolicitud
        FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;

        IF @estadoActual IS NULL
        BEGIN SET @error = 51; SET @errormsg = 'No existe el proveedor con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual = 'APROBADO'
        BEGIN SET @error = 52; SET @errormsg = 'El proveedor ya esta APROBADO.'; RETURN; END

        IF @estadoActual = 'RECHAZADO'
        BEGIN SET @error = 53; SET @errormsg = 'El proveedor esta RECHAZADO y no se puede aprobar.'; RETURN; END

        -- Validar que tenga al menos 1 cuota
        IF NOT EXISTS (SELECT 1 FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor AND eliminado = 0)
        BEGIN SET @error = 54; SET @errormsg = 'No se puede aprobar un proveedor sin cuotas registradas.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            -- Aprobar TODAS las cuotas del proveedor (aprobacion total manual)
            UPDATE tpex_DetalleSolicitud
            SET esAprobado = 1, audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor AND esAprobado = 0 AND eliminado = 0;

            UPDATE tpex_SolicitudProveedor
            SET estado = 'APROBADO',
                fechaAprobacion = GETDATE(),
                usuarioAprobador = @audUsuario,
                obsAprobacion = @obsAprobacion,
                -- todas aprobadas => totalAPagar = suma de todas (= suma de aprobadas)
                totalAPagarUsd = (SELECT ISNULL(SUM(montoAPagarUsd),0) FROM tpex_DetalleSolicitud
                                  WHERE idSolicitudProveedor = @idSolicitudProveedor AND eliminado = 0 AND esAprobado = 1),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd),0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudProv AND eliminado = 0),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitud = @idSolicitudProv;

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitudProv, @idSolicitudProveedor, @estadoActual, 'APROBADO',
                    ISNULL(@obsAprobacion, 'Aprobacion manual del proveedor (todas las cuotas).'), @audUsuario, GETDATE());

            SET @idGenerado = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor APROBADO correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al aprobar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- ACCION = 'P' : Aprobar proveedor PARCIAL (solo cuotas ya aprobadas)
    --   - Requiere >=1 cuota aprobada. No aprueba las restantes.
    --   - Si por casualidad estan TODAS aprobadas => APROBADO total.
    --   - totalAPagar = suma de cuotas aprobadas (las no aprobadas quedan fuera).
    -- ================================================================
    IF @ACCION = 'P'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 70; SET @errormsg = 'idSolicitudProveedor es obligatorio para aprobar parcialmente.'; RETURN; END

        SELECT @estadoActual = estado, @idSolicitudProv = idSolicitud
        FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;

        IF @estadoActual IS NULL
        BEGIN SET @error = 71; SET @errormsg = 'No existe el proveedor con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual = 'APROBADO'
        BEGIN SET @error = 72; SET @errormsg = 'El proveedor ya esta APROBADO en su totalidad.'; RETURN; END

        IF @estadoActual = 'RECHAZADO'
        BEGIN SET @error = 73; SET @errormsg = 'El proveedor esta RECHAZADO y no se puede aprobar.'; RETURN; END

        SELECT @countTotal = COUNT(*),
               @countAprob = SUM(CASE WHEN esAprobado = 1 THEN 1 ELSE 0 END)
        FROM tpex_DetalleSolicitud
        WHERE idSolicitudProveedor = @idSolicitudProveedor AND eliminado = 0;

        IF ISNULL(@countTotal, 0) = 0
        BEGIN SET @error = 74; SET @errormsg = 'No se puede aprobar un proveedor sin cuotas registradas.'; RETURN; END

        IF ISNULL(@countAprob, 0) = 0
        BEGIN SET @error = 75; SET @errormsg = 'Apruebe al menos una cuota antes de aprobar parcialmente al proveedor.'; RETURN; END

        SET @nuevoEstadoP = CASE WHEN @countAprob = @countTotal THEN 'APROBADO' ELSE 'APROBADO_PARCIAL' END;

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_SolicitudProveedor
            SET estado = @nuevoEstadoP,
                fechaAprobacion = GETDATE(),
                usuarioAprobador = @audUsuario,
                obsAprobacion = ISNULL(@obsAprobacion,
                    CASE WHEN @nuevoEstadoP = 'APROBADO_PARCIAL'
                         THEN 'Aprobacion parcial: ' + CAST(@countAprob AS VARCHAR) + ' de ' + CAST(@countTotal AS VARCHAR) + ' cuotas aprobadas.'
                         ELSE 'Aprobacion del proveedor (todas las cuotas estaban aprobadas).' END),
                -- totalAPagar = solo cuotas aprobadas
                totalAPagarUsd = (SELECT ISNULL(SUM(montoAPagarUsd),0) FROM tpex_DetalleSolicitud
                                  WHERE idSolicitudProveedor = @idSolicitudProveedor AND eliminado = 0 AND esAprobado = 1),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;

            UPDATE tpex_SolicitudPago
            SET montoTotalSolicitud = (SELECT ISNULL(SUM(totalAPagarUsd),0) FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitudProv AND eliminado = 0),
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitud = @idSolicitudProv;

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitudProv, @idSolicitudProveedor, @estadoActual, @nuevoEstadoP,
                    ISNULL(@obsAprobacion, 'Aprobacion parcial del proveedor (' + CAST(@countAprob AS VARCHAR) + '/' + CAST(@countTotal AS VARCHAR) + ' cuotas).'),
                    @audUsuario, GETDATE());

            SET @idGenerado = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = CASE WHEN @nuevoEstadoP = 'APROBADO'
                THEN 'Proveedor APROBADO (todas las cuotas estaban aprobadas).'
                ELSE 'Proveedor APROBADO_PARCIAL: ' + CAST(@countAprob AS VARCHAR) + ' de ' + CAST(@countTotal AS VARCHAR) + ' cuotas. Las no aprobadas quedan fuera del pago.' END;
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al aprobar parcialmente: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- ACCION = 'R' : Rechazar proveedor
    -- ================================================================
    IF @ACCION = 'R'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 60; SET @errormsg = 'idSolicitudProveedor es obligatorio para rechazar.'; RETURN; END

        SELECT @estadoActual = estado, @idSolicitudProv = idSolicitud
        FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;

        IF @estadoActual IS NULL
        BEGIN SET @error = 61; SET @errormsg = 'No existe el proveedor con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual = 'RECHAZADO'
        BEGIN SET @error = 62; SET @errormsg = 'El proveedor ya esta RECHAZADO.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_SolicitudProveedor
            SET estado = 'RECHAZADO',
                fechaAprobacion = GETDATE(),
                usuarioAprobador = @audUsuario,
                obsAprobacion = @obsAprobacion,
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitudProv, @idSolicitudProveedor, @estadoActual, 'RECHAZADO',
                    ISNULL(@obsAprobacion, 'Proveedor rechazado.'), @audUsuario, GETDATE());

            SET @idGenerado = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor RECHAZADO correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al rechazar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END
END
GO

/* ===========================================================================
   2/2 · p_abm_tpex_DetalleSolicitud
   =========================================================================== */
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

        -- APROBADO_PARCIAL tambien bloquea agregar cuotas (el proveedor ya fue aprobado parcialmente).
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

            -- Recalcular totales (proveedor PENDIENTE => totalAPagar = todas las cuotas)
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

        -- Si el proveedor fue aprobado parcialmente, primero hay que revertir esa aprobacion.
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
    -- ACCION = 'A' : Aprobar cuota individual + propagacion auto
    --   - Si quedan 0 cuotas sin aprobar y el proveedor estaba
    --     PENDIENTE o APROBADO_PARCIAL => pasa a APROBADO (total).
    --   - totalAPagar del proveedor = solo aprobadas si esta APROBADO/PARCIAL.
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
            WHERE idSolicitudProveedor = @idSolicitudProvActual AND esAprobado = 0 AND eliminado = 0;

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

            -- Recalcular totalAPagar: solo aprobadas si el proveedor esta APROBADO/APROBADO_PARCIAL; si sigue PENDIENTE, todas.
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
    --   - Si el proveedor estaba APROBADO/APROBADO_PARCIAL recalcula:
    --       0 cuotas aprobadas => PENDIENTE ; >=1 => APROBADO_PARCIAL.
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

            -- Recalcular estado del proveedor si estaba en un estado aprobado
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

            -- Recalcular totalAPagar segun estado resultante del proveedor
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
