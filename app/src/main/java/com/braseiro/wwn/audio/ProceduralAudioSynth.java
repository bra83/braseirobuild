package com.braseiro.wwn.audio;

/** Host-owned procedural presentation audio. It never reads or mutates game state. */
public final class ProceduralAudioSynth {
    private ProceduralAudioSynth() {}

    public static short[] renderLoop(String asset, int sampleRate, int seconds, long seed) {
        if (sampleRate < 8000 || sampleRate > 96000) throw new IllegalArgumentException("sampleRate inválido");
        if (seconds < 1 || seconds > 12) throw new IllegalArgumentException("duração inválida");
        final int n = sampleRate * seconds;
        final short[] out = new short[n];
        long state = mix64(seed ^ hash64(asset));
        double phaseA = 0.0, phaseB = 0.0;
        double smoothNoise = 0.0;
        final String key = asset == null ? "exploration" : asset.trim().toLowerCase();
        for (int i = 0; i < n; i++) {
            state ^= state << 13; state ^= state >>> 7; state ^= state << 17;
            final double noise = (((state >>> 11) & 0x1fffffL) / 1048575.5) - 1.0;
            // One-pole smoothing prevents raw white-noise hiss on phone speakers.
            smoothNoise += (noise - smoothNoise) * 0.012;
            final double air = smoothNoise;
            final double t = (double)i / sampleRate;
            double sample;
            switch (key) {
                case "rain" -> {
                    double rainBed = air * 0.12;
                    double drops = ((state & 2047L) < 4L ? 0.20 * Math.sin(2.0 * Math.PI * (620.0 + (state & 191)) * t) : 0.0);
                    sample = rainBed + drops;
                }
                case "wind" -> {
                    phaseA += 2.0 * Math.PI * 0.17 / sampleRate;
                    sample = air * (0.11 + 0.07 * (0.5 + 0.5 * Math.sin(phaseA)));
                }
                case "fire" -> sample = air * (((state & 127L) < 14L) ? 0.20 : 0.055);
                case "settlement", "tavern" -> {
                    phaseA += 2.0 * Math.PI * 110.0 / sampleRate;
                    phaseB += 2.0 * Math.PI * 164.81 / sampleRate;
                    sample = 0.040 * Math.sin(phaseA) + 0.030 * Math.sin(phaseB) + 0.022 * air;
                }
                case "dungeon" -> {
                    phaseA += 2.0 * Math.PI * 54.0 / sampleRate;
                    sample = 0.055 * Math.sin(phaseA) + 0.012 * air;
                }
                case "combat" -> {
                    phaseA += 2.0 * Math.PI * 73.42 / sampleRate;
                    phaseB += 2.0 * Math.PI * 110.0 / sampleRate;
                    double pulse = Math.pow(Math.max(0.0, Math.sin(2.0 * Math.PI * 1.6 * t)), 8.0);
                    sample = 0.085 * Math.sin(phaseA) + 0.060 * Math.sin(phaseB) + 0.055 * pulse * air;
                }
                case "journey" -> {
                    phaseA += 2.0 * Math.PI * 82.41 / sampleRate;
                    phaseB += 2.0 * Math.PI * 123.47 / sampleRate;
                    sample = 0.060 * Math.sin(phaseA) + 0.045 * Math.sin(phaseB) + 0.010 * air;
                }
                case "exploration" -> {
                    phaseA += 2.0 * Math.PI * 65.41 / sampleRate;
                    phaseB += 2.0 * Math.PI * 98.0 / sampleRate;
                    sample = 0.045 * Math.sin(phaseA) + 0.028 * Math.sin(phaseB) + 0.008 * air;
                }
                case "weather_dynamic" -> {
                    phaseA += 2.0 * Math.PI * 0.11 / sampleRate;
                    sample = air * (0.035 + 0.020 * (0.5 + 0.5 * Math.sin(phaseA)));
                }
                default -> sample = 0.0;
            }
            // short cosine fade at both ends avoids clicks at the loop seam.
            double edge = Math.min(1.0, Math.min(i, n - 1 - i) / (sampleRate * 0.025));
            out[i] = pcm(sample * Math.max(0.0, edge));
        }
        return out;
    }

    public static short[] renderOneShot(String asset, int sampleRate, long seed) {
        final String key = asset == null ? "ui" : asset.trim().toLowerCase();
        final double seconds = key.equals("thunder") ? 1.35 : key.equals("weapon_fire") ? 0.42 : 0.28;
        final int n = Math.max(1, (int)(sampleRate * seconds));
        final short[] out = new short[n];
        long state = mix64(seed ^ hash64(key));
        for (int i = 0; i < n; i++) {
            state ^= state << 13; state ^= state >>> 7; state ^= state << 17;
            double noise = (((state >>> 11) & 0x1fffffL) / 1048575.5) - 1.0;
            double t = (double)i / sampleRate;
            double env = Math.exp(-t * (key.equals("thunder") ? 2.1 : 9.0));
            double tone = switch (key) {
                case "door_knock" -> Math.sin(2.0 * Math.PI * 115.0 * t) * 0.75;
                case "door_open" -> Math.sin(2.0 * Math.PI * 82.0 * t) * 0.32 + noise * 0.16;
                case "weapon_fire" -> noise * 0.86 + Math.sin(2.0 * Math.PI * 58.0 * t) * 0.25;
                case "thunder" -> noise * 0.58 + Math.sin(2.0 * Math.PI * 43.0 * t) * 0.32;
                default -> Math.sin(2.0 * Math.PI * 440.0 * t) * 0.18;
            };
            out[i] = pcm(tone * env);
        }
        return out;
    }

    public static long stableSeed(String value) { return mix64(hash64(value)); }

    private static short pcm(double value) {
        double v = Math.max(-0.98, Math.min(0.98, value));
        return (short)Math.round(v * 32767.0);
    }
    private static long hash64(String value) {
        long h = 0xcbf29ce484222325L;
        String s = value == null ? "" : value;
        for (int i = 0; i < s.length(); i++) { h ^= s.charAt(i); h *= 0x100000001b3L; }
        return h;
    }
    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
