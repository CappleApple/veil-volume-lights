layout(std430, binding = 7) readonly buffer VeilVolumeTransmissionMedia {
    vec4 veilVolumeMediumData[];
};

const int VEIL_VOLUME_MAX_MEDIA = 96;
const int VEIL_VOLUME_STRIDE = 8;
const float VEIL_VOLUME_SOURCE_MATCH_LIMIT = 0.75;
const float VEIL_VOLUME_SURFACE_EDGE_FEATHER = 0.20;

struct VeilVolumeTransmissionResult {
    vec3 color;
    float surfacePass;
};

bool veilVolumeSegmentAabbInterval(
        vec3 start, vec3 end, vec3 minimum, vec3 maximum,
        out float nearT, out float farT
) {
    vec3 direction = end - start;
    nearT = 0.0;
    farT = 1.0;
    for (int axis = 0; axis < 3; axis++) {
        if (abs(direction[axis]) < 0.000001) {
            if (start[axis] < minimum[axis] || start[axis] > maximum[axis]) {
                return false;
            }
        } else {
            float inverseDirection = 1.0 / direction[axis];
            float first = (minimum[axis] - start[axis]) * inverseDirection;
            float second = (maximum[axis] - start[axis]) * inverseDirection;
            if (first > second) {
                float swapValue = first;
                first = second;
                second = swapValue;
            }
            nearT = max(nearT, first);
            farT = min(farT, second);
            if (nearT > farT) {
                return false;
            }
        }
    }
    return farT >= 0.0 && nearT <= 1.0;
}

bool veilVolumeTinted(vec3 tint) {
    float darkest = min(tint.r, min(tint.g, tint.b));
    float brightest = max(tint.r, max(tint.g, tint.b));
    return brightest - darkest > 0.08 || brightest < 0.90;
}

VeilVolumeTransmissionResult veilVolumeTransmission(
        vec3 matchingSource,
        vec3 raySource,
        vec3 samplePosition
) {
    vec3 transmission = vec3(1.0);
    float surfacePass = 0.0;
    float pathLength = distance(raySource, samplePosition);
    int mediumCount = clamp(int(veilVolumeMediumData[0].x + 0.5), 0, VEIL_VOLUME_MAX_MEDIA);
    float nearestSourceDistance = VEIL_VOLUME_SOURCE_MATCH_LIMIT + 1.0;
    for (int mediumIndex = 0; mediumIndex < mediumCount; mediumIndex++) {
        int baseIndex = 1 + mediumIndex * VEIL_VOLUME_STRIDE;
        nearestSourceDistance = min(nearestSourceDistance,
                distance(veilVolumeMediumData[baseIndex].xyz, matchingSource));
    }
    if (nearestSourceDistance > VEIL_VOLUME_SOURCE_MATCH_LIMIT) {
        return VeilVolumeTransmissionResult(transmission, surfacePass);
    }
    for (int mediumIndex = 0; mediumIndex < mediumCount; mediumIndex++) {
        int baseIndex = 1 + mediumIndex * VEIL_VOLUME_STRIDE;
        vec4 sourceAndRed = veilVolumeMediumData[baseIndex];
        // Veil's native instance transform and the managed-medium upload can
        // represent adjacent snapshots of a moving light. Match the nearest
        // managed owner, then include every medium belonging to that owner.
        if (distance(sourceAndRed.xyz, matchingSource)
                > nearestSourceDistance + 0.002) {
            continue;
        }
        vec4 minimumAndGreen = veilVolumeMediumData[baseIndex + 1];
        vec4 maximumAndBlue = veilVolumeMediumData[baseIndex + 2];
        vec4 opticalProperties = veilVolumeMediumData[baseIndex + 3];
        float nearT;
        float farT;
        if (veilVolumeSegmentAabbInterval(
                raySource, samplePosition,
                minimumAndGreen.xyz, maximumAndBlue.xyz,
                nearT, farT
        )) {
            float distanceInside = max(0.0, farT - nearT) * pathLength;
            if (distanceInside > 0.0001) {
                if (veilVolumeMediumData[baseIndex + 7].w >= 0.5) {
                    surfacePass = max(surfacePass, smoothstep(
                            0.0, VEIL_VOLUME_SURFACE_EDGE_FEATHER,
                            distanceInside));
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
        }
    }
    return VeilVolumeTransmissionResult(transmission, surfacePass);
}
