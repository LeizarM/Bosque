create table tpex_CanalesPago
(
    idCanal    bigint identity
        primary key,
    nombre     varchar(100) not null
        unique,
    tipo       varchar(20)  not null,
    contacto   varchar(200),
    activo     bit default 1,
    audUsuario bigint,
    audFecha   datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'BANCO_DIRECTO, EXPORTADORA_SINDAN, SRA_INES_USDT', 'SCHEMA', 'dbo',
     'TABLE', 'tpex_CanalesPago', 'COLUMN', 'nombre'
go

exec sp_addextendedproperty 'Column_Description', 'BANCARIO | EXPORTADORA | CRIPTO | EFECTIVO', 'SCHEMA', 'dbo',
     'TABLE', 'tpex_CanalesPago', 'COLUMN', 'tipo'
go

create table tpex_Monedas
(
    idMoneda   bigint identity
        primary key,
    codigo     varchar(5)  not null
        unique,
    nombre     varchar(50) not null,
    simbolo    varchar(5),
    decimales  int default 2,
    activo     bit default 1,
    audUsuario bigint,
    audFecha   datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'USD, EUR, BOB, USDT, ##', 'SCHEMA', 'dbo', 'TABLE', 'tpex_Monedas',
     'COLUMN', 'codigo'
go

create table tpex_Proveedores
(
    cardCode   varchar(20)  not null
        primary key,
    nombre     varchar(100) not null,
    codEmpresa bigint       not null
        references tb_empresa,
    idMoneda   bigint       not null
        references tpex_Monedas,
    audUsuario bigint,
    audFecha   datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'Código alfanumérico: TN00600, ZR802, VIZR829...', 'SCHEMA', 'dbo',
     'TABLE', 'tpex_Proveedores', 'COLUMN', 'cardCode'
go

exec sp_addextendedproperty 'Column_Description', 'Moneda por defecto del proveedor', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_Proveedores', 'COLUMN', 'idMoneda'
go

create table tpex_SolicitudPago
(
    idSolicitud         bigint identity
        primary key,
    codEmpresa          int                             not null,
    fechaSolicitud      datetime                        not null,
    montoTotalSolicitud decimal(18, 2),
    estado              varchar(20) default 'PENDIENTE' not null,
    audUsuario          int,
    audFecha            datetime    default getdate()   not null
)
go

create table tpex_Cotizaciones
(
    idCotizacion       bigint identity
        constraint PK__tpex_Cot__D931C39B6656DF1B
            primary key,
    idSolicitud        bigint
        references tpex_SolicitudPago,
    fechaCotizacion    date                                 not null,
    montoCompra        decimal(18, 2)                       not null,
    idMoneda           bigint                               not null
        constraint FK__tpex_Coti__idMon__64398C7F
            references tpex_Monedas,
    nroGiros           int
        constraint DF__tpex_Coti__nroGi__683F278D default 1 not null,
    codBanco           int                                  not null
        references tch_banco,
    tipoCambioOfrecido decimal(10, 6)                       not null,
    montoConvertido    decimal(18, 2),
    totalBolivianos    decimal(18, 2),
    esGanadora         bit
        constraint DF__tpex_Coti__esGan__69334BC6 default 0,
    estado             varchar(30),
    observaciones      text,
    audUsuario         bigint,
    audFecha           datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'VIGENTE | ACEPTADA | RECHAZADA | VENCIDA', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_Cotizaciones', 'COLUMN', 'estado'
go

create unique index tpex_Cotizaciones_index_2
    on tpex_Cotizaciones (idSolicitud, fechaCotizacion, codBanco)
go

create table tpex_SolicitudProveedor
(
    idSolicitudProveedor bigint identity
        primary key,
    idSolicitud          bigint                     not null
        constraint FK_SolProv_Solicitud
            references tpex_SolicitudPago,
    cardCode             varchar(50)                not null,
    cardName             varchar(255),
    totalFacturasUsd     decimal(18, 2),
    totalAmortizadoUsd   decimal(18, 2),
    totalAPagarUsd       decimal(18, 2),
    obs                  varchar(255),
    audUsuario           int,
    audFecha             datetime default getdate() not null
)
go

create table tpex_DetalleSolicitud
(
    idDetalle            bigint identity
        constraint PK__tpex_Det__49CAE2FB1452B3F5
            primary key,
    idSolicitudProveedor bigint                                     not null
        constraint FK_Detalle_SolProv
            references tpex_SolicitudProveedor,
    tipoDocumento        varchar(50),
    numeroDocumento      varchar(250),
    facturaProvSap       int,
    codigoImportacion    varchar(50),
    montoFacturaUsd      decimal(18, 2),
    montoAmortizadoUsd   decimal(18, 2),
    montoAPagarUsd       decimal(18, 2),
    fechaFactura         datetime,
    fechaVencimiento     datetime,
    concepto             varchar(255),
    obs                  varchar(255),
    esAprobado           bit
        constraint DF__tpex_Deta__esApr__163AFC67 default 0         not null,
    audUsuario           int,
    audFecha             datetime
        constraint DF__tpex_Deta__audFe__172F20A0 default getdate() not null
)
go

create table tpex_TiposCambio
(
    idTipoCambio    bigint identity
        constraint PK__tpex_Tip__5684363162864E37
            primary key,
    codBanco        int
        references tch_banco,
    fechaVigencia   date   not null,
    idMonedaOrigen  bigint not null
        references tpex_Monedas,
    idMonedaDestino bigint not null
        references tpex_Monedas,
    tasaCompra      decimal(10, 6),
    tasaVenta       decimal(10, 6),
    tasaPromedio    decimal(10, 6),
    fuente          varchar(50),
    audUsuario      bigint,
    audFecha        datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'BCB, MERCADO, NEGOCIADO', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_TiposCambio', 'COLUMN', 'fuente'
go

create unique index tpex_TiposCambio_index_1
    on tpex_TiposCambio (codBanco, fechaVigencia, idMonedaOrigen, idMonedaDestino)
go

create table tpex_TiposCargo
(
    idTipoCargo  bigint identity
        primary key,
    nombre       varchar(100) not null,
    esPorcentaje bit default 1,
    activo       bit default 1,
    audUsuario   bigint,
    audFecha     datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'CARGO_TRANSFERENCIA, ITF, COMISION_FORWARD, OUR...', 'SCHEMA', 'dbo',
     'TABLE', 'tpex_TiposCargo', 'COLUMN', 'nombre'
go

create table tpex_TiposTransaccion
(
    idTipoTransaccion bigint identity
        primary key,
    codigo            varchar(30)  not null
        unique,
    nombre            varchar(100) not null,
    descripcion       varchar(500),
    requiereForward   bit default 0,
    requiereBanco     bit default 1,
    activo            bit default 1,
    audUsuario        bigint,
    audFecha          datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'FORWARD, TC_DIRECTO, COTIZACION, PAGO_EXPORTADORA, USDT', 'SCHEMA',
     'dbo', 'TABLE', 'tpex_TiposTransaccion', 'COLUMN', 'codigo'
go

create table tpex_ConfigComisionesBanco
(
    idConfig           bigint identity
        constraint PK__tpex_Con__C7E5C6EF5BD950A8
            primary key,
    codBanco           int
        references tch_banco,
    idTipoTransaccion  bigint not null
        constraint FK__tpex_Conf__idTip__7D3A4473
            references tpex_TiposTransaccion,
    idTipoCargo        bigint not null
        constraint FK__tpex_Conf__idTip__7E2E68AC
            references tpex_TiposCargo,
    valorPorcentaje    decimal(10, 6),
    valorFijo          decimal(18, 2),
    idMoneda           bigint
        constraint FK__tpex_Conf__idMon__7F228CE5
            references tpex_Monedas,
    orden              int
        constraint DF__tpex_Conf__orden__5DC1991A default 0,
    baseCalculo        varchar(50)
        constraint DF__tpex_Conf__baseC__5EB5BD53 default 'MONTO_CONVERTIDO',
    activo             bit
        constraint DF__tpex_Conf__activ__5FA9E18C default 1,
    fechaVigenciaDesde date,
    fechaVigenciaHasta date,
    audUsuario         bigint,
    audFecha           datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'MONTO_CONVERTIDO | SUBTOTAL_ACUMULADO | MONTO_ORIGEN', 'SCHEMA',
     'dbo', 'TABLE', 'tpex_ConfigComisionesBanco', 'COLUMN', 'baseCalculo'
go

create unique index tpex_ConfigComisionesBanco_index_0
    on tpex_ConfigComisionesBanco (codBanco, idTipoTransaccion, idTipoCargo)
go

create table tpex_Transacciones
(
    idTransaccion          bigint identity
        constraint PK__tpex_Tra__5B8761F06C0FB871
            primary key,
    numeroTransaccion      varchar(50),
    idSolicitud            bigint
        constraint FK__tpex_Tran__idSol__6621D4F1
            references tpex_SolicitudPago,
    idCotizacion           bigint
        constraint FK__tpex_Tran__idCot__6715F92A
            references tpex_Cotizaciones,
    idTipoTransaccion      bigint         not null
        constraint FK__tpex_Tran__idTip__680A1D63
            references tpex_TiposTransaccion,
    codBanco               int
        references tch_banco,
    idCanal                bigint
        constraint FK__tpex_Tran__idCan__69F265D5
            references tpex_CanalesPago,
    codEmpresa             bigint         not null
        constraint FK__tpex_Tran__codEm__6AE68A0E
            references tb_empresa,
    cardCode               varchar(20)    not null,
    fechaTransaccion       date           not null,
    fechaValor             date,
    montoOrigen            decimal(18, 2) not null,
    idMonedaOrigen         bigint         not null
        constraint FK__tpex_Tran__idMon__6CCED280
            references tpex_Monedas,
    tipoCambioAplicado     decimal(10, 6) not null,
    montoConvertido        decimal(18, 2) not null,
    idMonedaDestino        bigint         not null
        constraint FK__tpex_Tran__idMon__6DC2F6B9
            references tpex_Monedas,
    totalCargos            decimal(18, 2)
        constraint DF__tpex_Tran__total__70D46D8E default 0,
    totalFinal             decimal(18, 2) not null,
    numeroContrato         varchar(50),
    fechaPactado           date,
    fechaVencimiento       date,
    tipoCambioForward      decimal(10, 6),
    tipoCambioReferencia   decimal(10, 6),
    equivalenteUsdRef      decimal(18, 2),
    diferenciaDeMas        decimal(18, 2),
    porcentajeDiferencia   decimal(10, 4),
    nombreExportadora      varchar(100),
    tcNegociadoExportadora decimal(10, 6),
    comisionExportadora    decimal(18, 2),
    metodoExportadora      varchar(50),
    estado                 varchar(20),
    observaciones          text,
    audUsuario             bigint,
    audFecha               datetime
)
go

exec sp_addextendedproperty 'Column_Description', 'TC oficial del día', 'SCHEMA', 'dbo', 'TABLE', 'tpex_Transacciones',
     'COLUMN', 'tipoCambioReferencia'
go

exec sp_addextendedproperty 'Column_Description', 'totalFinal / tipoCambioReferencia', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_Transacciones', 'COLUMN', 'equivalenteUsdRef'
go

exec sp_addextendedproperty 'Column_Description', 'equivalenteUsdRef - montoOrigen', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_Transacciones', 'COLUMN', 'diferenciaDeMas'
go

exec sp_addextendedproperty 'Column_Description', '(diferenciaDeMas / montoOrigen) x 100', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_Transacciones', 'COLUMN', 'porcentajeDiferencia'
go

exec sp_addextendedproperty 'Column_Description', 'TRANSFERENCIA | EFECTIVO | USDT', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_Transacciones', 'COLUMN', 'metodoExportadora'
go

exec sp_addextendedproperty 'Column_Description', 'PENDIENTE | PROCESADO | CONFIRMADO | CANCELADO', 'SCHEMA', 'dbo',
     'TABLE', 'tpex_Transacciones', 'COLUMN', 'estado'
go

create table tpex_Cargos
(
    idCargo       bigint identity
        primary key,
    idCotizacion  bigint
        constraint FK__tpex_Carg__idCot__6EB71AF2
            references tpex_Cotizaciones,
    idTransaccion bigint
        constraint FK__tpex_Carg__idTra__6FAB3F2B
            references tpex_Transacciones,
    idTipoCargo   bigint         not null
        references tpex_TiposCargo,
    baseCalculo   decimal(18, 2) not null,
    origenBase    varchar(50),
    porcentaje    decimal(10, 6),
    valorFijo     decimal(18, 2),
    montoCargo    decimal(18, 2) not null,
    idMoneda      bigint         not null
        references tpex_Monedas,
    orden         int default 0  not null,
    descripcion   varchar(200),
    audUsuario    bigint,
    audFecha      datetime
)
go

exec sp_addextendedproperty 'Table_Description',
     'Regla: exactamente una de (idCotizacion, idTransaccion) debe tener valor', 'SCHEMA', 'dbo', 'TABLE', 'tpex_Cargos'
go

exec sp_addextendedproperty 'Column_Description', 'NULL si es cargo de transacción', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_Cargos', 'COLUMN', 'idCotizacion'
go

exec sp_addextendedproperty 'Column_Description', 'NULL si es cargo de cotización', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_Cargos', 'COLUMN', 'idTransaccion'
go

exec sp_addextendedproperty 'Column_Description', 'MONTO_CONVERTIDO | SUBTOTAL_ANTERIOR | MONTO_ORIGEN', 'SCHEMA',
     'dbo', 'TABLE', 'tpex_Cargos', 'COLUMN', 'origenBase'
go

create table tpex_LogEstados
(
    idLog          bigint identity
        primary key,
    idSolicitud    bigint
        references tpex_SolicitudPago,
    idCotizacion   bigint
        constraint FK__tpex_LogE__idCot__737BD00F
            references tpex_Cotizaciones,
    idTransaccion  bigint
        constraint FK__tpex_LogE__idTra__746FF448
            references tpex_Transacciones,
    estadoAnterior varchar(20),
    estadoNuevo    varchar(20) not null,
    observaciones  text,
    audUsuario     bigint,
    audFecha       datetime
)
go

exec sp_addextendedproperty 'Table_Description',
     'Regla: exactamente una de (idSolicitud, idCotizacion, idTransaccion) debe tener valor', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_LogEstados'
go

exec sp_addextendedproperty 'Column_Description', 'NULL si no es solicitud', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_LogEstados', 'COLUMN', 'idSolicitud'
go

exec sp_addextendedproperty 'Column_Description', 'NULL si no es cotización', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_LogEstados', 'COLUMN', 'idCotizacion'
go

exec sp_addextendedproperty 'Column_Description', 'NULL si no es transacción', 'SCHEMA', 'dbo', 'TABLE',
     'tpex_LogEstados', 'COLUMN', 'idTransaccion'
go


