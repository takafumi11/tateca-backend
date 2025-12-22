# Branch Protection Configuration

## Current Setup (Legacy Branch Protection)

### Main Branch Protection Rules

**Status Checks:**
- Required: `CI / Java Tests`
- Strict: `false` (ブランチが最新でなくてもマージ可能)

**Pull Request Reviews:**
- Required approving review count: `1`
- Require code owner reviews: `true`
- Dismiss stale reviews on push: `false`

### Managing via GitHub CLI

**View current settings:**
```bash
gh api repos/takafumi11/tateca-backend/branches/main/protection --jq .
```

**Update status checks:**
```bash
gh api --method PATCH \
  repos/takafumi11/tateca-backend/branches/main/protection/required_status_checks \
  -F strict=false \
  -f 'checks[][context]=CI / Java Tests' \
  -F 'checks[][app_id]=15368'
```

**Update PR review requirements:**
```bash
gh api --method PATCH \
  repos/takafumi11/tateca-backend/branches/main/protection/required_pull_request_reviews \
  -F required_approving_review_count=1 \
  -F require_code_owner_reviews=true \
  -F dismiss_stale_reviews=false
```

---

## Future Migration: GitHub Rulesets

GitHub Rulesetsはブランチプロテクションの後継機能で、以下のメリットがあります：

### Benefits
- ✅ コード化された設定（JSONファイルで管理）
- ✅ 複数ブランチへの適用が容易
- ✅ より柔軟なルール管理（タグ保護、プッシュ制限など）
- ✅ GitOps対応（設定をバージョン管理可能）

### Migration Plan

**Step 1: Create Ruleset via Web UI**
1. Settings → Rules → Rulesets → New ruleset
2. Configure rules matching current branch protection
3. Test with a dummy PR

**Step 2: Export Configuration**
```bash
# Get ruleset ID
gh api repos/takafumi11/tateca-backend/rulesets --jq '.[].id'

# Export ruleset configuration
gh api repos/takafumi11/tateca-backend/rulesets/{RULESET_ID} > .github/ruleset-export.json
```

**Step 3: Create Ruleset Template**
- Use exported JSON as template
- Store in `.github/ruleset.json`
- Document creation/update process

**Step 4: Delete Legacy Branch Protection**
```bash
gh api --method DELETE repos/takafumi11/tateca-backend/branches/main/protection
```

### Resources
- [GitHub Rulesets Documentation](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
- [REST API: Repository Rules](https://docs.github.com/en/rest/repos/rules)

---

## Troubleshooting

### "Expected — Waiting for status to be reported"

This occurs when a required status check name doesn't match any workflow job.

**Diagnosis:**
```bash
# Check required status checks
gh api repos/takafumi11/tateca-backend/branches/main/protection/required_status_checks --jq '.contexts[]'

# Check actual CI workflow job names
cat .github/workflows/ci.yml | grep "name:"
```

**Fix:**
Update the status check name to match the workflow format: `<workflow-name> / <job-name>`

Example: `CI / Java Tests` for workflow "CI" with job "Java Tests"

---

## History

- **2025-12-22**: Migrated from "Build and Test" to "CI / Java Tests" to fix pending status issue
- **Future**: Plan to migrate to GitHub Rulesets for better management
