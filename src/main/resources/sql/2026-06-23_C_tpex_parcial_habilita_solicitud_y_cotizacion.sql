    /* ============================================================================
       TPEX · APROBADO_PARCIAL habilita aprobar solicitud y cotizar
       Fecha: 2026-06-23 (C) — EJECUTAR CON USUARIO DE ESCRITURA
       Requiere: migraciones A y B de 2026-06-23.

       Problema
       --------
       Dos validaciones solo reconocían el estado 'APROBADO' del proveedor y
       excluían 'APROBADO_PARCIAL', bloqueando todo el flujo cuando se aprueba
       parcialmente:

         1) p_abm_tpex_SolicitudPago (error 35): para pasar la solicitud a APROBADA
            exigía >=1 proveedor en estado EXACTO 'APROBADO'. Un proveedor
            APROBADO_PARCIAL no contaba -> no se podía aprobar la solicitud.

         2) p_abm_tpex_Cotizaciones (error 25): el tope del montoCompra sumaba las
            cuotas aprobadas SOLO de proveedores en estado 'APROBADO'. Con un
            proveedor APROBADO_PARCIAL la suma daba 0 -> cualquier monto fallaba.

       Fix
       ---
       Ambas pasan a reconocer estado IN ('APROBADO','APROBADO_PARCIAL'). En el
       tope de cotización se mantiene d.esAprobado = 1, de modo que solo se cotiza
       sobre las cuotas efectivamente aprobadas (las pendientes del parcial quedan
       fuera).
       ============================================================================ */

    SET ANSI_NULLS ON;
    SET QUOTED_IDENTIFIER ON;
    GO

    /* ===========================================================================
       1/2 · p_abm_tpex_SolicitudPago  (error 35: contar tambien APROBADO_PARCIAL)
       =========================================================================== */
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

                -- VALIDACION CLAVE: Para pasar a APROBADA, debe haber al menos 1 proveedor
                -- APROBADO o APROBADO_PARCIAL (basta con que algo este aprobado para pagar).
                IF @estado = 'APROBADA'
                BEGIN
                    SELECT @countProvAprobados = COUNT(*)
                    FROM tpex_SolicitudProveedor
                    WHERE idSolicitud = @idSolicitud AND estado IN ('APROBADO','APROBADO_PARCIAL');

                    IF @countProvAprobados = 0
                    BEGIN
                        SET @error = 35;
                        SET @errormsg = 'No se puede aprobar la solicitud: debe haber al menos 1 proveedor APROBADO o APROBADO_PARCIAL.';
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

    /* ===========================================================================
       2/2 · p_abm_tpex_Cotizaciones  (error 25: tope incluye APROBADO_PARCIAL)
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

            -- VALIDACION CLAVE: Solo cotizar si solicitud esta APROBADA
            IF @estadoSolicitud != 'APROBADA'
            BEGIN
                SET @error = 22;
                SET @errormsg = 'No se pueden agregar cotizaciones a una solicitud con estado ' + @estadoSolicitud +
                                '. La solicitud debe estar APROBADA (al menos 1 proveedor APROBADO o APROBADO_PARCIAL).';
                RETURN;
            END

            IF @fechaCotizacion IS NULL
            BEGIN SET @error = 23; SET @errormsg = 'El campo fechaCotizacion es obligatorio.'; RETURN; END

            IF ISNULL(@montoCompra, 0) <= 0
            BEGIN SET @error = 24; SET @errormsg = 'El montoCompra es obligatorio y debe ser mayor a cero.'; RETURN; END

            -- Validar montoCompra <= SUM cuotas aprobadas de proveedores APROBADO/APROBADO_PARCIAL.
            -- Se mantiene d.esAprobado = 1: en parcial solo cuentan las cuotas aprobadas.
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
