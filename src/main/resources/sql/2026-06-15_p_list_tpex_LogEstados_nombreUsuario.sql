/* ============================================================================
   2026-06-15  ·  p_list_tpex_LogEstados  ·  agregar nombreUsuario
   ----------------------------------------------------------------------------
   Objetivo:
     El timeline / historial mostraba "Usuario: 34" (solo el codUsuario).
     Se agrega la columna derivada [nombreUsuario] resolviendo el nombre real
     a partir de audUsuario (= codUsuario) mediante la cadena:

         tpex_LogEstados.audUsuario
            -> tb_usuario.codUsuario   (LEFT JOIN)
            -> tb_empleado.codEmpleado (LEFT JOIN)
            -> trh_persona.codPersona  (LEFT JOIN)   => nombres + apPaterno + apMaterno

     Fallback en cascada (por si no hay empleado/persona):
         nombre completo  ->  login del usuario  ->  'Usuario #<id>'

   Se aplica a las 3 acciones que devuelven historial: L, B, F.
   BeanPropertyRowMapper mapea por nombre de columna, así que basta con que el
   SELECT devuelva [nombreUsuario]; el orden de columnas es indiferente.
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
        WHERE (@idSolicitud   IS NULL OR l.idSolicitud   = @idSolicitud)
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
        WHERE (@fechaInicio IS NULL OR CAST(l.audFecha AS DATE) >= @fechaInicio)
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
        WHERE l.idSolicitud = @idSolicitud  -- cambios de la solicitud
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
