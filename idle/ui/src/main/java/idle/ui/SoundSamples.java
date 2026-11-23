package idle.ui;

import static idle.ui.Sound.*;

import java.util.*;
import javax.sound.sampled.*;

/**
 * La fabrication des sons (version 0.289, tirée de {@link Sound}) : les enregistrements lus dans les ressources (mono ou
 * stéréo), leur mise à niveau (même force perçue pour chaque son), les sons synthétisés quand un enregistrement manque,
 * et les petits outils de signal (rééchantillonnage, passage en octets). Rien n'y joue : {@link Sound} s'en sert et
 * garde la sortie audio.
 */
abstract class SoundSamples {
    static final float RATE = 44_100f;

    /** Aucun son ne dépasse cette crête, même mis à son niveau. */
    static final double MAX_PEAK = 0.98;

    /** Le volume perçu, en amplitude (de 0 à 1) : la plus forte moyenne quadratique sur 50 ms, tous canaux mêlés. */
    static double loudness(short[] samples, int channels) {
        int window = (int) (0.05 * RATE) * channels;
        double best = 0;
        for (int start = 0; start < Math.max(1, samples.length - window + 1); start += Math.max(1, window / 4)) {
            double sum = 0;
            int end = Math.min(samples.length, start + window);
            for (int i = start; i < end; i++) sum += (double) samples[i] * samples[i];
            if (end > start) best = Math.max(best, Math.sqrt(sum / (end - start)) / 32768.0);
        }
        return best;
    }

    /** Ce son mis au niveau perçu {@code targetDb}, sans dépasser {@link #MAX_PEAK} ; un son vide reste vide. */
    static short[] leveled(short[] samples, int channels, double targetDb) {
        double loudness = loudness(samples, channels);
        if (samples.length == 0 || loudness <= 0) return samples;
        int peak = 0;
        for (short sample : samples) peak = Math.max(peak, Math.abs(sample));
        double gain = Math.min(Math.pow(10, targetDb / 20) / loudness, MAX_PEAK * Short.MAX_VALUE / peak);
        short[] out = new short[samples.length];
        for (int i = 0; i < out.length; i++) out[i] = (short) Math.round(samples[i] * gain);
        return out;
    }

    /** L'explosion enregistrée et l'accord du Big Bang, mêlés en stéréo ; vide si la ressource manque ou se lit mal. */
    static short[] bigBangStereo() {
        short[] recorded = stereo("big-bang.wav");
        if (recorded.length == 0) return recorded;
        short[] chord = synthesize(Cue.BIG_BANG);
        double[] mix = new double[Math.max(recorded.length, chord.length * 2)];
        for (int i = 0; i < recorded.length; i++) mix[i] = RECORDED_GAIN * recorded[i] / 32768.0;
        for (int i = 0; i < chord.length; i++) {
            mix[2 * i] += CHORD_GAIN * chord[i] / 32768.0;
            mix[2 * i + 1] += CHORD_GAIN * chord[i] / 32768.0;
        }
        double peak = 0;
        for (double value : mix) peak = Math.max(peak, Math.abs(value));
        short[] out = new short[mix.length];
        for (int i = 0; i < mix.length; i++) out[i] = (short) Math.round(mix[i] / peak * BIG_BANG_PEAK * Short.MAX_VALUE);
        return out;
    }

    /**
     * Un enregistrement stéréo des ressources du jeu (16 bits, 44,1 kHz), en échantillons gauche, droite, gauche… ; vide
     * s'il manque ou se lit mal : le son synthétisé reste alors.
     */
    static short[] stereo(String resource) {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream(resource)) {
            if (raw == null) return new short[0];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 2 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0];
                byte[] bytes = in.readAllBytes();
                short[] out = new short[bytes.length / 2];
                for (int i = 0; i < out.length; i++) out[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
                return out;
            }
        } catch (Exception unreadable) {
            return new short[0];
        }
    }

    static short[][] seasons() {
        short[] all = monoResource("graine-saisons.wav");
        int each = (int) Math.round(SEASON_SECONDS * RATE);
        if (all.length < 4 * each) return new short[0][];
        short[][] out = new short[4][];
        for (int s = 0; s < 4; s++) out[s] = leveled(Arrays.copyOfRange(all, s * each, (s + 1) * each), 1, REPEATED_DB);
        return out;
    }

    static short[][] clickerBonus() {
        short[][] out = new short[CLICKER_CHORD.length][];
        int gap = (int) (CLICKER_STRUM * RATE);
        for (int stack = 1; stack <= CLICKER_CHORD.length; stack++) {
            // Version 0.258 : le clic est trop bref pour un accord plaqué ; chaque note part un peu après la précédente.
            double[] mix = new double[CLICKER_TAP.length + stack * gap];
            for (int i = 0; i < CLICKER_TAP.length; i++) mix[i] = CLICKER_TAP[i];
            for (int note = 0; note < stack; note++) {
                short[] up = resampled(CLICKER_TAP, CLICKER_CHORD[note]);
                int at = (note + 1) * gap;
                for (int i = 0; i < up.length && at + i < mix.length; i++) mix[at + i] += up[i];
            }
            short[] chord = new short[mix.length];
            for (int i = 0; i < mix.length; i++) chord[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(mix[i] / (1 + stack))));
            out[stack - 1] = leveled(chord, 1, REPEATED_DB);
        }
        return out;
    }

    /** Un enregistrement mono (16 bits, 44,1 kHz) ; vide s'il manque ou se lit mal. */
    static short[] monoResource(String resource) {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream(resource)) {
            if (raw == null) return new short[0];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 1 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0];
                byte[] bytes = in.readAllBytes();
                short[] out = new short[bytes.length / 2];
                for (int i = 0; i < out.length; i++) out[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
                return out;
            }
        } catch (Exception unreadable) {
            return new short[0];
        }
    }

    /** Les notes du piano, découpées dans {@code piano.wav} ; aucune si la ressource manque ou se lit mal. */
    static short[][] piano() {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream("piano.wav")) {
            if (raw == null) return new short[0][];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 1 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0][];
                byte[] bytes = in.readAllBytes();
                int each = (int) Math.round(PIANO_NOTE_SECONDS * RATE);
                if (bytes.length / 2 < each * PIANO_NOTES) return new short[0][];
                short[][] notes = new short[PIANO_NOTES][];
                for (int note = 0; note < PIANO_NOTES; note++) {
                    short[] out = new short[each];
                    for (int i = 0; i < each; i++) {
                        int at = 2 * (note * each + i);
                        out[i] = (short) ((bytes[at] & 0xff) | (bytes[at + 1] << 8));
                    }
                    notes[note] = leveled(out, 1, REPEATED_DB);
                }
                return notes;
            }
        } catch (Exception unreadable) {
            return new short[0][];
        }
    }

    static short[] pianoWrong() {
        if (PIANO.length < 2) return new short[0];
        short[] out = new short[PIANO[0].length];
        for (int i = 0; i < out.length; i++) out[i] = (short) Math.round(0.45 * (PIANO[0][i] + PIANO[1][i]));
        return out;
    }

    /** L'enregistrement de la particule à la hauteur de chaque palier ; vide si la ressource manque ou se lit mal. */
    static short[][] particleStages() {
        try (java.io.InputStream raw = Sound.class.getResourceAsStream("particule.wav")) {
            if (raw == null) return new short[0][];
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(raw))) {
                AudioFormat format = in.getFormat();
                if (format.getChannels() != 1 || format.getSampleSizeInBits() != 16 || format.getSampleRate() != RATE) return new short[0][];
                byte[] bytes = in.readAllBytes();
                double[] source = new double[bytes.length / 2];
                double peak = 0;
                for (int i = 0; i < source.length; i++) {
                    source[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
                    peak = Math.max(peak, Math.abs(source[i]));
                }
                short[][] stages = new short[idle.ui.particles.SpeedStages.COUNT][];
                for (int stage = 0; stage < stages.length; stage++) {
                    // Lu à ce rythme : un pas de plus d'un échantillon monte le son, un pas de moins le descend.
                    double step = idle.ui.particles.SpeedStages.pitch(stage);
                    short[] out = new short[(int) ((source.length - 1) / step)];
                    for (int i = 0; i < out.length; i++) {
                        double at = i * step;
                        int low = (int) at;
                        double value = source[low] + (source[low + 1] - source[low]) * (at - low);
                        out[i] = (short) Math.round(value / peak * PARTICLE_PEAK * Short.MAX_VALUE);
                    }
                    stages[stage] = leveled(out, 1, REPEATED_DB);
                }
                return stages;
            }
        } catch (Exception unreadable) {
            return new short[0][];
        }
    }

    /** Le son lu {@code pitch} fois plus vite : plus aigu et plus court (interpolé entre deux échantillons). */
    static short[] resampled(short[] sound, double pitch) {
        short[] out = new short[Math.max(0, (int) ((sound.length - 1) / pitch))];
        for (int i = 0; i < out.length; i++) {
            double at = i * pitch;
            int low = (int) at;
            out[i] = (short) Math.round(sound[low] + (sound[low + 1] - sound[low]) * (at - low));
        }
        return out;
    }

    /** Des octets stéréo ramenés en mono : la moyenne des deux canaux. */
    static byte[] mono(byte[] stereo) {
        byte[] out = new byte[stereo.length / 2];
        for (int i = 0; i < out.length / 2; i++) {
            int left = (short) ((stereo[4 * i] & 0xff) | (stereo[4 * i + 1] << 8));
            int right = (short) ((stereo[4 * i + 2] & 0xff) | (stereo[4 * i + 3] << 8));
            int value = (left + right) / 2;
            out[2 * i] = (byte) value;
            out[2 * i + 1] = (byte) (value >> 8);
        }
        return out;
    }

    /** Les échantillons, mis au volume voulu, en octets 16 bits petit-boutistes. */
    static byte[] pcm(short[] samples, double volume) {
        // Une courbe en carré : la moitié du curseur sonne comme la moitié du volume.
        double gain = volume * volume;
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int value = (int) Math.round(samples[i] * gain);
            out[2 * i] = (byte) value;
            out[2 * i + 1] = (byte) (value >> 8);
        }
        return out;
    }

    /** Fabrique un son, dans le timbre Classique. Les sons restent sous 70 % de la pleine échelle : jamais de saturation. */
    static short[] synthesize(Cue cue) {
        return switch (cue) {
            // Un grondement qui descend, une longue traîne.
            case BIG_BANG -> render(2.2, t -> {
                double pitch = 55 + 90 * Math.exp(-t * 2.2);
                return envelope(t, 0.02, 2.2) * (0.6 * sine(pitch, t) + 0.3 * sine(pitch * 1.5, t) + 0.25 * noise(t) * Math.exp(-t * 3));
            });
            // Un souffle bref sur une note grave.
            case EXPLOSION -> render(0.9, t -> envelope(t, 0.005, 0.9)
                    * (0.55 * noise(t) * Math.exp(-t * 6) + 0.5 * sine(80 + 60 * Math.exp(-t * 8), t)));
            // Trois notes qui montent : do, mi, sol.
            // Un défi réussi, si defi-reussi.wav manque.
            case VICTORY -> render(0.75, t -> notes(t, 0.12, 0.75, 523.25, 659.25, 783.99));
            // Deux notes claires.
            // Un succès, si succes.wav manque.
            case ACHIEVEMENT -> render(0.55, t -> notes(t, 0.11, 0.55, 659.25, 987.77));
            // Un scintillement, une octave sous celui d'avant la 0.247.
            case COMET -> render(0.7, t -> envelope(t, 0.01, 0.7)
                    * (0.35 * sine(880 + 200 * Math.sin(t * 30), t) + 0.25 * sine(1318.5, t) * Math.exp(-t * 4)));
            // Une cloche : une fondamentale et un partiel inharmonique.
            // (Une octave plus grave depuis la 0.247.)
            // Un nouvel élément, si nouvel-element.wav manque.
            case ELEMENT -> render(0.6, t -> envelope(t, 0.004, 0.6) * (0.5 * sine(440, t) + 0.25 * sine(440 * 2.76, t) * Math.exp(-t * 5)));
            // Un petit bip doux, une octave plus grave depuis la 0.247 : l'annonce de collision, et le palier de vitesse si palier-vitesse.wav manque.
            // Une autre annonce, si annonce.wav manque : une note douce.
            case NOTICE -> render(0.15, t -> envelope(t, 0.003, 0.15) * 0.3 * sine(880, t));
            case LEVEL, COLLISION_NEWS -> render(0.18, t -> envelope(t, 0.005, 0.18) * 0.45 * sine(523.25, t));
            // Un accord parfait qui s'élargit, la quinte et l'octave par-dessus, sur une basse qui tient.
            // L'univers formé si univers.wav manque, la fin de la partie si fin.wav manque : leur son d'avant.
            case UNIVERSE, GAME_END -> render(2.4, t -> envelope(t, 0.15, 2.4) * (0.25 * sine(130.8, t) + 0.12 * sine(261.6, t))
                    + notes(t, 0.18, 2.4, 523.25, 659.25, 783.99, 1046.5));
            // Une fanfare de deux notes, la seconde à la quinte, sur un écho grave.
            // Un palier du défi final, si defi-palier.wav manque.
            case FINALE -> render(1.0, t -> notes(t, 0.16, 1.0, 587.33, 880) + envelope(t, 0.01, 1.0) * 0.2 * sine(146.8, t));
            // Une pluie de trois notes aiguës, très vite.
            // Un skin débloqué, si skin.wav manque.
            case SKIN -> render(0.5, t -> notes(t, 0.05, 0.5, 1567.98, 1975.53, 2349.32));
            // Deux notes graves, la seconde un demi-ton plus haut : une contrainte qui s'annonce.
            // Un défi qui s'ouvre ou commence, si defi-ouvert.wav manque.
            case CHALLENGE -> render(0.6, t -> notes(t, 0.2, 0.6, 293.66, 311.13));
            // Un son sombre qui enfle : une quinte grave et un battement.
            case DARK -> render(0.9, t -> envelope(t, 0.2, 0.9) * (0.35 * sine(110, t) + 0.25 * sine(164.8, t) + 0.1 * sine(111.5, t)));
            // Une note claire et sa quinte, ensemble.
            // Un jalon, si jalon.wav manque.
            case MILESTONE -> render(0.45, t -> envelope(t, 0.005, 0.45) * (0.3 * sine(784, t) + 0.2 * sine(1174.7, t)));
            // Un petit « tchip » de caisse : le prix payé.
            // L'achat : le bip d'avant la 0.263, si achat.wav manque.
            case PURCHASE -> render(0.07, t -> envelope(t, 0.001, 0.07) * Math.exp(-t * 45) * (0.35 * sine(1760, t) + 0.15 * sine(2637, t)));
            // Un interrupteur : un bruit bref, une note sourde.
            case TOGGLE -> render(0.05, t -> envelope(t, 0.0005, 0.05) * (0.3 * noise(t) * Math.exp(-t * 300) + 0.3 * sine(523.25, t) * Math.exp(-t * 60)));
            // Version 0.213, les bruitages. Une particule : un point de lumière, très bref et léger.
            case PARTICLE -> render(0.05, t -> envelope(t, 0.001, 0.05) * Math.exp(-t * 60) * 0.3 * sine(2349.3, t));
            // Une fusion : deux particules qui se rejoignent, une hauteur qui monte.
            case ATOM -> render(0.14, t -> envelope(t, 0.002, 0.14) * 0.4 * sine(1, 600 * t + 300 * t * t / 0.28));
            // Un atome créé (si l'enregistrement manque) : deux notes claires, une quinte qui monte.
            case NEW_ATOM -> render(0.3, t -> notes(t, 0.05, 0.3, 880, 1318.5));
            // Une sonde lancée (si l'enregistrement manque) : un souffle qui monte et s'éloigne, sur un grondement de moteur.
            case PROBE -> render(1.1, t -> envelope(t, 0.05, 1.1) * (0.25 * noise(t) * (0.3 + t) + 0.3 * sine(1, 80 * t + 400 * t * t)));
            // Une note du miroir (si le piano manque) : une note claire.
            case MIRROR -> render(0.6, t -> envelope(t, 0.003, 0.6) * Math.exp(-t * 5) * 0.4 * sine(523.25, t));
            // Une collision (si l'enregistrement manque) : un choc bref et un tintement de métal.
            case COLLISION -> render(0.25, t -> envelope(t, 0.001, 0.25) * (0.2 * noise(t) * Math.exp(-t * 60)
                    + 0.3 * sine(1200, t) * Math.exp(-t * 12) + 0.15 * sine(1797, t) * Math.exp(-t * 16)));
            // Des hadrons : trois quarks liés, trois notes presque ensemble.
            // Les hadrons, si hadron.wav manque.
            case HADRON -> render(0.35, t -> notes(t, 0.03, 0.35, 783.99, 987.77, 1174.66));
            // Le vide : une paire née de rien, un souffle grave qui enfle et retombe.
            case VOID -> render(0.6, t -> envelope(t, 0.15, 0.6) * (0.3 * sine(220, t) + 0.15 * sine(110, t)));
            // Un palier de la graine, si son enregistrement manque : quatre notes qui poussent vers le haut.
            case SEED -> render(0.9, t -> notes(t, 0.09, 0.9, 523.25, 659.25, 783.99, 1046.5));
            // Le rond de croissance (si l'enregistrement manque) : une bulle qui remonte.
            case BLOOM -> render(0.4, t -> envelope(t, 0.005, 0.4) * 0.4 * sine(1, 240 * t + 600 * t * t));
            // Le passage d'une saison (si l'enregistrement manque) : deux notes de bois.
            case SEASON -> render(0.6, t -> notes(t, 0.1, 0.6, 523.25, 783.99));
            // Un clic : un tic sec et léger.
            // Un clic de bouton, si son enregistrement manque.
            case CLICK -> render(0.03, t -> envelope(t, 0.0005, 0.03) * Math.exp(-t * 120) * 0.3 * sine(1800, t));
            // Un changement d'onglet : un « toc » de bois, une page qu'on tourne.
            case TAB -> render(0.07, t -> envelope(t, 0.001, 0.07) * Math.exp(-t * 45) * (0.35 * sine(700, t) + 0.12 * sine(1400, t)));
            // Un tic de bois, très court : une note aiguë qui s'éteint aussitôt, un souffle d'attaque.
            case BEAT -> render(0.1, t -> envelope(t, 0.001, 0.1) * Math.exp(-t * 25) * (0.55 * sine(1568, t) + 0.2 * noise(t) * Math.exp(-t * 120)));
        };
    }

    /** Une forme d'onde : l'amplitude à un instant, en secondes. */
    interface Wave {
        double at(double seconds);
    }

    /** Calcule un son de cette durée, borné à 70 % de la pleine échelle. */
    static short[] render(double seconds, Wave wave) {
        short[] out = new short[(int) (seconds * RATE)];
        for (int i = 0; i < out.length; i++) {
            double value = Math.max(-0.7, Math.min(0.7, wave.at(i / RATE)));
            out[i] = (short) Math.round(value * Short.MAX_VALUE);
        }
        return out;
    }

    /** Attaque linéaire puis décroissance jusqu'au silence à la fin. */
    static double envelope(double t, double attack, double length) {
        if (t < attack) return t / attack;
        double rest = Math.max(0, 1 - (t - attack) / (length - attack));
        return rest * rest;
    }

    static double sine(double frequency, double t) { return Math.sin(2 * Math.PI * frequency * t); }

    /** Un bruit déterministe : le même son à chaque fois. */
    static double noise(double t) {
        long n = (long) (t * RATE) * 2_654_435_761L;
        n ^= n >>> 13;
        n *= 0x5bd1e995L;
        n ^= n >>> 15;
        return ((n & 0xffff) / 32_767.5) - 1;
    }

    /** Des notes l'une après l'autre, chacune tenue jusqu'à la fin. */
    static double notes(double t, double step, double length, double... frequencies) {
        double sum = 0;
        for (int i = 0; i < frequencies.length; i++) {
            double start = i * step;
            if (t < start) break;
            double local = t - start;
            sum += 0.3 * envelope(local, 0.006, length - start) * (sine(frequencies[i], local) + 0.3 * sine(2 * frequencies[i], local));
        }
        return sum;
    }
}
