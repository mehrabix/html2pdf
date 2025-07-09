FROM eclipse-temurin:23-jre

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    wget \
    unzip \
    fontconfig \
    gnupg \
    dirmngr libx11-6 libxcb1 libxext6 libxrender1 xfonts-75dpi xfonts-base

# Install libjpeg-turbo
RUN wget http://ftp.us.debian.org/debian/pool/main/libj/libjpeg-turbo/libjpeg62-turbo_2.1.5-4_amd64.deb && \
    dpkg -i libjpeg62-turbo_2.1.5-4_amd64.deb && \
    rm ./*.deb 


# Install wkhtmltopdf dependencies and wkhtmltopdf itself
RUN wget http://deb.debian.org/debian/pool/main/o/openssl/libssl1.1_1.1.1w-0+deb11u1_amd64.deb && \
    wget https://github.com/wkhtmltopdf/packaging/releases/download/0.12.6.1-2/wkhtmltox_0.12.6.1-2.bullseye_amd64.deb && \
    dpkg -i ./*.deb || apt-get -f install -y --no-install-recommends && \
    rm ./*.deb


# Copy and install fonts
RUN mkdir -p /usr/share/fonts/truetype/vazirmatn
COPY src/main/resources/static/fonts/*.ttf /usr/share/fonts/truetype/vazirmatn/
RUN fc-cache -f -v

WORKDIR /app

# Copy the pre-built jar from local machine into the image
COPY target/html2pdf-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 3000

CMD ["java", "-jar", "app.jar"]
