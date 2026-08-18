/* ============================================================================
   SOFT DELETE — Entidad 1/9: TRANSACCIONES
   Fase 2 (ABM): la accion 'D' ya NO borra fisico -> UPDATE eliminado=1 (mantiene
                 las guardas: solo PENDIENTE, sin cargos/participantes/devoluciones).
                 Las validaciones de existencia (U y D) ignoran filas eliminado=1.
   Fase 3 (lecturas): p_list_tpex_Transacciones filtra t.eliminado=0 en L/R/C/B.
   REQUISITO: correr antes 2026-06-17_soft_delete_01_schema.sql (columna eliminado).
   Ejecutar con un usuario de ESCRITURA.
   ============================================================================ */

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
            -- El tipo se valida primero: define qué campos son obligatorios.
            IF ISNULL(@idTipoTransaccion, 0) = 0
                BEGIN SET @error = 23; SET @errormsg = 'idTipoTransaccion es obligatorio.'; RETURN; END

            SELECT @requiereForward = requiereForward, @requiereBanco = requiereBanco, @codigoTipo = codigo
            FROM tpex_TiposTransaccion WHERE idTipoTransaccion = @idTipoTransaccion AND activo = 1;
            IF @codigoTipo IS NULL
                BEGIN SET @error = 24; SET @errormsg = 'Tipo de transacción inexistente o inactivo.'; RETURN; END

            SET @esPagoProveedor = CASE WHEN @codigoTipo IN ('TC_DIRECTO', 'TC_NEGOCIADO', 'FORWARD', 'EXPORTADORA') THEN 1 ELSE 0 END;

            -- Solicitud: obligatoria solo para pagos a proveedor; si llega, se valida siempre.
            IF @esPagoProveedor = 1 AND ISNULL(@idSolicitud, 0) = 0
                BEGIN SET @error = 20; SET @errormsg = 'idSolicitud es obligatorio para el tipo ' + @codigoTipo + '.'; RETURN; END

            IF ISNULL(@idSolicitud, 0) != 0
                BEGIN
                    SELECT @estadoSolicitud = estado FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud;
                    IF @estadoSolicitud IS NULL
                        BEGIN SET @error = 21; SET @errormsg = 'No existe la solicitud ' + CAST(@idSolicitud AS VARCHAR) + '.'; RETURN; END
                    IF @estadoSolicitud != 'APROBADA'
                        BEGIN SET @error = 22; SET @errormsg = 'Solo se pueden crear transacciones sobre solicitudes APROBADAS. Estado actual: ' + @estadoSolicitud + '.'; RETURN; END
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

            -- ► FIX 1: moneda origen = divisa que se compra (USD/EUR), destino = BOB.
            --   El dato primario es el monto en divisa; el pago se hace en bolivianos.
            --   (Antes exigía origen = BOB, lo que contradecía la precarga de la UI.)
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

            -- ► FIX 2: divisa * TC = bolivianos.  Bs = USD * TC (ej. 5.000 * 6,97 = 34.850).
            --   Coincide con la cotización (montoCompra * TC = totalBolivianos), con las
            --   planillas Excel y con el resto de este SP (equivalenteUsdRef = totalFinal / TCref).
            --   (Antes: montoOrigen / TC, que guardaba un valor ~48x menor que el mostrado en la UI.)
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
                    NULL,  -- voucher siempre NULL al crear
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

/* ============================================================================
   2026-06-16 · p_list_tpex_Transacciones · agrega idMonedaOrigen/idMonedaDestino
   Las acciones de lista (L, C, B) devolvian el CODIGO de moneda (mo.codigo) pero
   NO el id numerico. El dialogo "Confirmar Pago" resuelve el simbolo por id, y al
   llegar en 0 mostraba "?". Se agregan t.idMonedaOrigen/t.idMonedaDestino al SELECT
   de L, C y B (la accion R ya los trae via t.*). Solo agrega columnas: no rompe
   nada existente. Ejecutar con un usuario de ESCRITURA.
   ============================================================================ */
ALTER PROCEDURE [dbo].[p_list_tpex_Transacciones]

      @ACCION               VARCHAR(1)    = NULL
    , @idTransaccion        BIGINT        = NULL
    , @numeroTransaccion    VARCHAR(50)   = NULL
    , @idSolicitud          BIGINT        = NULL
    , @idCotizacion         BIGINT        = NULL
    , @idTipoTransaccion    BIGINT        = NULL
    , @codBanco             BIGINT        = NULL
    , @idCanal              BIGINT        = NULL
    , @codEmpresa           INT           = NULL
    , @cardCode             VARCHAR(20)   = NULL
    , @fechaTransaccion     DATE          = NULL
    , @fechaValor           DATE          = NULL
    , @montoOrigen          DECIMAL(18,2) = NULL
    , @idMonedaOrigen       BIGINT        = NULL
    , @tipoCambioAplicado   DECIMAL(18,6) = NULL
    , @montoConvertido      DECIMAL(18,2) = NULL
    , @idMonedaDestino      BIGINT        = NULL
    , @totalCargos          DECIMAL(18,2) = NULL
    , @totalFinal           DECIMAL(18,2) = NULL
    , @numeroContrato       VARCHAR(50)   = NULL
    , @fechaPactado         DATE          = NULL
    , @fechaVencimiento     DATE          = NULL
    , @tipoCambioForward    DECIMAL(18,6) = NULL
    , @tipoCambioReferencia DECIMAL(18,6) = NULL
    , @equivalenteUsdRef    DECIMAL(18,6) = NULL
    , @diferenciaDeMas      DECIMAL(18,2) = NULL
    , @porcentajeDiferencia DECIMAL(5,2)  = NULL
    , @nombreExportadora    VARCHAR(300)  = NULL
    , @tcNegociadoExportadora DECIMAL(18,6) = NULL
    , @comisionExportadora  DECIMAL(18,2) = NULL
    , @metodoExportadora    VARCHAR(100)  = NULL
    , @estado               VARCHAR(30)   = NULL
    , @observaciones        VARCHAR(400)  = NULL
    , @audUsuario           BIGINT        = NULL
    , @fechaInicio          DATE          = NULL
    , @fechaFin             DATE          = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @sqlProv  NVARCHAR(MAX);
    DECLARE @tempProv TABLE (
        cardCode  VARCHAR(10),
        cardName  VARCHAR(300),
        moneda    VARCHAR(5)
    );

    IF @codEmpresa IS NOT NULL
    BEGIN
        SET @sqlProv = N'SELECT * FROM OPENQUERY(SRV_2022,
            ''EXEC [CONEXION].[dbo].[p_list_Proveedores]
            @codEmpresa = ' + CONVERT(VARCHAR(5), @codEmpresa) + ',
            @ACCION = ''''A'''''')';
        INSERT INTO @tempProv EXEC sp_executesql @sqlProv;
    END

    -- ================================================================
    -- L: Grilla de transacciones por solicitud
    -- ================================================================
    IF @ACCION = 'L'
    BEGIN
        SELECT
             t.idTransaccion
            ,t.numeroTransaccion
            ,t.idMonedaOrigen
            ,t.idMonedaDestino
            ,t.idSolicitud
            ,t.idTipoTransaccion
            ,tt.nombre              AS tipoTransaccion
            ,t.codBanco
            ,b.nombre               AS banco
            ,t.cardCode
            ,ISNULL(p.cardName, '') AS proveedor
            ,t.fechaTransaccion
            ,t.fechaValor
            ,t.montoOrigen
            ,mo.codigo              AS monedaOrigen
            ,t.tipoCambioAplicado
            ,t.montoConvertido
            ,md.codigo              AS monedaDestino
            ,t.totalCargos
            ,t.totalFinal
            ,t.numeroContrato
            ,t.tipoCambioForward
            ,t.tipoCambioReferencia
            ,t.equivalenteUsdRef
            ,t.diferenciaDeMas
            ,t.porcentajeDiferencia
            ,t.nombreExportadora
            ,t.tcNegociadoExportadora
            ,t.comisionExportadora
            ,t.metodoExportadora
            ,t.tieneVoucher
            ,t.idTransaccionOrigen
            ,t.estado
        FROM tpex_Transacciones     t
        JOIN  tpex_TiposTransaccion tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN  tpex_Monedas          mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN  tpex_Monedas          md ON md.idMoneda          = t.idMonedaDestino
        LEFT JOIN @tempProv         p  ON p.cardCode           = t.cardCode
        LEFT JOIN tch_banco         b  ON b.codBanco           = t.codBanco
        WHERE t.eliminado = 0 AND (@idSolicitud       IS NULL OR t.idSolicitud       = @idSolicitud)
          AND (@cardCode          IS NULL OR t.cardCode          = @cardCode)
          AND (@codBanco          IS NULL OR t.codBanco          = @codBanco)
          AND (@estado            IS NULL OR t.estado            = @estado)
          AND (@idTipoTransaccion IS NULL OR t.idTipoTransaccion = @idTipoTransaccion)
        ORDER BY t.fechaTransaccion DESC;
    END

    -- ================================================================
    -- R: Registro completo (formulario)
    -- ================================================================
    IF @ACCION = 'R'
    BEGIN
        SELECT
             t.*
            ,tt.nombre              AS tipoTransaccion
            ,tt.requiereForward
            ,tt.requiereBanco
            ,b.nombre               AS banco
            ,ISNULL(p.cardName, '') AS proveedor
            ,mo.codigo              AS monedaOrigen
            ,md.codigo              AS monedaDestino
            ,e.nombre               AS empresa
        FROM tpex_Transacciones     t
        JOIN  tpex_TiposTransaccion tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN  tpex_Monedas          mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN  tpex_Monedas          md ON md.idMoneda          = t.idMonedaDestino
        LEFT JOIN @tempProv         p  ON p.cardCode           = t.cardCode
        JOIN  tb_empresa            e  ON e.codEmpresa         = t.codEmpresa
        LEFT JOIN tch_banco         b  ON b.codBanco           = t.codBanco
        WHERE t.eliminado = 0 AND t.idTransaccion = @idTransaccion;
    END

    -- ================================================================
    -- C: Transacciones por cotización
    -- ================================================================
    IF @ACCION = 'C'
    BEGIN
        SELECT
             t.idTransaccion
            ,t.numeroTransaccion
            ,t.idMonedaOrigen
            ,t.idMonedaDestino
            ,t.idSolicitud
            ,t.idCotizacion
            ,t.idTipoTransaccion
            ,tt.nombre              AS tipoTransaccion
            ,t.codBanco
            ,b.nombre               AS banco
            ,t.cardCode
            ,ISNULL(p.cardName, '') AS proveedor
            ,t.fechaTransaccion
            ,t.fechaValor
            ,t.montoOrigen
            ,mo.codigo              AS monedaOrigen
            ,t.tipoCambioAplicado
            ,t.montoConvertido
            ,md.codigo              AS monedaDestino
            ,t.totalCargos
            ,t.totalFinal
            ,t.numeroContrato
            ,t.tipoCambioForward
            ,t.tipoCambioReferencia
            ,t.equivalenteUsdRef
            ,t.diferenciaDeMas
            ,t.porcentajeDiferencia
            ,t.nombreExportadora
            ,t.tcNegociadoExportadora
            ,t.comisionExportadora
            ,t.metodoExportadora
            ,t.tieneVoucher
            ,t.idTransaccionOrigen
            ,t.estado
        FROM tpex_Transacciones     t
        JOIN  tpex_TiposTransaccion tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN  tpex_Monedas          mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN  tpex_Monedas          md ON md.idMoneda          = t.idMonedaDestino
        LEFT JOIN @tempProv         p  ON p.cardCode           = t.cardCode
        LEFT JOIN tch_banco         b  ON b.codBanco           = t.codBanco
        WHERE t.eliminado = 0 AND t.idCotizacion = @idCotizacion
        ORDER BY t.fechaTransaccion DESC;
    END

    -- ================================================================
    -- B: Reporte entre fechas  (usado por Cobranzas - Asientos)
    -- ================================================================
    IF @ACCION = 'B'
    BEGIN
        SELECT
             t.idTransaccion
            ,t.numeroTransaccion
            ,t.idMonedaOrigen
            ,t.idMonedaDestino
            ,e.nombre                           AS empresa
            ,t.cardCode
            ,t.codBanco
            ,ISNULL(tp.cardName, ISNULL(t.cardCode, '')) AS proveedor
            ,tt.nombre                          AS tipoTransaccion
            ,b.nombre                           AS banco
            ,t.fechaTransaccion
            ,t.montoOrigen
            ,mo.codigo                          AS monedaOrigen
            ,t.tipoCambioAplicado
            ,t.montoConvertido                  -- NUEVO: faltaba (chip "Convertido" daba 0)
            ,t.totalCargos
            ,t.totalFinal
            ,md.codigo                          AS monedaDestino
            ,t.diferenciaDeMas
            ,t.porcentajeDiferencia
            ,t.tieneVoucher
            ,t.idTransaccionOrigen
            ,t.estado
        FROM tpex_Transacciones     t
        JOIN  tpex_TiposTransaccion tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN  tpex_Monedas          mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN  tpex_Monedas          md ON md.idMoneda          = t.idMonedaDestino
        LEFT JOIN @tempProv         tp ON tp.cardCode          = t.cardCode
        JOIN  tb_empresa            e  ON e.codEmpresa         = t.codEmpresa
        LEFT JOIN tch_banco         b  ON b.codBanco           = t.codBanco
        WHERE t.eliminado = 0 AND (@fechaInicio       IS NULL OR t.fechaTransaccion  >= @fechaInicio)
          AND (@fechaFin          IS NULL OR t.fechaTransaccion  <= @fechaFin)
          AND (@estado            IS NULL OR t.estado            = @estado)
          AND (@cardCode          IS NULL OR t.cardCode          = @cardCode)
          AND (@idTipoTransaccion IS NULL OR t.idTipoTransaccion = @idTipoTransaccion)
        ORDER BY t.fechaTransaccion DESC;
    END
END

GO

-- Verificacion
SELECT
  (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_Transacciones')  AND definition LIKE '%UPDATE tpex_Transacciones SET eliminado = 1%') AS abm_soft_ok,
  (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_Transacciones')  AND definition LIKE '%DELETE FROM tpex_Transacciones%')              AS abm_delete_residual,
  (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_list_tpex_Transacciones') AND definition LIKE '%eliminado = 0%')                               AS list_filtra_ok;
