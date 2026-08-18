/* ============================================================================
   2026-06-16  ·  OPCIÓN B · Operaciones de TESORERÍA (sin solicitud/cotización)
   ----------------------------------------------------------------------------
   El SP p_abm_tpex_Transacciones YA permite crear transacciones sin solicitud
   ni proveedor: para los tipos con @esPagoProveedor = 0 (USDT, FONDEO_MERCURY,
   TRASPASO_MERCURY, DEVOLUCION) idSolicitud/cardCode son opcionales, y codBanco
   sólo se exige si el tipo tiene requiereBanco = 1.

   El ÚNICO ajuste necesario: en el INSERT, @idCotizacion y @codBanco se insertan
   DIRECTOS. Cuando el frontend manda 0 (sin cotización / sin banco para Traspaso
   Mercury) eso viola las FK a tpex_Cotizaciones / tch_banco. Igual que ya se hace
   con @idSolicitud, hay que envolverlos en NULLIF(...,0) para que 0 -> NULL.

   ----------------------------------------------------------------------------
   CAMBIO MÍNIMO (en el bloque  IF @ACCION = 'I' ... VALUES (...)):

   ANTES (≈ líneas 214-215):
       NULL, NULLIF(@idSolicitud, 0), @idCotizacion, @idTipoTransaccion,
       @codBanco, @idCanal, @codEmpresa, NULLIF(@cardCode, ''),

   DESPUÉS:
       NULL, NULLIF(@idSolicitud, 0), NULLIF(@idCotizacion, 0), @idTipoTransaccion,
       NULLIF(@codBanco, 0), @idCanal, @codEmpresa, NULLIF(@cardCode, ''),

   Es decir: @idCotizacion -> NULLIF(@idCotizacion, 0)
             @codBanco     -> NULLIF(@codBanco, 0)

   No cambia NADA del flujo de pago a proveedor (ahí idCotizacion y codBanco
   siempre llegan > 0, y NULLIF(x,0)=x). Sólo habilita las operaciones de
   tesorería donde llegan en 0.
   ============================================================================ */

-- Aplicar el cambio editando el SP (ALTER PROCEDURE) en las 2 líneas indicadas.
-- (No se incluye el ALTER completo para no arriesgar las ~366 líneas del SP;
--  son 2 reemplazos puntuales dentro del INSERT del bloque IF @ACCION = 'I'.)

-- Verificación posterior (debe devolver 1 = el SP ya envuelve ambos en NULLIF):
-- SELECT CASE WHEN OBJECT_DEFINITION(OBJECT_ID('p_abm_tpex_Transacciones'))
--               LIKE '%NULLIF(@idCotizacion, 0)%'
--          AND  OBJECT_DEFINITION(OBJECT_ID('p_abm_tpex_Transacciones'))
--               LIKE '%NULLIF(@codBanco, 0)%'
--        THEN 1 ELSE 0 END AS ok;
