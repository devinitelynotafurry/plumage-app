# Security Policy

## Reporting a vulnerability

Do not open a public GitHub issue for security vulnerabilities.

Report privately via GitHub's [private vulnerability reporting](https://github.com/devinitelynotafurry/plumage-app/security/advisories/new) (Security tab → Report a vulnerability). This requires enabling the feature once in repo settings; see the setup notes below.

Expect an acknowledgment within a few days. There's no bug bounty; this is a hobby open-source project.

## Scope

In scope: the Android client code in this repo, including how it handles e926 API responses, local storage, and any authenticated requests.

Out of scope: the e926 platform itself (report to e926/e621 directly), and anything in third-party dependencies (report upstream; feel free to also flag it here if it affects Plumage specifically).

## Supported versions

Only the latest tagged release is supported. There is no long-term support branch.
