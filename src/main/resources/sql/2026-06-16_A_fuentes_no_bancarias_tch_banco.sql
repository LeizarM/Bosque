/* ============================================================================
   2026-06-16  ·  OPCIÓN A · Fuentes de fondeo NO bancarias en tch_banco
   ----------------------------------------------------------------------------
   Brecha: las operaciones USDT / Fondeo Mercury exigen codBanco (requiereBanco=1
   en tpex_TiposTransaccion), pero en la realidad la "fuente" no es un banco
   tradicional sino un intermediario (compra P2P de USDT, la propia cuenta
   Mercury, etc.). Como tch_banco es IDENTITY y NO tiene columna 'tipo', la vía
   más simple y sin cambios de esquema es dar de alta esas fuentes como filas
   de tch_banco con un nombre claramente identificable.

   Idempotente: sólo inserta las que no existan por nombre.
   audUsuario = 34 (mjaimes). Ajustar si se requiere otro.
   ============================================================================ */

SET NOCOUNT ON;

DECLARE @fuentes TABLE (nombre VARCHAR(50));
INSERT INTO @fuentes (nombre) VALUES
      ('Mercury (cuenta propia)')      -- traspasos/fondeo a la cuenta Mercury
    , ('Compra USDT P2P')              -- USDT vía intermediario/exchange
    , ('Intermediario USDT - Ines')    -- el caso "SRA INES" de los Excel
    , ('Binance P2P');                 -- exchange

INSERT INTO tch_banco (nombre, audUsuario, audFecha)
SELECT f.nombre, 34, GETDATE()
FROM @fuentes f
WHERE NOT EXISTS (SELECT 1 FROM tch_banco b WHERE b.nombre = f.nombre);

-- Verificación
SELECT codBanco, nombre
FROM tch_banco
WHERE nombre IN (SELECT nombre FROM @fuentes)
ORDER BY codBanco;
