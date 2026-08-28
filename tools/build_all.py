# -*- coding: utf-8 -*-
"""Regenerates app/src/main/assets/words/{en,hr}.json from the source tables.

Run it from anywhere:  python3 tools/build_all.py

Both languages share category ids, ordering and difficulty tags: the Croatian
tables are keyed by the English word, so the two files can never drift apart.
Hints are checked in both languages for leaking their own word - including
inflected forms, which matters far more in Croatian than in English.
"""
import json
import os
import re
import sys
import unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from part1 import PART1
from part2 import PART2
from part3 import PART3
from part4 import PART4
from part5 import PART5
from hr1 import HR1
from hr2 import HR2
from hr3 import HR3
from hr4 import HR4
from hr5 import HR5

ORDER = [
    "food", "sports", "objects", "movies", "people", "athletes", "places",
    "animals", "vehicles", "games", "tech", "music", "books", "brands",
    "countries", "cities", "vacation", "jobs", "hobbies", "tvshows",
    "superheroes", "drinks", "clothing", "school", "nature",
]

EN = {}
for p in (PART1, PART2, PART3, PART4, PART5):
    EN.update(p)

HR = {}
for p in (HR1, HR2, HR3, HR4, HR5):
    HR.update(p)

STOP_EN = {"a", "an", "the", "of", "and", "in", "to", "on", "de", "la"}
STOP_HR = {
    "i", "u", "na", "za", "od", "se", "je", "su", "ne", "da", "sa", "s", "o",
    "iz", "po", "pa", "ali", "ili", "kao", "sve", "svi", "ono", "ova", "ovo",
    "koji", "koja", "koje", "vrlo", "jedan", "jedna", "jedno", "vrsta",
}

problems = []
warnings = []


def tokens(text):
    lowered = text.lower()
    return [t for t in re.split(r"[^0-9a-zà-öø-ÿčćđšž]+", lowered) if t]


def strip_accents(t):
    return "".join(c for c in unicodedata.normalize("NFD", t) if unicodedata.category(c) != "Mn")


def check_hint(where, word, hint, stop):
    """Flags a hint that gives away its own word, exactly or by shared stem."""
    wt = [t for t in tokens(word) if len(t) > 3 and t not in stop]
    ht = [t for t in tokens(hint) if len(t) > 3 and t not in stop]
    for w in wt:
        if w in hint.lower():
            problems.append(f"{where}: hint leaks '{w}' -> {hint}")
            continue
        aw = strip_accents(w)
        for h in ht:
            ah = strip_accents(h)
            common = 0
            for a, b in zip(aw, ah):
                if a != b:
                    break
                common += 1
            if common >= 4:
                problems.append(f"{where}: hint stem '{h}' matches word '{w}' -> {hint}")


def build(lang):
    cats = []
    total = 0
    for cid in ORDER:
        en_name, emoji, en_words = EN[cid]
        if lang == "en":
            name = en_name
            rows = [(w, e, h, d) for (w, e, h, d) in en_words]
            stop = STOP_EN
        else:
            if cid not in HR:
                problems.append(f"hr: category '{cid}' missing")
                continue
            hr_name, hr_words = HR[cid]
            name = hr_name
            stop = STOP_HR
            by_en = {}
            for row in hr_words:
                if len(row) != 4:
                    problems.append(f"hr/{cid}: malformed row {row}")
                    continue
                by_en[row[0]] = row
            missing = [w for (w, _, _, _) in en_words if w not in by_en]
            extra = [k for k in by_en if k not in {w for (w, _, _, _) in en_words}]
            if missing:
                problems.append(f"hr/{cid}: no translation for {missing}")
            if extra:
                problems.append(f"hr/{cid}: translation for unknown word {extra}")
            rows = []
            for (w, _e, _h, d) in en_words:
                if w not in by_en:
                    continue
                _, hw, he, hv = by_en[w]
                rows.append((hw, he, hv, d))

        seen = set()
        counts = {"easy": 0, "medium": 0, "hard": 0}
        out = []
        for (word, easy, vague, diff) in rows:
            key = word.lower()
            if key in seen:
                problems.append(f"{lang}/{cid}: duplicate word {word}")
            seen.add(key)
            counts[diff] = counts.get(diff, 0) + 1
            for label, hint in (("easy", easy), ("vague", vague)):
                if not hint or len(hint) > 60:
                    problems.append(f"{lang}/{cid}/{word}: {label} hint length {len(hint)}")
                check_hint(f"{lang}/{cid}/{word}[{label}]", word, hint, stop)
            out.append({"w": word, "e": easy, "h": vague, "d": diff})

        if len(out) < 25:
            problems.append(f"{lang}/{cid}: only {len(out)} words")
        for d, n in counts.items():
            if n < 5:
                problems.append(f"{lang}/{cid}: only {n} '{d}' words")
        total += len(out)
        cats.append({"id": cid, "name": name, "emoji": emoji, "words": out})
    return cats, total


def main():
    out_dir = os.path.abspath(os.path.join(HERE, "..", "app", "src", "main", "assets", "words"))
    os.makedirs(out_dir, exist_ok=True)
    summary = []
    for lang in ("en", "hr"):
        cats, total = build(lang)
        doc = {"version": 2, "language": lang, "categories": cats}
        path = os.path.join(out_dir, f"{lang}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(doc, f, ensure_ascii=False, separators=(",", ":"))
        with open(path, encoding="utf-8") as f:
            back = json.load(f)
        assert len(back["categories"]) == len(ORDER)
        summary.append((lang, len(cats), total, os.path.getsize(path)))

    if problems:
        print(f"--- {len(problems)} problem(s) ---")
        for p in problems:
            print("  ", p)
        return 1

    for lang, ncat, total, size in summary:
        print(f"{lang}: {ncat} categories, {total} words, {size} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
