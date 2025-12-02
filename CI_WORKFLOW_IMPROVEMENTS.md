# CI/CD Workflow Improvements

## Overview

This PR addresses issues with the auto-tag and release workflows where:
1. The release workflow was not being triggered after tag creation due to 403 errors with the GitHub API
2. The Gradle build could potentially fail if the wrapper JAR is missing or not found

## Changes Made

### 1. Updated `.github/workflows/auto-tag-on-merge.yml`

**Improvements:**
- **Enhanced logging**: Added detailed HTTP response code and body logging for all API calls to facilitate debugging
- **Fallback mechanism**: If the workflow dispatch API call fails (403 or other errors), the workflow now falls back to pushing the tag via git using REPO_PAT, which triggers the release workflow via the `push: tags:` trigger
- **Better error messages**: Clear error messages guide maintainers on what to check if failures occur

**How it works:**
1. Creates the tag via GitHub API using `REPO_PAT`
2. Attempts to trigger `release.yml` via workflow dispatch API
3. If dispatch fails (e.g., 403 error), falls back to pushing the tag via git with authentication
4. The git push triggers the release workflow through the existing `on: push: tags:` trigger

### 2. Updated `.github/workflows/release.yml`

**Improvements:**
- **Added `gradle/gradle-build-action@v3`**: This action ensures Gradle is properly set up, even if the wrapper is missing or corrupted
- **Gradle Wrapper validation**: Added validation step that checks for gradlew and gradle-wrapper.jar, with informative messages
- **Flexible build command**: Uses gradlew if available, otherwise falls back to the gradle command provided by gradle-build-action
- **Enhanced debugging**: Added `--stacktrace` flag to Gradle commands for better error diagnostics

**Note:** The `workflow_dispatch` trigger was already present, so no changes were needed there.

## Required Actions for Maintainers

### Verify REPO_PAT Secret

The workflow uses a secret called `REPO_PAT` (Personal Access Token). Please ensure:

1. **Secret exists**: Check that `REPO_PAT` is configured in repository Settings → Secrets and variables → Actions
2. **Required scopes**: The PAT must have the following permissions:
   - `repo` (Full control of private repositories)
   - `workflow` (Update GitHub Action workflows) - optional but recommended for dispatch
3. **SSO Authorization**: If this repository is part of an organization with SSO enabled:
   - Go to GitHub Settings → Developer settings → Personal access tokens
   - Find the token used for REPO_PAT
   - Click "Configure SSO" → "Authorize" for the organization

### Verify Other Secrets

Ensure these secrets are still present and valid for APK signing:
- `KEYSTORE_BASE64`: Base64-encoded keystore file
- `KEYSTORE_PASSWORD`: Password for the keystore
- `KEY_ALIAS`: Alias of the key in the keystore
- `KEY_PASSWORD`: Password for the key

### Testing the Workflow

To test these changes:

1. **Test auto-tag workflow**: Merge a PR and verify:
   - Tag is created successfully
   - Release workflow is triggered (either via dispatch or git push fallback)
   - Check Actions logs for clear HTTP response codes and messages

2. **Test release workflow**: Manually trigger the workflow:
   - Go to Actions → "Build and Release APK" → "Run workflow"
   - Select a tag or branch
   - Verify APK builds and release is created successfully

## Troubleshooting

If you see a 403 error in the logs:

1. **Check workflow dispatch response**: The logs will show the HTTP code and response body
2. **Verify fallback worked**: Look for the message "Attempting fallback: pushing tag via git"
3. **Check PAT permissions**: Ensure REPO_PAT has `repo` and `workflow` scopes
4. **Verify SSO**: If in an SSO-enabled organization, authorize the PAT for SSO

If the Gradle build fails:

1. **Check wrapper files**: Ensure `gradle/wrapper/gradle-wrapper.jar` exists in the repository
2. **Review logs**: The validation step will indicate what's missing
3. **gradle-build-action fallback**: The action should handle missing wrapper files automatically

## Security Notes

- The PAT value is never exposed in logs
- All authentication uses GitHub Secrets
- No secrets are modified or added by this PR
- The fallback mechanism uses the same REPO_PAT secret, just via git instead of API

## References

- [GitHub API: Workflow Dispatches](https://docs.github.com/en/rest/actions/workflows#create-a-workflow-dispatch-event)
- [gradle-build-action Documentation](https://github.com/gradle/gradle-build-action)
- [GitHub Personal Access Tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
