# Mac Release

Last update 2025-02-26

## Building

1. `mvn clean package -DskipTests` from the repository root.
2. The packaged JARs and dependencies will be in `beast-base/target/` and `beast-base/target/lib/`.

## No JRE

1. Build the disk image and perform notarisation (release script TBD — the old `ant mac` target has been removed).

2. `cd ~/tmp` to find _BEAST.v3.?.?.dmg_.

Note: this path depends on the path of the beast3 project, which is equivalent to `beast3/../../tmp`.

3. `xcrun notarytool submit BEAST.v3.?.?.dmg --apple-id username --password passwd --team-id TEAM_ID --wait`

4. `xcrun stapler staple BEAST.v3.?.?.dmg`

5. Upload dmg to GitHub.

## With JRE

1. Build the disk image with bundled JRE (release script TBD).

2. `cd ~/tmp` to find _BEAST\_with\_JRE.v3.?.?.dmg_.

3. `xcrun notarytool submit BEAST_with_JRE.v3.?.?.dmg --apple-id username --password passwd --team-id TEAM_ID --wait`

4. `xcrun stapler staple BEAST_with_JRE.v3.?.?.dmg`

5. Upload dmg to GitHub.

## Workflow

Refer to https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution

## TODO

1. Create a Maven-based release/packaging script to replace the old Ant `mac` and `macjre` targets.
