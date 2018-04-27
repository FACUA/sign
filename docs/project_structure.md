# Project structure

The Facua Sign project is modularized so that code can be easily reused. It is
divided in the following packages:

* `common`: contains code reused by all other modules. Extracting one of the
modules will also require to extract this one.
* `core`: contains the low level logic that interacts with the smart cards. The
`SmartCards` object contains a function `getAll()` that will return high level
APIs for interacting with them.
* `i18n`: short for "internationalization", contains the logic for displaying
localized messages based on the language of the end user's OS.
* `pdf`: contains the logic for interacting with PDFs. It currently has two main
functions: PDF rendering into images to display them in the UI, and PDF signing.
* `ui`: contains the `FacuaSign` JavaFX application that presents an UI to
interact with the rest of the application.

## Overview

The project is written in Kotlin and developed with
[IntelliJ IDEA](https://www.jetbrains.com/idea/) Community Edition. In order to
build it, you don't need IntelliJ as we provide a Dockerfile to achieve the same
task. However, for local development, we wouldn't recommend another IDE, as we
can't guarantee that everything will work fine.

## The `core` package

The core package loads all installed PKCS#11 modules, and attempts to find
smart cards with each. Generally, only one of the modules will successfully
detect a smart card, but in the event that two may detect the same one, the
order of drivers defined in `SmartCardDrivers` will determine the priority.

Modules are not bundled in the application `jar`; they must be installed in the
system and will be located at runtime. The following structure is used to store
modules:

The application is installed to `/usr/local/facua-sign`. This path is hardcoded
in `SmartCardDrivers`. Inside that directory, there is a `drivers` directory.
All drivers will be located there, identified by the vendor name. The supported
drivers are `opensc`, `cnp` and `bit4id`. So, if we packaged the application
with the `opensc` driver, it would be placed at
`/usr/local/facua-sign/drivers/opensc`.

Inside each driver's directory, the driver files will be placed, pretending that
it's an absolute path. So, the opensc library files would be located at
`/usr/local/facua-sign/drivers/opensc/usr/lib`. This allows us to isolate all
dependencies from the system to make sure that an installation of Facua Sign
always has the correct dependencies.

In order for the application to work correctly, the `LD_LIBRARY_PATH` must be
correctly set, containing each installed driver's `lib` folder. If it isn't
correct, the application will crash at startup.

The main classes and objects of the `core` package are:

* `SmartCards`: contains the function `get()` that will detect smart cards.
* `SmartCard` (and children): represent a single smart card. The parent class
contains abstract methods and properties to identify generic cards, and the
children contain their implementation.
* `SmartCardOperator`: acts as a low-level bridge between a `SmartCard` and
`PKCS#11`.
* `SmartCardDrivers`: contains utilities for detecting and working with the
installed `PKCS#11` modules.

## The `ui` package

The UI is written using the
[RxKotlinFx](https://github.com/thomasnield/RxKotlinFX) stack, with combines
JavaFX, RxJava and Kotlin. Each window has a corresponding view inside the
`view` package, and a controller inside the `controller` package.

The following pattern is used:

Controllers have an `init` method called from the view, which provides an easy
way to obtain a reference to their respective views. Inside them, they call two
other methods: `setupBindings` and `setupEventHandlers`.

`setupBindings` will bind the properties of UI components as needed, to external
data sources, or other UI components. An example of this is the `MainView`'s
`smartCardLabel` text, which is bound to the stringified value of the last
SmartCard emitted by the `SmartCardPoller`'s reactive stream.