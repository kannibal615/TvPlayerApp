#!/usr/bin/env python3
"""Render an Android TV audit JSON file as Markdown, CSV, and Codex fix prompts."""

from __future__ import annotations

import argparse
import csv
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


SEVERITIES = ["bloquant", "critique", "majeur", "mineur", "amélioration"]
SEVERITY_RANK = {value: index for index, value in enumerate(SEVERITIES)}
REQUIRED = [
    "id",
    "severity",
    "category",
    "screen",
    "title",
    "repro_steps",
    "actual",
    "expected",
    "impact",
    "evidence",
    "recommendation",
    "acceptance_criteria",
]


def text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, list):
        return "; ".join(str(item) for item in value)
    return str(value)


def clean_cell(value: Any) -> str:
    return text(value).replace("|", "\\|").replace("\n", " ").strip()


def anchor(value: str) -> str:
    value = value.lower().strip()
    value = re.sub(r"[^a-z0-9à-ÿ -]", "", value)
    return re.sub(r"\s+", "-", value)


def validate(data: dict[str, Any]) -> list[dict[str, Any]]:
    issues = data.get("issues")
    if not isinstance(issues, list):
        raise ValueError("issues must be a list")
    seen: set[str] = set()
    for index, issue in enumerate(issues, start=1):
        if not isinstance(issue, dict):
            raise ValueError(f"issue #{index} must be an object")
        missing = [key for key in REQUIRED if key not in issue]
        if missing:
            raise ValueError(f"issue #{index} is missing: {', '.join(missing)}")
        issue_id = str(issue["id"])
        if issue_id in seen:
            raise ValueError(f"duplicate issue id: {issue_id}")
        seen.add(issue_id)
        if issue["severity"] not in SEVERITY_RANK:
            raise ValueError(f"invalid severity for {issue_id}: {issue['severity']}")
        for key in ("repro_steps", "evidence", "acceptance_criteria"):
            if not isinstance(issue[key], list):
                raise ValueError(f"{issue_id}.{key} must be a list")
    return sorted(
        issues,
        key=lambda item: (
            SEVERITY_RANK[item["severity"]],
            str(item.get("screen", "")),
            str(item["id"]),
        ),
    )


def render_report(data: dict[str, Any], issues: list[dict[str, Any]]) -> str:
    title = text(data.get("title")) or "Audit Android TV"
    lines = [f"# {title}", "", text(data.get("summary")) or "Aucun résumé fourni.", ""]

    run = data.get("run") or {}
    if run:
        lines.extend(["## Contexte du test", "", "| Champ | Valeur |", "|---|---|"])
        for key, value in run.items():
            lines.append(f"| {clean_cell(key.replace('_', ' ').title())} | {clean_cell(value)} |")
        lines.append("")

    counts = Counter(issue["severity"] for issue in issues)
    lines.extend(["## Synthèse", "", "| Gravité | Nombre |", "|---|---:|"])
    for severity in SEVERITIES:
        lines.append(f"| {severity.capitalize()} | {counts.get(severity, 0)} |")
    lines.extend(["", "## Couverture", ""])
    coverage = data.get("coverage") or []
    if coverage:
        lines.extend(["| Écran/parcours | État | Notes |", "|---|---|---|"])
        for item in coverage:
            lines.append(
                f"| {clean_cell(item.get('screen'))} | {clean_cell(item.get('status'))} | {clean_cell(item.get('notes'))} |"
            )
    else:
        lines.append("Couverture non renseignée.")

    lines.extend(["", "## Backlog priorisé", "", "| ID | Gravité | Catégorie | Écran | Problème |", "|---|---|---|---|---|"])
    for issue in issues:
        issue_anchor = anchor(f"{issue['id']} {issue['title']}")
        lines.append(
            f"| [{clean_cell(issue['id'])}](#{issue_anchor}) | {clean_cell(issue['severity'])} | "
            f"{clean_cell(issue['category'])} | {clean_cell(issue['screen'])} | {clean_cell(issue['title'])} |"
        )

    lines.extend(["", "## Détails", ""])
    for issue in issues:
        lines.extend(
            [
                f"### {issue['id']} — {issue['title']}",
                "",
                f"- **Gravité :** {issue['severity']}",
                f"- **Catégorie :** {issue['category']}",
                f"- **Écran :** {issue['screen']}",
                f"- **Reproductibilité :** {text(issue.get('reproducibility')) or 'non renseignée'}",
                f"- **Confiance :** {text(issue.get('confidence')) or 'non renseignée'}",
                "",
                "**Étapes de reproduction**",
                "",
            ]
        )
        for index, step in enumerate(issue["repro_steps"], start=1):
            lines.append(f"{index}. {step}")
        lines.extend(
            [
                "",
                f"**Résultat actuel :** {issue['actual']}",
                "",
                f"**Résultat attendu :** {issue['expected']}",
                "",
                f"**Impact :** {issue['impact']}",
                "",
                "**Preuves**",
                "",
            ]
        )
        for item in issue["evidence"]:
            lines.append(f"- `{item}`")
        suspected = issue.get("suspected_area") or []
        if suspected:
            lines.extend(["", "**Zone de code suspectée**", ""])
            for item in suspected:
                lines.append(f"- {item}")
        lines.extend(["", f"**Correction proposée :** {issue['recommendation']}", "", "**Critères d’acceptation**", ""])
        for criterion in issue["acceptance_criteria"]:
            lines.append(f"- {criterion}")
        if issue.get("notes"):
            lines.extend(["", f"**Notes :** {issue['notes']}"])
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def write_csv(path: Path, issues: list[dict[str, Any]]) -> None:
    fields = [
        "id",
        "severity",
        "category",
        "screen",
        "title",
        "repro_steps",
        "actual",
        "expected",
        "impact",
        "reproducibility",
        "evidence",
        "suspected_area",
        "recommendation",
        "acceptance_criteria",
        "confidence",
        "notes",
    ]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        for issue in issues:
            writer.writerow({key: text(issue.get(key)) for key in fields})


def render_prompts(data: dict[str, Any], issues: list[dict[str, Any]]) -> str:
    groups: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for issue in issues:
        tier = "priorité-haute" if issue["severity"] in {"bloquant", "critique", "majeur"} else "finition"
        groups.setdefault((tier, issue["category"]), []).append(issue)

    lines = [
        "# Lots de correction pour Codex",
        "",
        "Traiter un lot à la fois. Inspecter les preuves et le code avant toute modification, préserver les changements existants, puis exécuter les tests ciblés et le scénario de validation Android TV.",
        "",
    ]
    batch = 0
    for (tier, category), group in groups.items():
        for start in range(0, len(group), 5):
            batch += 1
            chunk = group[start : start + 5]
            ids = ", ".join(issue["id"] for issue in chunk)
            lines.extend(
                [
                    f"## Lot {batch} — {tier} / {category}",
                    "",
                    "```text",
                    f"Corrige les anomalies Android TV suivantes : {ids}.",
                    "",
                    "Contraintes :",
                    "- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.",
                    "- Corrige les causes racines sans refonte hors périmètre.",
                    "- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.",
                    "- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.",
                    "- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.",
                    "",
                    "Anomalies :",
                ]
            )
            for issue in chunk:
                lines.extend(
                    [
                        f"- {issue['id']} — {issue['screen']} — {issue['title']}",
                        f"  Actuel : {issue['actual']}",
                        f"  Attendu : {issue['expected']}",
                        f"  Correction proposée : {issue['recommendation']}",
                        f"  Preuves : {text(issue['evidence'])}",
                        f"  Validation : {text(issue['acceptance_criteria'])}",
                    ]
                )
            lines.extend(["```", ""])
    if not issues:
        lines.append("Aucun lot : aucune anomalie n'est renseignée.")
    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, help="Path to issues.json")
    parser.add_argument("--output", required=True, help="Output directory")
    args = parser.parse_args()

    source = Path(args.input).expanduser().resolve()
    output = Path(args.output).expanduser().resolve()
    output.mkdir(parents=True, exist_ok=True)
    data = json.loads(source.read_text(encoding="utf-8"))
    issues = validate(data)

    (output / "audit-report.md").write_text(render_report(data, issues), encoding="utf-8")
    write_csv(output / "issues.csv", issues)
    (output / "codex-fix-prompts.md").write_text(render_prompts(data, issues), encoding="utf-8")
    print(output / "audit-report.md")
    print(output / "issues.csv")
    print(output / "codex-fix-prompts.md")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
