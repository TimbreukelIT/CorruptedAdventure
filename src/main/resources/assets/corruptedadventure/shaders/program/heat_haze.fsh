#version 150

// Heat haze of the ring 2 heat wave: the picture wavers softly, strongest low on the screen (the air above the
// ground). Strength 0-1 = weather strength x sky exposure x "Distortion Effects"; HazeTime in seconds.

uniform sampler2D DiffuseSampler;

uniform float Strength;
uniform float HazeTime;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // texCoord.y is 0 at the bottom of the screen: full shimmer there, none above three quarters.
    float weight = 1.0 - smoothstep(0.0, 0.75, texCoord.y);
    float amplitude = Strength * weight * 0.0035;
    float wave = sin(texCoord.y * 95.0 + HazeTime * 4.0)
            + 0.5 * sin(texCoord.y * 41.0 - HazeTime * 2.7 + texCoord.x * 13.0);
    float rise = sin(texCoord.x * 70.0 + HazeTime * 3.1);
    vec2 uv = clamp(texCoord + vec2(wave, 0.5 * rise) * amplitude, 0.0, 1.0);
    fragColor = vec4(texture(DiffuseSampler, uv).rgb, 1.0);
}
