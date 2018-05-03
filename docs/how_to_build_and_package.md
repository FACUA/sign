# How to build and package

The project can be divided into two parts:

* **The main project**: in a nutshell, everything that goes into the JAR. It is
created and maintained by FACUA.
* **The card drivers**: Facua Sign uses third party smart card drivers. Some of
them are free software, some of them are non-free. To find more information, go
to the `drivers/src` subdirectory. They are not owned nor maintained by FACUA.

Because some necessary drivers are non-free, FACUA doesn't distribute them,
which makes impossible to distribute the project packages publicly. Instead,
FACUA provides tools to download or compile the drivers from their original
source, and then tools to bundle them with the main project in a distributable
package.

This document explains how to achieve so.

## Building the main project

In the project root FACUA provides a `Dockerfile` that builds the project and
creates a `jar` archive with all the necessary dependencies. The only exception
is JavaFX, which isn't packaged into the jar, and is required at runtime. The
`scripts/package_debian.sh` creates a `.deb` package that lists `openjdk-8-jre`
and `openjfx` as dependencies, so this shouldn't be a problem.

In order to build the jar, the following commands can be ran:

```bash
docker build -t facua-sign .
docker run -v $(pwd):/out facua-sign export-jar
sudo chown $(id -u):$(id -g) facua-sign.jar
mkdir -p build
mv facua-sign.jar build
```

It will build the project, then extract the `jar` from the container into
`build/facua-sign.jar`.

## Building (or downloading) the card drivers

FACUA also provides Dockerfiles for building or downloading and extracting
card drivers. The drivers are not bundled into the `jar` archive; they must be
built independently and placed in the `drivers/build` directory. The packaging
script will take all the drivers from there and create a distributable package.
This means that in order to not include any of the supported drivers, one must
simply not build them.

In order to build the drivers, go to `drivers/src`, then navigate to the
directory of the driver you want to build, and follow the instructions on the
`README.md` file.

## Packaging for Debian

FACUA provides a `package_debian.sh` script in the `scripts` folder that will
create a distributable Debian package using the built project (expected to be
in `build/facua-sign.jar`) and the card drivers (expected to be in
`drivers/build/<driver-name>.tar.gz`).

In order to build the package, first build the project and the necessary
drivers, and then run the `script/package_debian.sh` script from the project
root.