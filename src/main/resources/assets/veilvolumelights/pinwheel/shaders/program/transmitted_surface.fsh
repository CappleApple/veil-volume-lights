#extension GL_ARB_shader_storage_buffer_object : require

#include veil:common
#include veil:space_helper
#include veil:light
#include veilvolumelights:transmission

uniform sampler2D AlbedoSampler;
uniform sampler2D NormalSampler;
uniform sampler2D OpaqueDepthSampler;

in vec2 texCoord;
out vec4 fragColor;

vec3 veilVolumeSurfaceAreaOrigin(
        vec3 source, vec3 samplePosition, vec3 forward, vec3 up,
        float halfWidth, float halfHeight
) {
    vec3 right = normalize(cross(forward, up));
    vec3 relative = samplePosition - source;
    return source
            + right * clamp(dot(relative, right), -halfWidth, halfWidth)
            + up * clamp(dot(relative, up), -halfHeight, halfHeight);
}

bool veilVolumeOwnerAlreadyProcessed(int volumeIndex, float ownerId) {
    for (int previousIndex = 0; previousIndex < volumeIndex; previousIndex++) {
        int previousBase = 1 + previousIndex * VEIL_VOLUME_STRIDE;
        if (abs(veilVolumeMediumData[previousBase + 7].z - ownerId) < 0.25) {
            return true;
        }
    }
    return false;
}

float veilVolumeOwnerSurfacePassWeight(
        float ownerId, vec3 raySource, vec3 samplePosition
) {
    float pathLength = distance(raySource, samplePosition);
    float surfacePass = 0.0;
    int mediumCount = clamp(int(veilVolumeMediumData[0].x + 0.5), 0, VEIL_VOLUME_MAX_MEDIA);
    for (int mediumIndex = 0; mediumIndex < mediumCount; mediumIndex++) {
        int baseIndex = 1 + mediumIndex * VEIL_VOLUME_STRIDE;
        if (abs(veilVolumeMediumData[baseIndex + 7].z - ownerId) >= 0.25) {
            continue;
        }
        if (veilVolumeMediumData[baseIndex + 7].w < 0.5) {
            continue;
        }
        vec4 sourceAndRed = veilVolumeMediumData[baseIndex];
        vec4 minimumAndGreen = veilVolumeMediumData[baseIndex + 1];
        vec4 maximumAndBlue = veilVolumeMediumData[baseIndex + 2];
        float nearT;
        float farT;
        if (veilVolumeSegmentAabbInterval(
                raySource, samplePosition,
                minimumAndGreen.xyz, maximumAndBlue.xyz,
                nearT, farT
        )) {
            float distanceInside = max(0.0, farT - nearT) * pathLength;
            surfacePass = max(surfacePass, smoothstep(
                    0.0, VEIL_VOLUME_SURFACE_EDGE_FEATHER,
                    distanceInside));
        }
    }
    return surfacePass;
}

vec3 veilVolumeOwnerTransmission(
        float ownerId, vec3 raySource, vec3 samplePosition
) {
    vec3 transmission = vec3(1.0);
    float pathLength = distance(raySource, samplePosition);
    int mediumCount = clamp(int(veilVolumeMediumData[0].x + 0.5), 0, VEIL_VOLUME_MAX_MEDIA);
    for (int mediumIndex = 0; mediumIndex < mediumCount; mediumIndex++) {
        int baseIndex = 1 + mediumIndex * VEIL_VOLUME_STRIDE;
        if (abs(veilVolumeMediumData[baseIndex + 7].z - ownerId) >= 0.25) {
            continue;
        }
        vec4 sourceAndRed = veilVolumeMediumData[baseIndex];
        vec4 minimumAndGreen = veilVolumeMediumData[baseIndex + 1];
        vec4 maximumAndBlue = veilVolumeMediumData[baseIndex + 2];
        vec4 opticalProperties = veilVolumeMediumData[baseIndex + 3];
        float nearT;
        float farT;
        if (!veilVolumeSegmentAabbInterval(
                raySource, samplePosition,
                minimumAndGreen.xyz, maximumAndBlue.xyz,
                nearT, farT
        )) {
            continue;
        }
        float distanceInside = max(0.0, farT - nearT) * pathLength;
        if (distanceInside <= 0.0001) {
            continue;
        }
        vec3 tint = vec3(sourceAndRed.w, minimumAndGreen.w, maximumAndBlue.w);
        vec3 filteredTint = veilVolumeTinted(tint)
                ? tint * opticalProperties.x
                : vec3(opticalProperties.x);
        transmission *= pow(
                clamp(filteredTint, vec3(0.001), vec3(1.0)),
                vec3(distanceInside)
        );
    }
    return transmission;
}

void main() {
    int volumeCount = clamp(int(veilVolumeMediumData[0].x + 0.5), 0, VEIL_VOLUME_MAX_MEDIA);
    if (volumeCount == 0) {
        fragColor = vec4(0.0);
        return;
    }

    float depth = texture(OpaqueDepthSampler, texCoord).r;
    vec4 albedoColor = texture(AlbedoSampler, texCoord);
    if (depth >= 0.999999 || albedoColor.a == 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 surfacePosition = screenToWorldSpace(texCoord, depth).xyz;
    vec3 normalVS = texture(NormalSampler, texCoord).xyz;
    vec3 contribution = vec3(0.0);

    for (int volumeIndex = 0; volumeIndex < volumeCount; volumeIndex++) {
        int baseIndex = 1 + volumeIndex * VEIL_VOLUME_STRIDE;
        vec4 sourceAndRed = veilVolumeMediumData[baseIndex];
        vec4 opticalAndShape = veilVolumeMediumData[baseIndex + 3];
        vec4 forwardAndRange = veilVolumeMediumData[baseIndex + 4];
        vec4 upAndOuterAngle = veilVolumeMediumData[baseIndex + 5];
        vec4 lightAndInnerAngle = veilVolumeMediumData[baseIndex + 6];
        vec4 areaSizeAndOwner = veilVolumeMediumData[baseIndex + 7];
        float ownerId = areaSizeAndOwner.z;
        if (veilVolumeOwnerAlreadyProcessed(volumeIndex, ownerId)) {
            continue;
        }

        vec3 source = sourceAndRed.xyz;
        vec3 forward = normalize(forwardAndRange.xyz);
        vec3 up = normalize(upAndOuterAngle.xyz);
        int shape = int(opticalAndShape.z + 0.5);
        vec3 raySource = source;
        if (shape == 2) {
            raySource = veilVolumeSurfaceAreaOrigin(
                    source, surfacePosition, forward, up,
                    areaSizeAndOwner.x, areaSizeAndOwner.y);
        }

        vec3 fromLight = surfacePosition - raySource;
        float lightDistance = length(fromLight);
        if (lightDistance <= 0.0001 || lightDistance >= forwardAndRange.w) {
            continue;
        }
        float surfacePass = veilVolumeOwnerSurfacePassWeight(
                ownerId, raySource, surfacePosition);
        if (surfacePass <= 0.0001) {
            continue;
        }

        float angleFalloff = 1.0;
        if (shape != 0) {
            float coneCos = dot(fromLight / lightDistance, forward);
            angleFalloff = smoothstep(
                    cos(upAndOuterAngle.w), cos(lightAndInnerAngle.w), coneCos);
        }
        if (angleFalloff <= 0.0) {
            continue;
        }

        vec3 offset = raySource - surfacePosition;
        vec3 lightDirection = normalize((VeilCamera.ViewMat * vec4(offset, 0.0)).xyz);
        float diffuse = shape == 0
                ? clamp(dot(normalVS, lightDirection), 0.0, 1.0)
                : (dot(normalVS, lightDirection) + 1.0) * 0.5;
        diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT)
                / (1.0 + MINECRAFT_AMBIENT_LIGHT);
        diffuse *= attenuate_no_cusp(lightDistance, forwardAndRange.w) * angleFalloff;

        vec3 transmission = veilVolumeOwnerTransmission(
                ownerId, raySource, surfacePosition);
        vec3 diffuseColor = diffuse * lightAndInnerAngle.rgb * transmission;
        float reflectivity = 0.05;
        contribution += (albedoColor.rgb * diffuseColor * (1.0 - reflectivity)
                + diffuseColor * reflectivity) * surfacePass;
    }

    fragColor = vec4(contribution, 1.0);
}
