# OpenSC driver

[OpenSC](https://github.com/OpenSC/OpenSC) is an open source PKCS#11 middleware
that supports many widely used smart cards. OpenSC enables Facua Sign to support
the DNIe and DNI 3.0.

## Building the drivers

The included Dockerfile clones and builds the OpenSC project, and packages it
into an archive placed in `/opensc/opensc.tar.gz` in the container. The
Dockerfile accepts the following build parameters:

* `opensc_commit`: the commit in the OpenSC repository from which to build the
project.
* `facua_sign_namespace`: the location of the `drivers` folder in the final
installation. The OpenSC drivers will be placed in `drivers/opensc/usr` and the
configuration in `drivers/opensc/etc/opensc`. This must be identical to the
location where the drivers will actually be placed at runtime.

The following commands will build the image and extract the archive with the
compiled binaries:

```bash
docker build -t opensc-linux . \
    --build-arg opensc_commit=fbc9ff84bcfdc72d54b90f65158f5e60c204864c \
    --build-arg facua_sign_namespace=/usr/local/facua-sign/drivers
docker run -v $(pwd):/out opensc-linux export-driver
sudo chown $(id -u):$(id -g) opensc.tar.gz
mkdir -p ../../../build
mv opensc.tar.gz ../../../build
```
