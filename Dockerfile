FROM eclipse-temurin:8-jre@sha256:0517e503352d136230732fe060bf78115597891162bc48bb4dc20cef0bf25eeb

WORKDIR /app

# Crear carpeta uploads (el volumen real se monta en runtime desde el servicio)
RUN mkdir -p /app/uploads && chmod 777 /app/uploads

# Instalar fuentes para JasperReports / AWT (necesario para PDFs)
RUN apt-get update && \
    apt-get install -y fontconfig fonts-dejavu-core fonts-liberation && \
    rm -rf /var/lib/apt/lists/*

# Copiar la aplicación compilada
COPY target/*.jar app.jar

# Headless mode para evitar errores de fuentes
ENV JAVA_OPTS="-Djava.awt.headless=true -Dnet.sf.jasperreports.awt.ignore.missing.font=true"

EXPOSE 8443

# Usamos sh para que JAVA_OPTS se aplique correctamente
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
