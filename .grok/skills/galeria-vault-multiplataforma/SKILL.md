---
name: galeria-vault-multiplataforma
description: |
  Usar esta skill cuando el usuario pida construir, diseñar o extender una app
  de galería multiplataforma (Flutter/Android/iOS) que agrupe fotos y videos
  por fuente/álbum (Google Photos, capturas de pantalla, WhatsApp, Downloads,
  cámara, genéricos) y que incluya un módulo "Vault" de bloqueo con PIN,
  patrón o biometría para ocultar y cifrar contenido fuera del almacenamiento
  público del sistema (MediaStore/PHPhotoLibrary).

  Activar para:
  - "hazme una skill/app de galería con bloqueo/vault/caja fuerte"
  - Requisitos sobre ocultar fotos/videos del sistema, cifrado local,
    autenticación biométrica en apps móviles
  - Arquitectura modular Flutter (feature-first) para apps multimedia
  - Compartir a redes sociales y edición básica de imagen/video en la misma app

  No activar para:
  - Apps web puras sin acceso a almacenamiento nativo
  - Integraciones de backend sin componente de galería/vault
license: Uso interno del usuario para su propio desarrollo.
---

# Skill: Galería + Vault Multiplataforma

Esta skill define cómo Claude debe razonar y construir (paso a paso, con
autorización explícita antes de cada archivo, según la metodología del
usuario) una app de galería multiplataforma con un módulo de bloqueo
cifrado ("Vault"). No es un generador automático: es la guía de arquitectura,
convenciones y checklist que Claude debe seguir durante la construcción.

## Alcance del producto

1. **Galería multi-fuente**: escanea y agrupa en álbumes automáticos:
   Google Photos (si el usuario vincula cuenta), Capturas de pantalla,
   WhatsApp Images/Video, Downloads, Cámara, Genéricos/Otros.
2. **Vault (bloqueo)**: mover fotos/videos a un almacenamiento privado,
   cifrado, invisible para Fotos del sistema, Google Fotos y Archivos.
   Acceso obligatorio con PIN o patrón; huella/Face ID solo como método
   adicional (nunca único).
3. **Compartir**: share intent nativo a redes, disponible en galería
   pública y en elementos exportados del vault (nunca se comparte
   directo desde dentro del vault sin exportar primero).
4. **Editor básico**: recorte, rotación, filtros, ajustes simples.

## Principio rector: modularidad sin mezclas

Cada feature es independiente y se comunica solo a través de contratos
(interfaces/abstracciones), nunca importando directamente la capa interna
de otro feature. Antes de tocar cualquier archivo, Claude debe identificar
a qué capa y feature pertenece y no debe "de paso" tocar otra.

```
lib/
 ├── core/
 │    ├── di/                  (inyección de dependencias, service locator)
 │    ├── encryption/          (AES-256, derivación de clave desde PIN)
 │    ├── secure_storage/      (Keystore/Keychain wrapper)
 │    ├── platform_channels/   (MediaStore / PHPhotoLibrary bridge)
 │    └── error/
 ├── features/
 │    ├── gallery/
 │    │    ├── data/           (fuentes: mediastore, whatsapp, downloads, google_photos)
 │    │    ├── domain/         (entidades: MediaItem, Album; casos de uso)
 │    │    └── presentation/   (grids, viewers, providers/blocs)
 │    ├── vault/
 │    │    ├── data/           (repositorio cifrado, DB local sqlcipher/Isar)
 │    │    ├── domain/         (LockMethod, VaultItem, casos de uso mover/exportar)
 │    │    └── presentation/   (pantalla auth, grid vault, visor)
 │    ├── editor/
 │    │    ├── domain/
 │    │    └── presentation/
 │    └── share/
 │         └── presentation/   (bottom sheet de compartir)
 └── shared/
      ├── widgets/
      └── theming/
android/  → MediaStore, MediaScannerConnection, .nomedia
ios/      → PHPhotoLibrary, PHAsset, atributo isExcludedFromBackup
```

**Regla dura**: `vault` nunca depende de `gallery` en dirección inversa;
`gallery` puede pedirle a `vault`, a través de una interfaz en `core`, que
"oculte" un item, pero no conoce su implementación interna de cifrado.

## Reglas de seguridad (no negociables)

- Autenticación del Vault: **PIN o patrón obligatorio**. Biometría es
  atajo opcional, con fallback siempre disponible a PIN/patrón.
- Clave de cifrado real vive en Keystore (Android) / Keychain (iOS),
  desbloqueada por el PIN vía derivación (PBKDF2 o Argon2), nunca
  hardcodeada ni derivada solo del PIN en texto plano guardado en disco.
- Archivos movidos al vault:
  - Se cifran (AES-256-GCM) antes de escribir a disco.
  - Se guardan en almacenamiento **privado de la app** (sandbox), no en
    almacenamiento externo/público.
  - Se eliminan del MediaStore/PHPhotoLibrary (no solo se marcan ocultos)
    para que no aparezcan en Google Fotos, Galería del sistema ni
    Archivos.
  - Carpeta interna marcada con `.nomedia` (Android) / excluida de
    backups e indexado (iOS) como defensa adicional.
- Metadata del vault (nombres, álbum, fecha) en base de datos local
  cifrada (ej. Drift/Isar + sqlcipher), nunca en texto plano.
- Exportar = descifrar + reinsertar en MediaStore/PHPhotoLibrary de forma
  explícita, solo por acción del usuario, item por item o en lote.
- Nunca loggear rutas, claves ni contenido del vault.

## Checklist de construcción (orden sugerido)

1. `core/encryption` + `core/secure_storage` (base de todo lo demás).
2. `features/vault/domain` (entidades y casos de uso, sin UI).
3. `features/vault/data` (repositorio cifrado + DB local).
4. `features/vault/presentation` (auth PIN/patrón/biometría, grid, visor).
5. `features/gallery/domain` + `data` (fuentes de medios y agrupación en
   álbumes).
6. `features/gallery/presentation` (grids por álbum, integración con
   "mover a vault").
7. `features/editor` (recorte/rotación/filtros).
8. `features/share` (share intent, restringido a exportados/galería
   pública).

Sigue esta skill junto con `desarrollo-paso-a-paso`: antes de crear cada
archivo de este checklist, explica su propósito y espera autorización
explícita del usuario antes de generarlo.

## Convenciones de código

- Un archivo = una responsabilidad clara (entidad, caso de uso o widget,
  no varios mezclados).
- Nombres de casos de uso en `domain` como verbos: `MoveToVaultUseCase`,
  `ExportFromVaultUseCase`, `ScanWhatsAppMediaUseCase`.
- Toda comunicación entre features pasa por una interfaz definida en
  `core`, nunca por import directo `features/x/data` → `features/y/data`.
- Sin dependencias circulares entre features.

## Errores comunes a evitar

- Ocultar el archivo solo con un flag en base de datos sin removerlo
  realmente del MediaStore (seguiría visible en Google Fotos).
- Guardar la clave de cifrado en `SharedPreferences`/`UserDefaults` en
  texto plano.
- Permitir compartir directo desde el vault sin paso de exportación.
- Mezclar lógica de UI del vault con la de la galería pública en el
  mismo widget o provider.
