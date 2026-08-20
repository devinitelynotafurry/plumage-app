#!/usr/bin/env bash
# Configures GitHub-side security/quality settings for plumage-app that
# can't be done by committing files (repo settings, branch protection).
# Requires: gh CLI, authenticated (gh auth login), repo admin access.
set -euo pipefail

REPO="devinitelynotafurry/plumage-app"

echo "Enabling Dependabot alerts and security updates..."
gh api -X PUT "/repos/${REPO}/vulnerability-alerts"
gh api -X PUT "/repos/${REPO}/automated-security-fixes"

echo "Enabling secret scanning + push protection..."
gh api -X PATCH "/repos/${REPO}" \
  -f security_and_analysis[secret_scanning][status]=enabled \
  -f security_and_analysis[secret_scanning_push_protection][status]=enabled

echo "Enabling private vulnerability reporting..."
gh api -X PUT "/repos/${REPO}/private-vulnerability-reporting"

echo "Setting branch protection on main..."
gh api -X PUT "/repos/${REPO}/branches/main/protection" \
  --input - <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["Analyze (java-kotlin)", "detekt", "android-lint"]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "required_approving_review_count": 0,
    "dismiss_stale_reviews": true
  },
  "restrictions": null,
  "required_linear_history": true,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON

echo "Done. Verify at: https://github.com/${REPO}/settings/branches"
