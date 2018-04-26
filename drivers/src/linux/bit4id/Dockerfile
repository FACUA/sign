FROM alpine:3.7

# Create export script

VOLUME /out
RUN echo "#!/bin/sh" > /usr/local/bin/export-driver && \
    echo "cp /bit4id/bit4id.tar.gz /out" >> /usr/local/bin/export-driver && \
    chmod +x /usr/local/bin/export-driver

# Install dependencies

RUN apk --no-cache add wget unzip

# Download the driver

WORKDIR /bit4id

ARG bit4id_mirror

RUN wget -O bit4id.zip ${bit4id_mirror}

# Package the driver

ARG facua_sign_namespace=/usr/local/facua-sign/drivers

RUN unzip bit4id.zip
RUN rm bit4id.zip
RUN ls && dir=$(ls) && cd "${dir}" && mv * .. && cd .. && rmdir "${dir}"

RUN mkdir -p "${facua_sign_namespace}/bit4id/usr/lib"
RUN mv linux64/libbit4ipki.so* "${facua_sign_namespace}/bit4id/usr/lib"

RUN tar -czf bit4id.tar.gz "${facua_sign_namespace}/bit4id"
