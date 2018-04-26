FROM ubuntu:16.04

# Create export script

VOLUME /out
RUN echo "#!/bin/sh" > /usr/local/bin/export-driver && \
    echo "cp /bit4id/bit4id.tar.gz /out" >> /usr/local/bin/export-driver && \
    chmod +x /usr/local/bin/export-driver

# Install dependencies

RUN apt-get update
RUN apt-get install -y wget

# Download and extract the driver

WORKDIR /cnp

ARG cnp_mirror

RUN wget -O driver.deb ${cnp_mirror}
RUN dpkg-deb -R driver.deb driver

# Package the driver

ARG facua_sign_namespace=/usr/local/facua-sign/drivers

RUN mkdir -p "${facua_sign_namespace}/cnp"
RUN mv driver/usr "${facua_sign_namespace}/cnp"

RUN tar -czf cnp.tar.gz "${facua_sign_namespace}/cnp"

VOLUME /out
RUN echo "#!/bin/sh" > /usr/local/bin/export-driver && \
    echo "cp /cnp/cnp.tar.gz /out" >> /usr/local/bin/export-driver && \
    chmod +x /usr/local/bin/export-driver