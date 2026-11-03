<!--
EGM integration guide for Cline (unofficial harness adapter).
Source plugin: https://github.com/google/elephant-goldfish-model (Apache-2.0).
-->

# Cline integration

Cline is not a supported EGM harness. This project installs EGM as a manual
bundle so Cline's Skills feature can load the canonical skills unchanged.
`cline plugin install` cannot install this repository: Cline plugins are
TypeScript/JavaScript entry files declared in `package.json`, and this feature
is limited to the Cline SDK, CLI, and Kanban.

## Install and locate the bundle

The bundle root for this project is `.cline/`:

```text
.cline/
├── skills/egm-*/SKILL.md   # discovered by Cline as workspace skills
├── agents/*.md             # 9 personas
├── resources/              # execution_instructions.md + document_standards/
├── docs/                   # EGM docs, including this file
├── plugin.json             # plugin identity and version
└── README.md LICENSE TESTING.md
```

From `.cline/skills/<skill>/SKILL.md`, resolve bundled `agents/`, `resources/`,
and `docs/` against the bundle root `.cline/`. The repository layout is
preserved, so a skill's `../../docs/<file>.md` resolves to
`.cline/docs/<file>.md`. Resolve `references/` against the active skill's own
directory. Resolve user documents and generated artifacts against the project
working directory. Never substitute similarly named files in the project for
bundled personas or standards, for example the project's own `docs/` folder.

Skills surface as `/egm-brainstorm`, `/egm-draft`, `/egm-review`,
`/egm-review-loop`, `/egm-resolve`, `/egm-bootstrap`, and `/egm-drift-report`
in the chat input, or by description matching through the `skills` tool. Manage
them in the Skills tab of the scale menu.

To make EGM available in every project, copy the same bundle to the global
layout: `~/.cline/skills/egm-*/` plus `agents/`, `resources/`, and `docs/`
under `~/.cline/`.

## Fresh-context reviewers

Cline maps "spawn a subagent" to the `spawn_agent` tool. Each subagent gets its
own prompt and context window, so it can act as a Goldfish.

| Surface | `spawn_agent` | Named personas | Nesting |
| --- | --- | --- | --- |
| Cline CLI | Yes | No | Yes; subagents can spawn subagents |
| Cline Desktop | Yes | No | Yes |
| VS Code / JetBrains extension | No | No | No |

Cline registers no named subagents, so request a general-purpose subagent and
tell it to read `agents/<file>.md` first, then the target and the standards it
is given. Pass the task, absolute bundle paths, the target, and the reading
list only; do not pass conversation history. Wait for completion and collect
each report. Subagents do not receive MCP tools, and approving the `spawn_agent`
call approves everything the subagent then does, so read the task before
approving it. In Plan mode a subagent cannot edit files, which is the preferred
mode for reviews.

Prefer the Cline CLI or Desktop for `/egm-brainstorm` and `/egm-review`. In the
VS Code extension there is no subagent tool, so the skills must stop rather than
review in the same context: a review in the same context is not a Goldfish
review. `/egm-draft`, `/egm-resolve`, `/egm-bootstrap`, and `/egm-drift-report`
that do not need delegation still work.

## Artifact directory

Add this line to a rule file that Cline loads, or to the prompt, to choose where
briefs, drafts, and reports are written:

```text
EGM artifact directory: .dev/docs
```

A relative path is relative to the working directory. Without this line, files
go to the working directory. Add the folder to `.gitignore` to keep it out of
version control.

## Status

Unverified end to end. Installing the skills does not establish workflow
support; the EGM test record covers only the harnesses listed in
`docs/harnesses.md`. Treat the CLI/Desktop review path as experimental until a
`/egm-brainstorm`, `/egm-draft`, and uninterrupted `/egm-review` run has been
recorded here.
