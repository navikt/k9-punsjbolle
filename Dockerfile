FROM navikt/java:15
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsjbolle
COPY app/build/libs/*.jar app.jar
