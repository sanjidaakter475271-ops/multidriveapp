# Fix Plan: Gradle Buildscript Compilation Error (`Unresolved reference: util`)

## Overview
During build execution (`./gradlew --no-daemon assembleDebug`), the build fails at script evaluation time:

```
e: file:///home/runner/work/multidriveapp/multidriveapp/app/build.gradle.kts:28:31: Unresolved reference: util
  Line 28:         val localProps = java.util.Properties()
                                         ^ Unresolved reference: util
```

Additionally, Kotlin compiler emits warnings on lines 35 & 38:
```
w: file:///home/runner/work/multidriveapp/multidriveapp/app/build.gradle.kts:35:13: Elvis operator (?:) always returns the left operand of non-nullable type String
w: file:///home/runner/work/multidriveapp/multidriveapp/app/build.gradle.kts:38:13: Elvis operator (?:) always returns the left operand of non-nullable type String
```

## Root Cause Analysis
1. **Name Collision in Gradle Kotlin DSL**:
   Inside Gradle build scripts (`.gradle.kts`), `java` is a top-level extension/accessor provided by Gradle (`org.gradle.api.plugins.JavaPluginExtension` / convention). When writing `java.util.Properties()`, Kotlin resolves the `java` identifier to the Gradle extension in the script's lexical scope rather than the Java root package `java`. Because the Gradle `java` extension has no `util` member, compilation fails with `Unresolved reference: util`.
2. **Elvis Warning**:
   In Kotlin, Java interop methods without nullability annotations return platform types (e.g., `String!`). Under certain Kotlin compiler evaluations, chaining `(as? String) ?: localProps.getProperty(...)` without explicit nullability cast can cause the compiler to treat the intermediate expression as non-null `String` or emit redundant Elvis warnings.

## Proposed Changes

### File: `app/build.gradle.kts`
1. **Add explicit import**:
   Add `import java.util.Properties` at the top of `app/build.gradle.kts`.
2. **Update instantiation**:
   Change `val localProps = java.util.Properties()` to `val localProps = Properties()`.
3. **Clean up property resolution & nullability**:
   Ensure `getProperty` calls cleanly fall back to default values without compiler warnings, e.g.:
   ```kotlin
   val clientId = (project.findProperty("GOOGLE_CLIENT_ID") as? String)
       ?: localProps.getProperty("GOOGLE_CLIENT_ID")
       ?: "1053073934306-multidrive.apps.googleusercontent.com"
   ```
   Or use `localProps.getProperty("GOOGLE_CLIENT_ID", defaultClientId)`.

## Verification Steps
1. Run `./gradlew assembleDebug` (or `assembleDebug` in CI).
2. Verify that `app/build.gradle.kts` compiles cleanly without script compilation errors or warnings.
3. Verify that `BuildConfig.GOOGLE_CLIENT_ID` and `BuildConfig.BACKEND_BASE_URL` are correctly generated in the debug build output.
