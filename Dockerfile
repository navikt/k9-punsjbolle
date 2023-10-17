FROM ghcr.io/navikt/baseimages/temurin:21
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsjbolle
COPY app/build/libs/app.jar app.jar
