#!/bin/bash
#
# Despliegue de Bosque backend.
#
# Regla de oro: si un paso falla, NO se despliega.
#
# La version anterior tenia esto en la linea del git pull:
#
#     git pull origin master || echo "Git pull fallo (continuando...)"
#
# Ese "|| echo" anula el "set -e" para esa linea. Si el pull fallaba —y falla
# siempre que haya cambios locales en el servidor, porque git se niega a
# pisarlos— el script imprimia un warning y compilaba el codigo VIEJO igual. El
# despliegue "salia bien" cada vez. Asi fue como produccion quedo corriendo
# Spring Boot 2.6.1 y un application.properties de otra epoca mientras el
# repositorio tenia otra cosa.

set -euo pipefail

PROYECTO="/home/newimpexpap/proyectos/Bosque"
SERVICIO="container-bosque-app.service"
ENV_FILE="$HOME/.config/bosque/bosque.env"
IMAGEN="bosque-app"
JAR="target/bosque-1.0.0.jar"
CONTENEDOR="bosque-app"

# Variables sin las cuales la aplicacion NO arranca. Se verifican ANTES de
# compilar: mejor cortar en dos segundos que descubrirlo despues de cuatro
# minutos de build, con el contenedor en bucle de reinicio.
REQUERIDAS=(JWT_SECRET DB_USERNAME DB_PASSWORD)

echo "Iniciando despliegue - $(date '+%Y-%m-%d %H:%M:%S')"
echo "=============================================="

cd "$PROYECTO"

# ============================================================
# 0. PRE-VUELO: secretos
# ============================================================
if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: no existe $ENV_FILE"
    echo "       Sin el, el contenedor arranca y muere con:"
    echo "       Could not resolve placeholder 'jwt.secret'"
    exit 1
fi

FALTAN=()
for VAR in "${REQUERIDAS[@]}"; do
    if ! grep -qE "^${VAR}=.+" "$ENV_FILE"; then
        FALTAN+=("$VAR (ausente o vacia)")
    elif grep -qE "^${VAR}=(PEGAR_AQUI|CAMBIAR|TODO)$" "$ENV_FILE"; then
        FALTAN+=("$VAR (quedo el valor de plantilla sin reemplazar)")
    fi
done

if [ ${#FALTAN[@]} -gt 0 ]; then
    echo "ERROR: faltan variables en $ENV_FILE"
    for F in "${FALTAN[@]}"; do
        echo "       - $F"
    done
    exit 1
fi
echo "OK  Secretos presentes en $ENV_FILE"

# ============================================================
# 1. JAVA 8 JDK (se busca javac, no java)
# ============================================================
JAVA8_PATH=$(find /usr/lib/jvm -path '*1.8*' -name javac -type f 2>/dev/null | head -1 | xargs -r dirname | xargs -r dirname)

if [ -z "$JAVA8_PATH" ] || [ ! -x "$JAVA8_PATH/bin/java" ]; then
    echo "ERROR: no se encontro un JDK completo de Java 8 (con javac)."
    echo "       Instalar con: sudo dnf install java-1.8.0-openjdk-devel"
    exit 1
fi

export JAVA_HOME="$JAVA8_PATH"
export PATH="$JAVA_HOME/bin:$PATH"
echo "OK  Java 8 JDK: $JAVA_HOME"
java -version

chmod +x mvnw deploy.sh

# ============================================================
# 2. TRAER EL CODIGO — sin red de contencion
# ============================================================
echo ""
echo "Actualizando codigo (rama master)..."

# --ff-only: o avanza limpio, o falla. Nada de merges automaticos en el
# servidor, que dejarian un arbol que no es ni lo de aca ni lo del repositorio.
if ! git pull --ff-only origin master; then
    echo ""
    echo "ERROR: git pull fallo. NO SE DESPLIEGA."
    echo ""
    echo "       Causa habitual: hay cambios locales en el servidor que git no"
    echo "       quiere pisar. Estado del arbol:"
    echo ""
    git status --short || true
    echo ""
    echo "       Si esos cambios ya no hacen falta:  git checkout -- <archivo>"
    echo "       Si hay que conservarlos:            git stash"
    exit 1
fi

COMMIT=$(git rev-parse --short HEAD)
echo "OK  Codigo en commit $COMMIT - $(git log -1 --pretty=%s)"

# ============================================================
# 3. COMPILAR
# ============================================================
# Se borra el jar anterior para que el paso siguiente no pueda confundir un
# artefacto viejo con uno nuevo: si Maven no produce nada, se nota.
rm -f "$JAR"

# NOTA: este borrado de Lombok viene de la version original del script. Se
# conserva porque presumiblemente resolvio un problema real, aunque obliga a
# descargarlo en cada despliegue y hace que el deploy dependa de internet.
# Cuando se confirme que ya no hace falta, sacarlo.
rm -rf ~/.m2/repository/org/projectlombok/lombok/

echo ""
echo "Compilando con Maven..."
./mvnw clean package -DskipTests

if [ ! -f "$JAR" ]; then
    echo "ERROR: Maven termino pero no genero $JAR"
    ls -la target/*.jar 2>/dev/null || echo "       (no hay ningun .jar en target/)"
    exit 1
fi
echo "OK  Compilacion exitosa: $JAR"

# ============================================================
# 4. IMAGEN
# ============================================================
# Sin --quiet: cuando el build falla, se quiere ver por que.
# Dos etiquetas: 'latest' para el servicio, y el commit para poder volver atras
# sin recompilar.
echo ""
echo "Construyendo imagen Podman..."
podman build -t "${IMAGEN}:latest" -t "${IMAGEN}:${COMMIT}" .
echo "OK  Imagen construida: ${IMAGEN}:latest y ${IMAGEN}:${COMMIT}"

# ============================================================
# 5. REINICIAR Y COMPROBAR
# ============================================================
echo ""
echo "Reiniciando $SERVICIO..."
systemctl --user restart "$SERVICIO"

# "systemctl status: active" NO significa que la aplicacion arranco: con
# Restart=always, un contenedor que muere y vuelve a nacer se ve activo. Lo
# unico que prueba que Spring levanto es su propio log.
echo "Esperando a que la aplicacion levante..."
ARRANCO=0
for _ in $(seq 1 30); do
    if podman logs "$CONTENEDOR" 2>&1 | grep -q "Started BosqueApplication"; then
        ARRANCO=1
        break
    fi
    sleep 2
done

if [ "$ARRANCO" -ne 1 ]; then
    echo ""
    echo "ERROR: la aplicacion NO llego a arrancar en 60 s."
    echo "       Ultimas lineas del log:"
    echo ""
    podman logs --tail 40 "$CONTENEDOR" 2>&1 | grep -vE '^[[:space:]]+at ' || true
    echo ""
    echo "       Para volver a la version anterior:"
    echo "         podman images ${IMAGEN}"
    echo "         podman tag ${IMAGEN}:<commit-anterior> ${IMAGEN}:latest"
    echo "         systemctl --user restart ${SERVICIO}"
    exit 1
fi

podman logs "$CONTENEDOR" 2>&1 | grep -E "Started BosqueApplication|Tomcat started|profile is active" | tail -3

# Prueba de vida real: 401 en /actuator/health es la respuesta CORRECTA —
# significa que Tomcat atiende y que la cadena de seguridad esta puesta.
CODIGO=$(curl -k -s -o /dev/null -w "%{http_code}" --max-time 10 \
         https://localhost:8443/actuator/health || echo "000")
case "$CODIGO" in
    401|200) echo "OK  Responde HTTPS en 8443 (HTTP $CODIGO)" ;;
    000)     echo "AVISO: no respondio el puerto 8443 - revisar" ;;
    *)       echo "AVISO: HTTP $CODIGO inesperado en /actuator/health" ;;
esac

echo ""
echo "Despliegue completado.  commit $COMMIT"
echo "=============================================="
