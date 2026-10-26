# App espejo de remax.pe + SIGI

App para Android e iOS (hecha con Capacitor) que muestra el sitio web y SIGI tal cual se ven en la computadora.

- **Clientes:** botón "Buscar propiedades", que abre https://www.remax.pe dentro de la app.
- **Agentes:** botón "Soy agente", que abre SIGI. Cada agente inicia sesión con su propio usuario. La app no guarda contraseñas: la sesión vive en el navegador interno, igual que en Chrome.

## SIGI

El botón "Soy agente" abre https://www.remax.pe/accounts/login-sigi/ (definido en `www/index.html`). Como SIGI está en el mismo dominio remax.pe, ya se abre dentro de la app. Si después del inicio de sesión SIGI te lleva a otro dominio, agrégalo a `server.allowNavigation` en `capacitor.config.json` y ejecuta `npx cap sync`.

## Generar el instalable

- **Android (automático):** sube esta carpeta a un repositorio de GitHub. El flujo `.github/workflows/android-apk.yml` genera el APK en cada cambio a `main` y lo publica en la sección Releases del repositorio como `REMAX-Peru.apk`.
- **Android (en tu PC):** instala Android Studio y ejecuta `npx cap open android`. Luego usa Build > Build APK.
- **iOS:** necesitas una Mac con Xcode. Ejecuta `npx cap open ios`.

## Marca

La app se llama "REMAX Perú" y muestra el logo oficial, incluido dentro de `www/index.html`. El ícono de la app usa el globo oficial (generado en `android/app/src/main/res/mipmap-*` y en `ios/App/App/Assets.xcassets`). Para publicar la app en Play Store o App Store con el nombre y el logo de REMAX se necesita autorización escrita de REMAX Perú. El ícono de la app en el teléfono también se reemplaza con el logo oficial antes de publicarla.
