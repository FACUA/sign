# CNP Driver

The Cuerpo Nacional de Policía provides their own proprietary PKCS#11 module
that enables communications with all DNI models. The OpenSC driver is a viable
open source alternative and generally performs better.

## Downloading the drivers

The included Dockerfile downloads the driver, and packages it into an archive
placed in `/cnp/cnp.tar.gz` in the container. The Dockerfile accepts the
following build parameters:

* `cnp_mirror`: the mirror from where to download the CNP drivers. 
* `facua_sign_namespace`: the location of the `drivers` folder in the final
installation. The CNP drivers will be placed in `drivers/cnp/usr`. This
must be identical to the location where the driver will actually be placed at
runtime.

The following commands will build the image and extract the archive with the
compiled binaries:

```bash
docker build -t cnp-linux . \
    --build-arg cnp_mirror=https://www.dnielectronico.es/descargas/distribuciones_linux/libpkcs11-dnie_1.4.1_amd64.deb \
    --build-arg facua_sign_namespace=/usr/local/facua-sign/drivers
docker run -v $(pwd):/out cnp-linux export-driver
sudo chown $(id -u):$(id -g) cnp.tar.gz
mkdir -p ../../../build
mv cnp.tar.gz ../../../build
```
