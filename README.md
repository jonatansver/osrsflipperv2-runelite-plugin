# RuneLite plugin

This folder contains the OsrsFlipperV2 RuneLite companion plugin.

## Build

```powershell
cd plugins\runelite
.\gradlew.bat build
```

The build output jar is written to `build\libs\osrsflipperv2-runelite-plugin-0.1.0-SNAPSHOT.jar`.

## Local beta sideload

1. Enable **Developer mode** in RuneLite.
2. Build the jar.
3. Copy the jar into `%USERPROFILE%\.runelite\sideloaded-plugins\`.
4. Restart RuneLite.
5. Open the sidebar button labeled **Osrs Flipper V2**.

## Pairing flow

1. Log in to the web app.
2. Open the plugin pairing settings and create a pairing token.
3. Paste the token into the plugin side panel.
4. Keep the default device name or change it before pairing.
5. Click **Pair device**.
6. Watch the log area for the exchange and heartbeat result.

## Stable release flow

1. Keep the plugin version and commit hash in sync with the repo branch you want to ship.
2. Open a PR against `runelite/plugin-hub`.
3. Add or update the plugin manifest entry with the repository URL and commit hash.
4. Wait for plugin-hub verification and CI to pass.
5. Publish the same jar build that was verified in the PR.
