# Release Guide

Repository now supports automated GitHub Releases from tags.

## 1) Create and push a semantic version tag

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

Pushing a tag that matches `v*.*.*` triggers `.github/workflows/release.yml`.

## 2) What the workflow does

- Builds the project with Gradle (`./gradlew clean build`).
- Creates a GitHub Release for the tag.
- Uploads every generated JAR from `build/libs/*.jar` as release assets.
- Includes auto-generated notes (plus a short artifact summary).

## 3) Manual release (optional)

You can run **Create Release** manually from Actions via `workflow_dispatch` by providing an existing tag.

## 4) First release checklist

1. Ensure `main` branch is green.
2. Merge desired PRs.
3. Create tag `v0.1.0` (or the intended version).
4. Verify release appears under GitHub **Releases** with JAR assets attached.
