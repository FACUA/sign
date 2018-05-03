# OpenSC driver

[OpenSC](https://github.com/OpenSC/OpenSC) is an open source PKCS#11 middleware
that supports many widely used smart cards. As of 17/04/2018, OpenSC enables
Facua Sign to support the DNIe. However, it does not support the DNI 3.0 yet
(although
[work is being put into it](https://github.com/OpenSC/OpenSC/issues/1313)).

The CNP proprietary drivers support both versions of the DNI, which makes ths an
optional dependency. That said, the OpenSC drivers perform much better for the
DNIe, which might make it worth it to include.

When the issue linked above is resolved, the CNP drivers will no longer be
needed, and these ones will be preferred.

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
    --build-arg opensc_commit=2c167a99825a3db4138acff7611801ef73f2c9cd \
    --build-arg facua_sign_namespace=/usr/local/facua-sign/drivers
docker run -v $(pwd):/out opensc-linux export-driver
sudo chown $(id -u):$(id -g) opensc.tar.gz
mkdir -p ../../../build
mv opensc.tar.gz ../../../build
```
