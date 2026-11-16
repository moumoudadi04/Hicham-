# Hicham Auto Parts

Arabic-first Android inventory app for managing dismantled engines and selling individual parts.

## Important inventory rule
Every engine record has its own database ID and its own parts rows. Two engines with the same name (for example, two 2.3 dCi engines) are tracked independently. Selling a part from one engine never changes the other engine.

## Features
- Renault, Peugeot and Volkswagen brand sections.
- Add, rename and delete individual engine records.
- Each new engine receives an editable starter catalogue of common mechanical, fuel, turbo, cooling, lubrication and electrical/sensor parts.
- Add custom parts and adjust their stock quantities.
- Record sales by quantity; stock is reduced only for the selected engine.
- Sales history.
- Local SQLite storage on the phone; no account or internet connection is required for inventory use.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK installed. GitHub Actions builds the debug APK on every push to main and on manual dispatch.
