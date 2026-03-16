# Git Push Command

Custom command to safely push changes to the current branch on GitHub.

## Quick Reference - Commit Message Conventions

**Standard Format:** `[Type] Brief summary`

- Examples: `[feat]`, `[fix]`, `[refactor]`, `[update]`, `[docs]`, etc.

**Special Prefixes (Override standard format):**
| Files Modified | Prefix | Example |
|----------------|--------|---------|
| `docs/database-updates.sql` | `--DB` | `--DB Add new column to users table` |
| `application*.properties` | `--CONFIG` | `--CONFIG Update database connection` |
| `*.xlsx` or `*.xls` files | `--EXCEL` | `--EXCEL Update export template` |

**Priority:** If multiple special file types are modified, use: DB > CONFIG > EXCEL, or create separate commits.

## Workflow

When this command is executed, you MUST follow these steps in order:

### 1. Pull Latest Changes

**MANDATORY FIRST STEP:**

```bash
git pull --rebase
```

- Always pull before pushing to avoid conflicts
- Use `--rebase` to keep history clean
- If conflicts occur, stop and ask user for guidance

### 2. Review All Modifications

Check what has been changed:

```bash
git status
git diff
```

**Analyze:**

- List all modified files
- **CRITICAL:** Check if any special files are modified (database-updates.sql, application*.properties, *.xlsx/xls)
- Determine if special prefix (--DB, --CONFIG, --EXCEL) is required
- Identify the nature of changes (feature, fix, refactor, etc.)
- Group related changes by theme/purpose
- If multiple special file types are modified, consider creating separate commits

### 3. Stage Changes

**Rules:**

- Stage specific files by name (avoid `git add .` or `git add -A`)
- Only stage files that belong together logically
- Ask user confirmation if unsure about including certain files

### 4. Create Commit Message

**MANDATORY REQUIREMENTS:**

- All commit messages MUST be in English
- Message must be clear, detailed, and concise
- Focus on the "why" and "what", not the "how"
- Format: `[Type] Brief summary (max 70 chars)` OR special prefix format (see below)
- Add body with details if needed
- List only key changes in the body and don't add the files changed
- **If multiple similar changes:**

- Group them into a single logical commit
- Summarize the overall impact in the message
- List key changes in the body if needed

**Special File Conventions:**

CRITICAL: Check modified files and use these special prefixes when applicable:

- **Database changes** (`docs/database-updates.sql`): Use `--DB` prefix
- **Configuration changes** (any `application*.properties` files in `core/src/main/resources/`): Use `--CONFIG` prefix
- **Excel template changes** (any `.xlsx` or `.xls` files): Use `--EXCEL` prefix

**Priority:** Special prefixes override standard [Type] format. If multiple special types are modified, use the most
critical one (DB > CONFIG > EXCEL) or create separate commits.

**Standard commit message structure:**

```
[Type] Brief summary of changes

- Detail 1
- Detail 2
- Detail 3

Pushed by agent.
```

**Special prefix commit message structure:**

```
--PREFIX Brief summary of changes

- Detail 1
- Detail 2
- Detail 3

Pushed by agent.
```

**Standard Types:**

- `feat` - New feature
- `fix` - Bug fix
- `refactor` - Code refactoring
- `update` - Enhancement to existing feature
- `style` - Formatting, missing semi colons, etc.
- `docs` - Documentation only
- `test` - Adding or updating tests
- `chore` - Maintenance tasks

### 5. Create Commit

```bash
git add [specific files]
git commit -m "$(cat <<'EOF'
[Commit message here]

Pushed by agent.
EOF
)"
```

**After commit:**

- Run `git status` to verify success
- If pre-commit hooks fail, fix issues and create a NEW commit (never use `--amend`)

### 6. Push to Current Branch

**Get current branch:**

```bash
git branch --show-current
```

**Push:**

```bash
git push origin [current-branch-name]
```

**If push fails:**

- Check if remote branch exists
- If first push: use `git push -u origin [branch-name]`
- Never use `--force` unless explicitly requested by user
- Never force push to `main` or `master`

### 7. Confirm Success

After successful push:

- Display pushed commit summary
- Show remote URL if available
- Confirm branch name and commit message

## Safety Rules

**NEVER:**

- Skip the pull step
- Use `git add -A` or `git add .` without reviewing
- Commit sensitive files (.env, credentials, etc.)
- Use `--force` or `--no-verify` without explicit user request
- Amend commits after pre-commit hook failures
- Push to protected branches with force
- Use French or any language other than English

**ALWAYS:**

- Review changes before staging
- Use explicit file paths when staging
- Create meaningful commit messages in English
- Verify current branch before pushing
- Handle merge conflicts properly
- Ask user if unsure about any step

## Error Handling

**If conflicts during pull:**

1. Stop the workflow
2. Show conflict details
3. Ask user to resolve manually

**If pre-commit hooks fail:**

1. Show error details
2. Fix the issues
3. Re-stage fixed files
4. Create a NEW commit (not amend)

**If push is rejected:**

1. Pull latest changes
2. Resolve any conflicts
3. Retry push

## Examples

### Example 1: Single Feature

```
[feat] Add user deletion feature

- Add new endpoint to delete users
..

Pushed by agent.
```

### Example 2: Multiple Related Fixes

```
[fix] Resolve accessibility on customer list

- Add new role to customer list
- Fix accessibility on customer list table
- Fix accessibility on customer list pagination

Pushed by agent.
```

### Example 3: Refactoring

```
[refactor] Refactor code and tests

- Refactoring customer list
- Refactoring tests
..

Pushed by agent.
```

### Example 4: Database Update (Special Prefix)

```
--DB Add searchableConcatenatedFields column to entities

- Add searchableConcatenatedFields TEXT column to users table
- Add searchableConcatenatedFields TEXT column to admin_customer table
- Add searchableConcatenatedFields TEXT column to admin_manufacturer table

Pushed by agent.
```

### Example 5: Configuration Update (Special Prefix)

```
--CONFIG Update database connection settings

- Change database URL to new server
- Update connection pool size
- Add new timezone configuration

Pushed by agent.
```

### Example 6: Excel Template Update (Special Prefix)

```
--EXCEL Update customer export template

- Add new columns for markup prices
- Update header formatting
- Fix date format in export template

Pushed by agent.
```

## Notes

- This command assumes you have proper git configuration and SSH/HTTPS access to the remote
- Branch protection rules on GitHub may prevent direct pushes to certain branches
- If working in a team, coordinate with teammates before force pushing
- Always verify you're on the correct branch before pushing
