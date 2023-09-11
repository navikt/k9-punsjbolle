FROM gcr.io/distroless/java17-debian11:latest
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsjbolle

COPY app/build/libs/app.jar /app/app.jar
WORKDIR /app
CMD [ "app.jar" ]
