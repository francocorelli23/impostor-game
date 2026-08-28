# -*- coding: utf-8 -*-
"""Regenerates the game's sound effects.

Writes WAVs next to the project; encode them to OGG with, for example:
    for f in app/src/main/res/raw_wav/*.wav; do
      ffmpeg -y -i "$f" -c:a libvorbis -q:a 5 "app/src/main/res/raw/$(basename "$f" .wav).ogg"
    done

Needs numpy and scipy:  pip install numpy scipy


The first set was pure sine and triangle tones with instant attacks, which is
exactly the recipe for a thin, artificial "ping". This set follows four rules
that make small sounds read as physical objects instead of test tones:

  1. Nothing lives above ~3 kHz. Brightness is what makes a UI sound shrill.
  2. Every sound has a real attack (3-40 ms). Instant onsets click.
  3. Overtones decay two to four times faster than the fundamental, which is
     what struck wood, felt and glass all actually do.
  4. A short burst of filtered noise at the onset gives the sound a body to
     come from - the difference between "thock" and "beep".
"""
import os
import wave

import numpy as np
from scipy.signal import butter, sosfilt, sosfiltfilt

SR = 44100
OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw_wav"))
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(20260825)


# ----------------------------------------------------------------- helpers

def t(dur):
    return np.linspace(0, dur, int(SR * dur), endpoint=False)


def env(x, attack, decay, curve=1.0):
    """Percussive envelope with a genuine attack ramp - never an instant onset."""
    a = np.clip(x / max(attack, 1e-6), 0, 1) ** curve
    d = np.exp(-x / decay)
    return a * d


def lowpass(sig, fc, order=2):
    sos = butter(order, min(fc, SR / 2 - 100), btype="low", fs=SR, output="sos")
    return sosfilt(sos, sig)


def bandpass(sig, lo, hi, order=2):
    sos = butter(order, [max(lo, 20), min(hi, SR / 2 - 100)], btype="band", fs=SR, output="sos")
    return sosfilt(sos, sig)


def noise(n):
    return rng.normal(0, 1, n)


def body(dur, freq, partials, attack, decay, tilt=2.6):
    """A struck body: fundamental plus overtones whose decay shortens with pitch."""
    x = t(dur)
    out = np.zeros(len(x))
    for i, (ratio, amp) in enumerate(partials):
        # Higher partials die faster - this is the whole trick.
        d = decay / (tilt ** i)
        out += amp * np.sin(2 * np.pi * freq * ratio * x) * env(x, attack, d)
    return out


def thock(dur, tone_hz, noise_lo, noise_hi, attack=0.004, tone_decay=0.055,
          noise_decay=0.012, noise_amt=0.55):
    """Soft percussive tap: a filtered noise transient over a low tonal thump."""
    x = t(dur)
    n = bandpass(noise(len(x)), noise_lo, noise_hi) * env(x, 0.0008, noise_decay)
    tone = np.sin(2 * np.pi * tone_hz * x) * env(x, attack, tone_decay)
    tone += 0.22 * np.sin(2 * np.pi * tone_hz * 2.0 * x) * env(x, attack, tone_decay / 3)
    return lowpass(tone + noise_amt * n, 2600)


def write(name, data, peak):
    d = np.asarray(data, dtype=np.float64)
    m = np.max(np.abs(d))
    if m > 0:
        d = d / m * peak
    # 4 ms fades so nothing clicks at the boundaries
    n = int(SR * 0.004)
    if len(d) > 2 * n:
        d[:n] *= np.linspace(0, 1, n)
        d[-n:] *= np.linspace(1, 0, n)
    pcm = (np.clip(d, -1, 1) * 32767).astype("<i2")
    path = f"{OUT}/{name}.wav"
    with wave.open(path, "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes(pcm.tobytes())
    print(f"  {name:16s} {len(pcm)/SR:5.2f}s  peak {peak:.2f}")


# ------------------------------------------------------------------ sounds

# Button press: a soft felt-on-wood tap. Low, short, no ring.
write("sfx_tap", thock(0.075, tone_hz=190, noise_lo=260, noise_hi=900), 0.26)

# Choosing a name or a suspect: same gesture, a touch brighter so it reads as
# a deliberate selection rather than a generic press.
write("sfx_vote", thock(0.085, tone_hz=250, noise_lo=380, noise_hi=1300,
                        tone_decay=0.07, noise_amt=0.45), 0.28)

# Hide & pass: a downward cloth-like whoosh, entirely noise, no pitch at all.
x = t(0.26)
sweep = noise(len(x))
seg = len(x) // 24
parts = []
for i in range(24):
    fc = 1500 * np.exp(-2.4 * i / 24) + 220          # 1720 Hz down to ~330 Hz
    parts.append(lowpass(sweep[i * seg:(i + 1) * seg], fc, order=3))
whoosh = np.concatenate(parts)
whoosh = np.pad(whoosh, (0, len(x) - len(whoosh)), mode="edge")
whoosh *= env(x, 0.030, 0.085)
thump = np.sin(2 * np.pi * 120 * x) * env(x, 0.006, 0.045) * 0.5
write("sfx_hide", lowpass(whoosh * 0.9 + thump, 2200), 0.30)

# Crew reveal: a warm bloom. Slow attack so it swells rather than pings.
warm = body(0.85, 293.66, [(1.0, 1.0), (2.0, 0.30), (3.0, 0.10), (4.01, 0.045)],
            attack=0.045, decay=0.42)
warm += 0.18 * body(0.85, 440.0, [(1.0, 1.0), (2.0, 0.22)], attack=0.11, decay=0.34)
write("sfx_reveal", lowpass(warm, 2400), 0.30)

# Impostor: dark and low, felt in the chest rather than heard in the ears.
x = t(0.9)
drop = np.sin(2 * np.pi * (132 * np.exp(-0.9 * x)) * x) * env(x, 0.010, 0.42)
under = np.sin(2 * np.pi * 74 * x) * env(x, 0.020, 0.55) * 0.8
grit = bandpass(noise(len(x)), 90, 420) * env(x, 0.004, 0.10) * 0.35
write("sfx_impostor", lowpass(drop + under + grit, 900, order=3), 0.38)

# Timer tick: barely there. Quiet, low, 20 ms.
write("sfx_tick", thock(0.035, tone_hz=330, noise_lo=500, noise_hi=1600,
                        tone_decay=0.016, noise_decay=0.006, noise_amt=0.5), 0.16)

# Crew win: a warm marimba-ish rise, C E G C, overtones dying fast.
x = t(1.05)
win = np.zeros(len(x))
for i, f in enumerate([261.63, 329.63, 392.00, 523.25]):
    start = int(SR * 0.085 * i)
    xx = x[:len(x) - start]
    note = body(len(xx) / SR, f, [(1.0, 1.0), (2.0, 0.26), (3.01, 0.08)],
                attack=0.008, decay=0.40)
    win[start:] += note * (0.95 - 0.08 * i)
write("sfx_win", lowpass(win, 2800), 0.34)

# Impostors win: the same shape falling, muted and minor.
x = t(1.05)
lose = np.zeros(len(x))
for i, f in enumerate([293.66, 246.94, 196.00, 146.83]):
    start = int(SR * 0.095 * i)
    xx = x[:len(x) - start]
    note = body(len(xx) / SR, f, [(1.0, 1.0), (2.0, 0.18), (2.99, 0.05)],
                attack=0.016, decay=0.45)
    lose[start:] += note * (0.9 - 0.06 * i)
write("sfx_lose", lowpass(lose, 1500), 0.34)

# Ambient bed: unchanged in character, just softer at the top end.
dur = 12.0
x = t(dur)
pad = np.zeros(len(x))
for f, amp, det in [(65.41, 1.0, 0), (98.0, 0.6, 0.4), (130.81, 0.5, -0.3),
                    (196.0, 0.30, 0.5), (261.63, 0.16, -0.5)]:
    lfo = 1 + 0.0035 * np.sin(2 * np.pi * (1 / dur) * x * 2 + det * 3)
    pad += amp * np.sin(2 * np.pi * (f + det) * x * lfo)
pad *= (0.72 + 0.28 * np.sin(2 * np.pi * x / dur * 2 - np.pi / 2))
pad = sosfiltfilt(butter(2, 700, btype="low", fs=SR, output="sos"), pad)
write("music_ambient", pad, 0.15)

print("\nspectral check (energy above 3 kHz should be near zero):")
for name in sorted(os.listdir(OUT)):
    with wave.open(f"{OUT}/{name}", "rb") as f:
        d = np.frombuffer(f.readframes(f.getnframes()), dtype="<i2").astype(float)
    spec = np.abs(np.fft.rfft(d))
    freqs = np.fft.rfftfreq(len(d), 1 / SR)
    total = spec.sum() or 1
    hi = spec[freqs > 3000].sum() / total
    centroid = (spec * freqs).sum() / total
    print(f"  {name:22s} >3kHz {hi*100:5.2f}%   centroid {centroid:6.0f} Hz")
