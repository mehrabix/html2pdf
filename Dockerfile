FROM eclipse-temurin:23-jre

# Define environment variables for versions and download URLs
ENV DEBIAN_FRONTEND=noninteractive \
    LIBJPEG_VERSION=2.1.5-4 \
    LIBJPEG_PKG=libjpeg62-turbo_2.1.5-4_amd64.deb \
    LIBSSL_VERSION=1.1.1w-0+deb11u1 \
    LIBSSL_PKG=libssl1.1_1.1.1w-0+deb11u1_amd64.deb \
    WKHTMLTOPDF_VERSION=0.12.6.1-2 \
    WKHTMLTOPDF_PKG=wkhtmltox_0.12.6.1-2.bullseye_amd64.deb \
    MAVEN_VERSION=3.9.10 \
    MAVEN_BASE_URL=https://dlcdn.apache.org/maven/maven-3 \
    MAVEN_HOME=/opt/maven \
    APP_DIR=/app

# Install dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    wget \
    unzip \
    fontconfig \
    gnupg \
    dirmngr \
    libx11-6 \
    libxcb1 \
    libxext6 \
    libxrender1 \
    xfonts-75dpi \
    xfonts-base && \
    rm -rf /var/lib/apt/lists/*

# Install libjpeg-turbo
RUN wget http://ftp.us.debian.org/debian/pool/main/libj/libjpeg-turbo/$LIBJPEG_PKG && \
    dpkg -i $LIBJPEG_PKG && \
    rm -f $LIBJPEG_PKG

# Install wkhtmltopdf dependencies and binary
RUN wget http://deb.debian.org/debian/pool/main/o/openssl/$LIBSSL_PKG && \
    wget https://github.com/wkhtmltopdf/packaging/releases/download/$WKHTMLTOPDF_VERSION/$WKHTMLTOPDF_PKG && \
    dpkg -i $LIBSSL_PKG $WKHTMLTOPDF_PKG || apt-get -f install -y --no-install-recommends && \
    rm -f $LIBSSL_PKG $WKHTMLTOPDF_PKG

# Copy custom fonts
RUN mkdir -p /usr/share/fonts/truetype/vazirmatn
COPY src/main/resources/static/fonts/*.ttf /usr/share/fonts/truetype/vazirmatn/
RUN fc-cache -f -v

# Install Apache Maven
RUN wget $MAVEN_BASE_URL/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip -P /tmp && \
    unzip /tmp/apache-maven-$MAVEN_VERSION-bin.zip -d /opt && \
    ln -s /opt/apache-maven-$MAVEN_VERSION $MAVEN_HOME && \
    rm /tmp/apache-maven-$MAVEN_VERSION-bin.zip

ENV PATH=$MAVEN_HOME/bin:$PATH

# Set working directory
WORKDIR $APP_DIR

# Build the application
COPY pom.xml .
RUN mvn -B -X clean package

COPY target/html2pdf-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 3000

CMD ["java", "-jar", "app.jar"]
