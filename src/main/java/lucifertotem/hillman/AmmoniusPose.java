package lucifertotem.hillman;

/**
 * Doctor Ammonius' erratic body language: pure math, no client classes (the model and renderer call it on the client;
 * the server only syncs the inputs: the idle mode and a talk trigger). Deterministic per burst/mode seed, so every
 * client sees the same chaos, but no two bursts look alike.
 */
public final class AmmoniusPose {
    private AmmoniusPose() {}

    // idle modes (server state machine, synced in the low 4 bits of EntityAmmonius.ANIM)
    public static final int PACE = 0, FREEZE = 1, LURCH = 2, LOOK = 3, CROUCH = 4, LECTURE = 5, MODES = 6;
    // talk kinds (low 2 bits of EntityAmmonius.TALK)
    public static final int TALK = 0, PURPLE = 1, GREET = 2;
    public static final int BURST = 50;

    // output slots
    public static final int HEAD_YAW = 0, HEAD_PITCH = 1, HEAD_ROLL = 2, R_X = 3, R_Y = 4, R_Z = 5, L_X = 6, L_Y = 7, L_Z = 8, ARM_W = 9,
        BODY_PITCH = 10, MITRE_Z = 11, MITRE_X = 12, SPIN = 13, HOP = 14, SHUDDER = 15, SNEAK = 16, FREEZE_F = 17, HEAD_ABS = 18, N = 19;

    public static float hash(int a, int b) {
        int x = a * 0x9E3779B1 ^ (b + 0x7F4A7C15) * 0x85EBCA77;
        x ^= x >>> 15; x *= 0x2C1B3C6D; x ^= x >>> 12; x *= 0x297A2D39; x ^= x >>> 15;
        return (x >>> 8) / 16777216f;
    }
    static float sin(float f) { return (float) Math.sin(f); }
    static float clamp01(float f) { return f < 0 ? 0 : f > 1 ? 1 : f; }
    static float smooth(float u) { u = clamp01(u); return u * u * (3 - 2 * u); }

    /**
     * @param age       ticksExisted + partial ticks
     * @param talkAge   ticks since the last voice line started (>= BURST: no burst)
     * @param talkKind  TALK / PURPLE / GREET
     * @param talkSeed  counter of voice lines (a fresh pattern each line)
     * @param mode      idle mode
     * @param modeAge   ticks since the mode began
     * @param modeSeed  counter of mode changes
     */
    public static float[] compute(float age, float talkAge, int talkKind, int talkSeed, int mode, float modeAge, int modeSeed) {
        float[] p = new float[N];
        float t = age;
        // ---------------- idle
        switch (mode) {
            case FREEZE:
                p[FREEZE_F] = 1; p[ARM_W] = 1; p[HEAD_ABS] = 1;
                p[HEAD_YAW] = (hash(modeSeed, 1) - 0.5f) * 1.2f; p[HEAD_PITCH] = (hash(modeSeed, 2) - 0.3f) * 0.6f;
                p[R_X] = (hash(modeSeed, 3) - 0.5f) * 1.6f; p[L_X] = (hash(modeSeed, 4) - 0.5f) * 1.6f; // frozen mid-gesture
                break;
            case LURCH: {
                float k = 1 - clamp01(modeAge / 12f);
                p[BODY_PITCH] = 0.55f * k; p[ARM_W] = k; p[R_X] = 0.9f; p[L_X] = 0.9f; p[R_Z] = 0.3f; p[L_Z] = -0.3f;
                p[HEAD_PITCH] = -0.4f * k;
                break;
            }
            case LOOK:
                p[HEAD_ABS] = 1;
                p[HEAD_YAW] = (hash(modeSeed, (int) (modeAge / 5)) - 0.5f) * 2.6f;   // rapid snaps left/right
                p[HEAD_PITCH] = (hash(modeSeed + 7, (int) (modeAge / 5)) - 0.5f) * 0.9f;
                break;
            case CROUCH:
                p[SNEAK] = 1; p[ARM_W] = 0.6f; p[R_X] = -0.4f; p[L_X] = -0.4f; p[R_Z] = 0.5f; p[L_Z] = -0.5f;
                break;
            case LECTURE:
                p[ARM_W] = 1;
                p[R_X] = -1.75f + sin(t * 0.25f) * 0.15f; p[R_Y] = -0.2f; p[R_Z] = sin(t * 1.3f) * 0.3f;           // finger raised, wagging
                p[L_X] = -0.35f; p[L_Z] = -0.65f;                                                                   // hand on hip
                p[HEAD_PITCH] = sin(t * 0.5f) * 0.22f; p[HEAD_ROLL] = sin(t * 0.21f) * 0.12f;                      // nodding at nobody
                break;
            default: // PACE: hands clasped behind his back, muttering
                p[ARM_W] = 0.85f; p[R_X] = 0.55f; p[L_X] = 0.55f; p[R_Z] = -0.25f; p[L_Z] = 0.25f; p[R_Y] = 0.3f; p[L_Y] = -0.3f;
                p[HEAD_PITCH] = 0.12f + sin(t * 0.13f) * 0.1f;
        }
        p[MITRE_Z] = sin(t * 0.17f) * 0.05f;
        // ---------------- talking burst (overrides the idle pose while it lasts)
        if (talkAge >= 0 && talkAge < BURST) {
            float k = smooth(talkAge / 4f) * (1 - smooth((talkAge - BURST + 12) / 12f));   // quick in, eased out
            int v = talkKind == PURPLE ? 3 : (int) (hash(talkSeed, 11) * 4);               // gesture style
            float ax, az, bx, bz;
            switch (v) {
                case 0: // flailing
                    ax = -1.2f + sin(t * 1.7f) * 1.4f; az = 0.6f + sin(t * 2.3f) * 0.7f;
                    bx = -1.0f + sin(t * 1.3f + 1) * 1.4f; bz = -0.6f - sin(t * 1.9f + 2) * 0.7f;
                    break;
                case 1: // pointing to the heavens, shaking
                    ax = -2.95f + sin(t * 3.1f) * 0.12f; az = 0.15f;
                    bx = -0.8f + sin(t * 1.4f) * 0.9f; bz = -0.4f;
                    break;
                case 2: // jabbing finger at you
                    ax = -1.55f + (((int) (t / 2.5f)) % 2 == 0 ? -0.35f : 0.3f); az = -0.1f;
                    bx = 0.3f + sin(t * 0.8f) * 0.3f; bz = -0.3f;
                    break;
                default: // both arms thrown up: PURPLE!
                    ax = -2.8f + sin(t * 0.9f) * 0.25f; az = 0.45f + sin(t * 1.1f) * 0.2f;
                    bx = -2.8f + sin(t * 0.9f + 1.5f) * 0.25f; bz = -0.45f - sin(t * 1.1f + 1) * 0.2f;
            }
            float w = k;
            p[R_X] = p[R_X] * (1 - w) + ax * w; p[R_Z] = p[R_Z] * (1 - w) + az * w; p[R_Y] *= 1 - w;
            p[L_X] = p[L_X] * (1 - w) + bx * w; p[L_Z] = p[L_Z] * (1 - w) + bz * w; p[L_Y] *= 1 - w;
            p[ARM_W] = Math.max(p[ARM_W], w);
            p[SNEAK] = 0; p[FREEZE_F] = 0; p[BODY_PITCH] *= 1 - w;
            // head jerks: a new random tilt every ~4 ticks
            int step = (int) (talkAge / (3 + hash(talkSeed, 12) * 3));
            p[HEAD_ABS] = 0;
            p[HEAD_YAW] = (hash(talkSeed, 100 + step) - 0.5f) * 1.1f * k;
            p[HEAD_PITCH] = (hash(talkSeed, 200 + step) - 0.6f) * 0.8f * k;
            p[HEAD_ROLL] = (hash(talkSeed, 300 + step) - 0.5f) * 0.7f * k;
            // bobbing
            p[HOP] = Math.abs(sin(t * 0.7f)) * 0.07f * k;
            // occasional full-body shudder
            if (hash(talkSeed, 400 + (int) (talkAge / 7)) < 0.18f) p[SHUDDER] = sin(t * 9f) * 6f * k;
            // sudden spin on the spot (a quick 360)
            if (hash(talkSeed, 13) < 0.35f) {
                float s0 = 4 + hash(talkSeed, 14) * 22;
                p[SPIN] = 360f * smooth((talkAge - s0) / 8f);
                if (p[SPIN] >= 360f) p[SPIN] = 0;
            }
            // a little hop
            if (hash(talkSeed, 15) < 0.4f || talkKind == PURPLE) {
                float h0 = 2 + hash(talkSeed, 16) * 25, u = (talkAge - h0) / 6f;
                if (u > 0 && u < 1) p[HOP] += 0.45f * sin((float) Math.PI * u);
            }
            // mitre wobble
            p[MITRE_Z] += sin(t * 0.9f) * 0.22f * (0.4f + k);
            p[MITRE_X] = sin(t * 0.7f + 1) * 0.12f * k;
        }
        for (int i = 0; i < N; i++) if (Float.isNaN(p[i]) || Float.isInfinite(p[i])) p[i] = 0;
        return p;
    }
}
