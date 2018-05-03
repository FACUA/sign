#!/usr/bin/env bash
set -e

APP_INSTALL_LOCATION="usr/local/facua-sign"

if [ ! -f build.gradle ]; then
    echo "Please run this script from the project root!"
    exit 1
fi

read -p "Enter the package version (format: Major.minor.patch-package-revision): " version

echo "The following drivers will be included in the distribution:"
echo

jar="./build/facua-sign.jar"

if [[ ! -e ${jar} ]]; then
    # If the jar hasn't been placed there by Docker, maybe it's been
    # compiled with IntelliJ
    jar=$(ls -1r build/libs/sign-prod-*.jar | head -n 1)
fi

if [[ ! -e ${jar} ]]; then
    echo "The compiled jar is not present. Please build the project and"
    echo "place the built jar in $(pwd)/build/facua-sign.jar"
    exit 1
fi

drivers=$(ls drivers/build/*.tar.gz)

package_name="facua-sign_${version}"
new_ld_library_path=""

for driver in ${drivers}; do
    name=$(echo ${driver} | sed 's/.tar.gz//g' | sed 's/drivers\/build\///g')

    echo ${name}
    new_ld_library_path="$new_ld_library_path:/${APP_INSTALL_LOCATION}/drivers/${name}/usr/lib"
done

echo
read -p "Continue? (Y/n) " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi

echo

tmp_dir=$(mktemp -dt facua-sign.XXXXX)
pkg_dir="${tmp_dir}/${package_name}"
mkdir ${pkg_dir}

for driver in ${drivers}; do
    tar -xzf ${driver} -C ${pkg_dir}
done

cp ${jar} "${pkg_dir}/${APP_INSTALL_LOCATION}/facua-sign.jar"
icon_path="usr/share/icons/hicolor/192x192/apps"
mkdir -p "${pkg_dir}/${icon_path}"
cp src/main/resources/icon.png "${pkg_dir}/${icon_path}/org.facua.sign.png"

pushd ${tmp_dir}
    pushd ${package_name}
        mkdir -p usr/share/applications
        pushd usr/share/applications

cat > org.facua.sign.desktop << EOF
[Desktop Entry]
Version=${version}
Name=Facua Sign
Comment=Una aplicación para firmar digitalmente. Versión ${version}.
Exec=/usr/local/bin/facua-sign
Icon=/${icon_path}/org.facua.sign.png
Type=Application
EOF
            chmod +x org.facua.sign.desktop

        popd

        mkdir -p usr/local/bin
        pushd usr/local/bin

cat > facua-sign << EOF
#!/usr/bin/env bash
openjdk_java="/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java"

if [[ -e \${openjdk_java} ]]; then
    java_executable=\${openjdk_java}
else
    java_executable=\$(which java)
fi

LD_LIBRARY_PATH="\$LD_LIBARY_PATH${new_ld_library_path}" \${java_executable} -jar /${APP_INSTALL_LOCATION}/facua-sign.jar
EOF
        chmod +x facua-sign

        popd

        mkdir DEBIAN
        pushd DEBIAN

cat > control << EOF
Package: facua-sign
Version: ${version}
Section: base
Priority: optional
Architecture: amd64
Depends: openjdk-8-jre (>= 8u162-b12-0), openjfx (>= 8u60-b27-4), pcscd (>= 1.8.14-1), pcsc-tools (>= 1.4.25-1), libccid (>= 1.4.22-1), libpcsclite1 (>= 1.8.14-1)
Maintainer: Departamento de Programación de FACUA <programacion@facua.org>
Description: An application for digitally signing documents
EOF

        popd

    popd

    dpkg-deb --build -Z gzip ${package_name}
popd

mv "${tmp_dir}/${package_name}.deb" .
rm -rf ${tmp_dir}
