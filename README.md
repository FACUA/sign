# Facua Sign

Facua Sign es una herramienta para firmar archivos con tarjetas inteligentes.
Actualmente soporta firmar con el DNIe y las tarjetas ACA.

Este proyecto surge debido a la carencia de soporte en el ecosistema de las
firmas digitales en entornos Linux. Los objetivos de Facua Sign son:

* Ser una aplicación sencilla de usar, donde el usuario final no tenga que
configurar nada.
* Minimizar las dependencias externas: una sola instalación del paquete de
Facua Sign deberá configurar el sistema para el correcto funcionamiento de la
aplicación.
* Ser extensible: si bien la aplicación está desarrollada para cubrir una
necesidad concreta de FACUA (firmar documentos PDF con tarjetas ACA), está
diseñada de forma que añadir soporte a otros tipos de tarjetas inteligentes
sea simple.


### Nota sobre las tarjetas ACA

[ACA](http://www.abogacia.es/site/aca/que-es-aca-y-que-ventajas-te-ofrece/) es
la Autoridad Certificadora de la Abogacía. Proveen dos modelos de tarjetas
inteligentes: TS 2048, y TS 2048 JS. Ambas solo funcionan con el driver
propietario de [Bit4Id](https://www.bit4id.com/es/) (ver
`drivers/src/linux/bit4id`). Sin embargo, hemos comprobado que las tarjetas
TS 2048 funcionan con lectores de tarjeta inteligente genéricos, mientras que
las TS 2048 JS solo funcionan con lectores fabricados por Bit4Id. El lector con
el que hemos tenido éxito ha sido el lector "Bit4Id miniLector EVO".

Este asunto está fuera de nuestro control, y no podemos conseguir que las
tarjetas TS 2048 JS funcionen con lectores genéricos.

### Documentación

Para compilar y/o modificar el proyecto, consulta la documentación (en inglés):

* [Estructura del proyecto](https://github.com/FACUA/sign/blob/master/docs/project_structure.md)
* [Cómo compilar y empaquetar](https://github.com/FACUA/sign/blob/master/docs/how_to_build_and_package.md)
