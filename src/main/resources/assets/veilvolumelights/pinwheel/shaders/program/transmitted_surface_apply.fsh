uniform sampler2D DiffuseSampler0;
uniform sampler2D TransmittedSurfaceSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(DiffuseSampler0, texCoord);
    vec3 transmitted = texture(TransmittedSurfaceSampler, texCoord).rgb;
    fragColor = vec4(scene.rgb + transmitted, scene.a);
}
