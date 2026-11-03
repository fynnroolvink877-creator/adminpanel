# Admin Panel (NeoForge 1.21.1)

OP-only troll panel voor Minecraft NeoForge 1.21.1.

## Gebruik
Typ `/ap` (alleen OP's). Kies een speler en daarna een troll:
Mini, Groot, Fly, Ragdoll, Blackout, Launch, Freeze, Spin, Scare, Drunk.
Met Reset haal je alle effecten weg, met Terug kies je een andere speler.

Het menu werkt server-side: alleen de server (of singleplayer met cheats) heeft de mod nodig.

## De jar bouwen

### Optie 1: GitHub (niets installeren)
1. Maak een repository op github.com en upload alle bestanden uit deze map, inclusief de verborgen map `.github`.
2. Open het tabblad **Actions** en wacht tot de build klaar is.
3. Download onder **Artifacts** de `adminpanel-1.0.0.jar`.

### Optie 2: lokaal
1. Installeer JDK 21 en Gradle.
2. Run in deze map: `gradle build`
3. De jar staat in `build/libs/adminpanel-1.0.0.jar`.

Faalt de build op de versie van `net.neoforged.moddev` in `build.gradle`? Neem dan de versie uit de officiële NeoForge MDK voor 1.21.1.

## Installeren
Zet de jar in de `mods`-map van je NeoForge 1.21.1 server.

## Modrinth
Project type: Mod. Loader: NeoForge. Game version: 1.21.1.
Omgeving: server required, client optional.

## Structuur
- `src/main/java/nl/adminpanel/` - broncode
- `src/main/resources/META-INF/neoforge.mods.toml` - mod-info
- `build.gradle` / `settings.gradle` - build
- `.github/workflows/build.yml` - automatische build op GitHub
