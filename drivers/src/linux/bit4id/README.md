# Bit4Id Driver

The Bit4Id Universal Middleware is a proprietary PKCS#11 module that enables
communications with some smart card models, including the ACA smart cards that
Facua Sign aims to support. Unfortunately, there is no open source alternative
that works with these kind of smart cards.

## Downloading the drivers

The included Dockerfile downloads the driver, and packages it into an archive
placed in `/bit4id/bit4id.tar.gz` in the container. The Dockerfile accepts the
following build parameters:

* `bit4id_mirror`: the mirror from where to download the Bit4Id middleware. 
* `facua_sign_namespace`: the location of the `drivers` folder in the final
installation. The Bit4Id driver will be placed in `drivers/bit4id/usr`. This
must be identical to the location where the driver will actually be placed at
runtime.

The following commands will build the image and extract the archive with the
compiled binaries:

```bash
docker build -t bit4id-linux . \
    --build-arg bit4id_mirror=http://cdn.bit4id.com/es/soporte/downloads/middleware/Bit4id_Middleware.zip \
    --build-arg facua_sign_namespace=/usr/local/facua-sign/drivers
docker run -v $(pwd):/out bit4id-linux export-driver
sudo chown $(id -u):$(id -g) bit4id.tar.gz
mkdir -p ../../../build
mv bit4id.tar.gz ../../../build
```