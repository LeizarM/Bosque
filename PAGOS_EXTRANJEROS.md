# Módulo: Pagos al Exterior (`tpex_`)

> **Sistema de gestión de pagos internacionales** para una empresa boliviana.  
> Cubre el ciclo completo: Solicitud → Cotización bancaria → Transacción → Confirmación.  
> Prefijo de tablas y SPs: `tpex_` (Transferencias/Pagos al Exterior).

---

## 📁 Archivos del módulo

| Capa | Archivo |
|------|---------|
| **Controller** | `controller/PagosExtranjerosController.java` |
| **Interfaces DAO** | `dao/ICanalesPago`, `ICargoPago`, `IConfigComisionesBanco`, `ICotizaciones`, `ILogEstados`, `IMonedas`, `ISolicitudPago`, `ISolicitudProveedor`, `IDetalleSolicitud`, `ITiposCambio`, `ITiposCargo`, `ITiposTransaccion`, `ITransacciones` |
| **Implementaciones DAO** | `dao/CanalesPagoDao`, `CargoPagoDao`, `ConfigComisionesBancoDao`, `CotizacionesDao`, `LogEstadosDao`, `MonedasDao`, `SolicitudPagoDao`, `SolicitudProveedorDao`, `DetalleSolicitudDao`, `TiposCambioDao`, `TiposCargoDao`, `TiposTransaccionDao`, `TransaccionesDao` |
| **Modelos** | `model/SolicitudPago`, `SolicitudProveedor`, `DetalleSolicitud`, `Cotizaciones`, `Transacciones`, `CargoPago`, `LogEstados`, `CanalesPago`, `Monedas`, `TiposCambio`, `TiposCargo`, `TiposTransaccion`, `ConfigComisionesBanco` |
| **DTOs** | `dto/SolicitudPagoDto`, `SolicitudProveedorDto`, `DetalleSolicitudDto`, `ReportePlanoDto`, `FiltroFechasDto`, `ConfirmarPagoRequest` |
| **Utilidades** | `utils/SpHelper`, `utils/RespuestaSp`, `utils/ApiResponse` |

---

## 🏛️ Arquitectura general

```
Angular Frontend
      │  POST JSON
      ▼
PagosExtranjerosController  (/pagos-extranjeros/*)
      │  13 DAOs inyectados vía constructor (interface)
      ▼
[Interfaz I*]  →  [*Dao implements I*]
      │  SpHelper.ejecutarAbm() / ejecutarListado()
      ▼
SQL Server  →  Stored Procedures  p_abm_tpex_* / p_list_tpex_*
```

- **Sin JPA/ORM.** Todo acceso a BD usa `JdbcTemplate` + `SimpleJdbcCall`.
- **SpHelper** serializa el modelo con Jackson, agrega el parámetro `ACCION` y ejecuta el SP.
- Los `SimpleJdbcCall` se cachean en un `ConcurrentHashMap` para evitar lecturas repetidas de metadatos.

---

## 🗄️ Tablas y Modelos Java

### Tablas Maestras / Catálogos

#### `tpex_Monedas` → `model/Monedas.java`
Catálogo de monedas disponibles en el sistema (USD, BOB, EUR, etc.).

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idMoneda` | `int` | PK |
| `codigo` | `String` | Ej: "USD", "BOB" |
| `nombre` | `String` | Nombre completo |
| `simbolo` | `String` | "$", "Bs.", "€" |
| `decimales` | `int` | Precisión decimal |
| `activo` | `int` | 1 = activo, 0 = inactivo |
| `audUsuario` | `int` | ID usuario auditoría |

---

#### `tpex_TiposCargo` → `model/TiposCargo.java`
Define los tipos de cargo que se pueden aplicar (por porcentaje o valor fijo).

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idTipoCargo` | `long` | PK |
| `nombre` | `String` | Nombre del cargo |
| `esPorcentaje` | `int` | 1 = porcentaje, 0 = valor fijo |
| `activo` | `int` | Estado |
| `audUsuario` | `int` | |

---

#### `tpex_TiposTransaccion` → `model/TiposTransaccion.java`
Tipos de operación cambiaria disponibles.

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idTipoTransaccion` | `long` | PK |
| `codigo` | `String` | Código corto |
| `nombre` | `String` | Nombre descriptivo |
| `descripcion` | `String` | Descripción detallada |
| `requiereForward` | `int` | 1 = requiere tipo de cambio forward |
| `requiereBanco` | `int` | 1 = requiere banco |
| `activo` | `int` | Estado |
| `audUsuario` | `int` | |

---

#### `tpex_CanalesPago` → `model/CanalesPago.java`
Canales a través de los cuales se ejecuta el pago (SWIFT, transferencia local, etc.).

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idCanal` | `long` | PK |
| `nombre` | `String` | Nombre del canal |
| `tipo` | `String` | Tipo de canal |
| `contacto` | `String` | Datos de contacto |
| `activo` | `int` | Estado |
| `audUsuario` | `int` | |

---

#### `tpex_TiposCambio` → `model/TiposCambio.java`
Tasas de cambio publicadas por banco y fecha.

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idTipoCambio` | `long` | PK |
| `codBanco` | `int` | FK → `tch_banco` |
| `fechaVigencia` | `Date` | Fecha de la tasa |
| `idMonedaOrigen` | `int` | FK → `tpex_Monedas` |
| `idMonedaDestino` | `int` | FK → `tpex_Monedas` |
| `tasaCompra` | `float` | |
| `tasaVenta` | `float` | |
| `tasaPromedio` | `float` | |
| `fuente` | `String` | Fuente de la tasa |
| `audUsuario` | `int` | |
| `fechaInicio` *(filtro)* | `Date` | Solo para búsqueda, no persiste |
| `fechaFin` *(filtro)* | `Date` | Solo para búsqueda, no persiste |

---

#### `tpex_ConfigComisionesBanco` → `model/ConfigComisionesBanco.java`
Configuración de comisiones por banco y tipo de transacción. Usada para calcular cargos automáticamente.

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idConfig` | `long` | PK |
| `codBanco` | `int` | FK → `tch_banco` |
| `idTipoTransaccion` | `long` | FK → `tpex_TiposTransaccion` |
| `idTipoCargo` | `long` | FK → `tpex_TiposCargo` |
| `valorPorcentaje` | `float` | Porcentaje si aplica |
| `valorFijo` | `float` | Valor fijo si aplica |
| `idMoneda` | `int` | FK → `tpex_Monedas` |
| `orden` | `int` | Orden de aplicación |
| `baseCalculo` | `String` | Default: `'MONTO_CONVERTIDO'` |
| `activo` | `int` | Estado |
| `fechaVigenciaDesde` | `Date` | Inicio vigencia |
| `fechaVigenciaHasta` | `Date` | Fin vigencia |
| `audUsuario` | `int` | |

---

### Tablas Transaccionales / Proceso

#### `tpex_SolicitudPago` → `model/SolicitudPago.java`
Cabecera de la solicitud de pago al exterior. **Punto de entrada del flujo.**

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idSolicitud` | `long` | PK |
| `codEmpresa` | `int` | FK → `tb_empresa` |
| `fechaSolicitud` | `Date` | |
| `montoTotalSolicitud` | `float` | |
| `estado` | `String` | `PENDIENTE → APROBADA → PAGADA` |
| `audUsuario` | `int` | |
| `proveedores` *(nested)* | `List<SolicitudProveedor>` | Solo JSON, no persiste en BD |
| `proveedoresAEliminar` *(nested)* | `List<Long>` | IDs a eliminar, solo JSON |

---

#### `tpex_SolicitudProveedor` → `model/SolicitudProveedor.java`
Desglosa la solicitud de pago por cada proveedor involucrado.

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idSolicitudProveedor` | `long` | PK |
| `idSolicitud` | `long` | FK → `tpex_SolicitudPago` |
| `cardCode` | `String` | Código proveedor SAP |
| `cardName` | `String` | Nombre proveedor SAP |
| `totalFacturasUsd` | `float` | |
| `totalAmortizadoUsd` | `float` | |
| `totalAPagarUsd` | `float` | |
| `obs` | `String` | Observaciones |
| `audUsuario` | `int` | |
| `codEmpresa` | `int` | Solo para filtros, no persiste directo |
| `detalles` *(nested)* | `List<DetalleSolicitud>` | Solo JSON |
| `detallesAEliminar` *(nested)* | `List<Long>` | IDs a eliminar, solo JSON |

---

#### `tpex_DetalleSolicitud` → `model/DetalleSolicitud.java`
Detalle de facturas individuales a pagar por proveedor.

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idDetalle` | `long` | PK |
| `idSolicitudProveedor` | `long` | FK → `tpex_SolicitudProveedor` |
| `tipoDocumento` | `String` | "FACTURA", "PROFORMA", etc. |
| `numeroDocumento` | `String` | Nro. de factura o proforma |
| `facturaProvSap` | `int` | DocNum en SAP |
| `codigoImportacion` | `String` | |
| `montoFacturaUsd` | `float` | |
| `montoAmortizadoUsd` | `float` | |
| `montoAPagarUsd` | `float` | |
| `fechaFactura` | `Date` | |
| `fechaVencimiento` | `Date` | |
| `concepto` | `String` | |
| `obs` | `String` | |
| `esAprobado` | `int` | 1 = aprobado |
| `audUsuario` | `int` | |
| `codEmpresa` | `int` | Solo para filtros |

---

#### `tpex_Cotizaciones` → `model/Cotizaciones.java`
Cotizaciones de tipo de cambio recibidas de los bancos para una solicitud.

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idCotizacion` | `long` | PK |
| `idSolicitud` | `long` | FK → `tpex_SolicitudPago` |
| `fechaCotizacion` | `Date` | |
| `montoCompra` | `float` | Monto en moneda origen |
| `idMoneda` | `int` | FK → `tpex_Monedas` |
| `nroGiros` | `int` | Número de giros/transferencias |
| `codBanco` | `int` | FK → `tch_banco` |
| `tipoCambioOfrecido` | `float` | TC ofertado por el banco |
| `montoConvertido` | `float` | Resultado de la conversión |
| `totalBolivianos` | `float` | Total en BOB incluido cargos |
| `esGanadora` | `int` | 1 = cotización ganadora |
| `estado` | `String` | `VIGENTE → ACEPTADA / RECHAZADA` |
| `observaciones` | `String` | |
| `audUsuario` | `int` | |
| `fechaInicio` *(filtro)* | `Date` | Solo para búsqueda |
| `fechaFin` *(filtro)* | `Date` | Solo para búsqueda |
| `cargos` *(nested)* | `List<CargoPago>` | Solo JSON |

---

#### `tpex_Transacciones` → `model/Transacciones.java`
Registro de la transacción ejecutada, vinculada a la cotización ganadora (`esGanadora = 1`).

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idTransaccion` | `long` | PK |
| `numeroTransaccion` | `String` | Nro. de referencia bancaria |
| `idSolicitud` | `long` | FK → `tpex_SolicitudPago` |
| `idCotizacion` | `long` | FK → `tpex_Cotizaciones` (ganadora) |
| `idTipoTransaccion` | `long` | FK → `tpex_TiposTransaccion` |
| `codBanco` | `int` | FK → `tch_banco` |
| `idCanal` | `int` | FK → `tpex_CanalesPago` |
| `codEmpresa` | `int` | FK → `tb_empresa` |
| `cardCode` | `String` | Proveedor principal SAP |
| `fechaTransaccion` | `Date` | |
| `fechaValor` | `Date` | Fecha de acreditación |
| `montoOrigen` | `float` | Monto en moneda origen |
| `idMonedaOrigen` | `int` | FK → `tpex_Monedas` |
| `tipoCambioAplicado` | `float` | TC definitivo |
| `montoConvertido` | `float` | |
| `idMonedaDestino` | `int` | FK → `tpex_Monedas` |
| `totalCargos` | `float` | Suma de todos los cargos |
| `totalFinal` | `float` | montoConvertido + totalCargos |
| `numeroContrato` | `String` | Para operaciones forward |
| `fechaPactado` | `Date` | |
| `fechaVencimiento` | `Date` | |
| `tipoCambioForward` | `float` | TC pactado (si aplica) |
| `tipoCambioReferencia` | `float` | TC de referencia |
| `equivalenteUsdRef` | `float` | |
| `diferenciaDeMas` | `float` | Diferencia vs TC referencia |
| `porcentajeDiferencia` | `float` | % de la diferencia |
| `nombreExportadora` | `String` | Exportadora involucrada |
| `tcNegociadoExportadora` | `float` | TC negociado con exportadora |
| `comisionExportadora` | `float` | |
| `metodoExportadora` | `String` | Método de pago a exportadora |
| `estado` | `String` | `PENDIENTE → PROCESADO → CONFIRMADO` |
| `observaciones` | `String` | |
| `audUsuario` | `int` | |
| `cargos` *(nested)* | `List<CargoPago>` | Solo JSON |

---

#### `tpex_Cargos` → `model/CargoPago.java`
Cargos calculados y aplicados sobre una cotización **o** una transacción (mutuamente excluyentes).

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idCargo` | `long` | PK |
| `idCotizacion` | `long` | FK → `tpex_Cotizaciones` (0 si pertenece a transacción) |
| `idTransaccion` | `long` | FK → `tpex_Transacciones` (0 si pertenece a cotización) |
| `idTipoCargo` | `long` | FK → `tpex_TiposCargo` |
| `baseCalculo` | `float` | Monto base sobre el que se calcula |
| `origenBase` | `String` | Descripción del origen de la base |
| `porcentaje` | `float` | Si aplica |
| `valorFijo` | `float` | Si aplica |
| `montoCargo` | `float` | Resultado calculado |
| `idMoneda` | `int` | FK → `tpex_Monedas` |
| `orden` | `int` | Orden de aplicación |
| `descripcion` | `String` | |
| `audUsuario` | `int` | |

---

#### `tpex_LogEstados` → `model/LogEstados.java`
Trazabilidad de **todos** los cambios de estado en solicitudes, cotizaciones y transacciones.

| Campo Java | Tipo | BD |
|-----------|------|-----|
| `idLog` | `long` | PK |
| `idSolicitud` | `long` | FK → `tpex_SolicitudPago` (0 si no aplica) |
| `idCotizacion` | `long` | FK → `tpex_Cotizaciones` (0 si no aplica) |
| `idTransaccion` | `long` | FK → `tpex_Transacciones` (0 si no aplica) |
| `estadoAnterior` | `String` | Estado previo (puede ser null en alta) |
| `estadoNuevo` | `String` | Estado nuevo |
| `observaciones` | `String` | |
| `audUsuario` | `int` | |

> ⚠️ Exactamente **uno** de los tres IDs debe ser `!= 0` por registro.

---

## 🔗 Referencias externas al módulo

| Tabla externa | Usada en |
|--------------|---------|
| `tch_banco` | `tpex_Cotizaciones`, `tpex_Transacciones`, `tpex_TiposCambio`, `tpex_ConfigComisionesBanco` |
| `tb_empresa` | `tpex_SolicitudPago`, `tpex_Transacciones` |

---

## 🔄 Flujo del Proceso (5 Fases)

```
1. SolicitudPago (PENDIENTE)
   └── SolicitudProveedor (1..N)
         └── DetalleSolicitud (1..N — facturas individuales)

2. Cotizaciones (ofertadas por bancos)
   └── CargoPago (cargos sobre cotización)
         └── Calculados con ConfigComisionesBanco + TiposCambio

3. Cotización ganadora (esGanadora = 1) → Solicitud APROBADA

4. Transacciones (ejecución del pago)
   └── CargoPago (cargos sobre transacción)

5. Confirmación → Transacción CONFIRMADO + Solicitud PAGADA
   └── Cada cambio de estado registrado en LogEstados
```

> Todos los endpoints de escritura están anotados con `@Transactional`.  
> Si cualquier SP falla → `RuntimeException` → rollback automático de toda la fase.

### FASE 1 — Solicitud de Pago

```
POST /guardar-solicitud-completa
Body: SolicitudPago {
  proveedoresAEliminar: [ids],
  proveedores: [
    SolicitudProveedor {
      detallesAEliminar: [ids],
      detalles: [ DetalleSolicitud ]
    }
  ]
}

Pasos:
  1. DELETE detalles en detallesAEliminar             → p_abm_tpex_DetalleSolicitud      "D"
  2. DELETE proveedores en proveedoresAEliminar        → p_abm_tpex_SolicitudProveedor    "D"
  3. INSERT/UPDATE SolicitudPago (cabecera)            → p_abm_tpex_SolicitudPago         "I"/"U"
     └─ Obtiene idSolicitud desde idGenerado del SP
  4. Si INSERT → LogEstados NULL → "PENDIENTE"         → p_abm_tpex_LogEstados            "I"
  5. Para cada proveedor:
     INSERT/UPDATE SolicitudProveedor                  → p_abm_tpex_SolicitudProveedor    "I"/"U"
     └─ Para cada detalle:
        INSERT/UPDATE DetalleSolicitud                 → p_abm_tpex_DetalleSolicitud      "I"/"U"

POST /aprobar-solicitud
Body: SolicitudPago { idSolicitud, estado: "APROBADA", audUsuario }
  1. UPDATE estado                                     → p_abm_tpex_SolicitudPago         "U"
  2. LogEstados PENDIENTE → "APROBADA"                 → p_abm_tpex_LogEstados            "I"
```

### FASE 2 — Cotización Bancaria

```
POST /guardar-cotizacion-completa
Body: Cotizaciones {
  idCotizacion: 0,    ← 0 = INSERT, >0 = UPDATE
  idSolicitud: 7,
  codBanco: 3,
  montoCompra: 50000.00,
  tipoCambioOfrecido: 6.97,
  cargos: [ CargoPago ]
}

Pasos:
  1. INSERT/UPDATE Cotizaciones                        → p_abm_tpex_Cotizaciones          "I"/"U"
  2. Si INSERT → INSERT cada CargoPago                 → p_abm_tpex_CargoPago             "I"
     └─ cargo.idCotizacion = idCotizacion generado
     └─ cargo.idTransaccion = 0  (exclusividad FK)
```

### FASE 3 — Aceptar Cotización Ganadora

```
POST /aceptar-cotizacion
Body: Cotizaciones { idCotizacion, estado: "ACEPTADA", audUsuario }

Pasos:
  1. UPDATE cotización                                 → p_abm_tpex_Cotizaciones          "U"
     └─ El SP internamente rechaza las demás cotizaciones de la misma solicitud
  2. LogEstados VIGENTE → "ACEPTADA"                   → p_abm_tpex_LogEstados            "I"
```

### FASE 4 — Transacción de Pago

```
POST /guardar-transaccion-completa
Body: Transacciones {
  idTransaccion: 0,
  idSolicitud: 7,
  idCotizacion: 12,
  montoOrigen: 50000.00,
  tipoCambioAplicado: 6.97,
  cargos: [ CargoPago ]
}

Pasos:
  1. INSERT/UPDATE Transacciones                       → p_abm_tpex_Transacciones         "I"/"U"
  2. Si INSERT → INSERT cada CargoPago                 → p_abm_tpex_CargoPago             "I"
     └─ cargo.idTransaccion = idTransaccion generado
     └─ cargo.idCotizacion = 0  (exclusividad FK)
  3. Si INSERT → LogEstados NULL → "PENDIENTE"         → p_abm_tpex_LogEstados            "I"

POST /cambiar-estado-transaccion
Body: Transacciones { idTransaccion, estado, audUsuario }
  1. UPDATE estado                                     → p_abm_tpex_Transacciones         "U"
  2. LogEstados → nuevo estado                         → p_abm_tpex_LogEstados            "I"
```

### FASE 5 — Confirmar y Cerrar ⚠️ Operación más crítica

```
POST /confirmar-pago
Body: ConfirmarPagoRequest { idTransaccion, idSolicitud, numeroTransaccion, fechaValor, audUsuario }

Pasos (ACID completo en una sola transacción Java):
  1. Transacciones → "CONFIRMADO"                      → p_abm_tpex_Transacciones         "U"
  2. LogEstados PROCESADO → "CONFIRMADO"               → p_abm_tpex_LogEstados            "I"
  3. SolicitudPago → "PAGADA"                          → p_abm_tpex_SolicitudPago         "U"
  4. LogEstados APROBADA → "PAGADA"                    → p_abm_tpex_LogEstados            "I"
```

---

## 🗃️ Stored Procedures

> Convención: `p_abm_tpex_<Tabla>` (ABM) y `p_list_tpex_<Tabla>` (Listado).  
> SPs retornan tres parámetros de salida: `error` (0=OK), `errormsg`, `idGenerado`.

| Tabla | SP ABM | SP Listado |
|-------|--------|-----------|
| `tpex_SolicitudPago` | `p_abm_tpex_SolicitudPago` | `p_list_tpex_SolicitudPago` |
| `tpex_SolicitudProveedor` | `p_abm_tpex_SolicitudProveedor` | `p_list_tpex_SolicitudProveedor` |
| `tpex_DetalleSolicitud` | `p_abm_tpex_DetalleSolicitud` | `p_list_tpex_DetalleSolicitud` |
| `tpex_Cotizaciones` | `p_abm_tpex_Cotizaciones` | `p_list_tpex_Cotizaciones` |
| `tpex_Transacciones` | `p_abm_tpex_Transacciones` | `p_list_tpex_Transacciones` |
| `tpex_Cargos` | `p_abm_tpex_Cargos` | `p_list_tpex_Cargos` |
| `tpex_LogEstados` | `p_abm_tpex_LogEstados` | `p_list_tpex_LogEstados` |
| `tpex_Monedas` | `p_abm_tpex_Monedas` | `p_list_tpex_Monedas` |
| `tpex_TiposCargo` | `p_abm_tpex_TiposCargo` | `p_list_tpex_TiposCargo` |
| `tpex_TiposTransaccion` | `p_abm_tpex_TiposTransaccion` | `p_list_tpex_TiposTransaccion` |
| `tpex_CanalesPago` | `p_abm_tpex_CanalesPago` | `p_list_tpex_CanalesPago` |
| `tpex_TiposCambio` | `p_abm_tpex_TiposCambio` | `p_list_tpex_TiposCambio` |
| `tpex_ConfigComisionesBanco` | `p_abm_tpex_ConfigComisionesBanco` | `p_list_tpex_ConfigComisionesBanco` |

---

## 🔗 Mapa completo: Interfaz ↔ DAO ↔ SP

| Interfaz | DAO | SP ABM | SP Listado |
|----------|-----|--------|-----------|
| `ICanalesPago` | `CanalesPagoDao` | `p_abm_tpex_CanalesPago` | `p_list_tpex_CanalesPago` |
| `ICargoPago` | `CargoPagoDao` | `p_abm_tpex_CargoPago` | `p_list_tpex_CargoPago` |
| `IConfigComisionesBanco` | `ConfigComisionesBancoDao` | `p_abm_tpex_ConfigComisionesBanco` | `p_list_tpex_ConfigComisionesBanco` |
| `ICotizaciones` | `CotizacionesDao` | `p_abm_tpex_Cotizaciones` | `p_list_tpex_Cotizaciones` |
| `IDetalleSolicitud` | `DetalleSolicitudDao` | `p_abm_tpex_DetalleSolicitud` | `p_list_tpex_DetalleSolicitud` |
| `ILogEstados` | `LogEstadosDao` | `p_abm_tpex_LogEstados` | `p_list_tpex_LogEstados` |
| `IMonedas` | `MonedasDao` | `p_abm_tpex_Monedas` | `p_list_tpex_Monedas` |
| `ISolicitudPago` | `SolicitudPagoDao` | `p_abm_tpex_SolicitudPago` | `p_list_tpex_SolicitudPago` |
| `ISolicitudProveedor` | `SolicitudProveedorDao` | `p_abm_tpex_SolicitudProveedor` | `p_list_tpex_SolicitudProveedor` |
| `ITiposCambio` | `TiposCambioDao` | `p_abm_tpex_TiposCambio` | `p_list_tpex_TiposCambio` |
| `ITiposCargo` | `TiposCargoDao` | `p_abm_tpex_TiposCargo` | `p_list_tpex_TiposCargo` |
| `ITiposTransaccion` | `TiposTransaccionDao` | `p_abm_tpex_TiposTransaccion` | `p_list_tpex_TiposTransaccion` |
| `ITransacciones` | `TransaccionesDao` | `p_abm_tpex_Transacciones` | `p_list_tpex_Transacciones` |

---

## ⚙️ SpHelper — Núcleo de acceso a datos

```java
// ABM (Insert / Update / Delete)
spHelper.ejecutarAbm("p_abm_tpex_Xxx", modelObject, "I" | "U" | "D")
  // 1. Serializa el objeto → Map<String, Object> vía Jackson ObjectMapper
  // 2. Agrega clave "ACCION" al map
  // 3. Obtiene/crea SimpleJdbcCall del caché (ConcurrentHashMap por nombre SP)
  // 4. Ejecuta el SP con MapSqlParameterSource
  // 5. Si error != 0 → lanza SpBusinessException → GlobalExceptionHandler → HTTP 400
  // 6. Retorna RespuestaSp { error, errormsg, idGenerado }

// Listados (SELECT)
spHelper.ejecutarListado("p_list_tpex_Xxx", filtro, "ACCION", Clase.class)
  // Igual, pero usa BeanPropertyRowMapper → devuelve List<T>
  // Clave de caché = spName + "_list_" + clazz.getName()
```

### Códigos de acción usados en este módulo

| Código | Significado | DAOs que lo usan |
|--------|------------|-----------------|
| `"I"` | INSERT | Todos los DAOs ABM |
| `"U"` | UPDATE | Todos los DAOs ABM |
| `"D"` | DELETE | Todos los DAOs ABM |
| `"L"` | SELECT por ID (o todos si id = 0) | Todos los DAOs listado |
| `"B"` | SELECT por banco / por solicitud / rango fechas | `CotizacionesDao`, `ConfigComisionesBancoDao`, `SolicitudPagoDao` |
| `"S"` | SELECT por `idSolicitud` | `TransaccionesDao`, `LogEstadosDao` |
| `"C"` | SELECT por `idCotizacion` | `CargoPagoDao`, `TransaccionesDao` |
| `"T"` | SELECT por `idTransaccion` | `CargoPagoDao`, `LogEstadosDao` |
| `"R"` | SELECT por banco (tipos de cambio) | `TiposCambioDao` |

---

## 📐 DTOs y su propósito

| DTO | Usado en | Descripción |
|-----|---------|-------------|
| `SolicitudPagoDto` | `reporteSolicitudesXFecha` (response) | Vista agregada con `nombre` de empresa + lista anidada de proveedores |
| `SolicitudProveedorDto` | Anidado en `SolicitudPagoDto` | Usa `BigDecimal` para montos; contiene lista de `DetalleSolicitudDto` |
| `DetalleSolicitudDto` | Anidado en `SolicitudProveedorDto` | Fila de factura/detalle en el reporte |
| `ReportePlanoDto` | `SolicitudPagoDao` interno | Fila plana devuelta por SP acción `"B"`, reconstruida en árbol en memoria |
| `FiltroFechasDto` | `POST /reporte-solicitudes-fechas` (request) | Filtro de rango `fechaInicio` / `fechaFin` |
| `ConfirmarPagoRequest` | `POST /confirmar-pago` (request) | Agrupa datos de Transaccion + SolicitudPago para la fase crítica |

### Reconstrucción de árbol en `SolicitudPagoDao.reporteSolicitudesXFecha`

```
SP p_list_tpex_SolicitudPago acción "B"
  └─ Devuelve List<ReportePlanoDto>  (fila plana por cada detalle)
       │
       ▼  agrupado por idSolicitud  (LinkedHashMap → preserva orden)
  SolicitudPagoDto
    │  BeanUtils.copyProperties(fila, solicitud)
    ▼  agrupado por idSolicitudProveedor  (HashMap)
  SolicitudProveedorDto
    │  BeanUtils.copyProperties(fila, proveedor)
    ▼  por cada fila con idDetalle > 0
  DetalleSolicitudDto
       BeanUtils.copyProperties(fila, detalle)
```

---

## 📋 Endpoints completos

Ruta base: `POST /pagos-extranjeros/**`  
Todos los métodos son `POST`. Requiere JWT válido + rol `ROLE_ADM` o `ROLE_LIM`.

### Escrituras transaccionales (`@Transactional`)

| Endpoint | Body principal | Descripción |
|---------|---------------|-------------|
| `POST /guardar-solicitud-completa` | `SolicitudPago` | Crea/actualiza solicitud + proveedores + facturas (ACID) |
| `POST /aprobar-solicitud` | `SolicitudPago` | Cambia estado a APROBADA + log |
| `POST /guardar-cotizacion-completa` | `Cotizaciones` | Crea cotización + cargos (ACID) |
| `POST /aceptar-cotizacion` | `Cotizaciones` | Acepta ganadora, rechaza demás + log |
| `POST /guardar-transaccion-completa` | `Transacciones` | Crea transacción + cargos + log (ACID) |
| `POST /cambiar-estado-transaccion` | `Transacciones` | Cambia estado + log |
| `POST /confirmar-pago` | `ConfirmarPagoRequest` | Confirma transacción + cierra solicitud como PAGADA (ACID crítico) |

### Lecturas (sin `@Transactional`)

| Endpoint | Filtro | Devuelve |
|---------|--------|---------|
| `POST /obtener-solicitudes` | `{ idSolicitud }` | `List<SolicitudPago>` |
| `POST /obtener-solicitud-proveedor` | `{ idSolicitudProveedor }` | `List<SolicitudProveedor>` |
| `POST /obtener-detalle-solicitud` | `{ idDetalle }` | `List<DetalleSolicitud>` |
| `POST /obtener-proveedores-empresa` | `{ codEmpresa }` | `List<SolicitudProveedor>` |
| `POST /obtener-docnum-empresa` | `{ codEmpresa }` | `List<DetalleSolicitud>` |
| `POST /reporte-solicitudes-fechas` | `FiltroFechasDto` | `List<SolicitudPagoDto>` (árbol reconstruido) |
| `POST /obtener-cotizaciones-solicitud` | `{ idSolicitud }` | `List<Cotizaciones>` |
| `POST /obtener-transacciones-solicitud` | `{ idSolicitud }` | `List<Transacciones>` |
| `POST /obtener-transacciones-cotizacion` | `{ idCotizacion }` | `List<Transacciones>` |
| `POST /obtener-cargos-cotizacion` | `{ idCotizacion }` | `List<CargoPago>` |
| `POST /obtener-cargos-transaccion` | `{ idTransaccion }` | `List<CargoPago>` |
| `POST /obtener-log-solicitud` | `{ idSolicitud }` | `List<LogEstados>` |
| `POST /obtener-log-transaccion` | `{ idTransaccion }` | `List<LogEstados>` |

### Catálogos (CRUD simple, atómico)

| Entidad | Registrar | Eliminar | Obtener | Extra |
|---------|----------|---------|---------|-------|
| `CanalesPago` | `/registrar-canal-pago` | `/eliminar-canal-pago` | `/obtener-canales-pago` | — |
| `Monedas` | `/registrar-moneda` | `/eliminar-moneda` | `/obtener-monedas` | — |
| `TiposCambio` | `/registrar-tipo-cambio` | `/eliminar-tipo-cambio` | `/obtener-tipos-cambio` | `/obtener-tipos-cambio-banco` |
| `TiposCargo` | `/registrar-tipo-cargo` | `/eliminar-tipo-cargo` | `/obtener-tipos-cargo` | — |
| `TiposTransaccion` | `/registrar-tipo-transaccion` | `/eliminar-tipo-transaccion` | `/obtener-tipos-transaccion` | — |
| `ConfigComisionesBanco` | `/registrar-config-comisiones` | `/eliminar-config-comisiones` | `/obtener-config-comisiones` | `/obtener-config-comisiones-banco` |

---

## 🛡️ Seguridad y Respuestas HTTP

### Acceso
- Ruta base: `POST /pagos-extranjeros/**`
- Requiere JWT válido + rol `ROLE_ADM` o `ROLE_LIM`
- `@CrossOrigin(origins = "*")` habilitado para el frontend Angular
- `@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")` a nivel de clase

### Códigos de respuesta

| Código | Cuándo | Body |
|--------|--------|------|
| `201 Created` | Escritura exitosa | `ApiResponse { message: "Operación realizada exitosamente", data: idGenerado }` |
| `200 OK` | Lista con resultados | `ApiResponse { data: List<T> }` |
| `204 No Content` | Lista vacía | `ApiResponse { message: "No se encontraron..." }` |
| `400 Bad Request` | SP devuelve `error != 0` → `SpBusinessException` | `ApiResponse { message: errormsg del SP }` |
| `403 Forbidden` | Sin rol requerido | — |
| `500 Internal Server Error` | Fallo de BD o excepción inesperada | — |

### Métodos auxiliares del controller

```java
// Dispara rollback si el SP falla (dentro de @Transactional)
private void ejecutar(RespuestaSp res, String contexto) {
    if (res.getError() != 0) {
        throw new RuntimeException(contexto + ": " + res.getErrormsg());
    }
}

// Exactamente uno de los tres IDs debe ser != null
private void registrarLog(Long idSolicitud, Long idCotizacion, Long idTransaccion,
                          String estadoNuevo, int audUsuario) {
    // Construye LogEstados y llama a logEstadosDao.registrarLogEstados(log, "I")
    // Si falla → RuntimeException → rollback de toda la transacción padre
}
```
