FROM ubuntu:16.04

# Create export script

VOLUME /out
RUN echo "#!/bin/sh" > /usr/local/bin/export-driver && \
    echo "cp /bit4id/bit4id.tar.gz /out" >> /usr/local/bin/export-driver && \
    chmod +x /usr/local/bin/export-driver

# Install dependencies

RUN apt-get update
RUN apt-get install -y git pcscd libccid libpcsclite-dev libssl-dev \
    libreadline-dev autoconf automake build-essential docbook-xsl \
    xsltproc libtool pkg-config

# Download OpenSC

WORKDIR /opensc

ARG opensc_commit

RUN git clone https://github.com/OpenSC/OpenSC .
RUN git checkout ${opensc_commit}

# Build OpenSC

ARG facua_sign_namespace=/usr/local/facua-sign/drivers

RUN ./bootstrap
RUN ./configure \
    --prefix="${facua_sign_namespace}/opensc/usr" \
    --sysconfdir="${facua_sign_namespace}/opensc/etc/opensc"
RUN make
RUN make install

# Package OpenSC

RUN tar -czf opensc.tar.gz "${facua_sign_namespace}/opensc"

VOLUME /out
RUN echo "#!/bin/sh" > /usr/local/bin/export-driver && \
    echo "cp /opensc/opensc.tar.gz /out" >> /usr/local/bin/export-driver && \
    chmod +x /usr/local/bin/export-driver