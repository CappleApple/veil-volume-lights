#extension GL_ARB_shader_storage_buffer_object : require

#include veil:common
#include veil:space_helper
#include veil:color_utilities
#include veil:light
#include veil:voxel_shadow
#include veilvolumelights:transmission
#ifdef INSCATTERING
#include veil:inscattering
#endif

in mat4 lightMat;
in mat4 invLightMat;
in vec3 lightColor;
#ifdef SPOTLIGHT
in float size;
#else
in vec2 size;
#endif
in float maxAngle;
in float maxDistance;
in float occluded;
in float inscattering;

#ifndef INSCATTERING
uniform sampler2D AlbedoSampler;
uniform sampler2D NormalSampler;
#endif
uniform sampler2D DepthSampler;
uniform vec2 ScreenSize;
out vec4 fragColor;

float sacos(float x) {
    float y = abs(clamp(x, -1.0, 1.0));
    float z = (-0.168577 * y + 1.56723) * sqrt(1.0 - y);
    return mix(0.5 * 3.1415927, z, sign(x));
}

struct AreaLightResult { vec3 position; float angle; };
AreaLightResult closestPointOnPlaneAndAngle(
        vec3 point, mat4 planeMatrix, mat4 invPlaneMatrix, vec2 planeSize
) {
    planeMatrix[3].xyz *= -1.0;
    invPlaneMatrix[3].xyz *= -1.0;
    vec3 localSpacePoint = (planeMatrix * vec4(point, 1.0)).xyz;
    vec3 localSpacePointOnPlane = vec3(clamp(localSpacePoint.xy, -planeSize, planeSize), 0.0);
    vec3 direction = normalize(localSpacePoint - localSpacePointOnPlane);
    float angle = sacos(dot(direction, vec3(0.0, 0.0, 1.0)));
    return AreaLightResult(
            (invPlaneMatrix * vec4(localSpacePointOnPlane, 1.0)).xyz,
            angle
    );
}

vec3 areaLightCenter(mat4 inversePlaneMatrix) {
    inversePlaneMatrix[3].xyz *= -1.0;
    return (inversePlaneMatrix * vec4(0.0, 0.0, 0.0, 1.0)).xyz;
}

struct SpotLightResult { vec3 position; float angle; };
SpotLightResult spotLightPositionAndAngle(vec3 point, mat4 lightMatrix) {
    lightMatrix[3].xyz *= -1.0;
    vec3 localSpacePoint = (lightMatrix * vec4(point, 1.0)).xyz;
    vec3 localDir = normalize(localSpacePoint);
    float angle = sacos(dot(localDir, vec3(0.0, 0.0, 1.0)));
    vec3 worldPos = (inverse(lightMatrix) * vec4(0.0, 0.0, 0.0, 1.0)).xyz;
    return SpotLightResult(worldPos, angle);
}

#ifdef INSCATTERING
vec3 ray(vec3 dir, vec3 pos, vec3 fragPos) {
    #define BRIGHTNESS 0.004
    vec3 col = vec3(0.0);
    float d = 0.0;
    float fragDistance = distance(pos, fragPos);
    for (float i = 0.0; i < STEPS; i++) {
        #ifdef SPOTLIGHT
        SpotLightResult lightInfo = spotLightPositionAndAngle(pos, lightMat);
        vec3 lightPos = lightInfo.position;
        vec3 matchingSource = lightPos;
        float angleFalloff = smoothstep(size, size - maxAngle, lightInfo.angle);
        #else
        AreaLightResult lightInfo = closestPointOnPlaneAndAngle(pos, lightMat, invLightMat, size);
        vec3 lightPos = lightInfo.position;
        vec3 matchingSource = areaLightCenter(invLightMat);
        float normalizedAngle = clamp(lightInfo.angle, 0.0, maxAngle) / maxAngle;
        float angleFalloff = smoothstep(1.0, 0.0, normalizedAngle);
        #endif

        float vol = sdSphere(pos - lightPos, 1.0);
        vec3 offset = lightPos - pos;
        float atten = attenuate_no_cusp(length(offset), maxDistance);
        vec3 transmission = veilVolumeTransmission(matchingSource, lightPos, pos).color;
        pos += dir * vol;
        d += vol;
        if (fragDistance - d < 1.0) {
            atten *= smoothstep(0.0, 1.0, fragDistance - d);
        }
        col += (lightColor * transmission * inscattering * atten * angleFalloff) / vol;
    }
    return tanh(BRIGHTNESS * col);
}

void main() {
    vec2 screenUv = gl_FragCoord.xy / (ScreenSize / 4.0);
    vec3 volume = vec3(0.0);
    if (inscattering > 0.0) {
        float depth = texture(DepthSampler, screenUv).r;
        vec3 viewStart = VeilCamera.CameraPosition + VeilCamera.CameraBobOffset;
        vec3 viewEnd = screenToWorldSpace(screenUv, depth).xyz;
        volume = ray(viewDirFromUv(screenUv), viewStart, viewEnd);
    }
    fragColor = vec4(volume, 1.0);
}
#else
void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;
    vec4 albedoColor = texture(AlbedoSampler, screenUv);
    if (albedoColor.a == 0.0) {
        discard;
    }
    vec3 normalVS = texture(NormalSampler, screenUv).xyz;
    float depth = texture(DepthSampler, screenUv).r;
    vec3 pos = screenToWorldSpace(screenUv, depth).xyz;

    #ifdef SPOTLIGHT
    SpotLightResult lightInfo = spotLightPositionAndAngle(pos, lightMat);
    vec3 lightPos = lightInfo.position;
    vec3 matchingSource = lightPos;
    float angleFalloff = smoothstep(size, size - maxAngle, lightInfo.angle);
    #else
    AreaLightResult lightInfo = closestPointOnPlaneAndAngle(pos, lightMat, invLightMat, size);
    vec3 lightPos = lightInfo.position;
    vec3 matchingSource = areaLightCenter(invLightMat);
    float normalizedAngle = clamp(lightInfo.angle, 0.0, maxAngle) / maxAngle;
    float angleFalloff = smoothstep(1.0, 0.0, normalizedAngle);
    #endif

    vec3 offset = lightPos - pos;
    vec3 lightDirection = normalize((VeilCamera.ViewMat * vec4(offset, 0.0)).xyz);
    float diffuse = (dot(normalVS, lightDirection) + 1.0) * 0.5;
    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= attenuate_no_cusp(length(offset), maxDistance) * angleFalloff;
    if (occluded > 0.5) {
        vec3 normalWS = normalize((VeilCamera.IViewMat * vec4(normalVS, 0.0)).xyz);
        diffuse *= voxelshadowVisibility(pos + normalWS * 0.01, lightPos);
    }
    VeilVolumeTransmissionResult transmission =
            veilVolumeTransmission(matchingSource, lightPos, pos);
    float reflectivity = 0.05;
    vec3 diffuseColor = diffuse * lightColor * transmission.color;
    vec3 surfaceColor = albedoColor.rgb * diffuseColor * (1.0 - reflectivity)
            + diffuseColor * reflectivity;
    fragColor = vec4(surfaceColor * (1.0 - transmission.surfacePass), 1.0);
}
#endif
