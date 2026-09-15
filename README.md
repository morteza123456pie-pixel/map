# AI Maps

An Android map application built with Jetpack Compose, Material 3 and MapLibre, rendering
OpenStreetMap vector tiles.

This repository contains **Phase 1**: a working, full-screen map with pan and zoom, the
user's current location, and a recentre control. The architecture is laid out so that
search, places, routing, navigation and assistant features can be added later without
restructuring anything.

---

## Status

| | |
|---|---|
| Phase | 1 — map foundation |
| Version | 0.1.0 (versionCode 1) |
| Package | `com.aimaps.app` |
| Min SDK | 24 (Android 7.0) |
| Target / Compile SDK | 35 (Android 15) |

> **Build verification.** The environment this code was authored in had no Android SDK, no
> Gradle and no network access, so the project has **not** been compiled locally. The
> included GitHub Actions workflow (`.github/workflows/android-build.yml`) performs the
> full verification — configuration, compile, unit tests, lint and a debug APK — and is the
> intended way to confirm the build. See [First build](#first-build).

---

## What works in Phase 1

- Full-screen MapLibre map with OpenStreetMap vector tiles
- Free panning, pinch and double-tap zoom, rotation and tilt
- The user's current location drawn as an accuracy disc with a dot on top
- A recentre button, pinned to the bottom-right corner, that animates the camera to the
  current position
- A search bar with a clear button and a search IME action — presentation only this phase
- Light and dark themes, each with its own basemap style
- Edge-to-edge layout with overlays kept inside the safe area
- Graceful handling of denied permission, disabled location services, no connection,
  unavailable tiles and an unobtainable fix — none of which crash the app

## What is deliberately not in Phase 1

Place search results, reverse geocoding, routing, turn-by-turn navigation, traffic, an AI
or voice assistant, favourites, history, user settings, accounts, payments and advertising.
Extension points exist for several of these (see [Roadmap](#roadmap)) but no behaviour is
implemented.

---

## Technologies

| Concern | Choice | Why |
|---|---|---|
| Language | Kotlin 2.0.21 | |
| UI | Jetpack Compose + Material 3 (BOM 2024.10.01) | |
| Architecture | MVVM, `StateFlow`, Coroutines | |
| DI | Hilt 2.52 (via KSP) | |
| Map | MapLibre GL Android 11.5.0 | Open source, no key required, OSM-native |
| Tiles | OpenFreeMap (light) / CARTO dark-matter (dark) | Both OSM-derived and key-less |
| Location | `LocationManagerCompat` from `androidx.core` | |
| Build | AGP 8.7.3, Gradle 8.11.1, JDK 17, Kotlin DSL, version catalog | |
| Tests | JUnit 4, `kotlinx-coroutines-test`, Turbine | |

### Dependencies that were deliberately *not* added

- **Google Play Services Location** (`FusedLocationProviderClient`). `androidx.core`'s
  `LocationManagerCompat` covers everything Phase 1 needs, adds no dependency on Google
  Play, and keeps the app working on devices where Play Services is absent or unreliable.
  On API 31+ the platform's own fused provider is preferred automatically.
- **Accompanist Permissions.** The permission flow is a single launcher and a handful of
  states; the AndroidX `ActivityResultContracts` API expresses it directly.
- **`material-icons-extended`.** It adds several thousand vector assets. Three icons come
  from `material-icons-core` and the crosshair is a local vector drawable.
- **A mocking framework.** The repository interfaces are small enough that hand-written
  fakes are shorter and clearer.

---

## Project structure

```
app/src/main/java/com/aimaps/app/
├── AiMapsApplication.kt        Hilt entry point; initialises MapLibre
├── MainActivity.kt             The single Activity; enables edge-to-edge
│
├── core/
│   ├── common/AppResult.kt     AppResult / AppError — the error taxonomy
│   └── di/                     Dispatcher qualifiers and their bindings
│
├── domain/                     Pure Kotlin. No Android, no MapLibre.
│   ├── model/                  GeoPoint, UserLocation, CameraPosition, permission states
│   └── repository/             LocationRepository, SearchRepository, PlacesRepository
│
├── data/                       Platform implementations of the domain interfaces
│   ├── location/               LocationManager data source + repository
│   ├── network/                Connectivity monitoring
│   ├── search/                 Inert Phase 1 stub
│   └── di/                     DataModule — where implementations are bound
│
├── map/                        Everything that knows MapLibre exists
│   ├── MapLibreMapView.kt      The Compose wrapper and lifecycle bridge
│   ├── MapLibreExtensions.kt   Domain <-> MapLibre conversions, suspend adapters
│   ├── UserLocationLayer.kt    The location dot, drawn as GeoJSON circle layers
│   ├── CameraMath.kt           Web Mercator scale maths
│   └── style/                  Style URL selection and attribution
│
└── ui/
    ├── theme/                  Colours, type, shapes, AiMapsTheme
    ├── util/                   Small Compose/Context helpers
    └── map/                    MapScreen, MapViewModel, MapUiState, components/
```

### Layering rules

`domain` depends on nothing. `data` and `map` depend on `domain`. `ui` depends on `domain`
and `map`, and reaches into `data` only for the `NetworkMonitor` interface. Dependencies
point inwards, and every repository is an interface bound in `data/di/DataModule.kt`, so
an implementation can be swapped without touching a call site.

### Where state lives

The map screen has exactly one state holder. `MapViewModel` exposes a single
`StateFlow<MapUiState>` covering permission status, location service status, the current
location, the camera, the search query, the locating and offline flags, and any pending
message. There are no scattered `MutableState` fields, so the UI can never observe a
half-applied update.

One-shot instructions — "move the camera", "ask for permission" — travel on a separate
`Channel`-backed event flow. They cannot live in the state object: replaying a camera
animation after every configuration change would fight the user for control of the map.

---

## Getting started

### Prerequisites

- JDK 17
- Android SDK with platform 35 and build-tools 35.0.0
- Android Studio Ladybug (2024.2.1) or newer, or Gradle 8.11.1 on the command line

### First build

**The Gradle wrapper is not committed to this repository.** The authoring environment could
not produce the wrapper's binary `.jar`. Generate it once, from the project root, using a
local Gradle 8.11.1 installation:

```bash
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
```

Alternatively, run the CI workflow once and download the `gradle-wrapper` artifact it
publishes, then unpack `gradlew`, `gradlew.bat` and `gradle/wrapper/` into the project root.

Android Studio will also offer to generate the wrapper the first time the project is
opened.

Once the wrapper exists:

```bash
./gradlew assembleDebug          # Debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # Unit tests
./gradlew lintDebug              # Android Lint
./gradlew installDebug           # Install onto a connected device
```

### Running on a device

The map needs an internet connection to fetch its style and tiles on first run, and
location permission plus a device location switch to show your position. Both are handled
gracefully if unavailable — the app explains the problem rather than failing.

An emulator has no real GPS. Use the emulator's extended controls to push a mock location,
otherwise the fix will legitimately time out.

---

## Map configuration

### The default: no key required

Phase 1 ships key-less. A fresh clone renders a real map on first run, with no account, no
token and no billing:

| Theme | Style | Attribution |
|---|---|---|
| Light | OpenFreeMap Liberty — `https://tiles.openfreemap.org/styles/liberty` | © OpenStreetMap contributors · OpenFreeMap |
| Dark | CARTO dark-matter — `https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json` | © OpenStreetMap contributors · CARTO |

Both are built from OpenStreetMap data. The attribution shown in the bottom-left corner of
the map is a licence obligation — please keep it visible.

These are public community endpoints with no contractual uptime. For anything beyond
development, point the app at a provider you control or hold an account with.

### Optional: using MapTiler instead

If a MapTiler key is present at build time, the app uses MapTiler's `streets-v2` and
`streets-v2-dark` styles instead. **No key is hard-coded anywhere in the source.** The value
is read at configuration time and injected through `BuildConfig`.

Supply it in *either* of these ways.

**1. `local.properties`** (git-ignored, and the usual choice for local development).
Create or edit `local.properties` in the project root:

```properties
MAPTILER_API_KEY=your_key_here
```

**2. An environment variable** (the usual choice for CI):

```bash
export MAPTILER_API_KEY=your_key_here
./gradlew assembleDebug
```

`local.properties` takes precedence. If neither is set, the value is an empty string and
the key-less styles are used — this is a fully supported configuration, not a degraded one.

Never commit `local.properties`; it is already listed in `.gitignore`. In CI, store the key
as an encrypted repository secret and expose it as an environment variable, never as a
literal in the workflow file.

### Pointing at a different provider

All style URLs live in `map/style/MapStyleProvider.kt`. Nothing else in the codebase knows
where tiles come from, so switching providers means editing that one file (and its
attribution strings, which travel with the style precisely so they cannot fall out of sync).

---

## Permissions

| Permission | Why | When it is requested |
|---|---|---|
| `ACCESS_FINE_LOCATION` | Precise position for the location dot and recentring | Once, on first launch of the map screen |
| `ACCESS_COARSE_LOCATION` | Requested alongside fine location; the app works with coarse only | Same request |
| `INTERNET` | Fetching map styles and vector tiles | Install time |
| `ACCESS_NETWORK_STATE` | Detecting a lost connection so blank tiles can be explained | Install time |

`ACCESS_BACKGROUND_LOCATION` is **not** declared and is not used. The app only needs a
position while the user is looking at the map.

Permission is requested exactly once per session. A denial is never re-prompted
automatically — the app explains the consequence and offers a retry, and after a permanent
denial it offers a route to system settings instead, since the system will silently refuse
to show the dialog again.

GPS hardware is declared optional (`android:required="false"`), so the app remains
installable on devices without it.

---

## Error handling

Every failure has a defined state, a message and, where one exists, a single remedy the
user can act on.

| Situation | Behaviour |
|---|---|
| Permission denied | Map stays fully usable; message offers to ask again |
| Permission permanently denied | Message offers to open app settings |
| Location services off | Message offers to open location settings |
| No fix obtainable | Message offers a retry; last known position stays on screen |
| Fix timed out | Message offers a retry |
| No internet | Persistent banner; a cached map remains pannable |
| Map style unavailable | Message offers a retry; cleared automatically on recovery |

Repositories never throw at their boundary. Platform exceptions are translated into the
`AppError` taxonomy in `core/common/AppResult.kt` and returned as values, so no caller has
to handle a `SecurityException` or a null provider.

---

## Testing

```bash
./gradlew testDebugUnitTest
```

JVM unit tests cover the coordinate model (including antimeridian wrapping, which MapLibre
will trigger the first time a user pans across the Pacific), the Web Mercator scale maths
behind the accuracy disc, and `MapViewModel` end to end against fake repositories —
permission transitions, recentring, the location stream, connectivity and error mapping.

There are no instrumented tests in Phase 1. Rendering a real MapLibre surface on CI needs an
emulator, which is disproportionate for this amount of UI.

---

## Roadmap

The structure below already exists; the behaviour does not.

| Phase | Feature | Prepared extension point |
|---|---|---|
| 2 | Place search | `domain/repository/SearchRepository.kt`, currently bound to an inert stub. `MapViewModel.onSearchQueryChanged` is where debouncing goes; the search bar's UI contract does not change. |
| 3 | Place details, reverse geocoding | `domain/repository/PlacesRepository.kt` — declared and intentionally unbound, because a stub binding would only disguise that it does nothing. |
| 4 | Routing | `MapCameraCommand` is a sealed interface with one case today; a `FitBounds` case slots in beside it without touching existing call sites. Route geometry follows the same GeoJSON-source pattern as `UserLocationLayer`. |
| 5 | Turn-by-turn navigation | `CameraPosition` already carries bearing and tilt so a tilted, rotating camera needs no model change. `UserLocation.bearingDegrees` is populated whenever the device is genuinely moving. |
| 6 | Favourites, history | Needs a persistence module under `data/`; `allowBackup` is currently off precisely because nothing is stored yet. |
| 7 | Assistant features | Would be a new `ui/` destination consuming the existing repositories. |

---

## Licence and attribution

Map data © OpenStreetMap contributors, available under the
[Open Database License](https://www.openstreetmap.org/copyright). The attribution shown on
the map must remain visible in any derivative of this app.
