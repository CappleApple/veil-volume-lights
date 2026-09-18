# Veil Volume Lights

Veil Volume Lights is a small client rendering library for adding point, spot, and rectangular area lights through Veil.

The main extra feature is support for volumetric light passing through transparent media, so integrations can do things like flashlight beams through fog, water, stained glass, or other translucent volumes without each mod building its own lighting hooks.

## API

The public API is under:

```text
com.cappleapple.veilvolumelights.api.client
```

Create a persistent `VolumeLightHandle`, update it when the source moves or changes, and free it when the light no longer exists.

```java
VolumeLightHandle point = VeilVolumeLights.create(new VolumeLight.Point(
        position, color, 1.0F, 16.0F, true, 0.6F
));

VolumeLightHandle spot = VeilVolumeLights.create(new VolumeLight.Spot(
        position, forward, up, color, 1.0F, 40.0F,
        24.0F, 10.0F, true, 0.8F
));

VolumeLightHandle area = VeilVolumeLights.create(new VolumeLight.Area(
        position, forward, up, color, 1.0F, 24.0F,
        4.0F, 2.0F, 70.0F, true, 0.7F
));
```

Point lights take a position, color, intensity, range, shadow flag, and volumetric strength.

Spot lights additionally use forward/up vectors and inner/outer cone angles.

Area lights use forward/up vectors, emitter width and height, and a spread angle.

Angles are specified as full angles in degrees, and area-light dimensions are measured in blocks.

Create and update handles on the client render thread.

## Testing lights in-game

Test items are available from the Functional Blocks creative tab, or with:

```text
/give @s veilvolumelights:test_point_light
/give @s veilvolumelights:test_spot_light
/give @s veilvolumelights:test_area_light
```

These are mainly intended for checking rendering changes without needing another integration mod loaded.

## Example integration

[Veil Lights for TaCZ](https://github.com/CappleApple/veil-lights-for-tacz) uses this library to attach spotlights to animated weapon attachments.

That project is also a useful reference for managing a persistent handle whose position and direction change every frame.

## Requirements

- Minecraft 1.21.1
- NeoForge
- Veil
- Java 21

This is a client-side library.

## Building

```bash
./gradlew test build
```

Windows:

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient
```

The built jar is written to `build/libs/`.

## License

Veil Volume Lights is licensed under [CC BY-NC-SA 4.0 with a Modpack/Server Exception](LICENSE). Modpacks and Minecraft servers, including monetized ones, may use it under the additional permission in the LICENSE.
