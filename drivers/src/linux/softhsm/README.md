# SoftHSM driver

[SoftHSM](https://www.opendnssec.org/softhsm/) is a software implementation of
the PKCS#11 interface, useful to test the application. If the driver is
installed, it will behave as if there was a regular smart card plugged in.

## Building the drivers

```bash
docker build -t softhsm-linux . \
    --build-arg softhsm_commit=2c167a99825a3db4138acff7611801ef73f2c9cd \
    --build-arg facua_sign_namespace=/usr/local/facua-sign/drivers
docker run -v $(pwd):/out softhsm-linux export-driver
sudo chown $(id -u):$(id -g) softhsm.tar.gz
mv softhsm.tar.gz ../../../build
```
