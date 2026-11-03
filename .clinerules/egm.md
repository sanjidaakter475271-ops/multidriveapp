# EGM (Elephant-Goldfish Model)

EGM is installed in this project as a manual bundle. EGM artifact directory: `.dev/docs`.

Bundle root: `.cline/`. When any `egm-*` skill is active, first read
`.cline/docs/cline.md`, then follow the canonical skill in
`.cline/skills/<skill>/SKILL.md`.

- Resolve bundled `agents/`, `resources/`, and `docs/` against `.cline/`, not
  against the project's own `docs/`. Resolve `references/` against the active
  skill directory. Write briefs, drafts, and reports to the artifact directory.
- Goldfish reviewers are fresh-context subagents: use the `spawn_agent` tool,
  ask for a general-purpose subagent, and tell it to read
  `.cline/agents/<persona>.md` first. Pass only the task, absolute bundle paths,
  the target, and the standards. Do not pass conversation history.
- If the current Cline surface has no subagent tool (VS Code and JetBrains
  extensions), stop the review and say so in chat. Do not review in the same
  context, and do not use the synthesizer without nested delegation.
- Do not edit the bundled skill, persona, or standard files; they are upstream
  text from https://github.com/google/elephant-goldfish-model.
