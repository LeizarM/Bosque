# =============================================================================
#  Bosque ERP Backend
# =============================================================================

# Imagen base PINEADA POR DIGEST, no por etiqueta.
#
# "eclipse-temurin:8-jre" flota: hoy da 8u492, manana otro. Y la version del JDK
# no es un detalle cosmetico aca — 8u291 deshabilito TLS 1.0 y 1.1 por defecto,
# y este backend habla con un SQL Server 2008. Un cambio de base puede cortar la
# conexion a la base sin que nadie toque una linea de codigo.
#
# Para actualizar a proposito:
#   podman pull eclipse-temurin:8-jre
#   podman image inspect eclipse-temurin:8-jre --format '{{.Digest}}'
# y se reemplaza el sha de abajo, probando despues.
FROM eclipse-temurin:8-jre@sha256:0517e503352d136230732fe060bf78115597891162bc48bb4dc20cef0bf25eeb

# ── Locale ───────────────────────────────────────────────────────────────────
# Java 8 toma su charset por defecto del locale del sistema. En una imagen
# minima ese locale es POSIX, o sea ASCII: acentos y enies se convierten en '?'
# al leer archivos, al escribir logs y al generar PDFs.
#
# En Windows el desarrollador no lo ve, porque ahi el default es Cp1252 y los
# acentos andan. Es una diferencia dev/prod que no da error: da texto roto.
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

WORKDIR /app

# ── Fuentes ──────────────────────────────────────────────────────────────────
# Va ANTES del COPY del jar para que la capa se cachee: cambiar el jar no
# reinstala las fuentes.
#
# fontconfig es obligatorio: sin el, AWT en modo headless tira
# "Fontconfig head is null" al primer reporte. Las familias Liberation y DejaVu
# son el respaldo del sistema; el mapeo real Arial/SansSerif -> Liberation viaja
# DENTRO del jar (src/main/resources/fonts + jasperreports_extension.properties),
# que es lo que garantiza que el PDF salga igual en Windows y aca.
#
# fc-cache construye el indice: sin el, la primera consulta de fuentes de cada
# arranque lo reconstruye sola y el primer reporte tarda de mas.
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        fontconfig \
        fonts-dejavu-core \
        fonts-liberation && \
    fc-cache -f && \
    rm -rf /var/lib/apt/lists/*

# 755 y no 777. El proceso escribe como su propio usuario; nadie mas necesita
# escribir aca. En runtime este punto lo tapa el volumen bosque-uploads.
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

# Nombre explicito, no "target/*.jar": si target/ quedo con el jar de otra
# version, el glob matchea dos y COPY falla con "copying multiple files to a
# single file" — un error que aparece recien al construir la imagen.
COPY target/bosque-1.0.0.jar app.jar

# ── Opciones de la JVM ───────────────────────────────────────────────────────
#   headless                  no hay servidor grafico; sin esto AWT falla al
#                             renderizar un reporte
#   ignore.missing.font       si un reporte pide una fuente sin mapear, sustituye
#                             en vez de tirar el PDF entero
#   file.encoding=UTF-8       redundante con LANG, y a proposito: si alguien
#                             lanza el contenedor pisando el locale, esto queda
#   MaxRAMPercentage=75       la JVM respeta el limite de memoria del contenedor
#                             en vez de mirar la RAM del host. Requiere 8u191+;
#                             la base pineada es 8u492
#   ExitOnOutOfMemoryError    ante un OOM el proceso muere y systemd lo levanta,
#                             en vez de quedar vivo respondiendo a medias
ENV JAVA_OPTS="-Djava.awt.headless=true \
-Dnet.sf.jasperreports.awt.ignore.missing.font=true \
-Dfile.encoding=UTF-8 \
-XX:MaxRAMPercentage=75.0 \
-XX:+ExitOnOutOfMemoryError"

# 8443 es el HTTPS real. 8080 lo abre BosqueApplication solo para redirigir a
# HTTPS, y hoy no se publica en el podman run: si se quiere que alguien escriba
# http:// y termine en https://, hay que mapearlo tambien.
EXPOSE 8443 8080

# Sonda de vida sin instalar nada: /dev/tcp es una facilidad de bash, no un
# programa. Solo confirma que el puerto acepta conexiones; el estado sale en
# 'podman ps'.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8443' || exit 1

# 'exec' es la palabra clave. Sin ella, sh queda como PID 1 y java como hijo: el
# SIGTERM de 'podman stop' o de systemctl lo recibe sh, java no se entera, Spring
# no cierra ordenado y a los 10 s lo matan a lo bruto — con conexiones del pool y
# escrituras a medio camino. Con exec, java ES el PID 1 y recibe la senal.
#
# Se conserva 'sh -c' porque JAVA_OPTS tiene que expandirse; en forma exec pura
# viajaria como un unico argumento literal.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
