FROM ubuntu:18.04

RUN apt-get update
RUN apt-get install -y git openssl libssl-dev automake autoconf libtool make \
    g++ opensc

ARG facua_sign_namespace=/usr/local/facua-sign/drivers
ARG softhsm_commit=bdb8031c99315b6fe8e2e791385898b9275665d0

WORKDIR /softhsm
RUN git clone https://github.com/opendnssec/SoftHSMv2.git .
RUN git checkout ${opensc_commit}
RUN sh autogen.sh
RUN dir="${facua_sign_namespace}/softhsm" && \
    mkdir -p ${dir} && \
    mkdir -p ${dir} && \
    mkdir -p ${dir} && \
    ./configure --disable-gost \
        --prefix ${dir} \
        --bindir ${dir}/usr/bin \
        --libdir ${dir}/usr/lib \
        --datarootdir ${dir}/usr/share
RUN make
RUN make install

ENV PATH="${PATH}:${facua_sign_namespace}/softhsm/usr/bin"
ENV LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${facua_sign_namespace}/softhsm/usr/lib"

RUN softhsm2-util --init-token --slot 0 --label "SoftHSM Test Token" \
        --pin 1234 --so-pin 4321 && \
    openssl req -x509 -nodes -newkey rsa:4096 -keyout key.pem -out cert.pem \
        -days 365 -subj '/CN=localhost' && \
    openssl pkcs8 -in key.pem -topk8 -nocrypt -out key.pk8 && \
    softhsm2-util --pin 1234 --so-pin 4321 --import key.pk8 \
        --token "SoftHSM Test Token" --label "SoftHSM Test Key" --id 12341234 && \
    pkcs11-tool \
        --module "${facua_sign_namespace}/softhsm/usr/lib/softhsm/libsofthsm2.so" \
        --so-pin 4321 --write-object cert.pem --type cert --id 12341234 \
        --label "SoftHSM Test Cert"

RUN tar -czf softhsm.tar.gz "${facua_sign_namespace}/softhsm"
VOLUME /out

RUN echo "#!/bin/sh" > /usr/local/bin/export-driver && \
    echo "cp /softhsm/softhsm.tar.gz /out" >> /usr/local/bin/export-driver && \
    chmod +x /usr/local/bin/export-driver