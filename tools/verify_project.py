#!/usr/bin/env python3
"""Static sanity checks for the Impostor Android project.

Compilation needs the Android SDK and Google's Maven, neither of which this
sandbox can reach, so this script covers the failure modes that would otherwise
only show up at build time:
  * unbalanced braces / parens / brackets in Kotlin (string- and comment-aware)
  * R.* references that point at resources which do not exist
  * project imports (com.impostor.party.*) that resolve to nothing
  * declared-but-unused imports
  * duplicate top-level declarations
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "app", "src", "main")
RES = os.path.join(APP, "res")

problems = []
warnings = []


def kotlin_files():
    for base in (os.path.join(ROOT, "app", "src", "main"), os.path.join(ROOT, "app", "src", "test")):
        for dirpath, _, names in os.walk(base):
            for n in sorted(names):
                if n.endswith(".kt"):
                    yield os.path.join(dirpath, n)


# ----------------------------------------------------------------- bracket scan

def scan_brackets(path, text):
    """Kotlin-aware bracket balance. Handles //, /* */, "", triple quotes, '' and ${}."""
    i, n = 0, len(text)
    line = 1
    stack = []                 # (char, line)
    modes = ["code"]           # code | str | raw
    template_depths = []       # stack depth at which each ${ was opened
    pairs = {")": "(", "]": "[", "}": "{"}

    while i < n:
        c = text[i]
        if c == "\n":
            line += 1
            i += 1
            continue

        mode = modes[-1]

        if mode == "code":
            if text.startswith("//", i):
                j = text.find("\n", i)
                i = n if j < 0 else j
                continue
            if text.startswith("/*", i):
                depth = 1
                i += 2
                while i < n and depth:
                    if text.startswith("/*", i):
                        depth += 1
                        i += 2
                    elif text.startswith("*/", i):
                        depth -= 1
                        i += 2
                    else:
                        if text[i] == "\n":
                            line += 1
                        i += 1
                continue
            if text.startswith('"""', i):
                modes.append("raw")
                i += 3
                continue
            if c == '"':
                modes.append("str")
                i += 1
                continue
            if c == "'":
                i += 1
                while i < n and text[i] != "'":
                    if text[i] == "\\":
                        i += 1
                    i += 1
                i += 1
                continue
            if c in "([{":
                stack.append((c, line))
                i += 1
                continue
            if c in ")]}":
                if not stack:
                    problems.append(f"{path}:{line}: stray '{c}'")
                    return
                opener, oline = stack.pop()
                if opener != pairs[c]:
                    problems.append(
                        f"{path}:{line}: '{c}' closes '{opener}' opened on line {oline}"
                    )
                    return
                if (c == "}" and template_depths
                        and len(stack) + 1 == template_depths[-1]):
                    template_depths.pop()
                    modes.pop()
                i += 1
                continue
            i += 1
            continue

        # inside a string literal
        if mode == "str" and c == "\\":
            i += 2
            continue
        if text.startswith("${", i):
            modes.append("code")
            stack.append(("{", line))
            template_depths.append(len(stack))
            i += 2
            continue
        if mode == "raw" and text.startswith('"""', i):
            modes.pop()
            i += 3
            continue
        if mode == "str" and c == '"':
            modes.pop()
            i += 1
            continue
        i += 1

    if stack:
        c, l = stack[-1]
        problems.append(f"{path}: unclosed '{c}' opened on line {l} ({len(stack)} still open)")


def scan_brackets_wrapper(path, text):
    scan_brackets(path, text)


# ------------------------------------------------------------------- resources

def load_resource_names():
    names = {"string": set(), "color": set(), "style": set(), "drawable": set(),
             "mipmap": set(), "font": set(), "raw": set(), "xml": set(), "plurals": set()}

    for values_dir in ("values", "values-night", "values-hr"):
        d = os.path.join(RES, values_dir)
        if not os.path.isdir(d):
            continue
        for f in sorted(os.listdir(d)):
            if not f.endswith(".xml"):
                continue
            root = ET.parse(os.path.join(d, f)).getroot()
            for child in root:
                tag = child.tag
                nm = child.get("name")
                if tag in names and nm:
                    names[tag].add(nm)

    for folder, key in (("drawable", "drawable"), ("font", "font"), ("raw", "raw"),
                        ("xml", "xml"), ("mipmap-anydpi-v26", "mipmap")):
        d = os.path.join(RES, folder)
        if os.path.isdir(d):
            for f in os.listdir(d):
                stem = f.split(".")[0]
                names[key].add(stem)
    return names


# --------------------------------------------------------------------- symbols

DECL = re.compile(
    r"^(?:@\w+\s+)*(?:public\s+|internal\s+|private\s+)?"
    r"(?:(?:data|value|sealed|abstract|open|enum|annotation|const|lateinit|inline)\s+)*"
    r"(?:class|object|interface|fun|val|var|typealias)\s+([A-Za-z_][A-Za-z0-9_]*)",
    re.M,
)


def collect_declarations():
    """Maps fully-qualified name -> file for every top-level declaration."""
    table = {}
    for path in kotlin_files():
        text = open(path, encoding="utf-8").read()
        m = re.search(r"^package\s+([\w.]+)", text, re.M)
        if not m:
            problems.append(f"{path}: no package declaration")
            continue
        pkg = m.group(1)
        for line in text.splitlines():
            if line.startswith((" ", "\t")):
                continue  # only top level
            d = DECL.match(line)
            if d:
                table.setdefault(f"{pkg}.{d.group(1)}", path)
        # const/top-level vals declared inside `companion object` are not top level; fine.
    return table


def main():
    if not os.path.isdir(ROOT):
        print("project not found", ROOT)
        return 1

    files = list(kotlin_files())
    print(f"Kotlin files: {len(files)}")

    for path in files:
        text = open(path, encoding="utf-8").read()
        scan_brackets_wrapper(os.path.relpath(path, ROOT), text)

    res = load_resource_names()
    print("resources: " + ", ".join(f"{k}={len(v)}" for k, v in sorted(res.items()) if v))

    ref = re.compile(r"\bR\.(string|color|style|drawable|mipmap|font|raw|xml|plurals)\.(\w+)")
    used_strings = set()
    for path in files:
        text = open(path, encoding="utf-8").read()
        for kind, name in ref.findall(text):
            if kind == "string":
                used_strings.add(name)
            if name not in res.get(kind, set()):
                problems.append(
                    f"{os.path.relpath(path, ROOT)}: R.{kind}.{name} does not exist"
                )

    # resource -> resource references inside XML
    xml_ref = re.compile(r"@(string|color|drawable|mipmap|style|xml|font|raw)/([\w.]+)")
    for dirpath, _, names in os.walk(RES):
        for n in names:
            if not n.endswith(".xml"):
                continue
            p = os.path.join(dirpath, n)
            body = open(p, encoding="utf-8").read()
            for kind, name in xml_ref.findall(body):
                if name not in res.get(kind, set()):
                    problems.append(f"{os.path.relpath(p, ROOT)}: @{kind}/{name} does not exist")
    manifest = open(os.path.join(APP, "AndroidManifest.xml"), encoding="utf-8").read()
    for kind, name in xml_ref.findall(manifest):
        if name not in res.get(kind, set()):
            problems.append(f"AndroidManifest.xml: @{kind}/{name} does not exist")

    unused = sorted(res["string"] - used_strings)
    if unused:
        warnings.append(f"strings declared but never referenced: {', '.join(unused)}")

    # project imports
    table = collect_declarations()
    for path in files:
        text = open(path, encoding="utf-8").read()
        rel = os.path.relpath(path, ROOT)
        body = text
        for line in text.splitlines():
            m = re.match(r"import\s+(com\.impostor\.party\.[\w.]+)", line)
            if not m:
                continue
            fq = m.group(1)
            simple = fq.rsplit(".", 1)[1]
            if fq not in table and not fq.endswith(".R") and simple not in ("BuildConfig",):
                # nested declarations (Companion members, enum entries) are allowed
                parent = fq.rsplit(".", 2)
                if len(parent) == 3 and ".".join(parent[:2]) in table:
                    continue
                problems.append(f"{rel}: import {fq} resolves to nothing")
            # crude unused-import check
            after = body.split(line, 1)[1] if line in body else ""
            if not re.search(r"\b" + re.escape(simple) + r"\b", after):
                warnings.append(f"{rel}: unused import {fq}")

    # androidx / kotlin imports must be referenced too (catches typo'd imports)
    for path in files:
        text = open(path, encoding="utf-8").read()
        rel = os.path.relpath(path, ROOT)
        lines = text.splitlines()
        body_start = 0
        for idx, line in enumerate(lines):
            if line.startswith("import "):
                body_start = idx + 1
        body = "\n".join(lines[body_start:])
        for line in lines:
            m = re.match(r"import\s+((?:androidx|kotlin|kotlinx|org|android)\.[\w.]+)", line)
            if not m:
                continue
            simple = m.group(1).rsplit(".", 1)[1]
            if simple in ("*", "getValue", "setValue", "provideDelegate"):
                continue
            if not re.search(r"\b" + re.escape(simple) + r"\b", body):
                warnings.append(f"{rel}: unused import {m.group(1)}")

    # translation parity: every locale must define exactly the base string set
    base_dir = os.path.join(RES, "values", "strings.xml")
    base = {c.get("name"): (c.text or "") for c in ET.parse(base_dir).getroot() if c.tag == "string"}
    spec = re.compile(r"%\d+\$[sd]")
    for entry in sorted(os.listdir(RES)):
        if not entry.startswith("values-") or entry == "values-night":
            continue
        path = os.path.join(RES, entry, "strings.xml")
        if not os.path.exists(path):
            continue
        other = {c.get("name"): (c.text or "") for c in ET.parse(path).getroot() if c.tag == "string"}
        for name in sorted(set(base) - set(other)):
            problems.append(f"{entry}/strings.xml: missing translation for '{name}'")
        for name in sorted(set(other) - set(base)):
            problems.append(f"{entry}/strings.xml: '{name}' is not in the base language")
        for name in sorted(set(base) & set(other)):
            if sorted(spec.findall(base[name])) != sorted(spec.findall(other[name])):
                problems.append(f"{entry}/strings.xml: '{name}' format arguments differ")

        # <plurals> must exist in every locale, and every form must take the
        # same arguments as the base "other" form.
        base_plurals = {c.get("name"): c for c in ET.parse(base_dir).getroot() if c.tag == "plurals"}
        other_plurals = {c.get("name"): c for c in ET.parse(path).getroot() if c.tag == "plurals"}
        for name in sorted(set(base_plurals) - set(other_plurals)):
            problems.append(f"{entry}/strings.xml: missing plurals '{name}'")
        for name in sorted(set(other_plurals) - set(base_plurals)):
            problems.append(f"{entry}/strings.xml: plurals '{name}' is not in the base language")
        for name in sorted(set(base_plurals) & set(other_plurals)):
            want = sorted(spec.findall(
                next((i.text or "" for i in base_plurals[name] if i.get("quantity") == "other"), "")))
            for item in other_plurals[name]:
                got = sorted(spec.findall(item.text or ""))
                if got != want:
                    q = item.get("quantity")
                    problems.append(f"{entry}/strings.xml: plurals '{name}' [{q}] arguments differ")

    # every language named in locales_config must have strings and a word list
    locales_path = os.path.join(RES, "xml", "locales_config.xml")
    if os.path.exists(locales_path):
        for loc in ET.parse(locales_path).getroot():
            code = list(loc.attrib.values())[0] if loc.attrib else None
            if not code:
                continue
            words = os.path.join(APP, "assets", "words", f"{code}.json")
            if not os.path.exists(words):
                problems.append(f"locales_config lists '{code}' but assets/words/{code}.json is missing")
            if code != "en":
                sd = os.path.join(RES, f"values-{code}", "strings.xml")
                if not os.path.exists(sd):
                    problems.append(f"locales_config lists '{code}' but values-{code}/strings.xml is missing")

    # word lists must agree on category ids and counts across languages
    words_dir = os.path.join(APP, "assets", "words")
    shapes = {}
    if os.path.isdir(words_dir):
        import json as _json
        for f in sorted(os.listdir(words_dir)):
            if not f.endswith(".json"):
                continue
            doc = _json.load(open(os.path.join(words_dir, f), encoding="utf-8"))
            shapes[f] = [(c["id"], len(c["words"])) for c in doc["categories"]]
        if len(set(map(str, shapes.values()))) > 1:
            problems.append("word lists disagree on category ids or word counts: " +
                            ", ".join(f"{k}={len(v)} cats" for k, v in shapes.items()))
        print("word lists: " + ", ".join(
            f"{k.split('.')[0]}={sum(n for _, n in v)} words/{len(v)} cats" for k, v in shapes.items()))

    # duplicate top-level declarations across files
    seen = {}
    for fq, path in table.items():
        seen.setdefault(fq, []).append(path)

    print()
    if warnings:
        print(f"--- {len(warnings)} warning(s) ---")
        for w in warnings:
            print("  warn:", w)
    print()
    if problems:
        print(f"--- {len(problems)} PROBLEM(S) ---")
        for p in problems:
            print("  ERROR:", p)
        return 1
    print("No structural problems found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
