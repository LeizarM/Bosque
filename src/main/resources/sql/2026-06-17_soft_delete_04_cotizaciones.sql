/* SOFT DELETE - Entidad 2/9: COTIZACIONES
   Fase 2 (ABM p_abm_tpex_Cotizaciones): accion D -> UPDATE eliminado=1 (mantiene guardas).
   Fase 3 (p_list_tpex_Cotizaciones): filtra c.eliminado=0 en L/R/B.
   REQUISITO: 01_schema. Ejecutar con usuario de ESCRITURA. */
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
                            '. La solicitud debe estar APROBADA (al menos 1 proveedor APROBADO).';
            RETURN;
        END

        IF @fechaCotizacion IS NULL
        BEGIN SET @error = 23; SET @errormsg = 'El campo fechaCotizacion es obligatorio.'; RETURN; END

        IF ISNULL(@montoCompra, 0) <= 0
        BEGIN SET @error = 24; SET @errormsg = 'El montoCompra es obligatorio y debe ser mayor a cero.'; RETURN; END

        -- Validar montoCompra <= SUM cuotas aprobadas de proveedores APROBADOS
        SELECT @sumCuotasAprobadas = ISNULL(SUM(d.montoAPagarUsd), 0)
        FROM tpex_DetalleSolicitud d
        JOIN tpex_SolicitudProveedor sp ON sp.idSolicitudProveedor = d.idSolicitudProveedor
        WHERE sp.idSolicitud = @idSolicitud
          AND sp.estado = 'APROBADO'
          AND d.esAprobado = 1;

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


ALTER PROCEDURE [dbo].[p_list_tpex_Cotizaciones]

      @ACCION          VARCHAR(1)  = NULL
    , @idCotizacion    BIGINT      = NULL
    , @idSolicitud     BIGINT      = NULL
    , @fechaCotizacion DATE        = NULL
    , @montoCompra     DECIMAL(18,2) = NULL
    , @idMoneda        INT          = NULL
    , @nroGiros        INT          = NULL
    , @codBanco        INT          = NULL
    , @tipoCambioOfrecido DECIMAL(18,6) = NULL
    , @montoConvertido  DECIMAL(18,2) = NULL
    , @totalBolivianos  DECIMAL(18,2) = NULL
    , @esGanadora       BIT          = NULL
    , @estado           VARCHAR(20)  = NULL
    , @observaciones    VARCHAR(500) = NULL
    , @audUsuario       BIGINT  = NULL
    , @fechaInicio     DATE        = NULL
    , @fechaFin        DATE        = NULL

  
AS
BEGIN
    SET NOCOUNT ON;

    -- ================================================================
    -- L: Cotizaciones de una solicitud (grilla comparativa de bancos)
    -- ================================================================
    IF @ACCION = 'L'
    BEGIN
        SELECT
             c.idCotizacion
            ,c.idSolicitud
            ,c.fechaCotizacion
            ,c.montoCompra
            ,m.codigo           AS moneda
            ,c.nroGiros
            ,c.codBanco
            ,b.nombre           AS banco
            ,c.tipoCambioOfrecido
            ,c.montoConvertido
            ,c.totalBolivianos
            ,c.esGanadora
            ,c.estado
            ,c.observaciones
            ,c.audUsuario
            ,c.audFecha
        FROM tpex_Cotizaciones c
        JOIN tch_banco         b ON b.codBanco  = c.codBanco
        JOIN tpex_Monedas      m ON m.idMoneda  = c.idMoneda
        WHERE c.eliminado = 0 AND (@idSolicitud  IS NULL OR c.idSolicitud  = @idSolicitud)
          AND (@codBanco      IS NULL OR c.codBanco     = @codBanco)
          AND (@estado        IS NULL OR c.estado       = @estado)
        ORDER BY c.totalBolivianos ASC  -- de menor a mayor costo
    END

    -- ================================================================
    -- R: Un solo registro (formulario de edición)
    -- ================================================================
    IF @ACCION = 'R'
    BEGIN
        SELECT
             c.idCotizacion
            ,c.idSolicitud
            ,c.fechaCotizacion
            ,c.montoCompra
            ,c.idMoneda
            ,m.codigo           AS moneda
            ,c.nroGiros
            ,c.codBanco
            ,b.nombre           AS banco
            ,c.tipoCambioOfrecido
            ,c.montoConvertido
            ,c.totalBolivianos
            ,c.esGanadora
            ,c.estado
            ,c.observaciones
            ,c.audUsuario
            ,c.audFecha
        FROM tpex_Cotizaciones c
        JOIN tch_banco         b ON b.codBanco = c.codBanco
        JOIN tpex_Monedas      m ON m.idMoneda = c.idMoneda
        WHERE c.eliminado = 0 AND c.idCotizacion = @idCotizacion
    END

    -- ================================================================
    -- B: Reporte entre fechas con datos completos de la solicitud
    -- ================================================================
    IF @ACCION = 'B'
    BEGIN
        SELECT
             s.idSolicitud
            ,e.nombre           AS empresa
            ,s.fechaSolicitud
            ,s.montoTotalSolicitud
            ,s.estado           AS estadoSolicitud
            ,c.idCotizacion
            ,b.nombre           
            ,c.fechaCotizacion
            ,c.montoCompra
            ,m.codigo           AS moneda
            ,c.tipoCambioOfrecido
            ,c.totalBolivianos
            ,c.esGanadora
            ,c.estado           
            ,c.montoConvertido
            ,c.nroGiros
        FROM tpex_Cotizaciones  c
        JOIN tpex_SolicitudPago s ON s.idSolicitud = c.idSolicitud
        JOIN tch_banco          b ON b.codBanco    = c.codBanco
        JOIN tpex_Monedas       m ON m.idMoneda    = c.idMoneda
        JOIN tb_empresa         e ON e.codEmpresa  = s.codEmpresa
        WHERE c.eliminado = 0 AND (@fechaInicio IS NULL OR c.fechaCotizacion >= @fechaInicio)
          AND (@fechaFin    IS NULL OR c.fechaCotizacion <= @fechaFin)
          AND (@idSolicitud IS NULL OR c.idSolicitud     = @idSolicitud)
          AND (@estado      IS NULL OR c.estado          = @estado)
        ORDER BY c.fechaCotizacion DESC, c.totalBolivianos ASC
    END

END

GO

SELECT
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_Cotizaciones')  AND definition LIKE '%UPDATE tpex_Cotizaciones SET eliminado = 1%') AS abm_soft_ok,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_Cotizaciones')  AND definition LIKE '%DELETE FROM tpex_Cotizaciones%')               AS abm_delete_residual,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_list_tpex_Cotizaciones') AND definition LIKE '%c.eliminado = 0%')                              AS list_filtra_ok;
