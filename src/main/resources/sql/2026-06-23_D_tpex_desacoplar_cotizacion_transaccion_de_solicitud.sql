/* ============================================================================
   TPEX · Desacoplar cotización/transacción del estado de la solicitud
   Fecha: 2026-06-23 (D) — EJECUTAR CON USUARIO DE ESCRITURA
   Requiere: migraciones A, B y C de 2026-06-23.

   Decisión de negocio
   -------------------
   Mientras un proveedor tenga >=1 cuota aprobada (APROBADO o APROBADO_PARCIAL)
   se debe poder COTIZAR y TRANSACCIONAR ese monto aprobado, AUNQUE la solicitud
   siga en PENDIENTE. La solicitud NO se cierra automáticamente: el pago vive en
   la transacción (CONFIRMADO) y la solicitud permanece PENDIENTE (estilo SAP).
   -> El cierre a PAGADA se quita del flujo de "confirmar pago" (cambio en el
      backend Java, no en SQL).

   Cambios SQL (este script)
   -------------------------
   1) p_abm_tpex_Cotizaciones (error 22): ya no exige solicitud APROBADA.
      Permite cotizar si la solicitud NO está PAGADA/RECHAZADA y existe >=1
      cuota aprobada de un proveedor APROBADO/APROBADO_PARCIAL.
   2) p_abm_tpex_Transacciones (error 22): ya no exige solicitud APROBADA.
      Solo bloquea si la solicitud está PAGADA o RECHAZADA. (La garantía de que
      hay algo aprobado llega vía la cotización ACEPTADA, error 28.)
   ============================================================================ */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* ===========================================================================
   1/2 · p_abm_tpex_Cotizaciones  (error 22: cotizar con solicitud PENDIENTE)
   =========================================================================== */
ALTER PROCEDURE [dbo].[p_abm_tpex_Cotizaciones]
    @ACCION VARCHAR(1)
    , @idCotizacion BIGINT = NULL
    , @idSolicitud BIGINT = NULL
    , @fechaCotizacion DATE = NULL
    , @montoCompra DECIMAL(18, 2) = NULL
    , @idMoneda BIGINT = NULL
    , @nroGiros INT = NULL
    , @codBanco BIGINT = NULL
    , @tipoCambioOfrecido DECIMAL(10, 6) = NULL
    , @montoConvertido DECIMAL(18, 2) = NULL
    , @totalBolivianos DECIMAL(18, 2) = NULL
    , @esGanadora BIT = NULL
    , @estado VARCHAR(20) = NULL
    , @observaciones TEXT = NULL
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
    DECLARE @esGanadoraActual BIT;
    DECLARE @idSolicitudCot   BIGINT;
    DECLARE @fechaCotizacionCot DATE;
    DECLARE @sumCuotasAprobadas DECIMAL(18, 2);

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idSolicitud, 0) = 0
        BEGIN SET @error = 20; SET @errormsg = 'El campo idSolicitud es obligatorio.'; RETURN; END

        SELECT @estadoSolicitud = estado FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud;
        IF @estadoSolicitud IS NULL
        BEGIN SET @error = 21; SET @errormsg = 'No existe la solicitud con idSolicitud = ' + CAST(@idSolicitud AS VARCHAR) + '.'; RETURN; END

        -- DESACOPLADO: ya no se exige solicitud APROBADA. Solo se bloquea si esta
        -- cerrada (PAGADA/RECHAZADA) y se exige que exista >=1 cuota aprobada.
        IF @estadoSolicitud IN ('PAGADA','RECHAZADA')
        BEGIN SET @error = 22; SET @errormsg = 'No se pueden agregar cotizaciones a una solicitud ' + @estadoSolicitud + '.'; RETURN; END

        IF NOT EXISTS (
            SELECT 1
            FROM tpex_DetalleSolicitud d
            JOIN tpex_SolicitudProveedor sp ON sp.idSolicitudProveedor = d.idSolicitudProveedor
            WHERE sp.idSolicitud = @idSolicitud
              AND sp.estado IN ('APROBADO','APROBADO_PARCIAL')
              AND d.esAprobado = 1
              AND d.eliminado = 0)
        BEGIN
            SET @error = 22;
            SET @errormsg = 'No hay cuotas aprobadas para cotizar. Apruebe al menos una cuota de un proveedor.';
            RETURN;
        END

        IF @fechaCotizacion IS NULL
        BEGIN SET @error = 23; SET @errormsg = 'El campo fechaCotizacion es obligatorio.'; RETURN; END

        IF ISNULL(@montoCompra, 0) <= 0
        BEGIN SET @error = 24; SET @errormsg = 'El montoCompra es obligatorio y debe ser mayor a cero.'; RETURN; END

        -- Validar montoCompra <= SUM cuotas aprobadas de proveedores APROBADO/APROBADO_PARCIAL.
        SELECT @sumCuotasAprobadas = ISNULL(SUM(d.montoAPagarUsd), 0)
        FROM tpex_DetalleSolicitud d
        JOIN tpex_SolicitudProveedor sp ON sp.idSolicitudProveedor = d.idSolicitudProveedor
        WHERE sp.idSolicitud = @idSolicitud
          AND sp.estado IN ('APROBADO','APROBADO_PARCIAL')
          AND d.esAprobado = 1
          AND d.eliminado = 0;

        IF @montoCompra > @sumCuotasAprobadas
        BEGIN
            SET @error = 25;
            SET @errormsg = 'El montoCompra (' + CAST(@montoCompra AS VARCHAR) +
                            ') supera la suma de cuotas aprobadas (' + CAST(@sumCuotasAprobadas AS VARCHAR) + ').';
            RETURN;
        END

        IF ISNULL(@idMoneda, 0) = 0
        BEGIN SET @error = 26; SET @errormsg = 'El campo idMoneda es obligatorio.'; RETURN; END

        IF NOT EXISTS (SELECT 1 FROM tpex_Monedas WHERE idMoneda = @idMoneda AND activo = 1)
        BEGIN SET @error = 27; SET @errormsg = 'La moneda no existe o esta inactiva.'; RETURN; END

        IF ISNULL(@codBanco, 0) = 0
        BEGIN SET @error = 28; SET @errormsg = 'El campo codBanco es obligatorio.'; RETURN; END

        IF NOT EXISTS (SELECT 1 FROM tch_banco WHERE codBanco = @codBanco)
        BEGIN SET @error = 29; SET @errormsg = 'No existe el banco con codBanco = ' + CAST(@codBanco AS VARCHAR) + '.'; RETURN; END

        IF ISNULL(@tipoCambioOfrecido, 0) <= 0
        BEGIN SET @error = 30; SET @errormsg = 'El tipoCambioOfrecido es obligatorio y debe ser mayor a cero.'; RETURN; END

        IF EXISTS (SELECT 1 FROM tpex_Cotizaciones WHERE idSolicitud = @idSolicitud AND fechaCotizacion = @fechaCotizacion AND codBanco = @codBanco)
        BEGIN SET @error = 31; SET @errormsg = 'Ya existe una cotizacion para este banco en esta solicitud y fecha.'; RETURN; END

        SET @montoConvertido = ISNULL(@montoConvertido, ROUND(@montoCompra * @tipoCambioOfrecido, 2));
        SET @totalBolivianos = ISNULL(@totalBolivianos, @montoConvertido);

        BEGIN TRY
            BEGIN TRANSACTION;
            INSERT INTO tpex_Cotizaciones (idSolicitud, fechaCotizacion, montoCompra, idMoneda, nroGiros,
                                           codBanco, tipoCambioOfrecido, montoConvertido, totalBolivianos,
                                           esGanadora, estado, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitud, @fechaCotizacion, @montoCompra, @idMoneda, ISNULL(@nroGiros, 1),
                    @codBanco, @tipoCambioOfrecido, @montoConvertido, @totalBolivianos,
                    0, 'VIGENTE', @observaciones, @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();

            INSERT INTO tpex_LogEstados (idSolicitud, idCotizacion, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitud, @idGenerado, NULL, 'VIGENTE', 'Cotizacion creada.', @audUsuario, GETDATE());

            COMMIT TRANSACTION;
            SET @errormsg = 'Cotizacion registrada correctamente.';
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
        IF ISNULL(@idCotizacion, 0) = 0
        BEGIN SET @error = 40; SET @errormsg = 'El campo idCotizacion es obligatorio.'; RETURN; END

        SELECT @estadoActual = estado FROM tpex_Cotizaciones WHERE idCotizacion = @idCotizacion;
        IF @estadoActual IS NULL
        BEGIN SET @error = 41; SET @errormsg = 'No existe la cotizacion.'; RETURN; END

        IF @estadoActual != 'VIGENTE'
        BEGIN SET @error = 42; SET @errormsg = 'Solo se modifican cotizaciones VIGENTES. Estado actual: ' + @estadoActual + '.'; RETURN; END

        IF ISNULL(@estado, '') != '' AND @estado != @estadoActual AND @estado NOT IN ('ACEPTADA','RECHAZADA','VENCIDA')
        BEGIN SET @error = 43; SET @errormsg = 'Estado invalido: ' + @estado + '. Permitidos: ACEPTADA, RECHAZADA, VENCIDA.'; RETURN; END

        IF ISNULL(@estado, '') = 'ACEPTADA'
        BEGIN
            IF EXISTS (SELECT 1 FROM tpex_Cotizaciones c
                       JOIN tpex_Cotizaciones c2 ON c2.idCotizacion = @idCotizacion
                       WHERE c.idSolicitud = c2.idSolicitud
                         AND c.fechaCotizacion = c2.fechaCotizacion
                         AND c.estado = 'ACEPTADA'
                         AND c.idCotizacion != @idCotizacion)
            BEGIN SET @error = 44; SET @errormsg = 'Ya existe una cotizacion ACEPTADA en esta ronda.'; RETURN; END
        END

        SELECT @idSolicitudCot = idSolicitud, @fechaCotizacionCot = fechaCotizacion FROM tpex_Cotizaciones WHERE idCotizacion = @idCotizacion;

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_Cotizaciones
            SET montoCompra        = ISNULL(@montoCompra, montoCompra),
                nroGiros           = ISNULL(@nroGiros, nroGiros),
                tipoCambioOfrecido = ISNULL(@tipoCambioOfrecido, tipoCambioOfrecido),
                montoConvertido    = ISNULL(@montoConvertido, montoConvertido),
                totalBolivianos    = ISNULL(@totalBolivianos, totalBolivianos),
                estado             = ISNULL(@estado, estado),
                esGanadora         = CASE WHEN @estado = 'ACEPTADA' THEN 1 ELSE esGanadora END,
                observaciones      = ISNULL(@observaciones, observaciones),
                audUsuario         = @audUsuario, audFecha = GETDATE()
            WHERE idCotizacion = @idCotizacion;

            IF ISNULL(@estado, '') != '' AND @estado != @estadoActual
            BEGIN
                INSERT INTO tpex_LogEstados (idSolicitud, idCotizacion, estadoAnterior, estadoNuevo, audUsuario, audFecha)
                VALUES (@idSolicitudCot, @idCotizacion, @estadoActual, @estado, @audUsuario, GETDATE());
            END

            -- Rechazar otras VIGENTES de la misma ronda al aceptar una
            IF ISNULL(@estado, '') = 'ACEPTADA'
            BEGIN
                INSERT INTO tpex_LogEstados (idSolicitud, idCotizacion, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                SELECT @idSolicitudCot, idCotizacion, 'VIGENTE', 'RECHAZADA', 'Rechazada por seleccion de ganadora.', @audUsuario, GETDATE()
                FROM tpex_Cotizaciones
                WHERE idCotizacion != @idCotizacion AND estado = 'VIGENTE'
                  AND idSolicitud = @idSolicitudCot AND fechaCotizacion = @fechaCotizacionCot;

                UPDATE tpex_Cotizaciones SET estado = 'RECHAZADA', audUsuario = @audUsuario, audFecha = GETDATE()
                WHERE idCotizacion != @idCotizacion AND estado = 'VIGENTE'
                  AND idSolicitud = @idSolicitudCot AND fechaCotizacion = @fechaCotizacionCot;
            END

            SET @idGenerado = @idCotizacion;
            COMMIT TRANSACTION;
            SET @errormsg = 'Cotizacion actualizada correctamente.';
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
        IF ISNULL(@idCotizacion, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'idCotizacion es obligatorio para eliminar.'; RETURN; END

        SELECT @estadoActual = estado, @esGanadoraActual = esGanadora FROM tpex_Cotizaciones WHERE idCotizacion = @idCotizacion;
        IF @estadoActual IS NULL
        BEGIN SET @error = 51; SET @errormsg = 'No existe la cotizacion.'; RETURN; END
        IF @estadoActual != 'VIGENTE'
        BEGIN SET @error = 52; SET @errormsg = 'Solo se eliminan cotizaciones VIGENTES.'; RETURN; END
        IF @esGanadoraActual = 1
        BEGIN SET @error = 53; SET @errormsg = 'No se puede eliminar la cotizacion ganadora.'; RETURN; END
        IF EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idCotizacion = @idCotizacion)
        BEGIN SET @error = 54; SET @errormsg = 'La cotizacion ya tiene transacciones asociadas.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_Cotizaciones SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idCotizacion = @idCotizacion;
            COMMIT TRANSACTION;
            SET @errormsg = 'Cotizacion eliminada correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al eliminar: ' + ERROR_MESSAGE();
        END CATCH;
        RETURN;
    END
END
GO

/* ===========================================================================
   2/2 · p_abm_tpex_Transacciones  (error 22: transaccionar con solicitud PENDIENTE)
   Reproduce el SP vigente; SOLO cambia la validacion del estado de la solicitud.
   =========================================================================== */
ALTER PROCEDURE [dbo].[p_abm_tpex_Transacciones]
    @ACCION VARCHAR(1)
    , @idTransaccion BIGINT = NULL
    , @numeroTransaccion VARCHAR(50) = NULL
    , @idSolicitud BIGINT = NULL
    , @idCotizacion BIGINT = NULL
    , @idTipoTransaccion BIGINT = NULL
    , @codBanco BIGINT = NULL
    , @idCanal BIGINT = NULL
    , @codEmpresa BIGINT = NULL
    , @cardCode VARCHAR(20) = NULL
    , @fechaTransaccion DATE = NULL
    , @fechaValor DATE = NULL
    , @montoOrigen DECIMAL(18, 2) = NULL
    , @idMonedaOrigen BIGINT = NULL
    , @tipoCambioAplicado DECIMAL(10, 6) = NULL
    , @montoConvertido DECIMAL(18, 2) = NULL
    , @idMonedaDestino BIGINT = NULL
    , @totalCargos DECIMAL(18, 2) = NULL
    , @totalFinal DECIMAL(18, 2) = NULL
    , @numeroContrato VARCHAR(50) = NULL
    , @fechaPactado DATE = NULL
    , @fechaVencimiento DATE = NULL
    , @tipoCambioForward DECIMAL(10, 6) = NULL
    , @tipoCambioReferencia DECIMAL(10, 6) = NULL
    , @equivalenteUsdRef DECIMAL(18, 2) = NULL
    , @diferenciaDeMas DECIMAL(18, 2) = NULL
    , @porcentajeDiferencia DECIMAL(10, 4) = NULL
    , @nombreExportadora VARCHAR(100) = NULL
    , @tcNegociadoExportadora DECIMAL(10, 6) = NULL
    , @comisionExportadora DECIMAL(18, 2) = NULL
    , @metodoExportadora VARCHAR(50) = NULL
    , @estado VARCHAR(20) = NULL
    , @observaciones TEXT = NULL
    , @rutaVoucher VARCHAR(500) = NULL
    , @idTransaccionOrigen BIGINT = NULL
    , @audUsuario INT = NULL
    , @error INT = 0 OUTPUT
    , @errormsg NVARCHAR(500) = '' OUTPUT
    , @idGenerado BIGINT = 0 OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0;
    SET @errormsg = '';
    SET @idGenerado = 0;

    DECLARE @estadoSolicitud VARCHAR(20);
    DECLARE @estadoActual VARCHAR(20);
    DECLARE @requiereForward BIT;
    DECLARE @requiereBanco BIT;
    DECLARE @codigoTipo VARCHAR(30);
    DECLARE @estadoCotizacion VARCHAR(20);
    DECLARE @idMonedaProveedor BIGINT;
    DECLARE @esPagoProveedor BIT;

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
        BEGIN
            IF ISNULL(@idTipoTransaccion, 0) = 0
                BEGIN SET @error = 23; SET @errormsg = 'idTipoTransaccion es obligatorio.'; RETURN; END

            SELECT @requiereForward = requiereForward, @requiereBanco = requiereBanco, @codigoTipo = codigo
            FROM tpex_TiposTransaccion WHERE idTipoTransaccion = @idTipoTransaccion AND activo = 1;
            IF @codigoTipo IS NULL
                BEGIN SET @error = 24; SET @errormsg = 'Tipo de transacción inexistente o inactivo.'; RETURN; END

            SET @esPagoProveedor = CASE WHEN @codigoTipo IN ('TC_DIRECTO', 'TC_NEGOCIADO', 'FORWARD', 'EXPORTADORA') THEN 1 ELSE 0 END;

            -- Solicitud: obligatoria solo para pagos a proveedor; si llega, se valida.
            IF @esPagoProveedor = 1 AND ISNULL(@idSolicitud, 0) = 0
                BEGIN SET @error = 20; SET @errormsg = 'idSolicitud es obligatorio para el tipo ' + @codigoTipo + '.'; RETURN; END

            IF ISNULL(@idSolicitud, 0) != 0
                BEGIN
                    SELECT @estadoSolicitud = estado FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud;
                    IF @estadoSolicitud IS NULL
                        BEGIN SET @error = 21; SET @errormsg = 'No existe la solicitud ' + CAST(@idSolicitud AS VARCHAR) + '.'; RETURN; END
                    -- DESACOPLADO: ya no se exige APROBADA. Se permite PENDIENTE/APROBADA;
                    -- la garantia de que hay algo aprobado llega via la cotizacion ACEPTADA (error 28).
                    IF @estadoSolicitud IN ('PAGADA', 'RECHAZADA')
                        BEGIN SET @error = 22; SET @errormsg = 'No se pueden crear transacciones sobre una solicitud ' + @estadoSolicitud + '.'; RETURN; END
                END

            IF @requiereBanco = 1 AND ISNULL(@codBanco, 0) = 0
                BEGIN SET @error = 25; SET @errormsg = 'El tipo ' + @codigoTipo + ' requiere codBanco.'; RETURN; END
            IF ISNULL(@codBanco, 0) != 0 AND NOT EXISTS (SELECT 1 FROM tch_banco WHERE codBanco = @codBanco)
                BEGIN SET @error = 26; SET @errormsg = 'No existe el banco ' + CAST(@codBanco AS VARCHAR) + '.'; RETURN; END

            IF ISNULL(@idCotizacion, 0) != 0
                BEGIN
                    SELECT @estadoCotizacion = estado FROM tpex_Cotizaciones WHERE idCotizacion = @idCotizacion;
                    IF @estadoCotizacion IS NULL
                        BEGIN SET @error = 27; SET @errormsg = 'No existe la cotización ' + CAST(@idCotizacion AS VARCHAR) + '.'; RETURN; END
                    IF @estadoCotizacion != 'ACEPTADA'
                        BEGIN SET @error = 28; SET @errormsg = 'La cotización debe estar ACEPTADA. Estado actual: ' + @estadoCotizacion + '.'; RETURN; END
                END

            IF ISNULL(@codEmpresa, 0) = 0
                BEGIN SET @error = 29; SET @errormsg = 'codEmpresa es obligatorio.'; RETURN; END

            -- cardCode: obligatorio solo para pagos a proveedor.
            IF @esPagoProveedor = 1 AND ISNULL(@cardCode, '') = ''
                BEGIN SET @error = 30; SET @errormsg = 'cardCode es obligatorio para el tipo ' + @codigoTipo + '.'; RETURN; END

            -- DEVOLUCION exige el vínculo a la transacción que se devuelve.
            IF @codigoTipo = 'DEVOLUCION' AND ISNULL(@idTransaccionOrigen, 0) = 0
                BEGIN SET @error = 45; SET @errormsg = 'DEVOLUCION requiere idTransaccionOrigen (transacción que se devuelve).'; RETURN; END
            IF ISNULL(@idTransaccionOrigen, 0) != 0
               AND NOT EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idTransaccion = @idTransaccionOrigen)
                BEGIN SET @error = 46; SET @errormsg = 'No existe la transacción origen ' + CAST(@idTransaccionOrigen AS VARCHAR) + '.'; RETURN; END

            IF @fechaTransaccion IS NULL
                BEGIN SET @error = 33; SET @errormsg = 'Fecha Transaccion es obligatorio.'; RETURN; END
            IF ISNULL(@montoOrigen, 0) <= 0
                BEGIN SET @error = 34; SET @errormsg = 'Monto Origen debe ser mayor a cero.'; RETURN; END
            IF ISNULL(@idMonedaOrigen, 0) = 0 OR NOT EXISTS (SELECT 1 FROM tpex_Monedas WHERE idMoneda = @idMonedaOrigen AND activo = 1)
                BEGIN SET @error = 35; SET @errormsg = 'idMonedaOrigen inválida o inactiva.'; RETURN; END
            IF ISNULL(@tipoCambioAplicado, 0) <= 0
                BEGIN SET @error = 36; SET @errormsg = 'Tipo Cambio Aplicado debe ser mayor a cero.'; RETURN; END
            IF ISNULL(@idMonedaDestino, 0) = 0 OR NOT EXISTS (SELECT 1 FROM tpex_Monedas WHERE idMoneda = @idMonedaDestino AND activo = 1)
                BEGIN SET @error = 37; SET @errormsg = 'idMonedaDestino inválida o inactiva.'; RETURN; END

            -- Misma moneda permitida en traspasos internos y devoluciones (p.ej. USD→USD).
            IF @idMonedaOrigen = @idMonedaDestino AND @codigoTipo NOT IN ('TRASPASO_MERCURY', 'DEVOLUCION')
                BEGIN
                    DECLARE @codigoMonedaIgual VARCHAR(5);
                    SELECT @codigoMonedaIgual = codigo FROM tpex_Monedas WHERE idMoneda = @idMonedaOrigen;
                    SET @error = 38;
                    SET @errormsg = 'idMonedaOrigen e idMonedaDestino no pueden ser la misma moneda (' + ISNULL(@codigoMonedaIgual, '?') + ') para el tipo ' + @codigoTipo + '.';
                    RETURN;
                END

            -- moneda origen = divisa que se compra (USD/EUR), destino = BOB.
            IF @codigoTipo IN ('TC_DIRECTO', 'TC_NEGOCIADO', 'FORWARD')
                BEGIN
                    DECLARE @codigoOrigenDir VARCHAR(5);
                    DECLARE @codigoDestinoDir VARCHAR(5);
                    SELECT @codigoOrigenDir  = codigo FROM tpex_Monedas WHERE idMoneda = @idMonedaOrigen;
                    SELECT @codigoDestinoDir = codigo FROM tpex_Monedas WHERE idMoneda = @idMonedaDestino;
                    IF @codigoOrigenDir = 'BOB'
                        BEGIN SET @error = 39; SET @errormsg = 'Para el tipo ' + @codigoTipo + ' la moneda origen debe ser la divisa que se compra (USD/EUR), no BOB.'; RETURN; END
                    IF @codigoDestinoDir != 'BOB'
                        BEGIN SET @error = 40; SET @errormsg = 'Para el tipo ' + @codigoTipo + ' la moneda destino debe ser BOB (el pago se realiza en bolivianos). Recibido: ' + ISNULL(@codigoDestinoDir,'?') + '.'; RETURN; END
                END

            IF @requiereForward = 1
                BEGIN
                    IF ISNULL(@tipoCambioForward, 0) = 0
                        BEGIN SET @error = 41; SET @errormsg = 'FORWARD requiere Tipo CambioForward.'; RETURN; END
                    IF @fechaVencimiento IS NULL
                        BEGIN SET @error = 42; SET @errormsg = 'FORWARD requiere Fecha Vencimiento del contrato.'; RETURN; END
                END

            IF @codigoTipo = 'EXPORTADORA'
                BEGIN
                    IF ISNULL(@nombreExportadora, '') = ''
                        BEGIN SET @error = 43; SET @errormsg = 'EXPORTADORA requiere nombreExportadora.'; RETURN; END
                    IF ISNULL(@tcNegociadoExportadora, 0) = 0
                        BEGIN SET @error = 44; SET @errormsg = 'EXPORTADORA requiere tcNegociadoExportadora.'; RETURN; END
                END

            -- divisa * TC = bolivianos.
            SET @montoConvertido = ROUND(@montoOrigen * @tipoCambioAplicado, 2);
            SET @totalFinal      = ROUND(@montoConvertido + ISNULL(@totalCargos, 0), 2);

            IF ISNULL(@tipoCambioReferencia, 0) = 0
                BEGIN
                    SELECT TOP 1 @tipoCambioReferencia = tasaVenta
                    FROM tpex_TiposCambio
                    WHERE codBanco IS NULL
                      AND idMonedaOrigen  = @idMonedaOrigen
                      AND idMonedaDestino = @idMonedaDestino
                    ORDER BY fechaVigencia DESC;
                END

            IF ISNULL(@tipoCambioReferencia, 0) > 0
                BEGIN
                    SET @equivalenteUsdRef    = ROUND(@totalFinal / @tipoCambioReferencia, 2);
                    SET @diferenciaDeMas      = ROUND(@equivalenteUsdRef - @montoOrigen, 2);
                    SET @porcentajeDiferencia = ROUND((@diferenciaDeMas / @montoOrigen) * 100, 4);
                END
            ELSE
                BEGIN
                    SET @equivalenteUsdRef    = 0;
                    SET @diferenciaDeMas      = 0;
                    SET @porcentajeDiferencia = 0;
                END

            BEGIN TRY
                BEGIN TRANSACTION;

                INSERT INTO tpex_Transacciones (
                    numeroTransaccion, idSolicitud, idCotizacion, idTipoTransaccion,
                    codBanco, idCanal, codEmpresa, cardCode,
                    fechaTransaccion, fechaValor,
                    montoOrigen, idMonedaOrigen, tipoCambioAplicado, montoConvertido, idMonedaDestino,
                    totalCargos, totalFinal,
                    numeroContrato, fechaPactado, fechaVencimiento,
                    tipoCambioForward, tipoCambioReferencia,
                    equivalenteUsdRef, diferenciaDeMas, porcentajeDiferencia,
                    nombreExportadora, tcNegociadoExportadora, comisionExportadora, metodoExportadora,
                    rutaVoucher, idTransaccionOrigen,
                    estado, observaciones, audUsuario, audFecha
                )
                VALUES (
                    NULL, NULLIF(@idSolicitud, 0), NULLIF(@idCotizacion, 0), @idTipoTransaccion,
                    NULLIF(@codBanco, 0), @idCanal, @codEmpresa, NULLIF(@cardCode, ''),
                    @fechaTransaccion, NULL,
                    @montoOrigen, @idMonedaOrigen, @tipoCambioAplicado, @montoConvertido, @idMonedaDestino,
                    ISNULL(@totalCargos, 0), @totalFinal,
                    @numeroContrato, @fechaPactado, @fechaVencimiento,
                    @tipoCambioForward, @tipoCambioReferencia,
                    @equivalenteUsdRef, @diferenciaDeMas, @porcentajeDiferencia,
                    @nombreExportadora, @tcNegociadoExportadora, @comisionExportadora, @metodoExportadora,
                    NULL,
                    NULLIF(@idTransaccionOrigen, 0),
                    'PENDIENTE', @observaciones, @audUsuario, GETDATE()
                );

                SET @idGenerado = SCOPE_IDENTITY();

                INSERT INTO dbo.tpex_LogEstados (idSolicitud, idCotizacion, idTransaccion, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                VALUES (NULL, NULL, @idGenerado, NULL, 'PENDIENTE', 'Transacción creada.', @audUsuario, GETDATE());

                COMMIT TRANSACTION;
                SET @error    = 0;
                SET @errormsg = 'Transacción registrada correctamente.';
            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
                SET @error    = 99;
                SET @errormsg = 'Error al insertar: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
                SET @idGenerado = 0;
            END CATCH;
            RETURN;
        END

    -- ================================================================
    -- UPDATE
    -- ================================================================
    IF @ACCION = 'U'
        BEGIN
            IF ISNULL(@idTransaccion, 0) = 0
                BEGIN SET @error = 50; SET @errormsg = 'idTransaccion es obligatorio para actualizar.'; RETURN; END

            SELECT @estadoActual = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion AND eliminado = 0;
            IF @estadoActual IS NULL
                BEGIN SET @error = 51; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END
            IF @estadoActual IN ('CONFIRMADO', 'CANCELADO')
                BEGIN SET @error = 52; SET @errormsg = 'No se puede modificar una transacción en estado ' + @estadoActual + '.'; RETURN; END

            IF ISNULL(@estado, '') != '' AND @estado != @estadoActual
                BEGIN
                    IF NOT (
                        (@estadoActual = 'PENDIENTE'  AND @estado IN ('PROCESADO', 'CANCELADO'))
                        OR (@estadoActual = 'PROCESADO' AND @estado IN ('CONFIRMADO', 'CANCELADO'))
                    )
                    BEGIN
                        SET @error = 53;
                        SET @errormsg = 'Transición no permitida: ' + @estadoActual + ' → ' + @estado +
                                        '. Permitidas: PENDIENTE→PROCESADO, PENDIENTE→CANCELADO, ' +
                                        'PROCESADO→CONFIRMADO, PROCESADO→CANCELADO.';
                        RETURN;
                    END
                END

            DECLARE @tcRefActual       DECIMAL(10, 6);
            DECLARE @totalFinalActual  DECIMAL(18, 2);
            DECLARE @montoOrigenActual DECIMAL(18, 2);

            SELECT @tcRefActual       = tipoCambioReferencia,
                   @totalFinalActual  = totalFinal,
                   @montoOrigenActual = montoOrigen
            FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;

            DECLARE @tcRefFinal DECIMAL(10, 6) = ISNULL(@tipoCambioReferencia, @tcRefActual);
            DECLARE @tfFinal    DECIMAL(18, 2) = ISNULL(@totalFinal, @totalFinalActual);

            IF ISNULL(@tcRefFinal, 0) > 0 AND @tfFinal > 0
                BEGIN
                    SET @equivalenteUsdRef    = ROUND(@tfFinal / @tcRefFinal, 2);
                    SET @diferenciaDeMas      = ROUND(@equivalenteUsdRef - @montoOrigenActual, 2);
                    SET @porcentajeDiferencia = ROUND((@diferenciaDeMas / @montoOrigenActual) * 100, 4);
                END

            BEGIN TRY
                BEGIN TRANSACTION;

                UPDATE tpex_Transacciones
                SET fechaValor           = ISNULL(@fechaValor,           fechaValor),
                    numeroTransaccion    = ISNULL(@numeroTransaccion,    numeroTransaccion),
                    totalCargos          = ISNULL(@totalCargos,          totalCargos),
                    totalFinal           = ISNULL(@totalFinal,           totalFinal),
                    tipoCambioReferencia = ISNULL(@tipoCambioReferencia, tipoCambioReferencia),
                    equivalenteUsdRef    = ISNULL(@equivalenteUsdRef,    equivalenteUsdRef),
                    diferenciaDeMas      = ISNULL(@diferenciaDeMas,      diferenciaDeMas),
                    porcentajeDiferencia = ISNULL(@porcentajeDiferencia, porcentajeDiferencia),
                    comisionExportadora  = ISNULL(@comisionExportadora,  comisionExportadora),
                    rutaVoucher          = ISNULL(@rutaVoucher,          rutaVoucher),
                    idTransaccionOrigen  = ISNULL(NULLIF(@idTransaccionOrigen, 0), idTransaccionOrigen),
                    estado               = ISNULL(@estado,               estado),
                    observaciones        = ISNULL(@observaciones,        observaciones),
                    audUsuario           = @audUsuario,
                    audFecha             = GETDATE()
                WHERE idTransaccion = @idTransaccion;

                IF ISNULL(@estado, '') != '' AND @estado != @estadoActual
                    BEGIN
                        INSERT INTO dbo.tpex_LogEstados (idSolicitud, idCotizacion, idTransaccion, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                        VALUES (NULL, NULL, @idTransaccion, @estadoActual, @estado, NULL, @audUsuario, GETDATE());
                    END

                SET @idGenerado = @idTransaccion;
                COMMIT TRANSACTION;
                SET @error    = 0;
                SET @errormsg = 'Transacción actualizada correctamente.';
            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
                SET @error    = 99;
                SET @errormsg = 'Error al actualizar: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            END CATCH;
            RETURN;
        END

    -- ================================================================
    -- DELETE
    -- ================================================================
    IF @ACCION = 'D'
        BEGIN
            IF ISNULL(@idTransaccion, 0) = 0
                BEGIN SET @error = 60; SET @errormsg = 'idTransaccion es obligatorio para eliminar.'; RETURN; END

            SELECT @estadoActual = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion AND eliminado = 0;
            IF @estadoActual IS NULL
                BEGIN SET @error = 61; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END
            IF @estadoActual != 'PENDIENTE'
                BEGIN SET @error = 62; SET @errormsg = 'Solo se pueden eliminar transacciones PENDIENTES. Estado actual: ' + @estadoActual + '.'; RETURN; END
            IF EXISTS (SELECT 1 FROM tpex_Cargos WHERE idTransaccion = @idTransaccion)
                BEGIN SET @error = 63; SET @errormsg = 'La transacción tiene cargos. Elimine primero los cargos.'; RETURN; END
            IF EXISTS (SELECT 1 FROM tpex_TransaccionParticipantes WHERE idTransaccion = @idTransaccion)
                BEGIN SET @error = 64; SET @errormsg = 'La transacción tiene participantes. Elimine primero los participantes.'; RETURN; END
            IF EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idTransaccionOrigen = @idTransaccion)
                BEGIN SET @error = 65; SET @errormsg = 'La transacción tiene devoluciones vinculadas. Elimine primero las devoluciones.'; RETURN; END

            BEGIN TRY
                BEGIN TRANSACTION;
                UPDATE tpex_Transacciones SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idTransaccion = @idTransaccion;
                COMMIT TRANSACTION;
                SET @error    = 0;
                SET @errormsg = 'Transacción eliminada correctamente.';
            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
                SET @error    = 99;
                SET @errormsg = 'Error al eliminar: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            END CATCH;
            RETURN;
        END
END
GO
