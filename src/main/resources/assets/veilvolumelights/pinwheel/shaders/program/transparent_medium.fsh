#extension GL_ARB_shader_storage_buffer_object : require

#include veil:common
#include veil:space_helper
#include veil:light
#include veilvolumelights:transmission

uniform sampler2D OpaqueDepthSampler;

in vec2 texCoord;
out vec4 fragColor;

const int VEIL_VOLUME_INTEGRATION_STEPS = 8;

vec3 veilVolumeAreaOrigin(
        vec3 source, vec3 samplePosition, vec3 forward, vec3 up,
        float halfWidth, float halfHeight
) {
    vec3 right = normalize(cross(forward, up));
    vec3 relative = samplePosition - source;
    return source
            + right * clamp(dot(relative, right), -halfWidth, halfWidth)
            + up * clamp(dot(relative, up), -halfHeight, halfHeight);
}

void main() {
    int volumeCount = clamp(int(veilVolumeMediumData[0].x + 0.5), 0, VEIL_VOLUME_MAX_MEDIA);
    if (volumeCount == 0) {
        fragColor = vec4(0.0);
        return;
    }

    float depth = texture(OpaqueDepthSampler, texCoord).r;
    vec3 viewStart = VeilCamera.CameraPosition + VeilCamera.CameraBobOffset;
    vec3 viewEnd = screenToWorldSpace(texCoord, depth).xyz;
    float viewLength = distance(viewStart, viewEnd);
    if (viewLength <= 0.0001) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 contribution = vec3(0.0);
    for (int volumeIndex = 0; volumeIndex < volumeCount; volumeIndex++) {
        int baseIndex = 1 + volumeIndex * VEIL_VOLUME_STRIDE;
        vec4 sourceAndRed = veilVolumeMediumData[baseIndex];
        vec4 minimumAndGreen = veilVolumeMediumData[baseIndex + 1];
        vec4 maximumAndBlue = veilVolumeMediumData[baseIndex + 2];
        vec4 opticalAndShape = veilVolumeMediumData[baseIndex + 3];
        vec4 forwardAndRange = veilVolumeMediumData[baseIndex + 4];
        vec4 upAndOuterAngle = veilVolumeMediumData[baseIndex + 5];
        vec4 lightAndInnerAngle = veilVolumeMediumData[baseIndex + 6];
        vec4 areaSize = veilVolumeMediumData[baseIndex + 7];

        float nearT;
        float farT;
        if (!veilVolumeSegmentAabbInterval(
                viewStart, viewEnd, minimumAndGreen.xyz, maximumAndBlue.xyz,
                nearT, farT
        )) {
            continue;
        }
        float entryT = clamp(nearT, 0.0, 1.0);
        float exitT = clamp(farT, 0.0, 1.0);
        float distanceInside = max(0.0, exitT - entryT) * viewLength;
        if (distanceInside <= 0.0001) {
            continue;
        }

        float sampleDistance = distanceInside / float(VEIL_VOLUME_INTEGRATION_STEPS);
        vec3 source = sourceAndRed.xyz;
        vec3 forward = normalize(forwardAndRange.xyz);
        vec3 up = normalize(upAndOuterAngle.xyz);
        int shape = int(opticalAndShape.z + 0.5);
        float outerCos = cos(upAndOuterAngle.w);
        float innerCos = cos(lightAndInnerAngle.w);

        for (int stepIndex = 0; stepIndex < VEIL_VOLUME_INTEGRATION_STEPS; stepIndex++) {
            float fraction = (float(stepIndex) + 0.5) / float(VEIL_VOLUME_INTEGRATION_STEPS);
            vec3 samplePosition = mix(viewStart, viewEnd, mix(entryT, exitT, fraction));
            vec3 raySource = source;
            if (shape == 2) {
                raySource = veilVolumeAreaOrigin(
                        source, samplePosition, forward, up, areaSize.x, areaSize.y);
            }
            vec3 fromLight = samplePosition - raySource;
            float lightDistance = length(fromLight);
            if (lightDistance <= 0.0001 || lightDistance >= forwardAndRange.w) {
                continue;
            }

            float angleFalloff = 1.0;
            if (shape != 0) {
                float coneCos = dot(fromLight / lightDistance, forward);
                angleFalloff = smoothstep(outerCos, innerCos, coneCos);
            }
            if (angleFalloff <= 0.0) {
                continue;
            }

            float attenuation = attenuate_no_cusp(lightDistance, forwardAndRange.w);
            vec3 transmission = veilVolumeTransmission(source, raySource, samplePosition).color;
            contribution += lightAndInnerAngle.rgb
                    * transmission
                    * opticalAndShape.w
                    * opticalAndShape.y
                    * attenuation
                    * angleFalloff
                    * sampleDistance
                    * 48.0;
        }
    }
    fragColor = vec4(tanh(0.004 * contribution), 1.0);
}
