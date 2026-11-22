# iPlayer Web – Android TV launcher

Opens the official BBC iPlayer website (https://www.bbc.co.uk/iplayer) full-screen on
Sony Bravia / any Android TV or Google TV. Only BBC domains are opened inside the app.

## Remote control
| Button | Action |
|---|---|
| D-pad | Move the on-screen pointer (page scrolls at the edges) |
| OK | Click |
| Back | Leave fullscreen → go back → exit |
| Menu | Switch pointer mode / normal key navigation |
| Play/Pause | Toggle the playing video |

## Build the APK

**Option A – GitHub (no software to install)**
1. Create a new GitHub repository and upload this whole folder (including `.github`).
2. Open the **Actions** tab → **Build APK** → **Run workflow**.
3. When it finishes (~5 min), download the `iplayer-web-apk` artifact and unzip it.

**Option B – Android Studio**
Open the folder in Android Studio → *Build → Build APK(s)*.
The APK is in `app/build/outputs/apk/`.

## Install on the Sony TV
1. TV: *Settings → Device Preferences → Security & restrictions → Unknown sources* → allow
   your file manager (or *Settings → System → About* → click *Build* 7× for developer mode
   if needed).
2. Copy the APK by USB stick and open it with a file manager (e.g. "File Commander"),
   or use the "Send files to TV" app from your phone.
3. The app appears on the home screen as **iPlayer Web**.

## Notes
- **Video quality:** the app sets no quality limit. It has hardware acceleration and DRM
  playback turned on, and it identifies as a desktop browser so BBC serves the full
  website. BBC's player still picks the resolution from your connection speed and
  device, so 1080p can't be guaranteed.
- **Region and licence:** iPlayer only plays in the UK, and live TV needs a UK TV licence.
  Outside the UK the site will say it isn't available there.
