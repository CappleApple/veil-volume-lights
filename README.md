# Veil Volume Lights

A reusable NeoForge 1.21.1 client rendering library for Veil point, spot, and rectangular area lights with continuous transparent-medium volumes.

The public API is under `com.cappleapple.veilvolumelights.api.client`. Create a persistent `VolumeLightHandle` with `VeilVolumeLights.create(...)`, update it when the source changes, and free it when the source disappears.

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

Definitions use full cone/spread angles in degrees and full area width/height in blocks. Calls are client-side and should be made on the render thread.

For visual testing, use the Functional Blocks creative tab or:

```text
/give @s veilvolumelights:test_point_light
/give @s veilvolumelights:test_spot_light
/give @s veilvolumelights:test_area_light
```

## Development

Requires Java 21.

```powershell
./gradlew.bat test build
./gradlew.bat runClient
```

Veil Volume Lights is available under the MIT License.

The reference integration is [Veil Lights for TaCZ](https://github.com/CappleApple/veil-lights-for-tacz).
