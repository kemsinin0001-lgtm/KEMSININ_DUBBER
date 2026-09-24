# 🎬 Phorn Dubber — KEMSININ AI DUBBER (Android)

A Khmer-first Android dubbing studio built with **Jetpack Compose** and a **Chaquopy (Python)**
engine that runs on-device. It matches the "Phorn Dubber (សាកល្បង)" reference design: a dark
navy/violet studio with a preview panel, a Douyin link bar, and a five-tab workflow.

## ✨ Screens

| Tab | Khmer | What it does |
| --- | --- | --- |
| Studio | វីដេអូ & ក្រុម | Preview panel + timer, Douyin link field with copy button, the main gradient CTA, "រក្សាទុកក្នុង PC" export, import video / import folder, and the video group list |
| Voice | សិទ្ធិអ្នក | Neural voice picker (Khmer Piseth / Sreymom + more), rate / pitch / volume sliders, voice preview and batch synthesis |
| Subtitles | អក្សរ & សិលាចារឹក | Paste a script or import an `.srt`, auto-spread cues over the video duration, translate the cues, and browse original vs translated lines |
| Edit | កែសម្រួល | Gemini API key + verification, target language, the four-step pipeline tracker, engine diagnostics and the run log |
| Timeline | លំដាប់ | Total duration / clip / cue counters, the current export artifact, export + save-as-SRT actions, and the ordered clip sequence with time offsets |

## 🔧 How it works

1. **Source** — paste a Douyin share link; the engine resolves `v.douyin.com` short links,
   reads the item metadata and downloads a watermark-free stream
   (`playwm` → `play`). Videos can also be imported from the device or a whole folder.
2. **Subtitles** — cues come from a pasted script (one line per cue, spread evenly across the
   video duration) or from an imported SRT file. SRT parsing/building happens in Kotlin so the
   editor stays responsive.
3. **Translation** — `gemini-1.5-flash` translates the whole SRT while preserving cue numbers
   and timings (`google-generativeai` inside the Chaquopy engine).
4. **Voice** — `edge-tts` synthesizes Khmer neural speech per cue, honoring the rate, pitch and
   volume sliders, so the dub stays aligned with the subtitle timeline. Results can be played
   back inside the app.
5. **Export** — the current artifact (video, MP3 or SRT) is copied to the location you pick
   through the system file picker ("រក្សាទុកក្នុង PC").

### AI service

Subtitle translation uses the **Google AI (Gemini) API**. Create a key in
[Google AI Studio](https://aistudio.google.com/), paste it into the app's **កែសម្រួល** tab and
tap "ផ្ទៀងផ្ទាត់ API Key". The key is stored only in the app's own state.

## 📦 Download / build the APK

Every push to `main` (and every manual **workflow_dispatch** run) builds release APKs in
[GitHub Actions](https://github.com/kemsinin0001-lgtm/KEMSININ_DUBBER/actions). Download the
**universal** APK artifact and install it (allow "install unknown apps" — release builds are
signed with the debug key so sideloading works).

To build locally:

```bash
./gradlew assembleRelease
# outputs: app/build/outputs/apk/release/
```

Requirements: JDK 17, Android SDK 37, Python 3.12 available for Chaquopy (set
`PYTHON_BUILD_PATH` if it is not on `PATH`). Chaquopy installs `google-generativeai`,
`edge-tts` and `requests` into the APK.

## 📁 Project layout

```
app/src/main/java/com/kemsinin/dubber/
  MainActivity.kt            # starts Chaquopy Python + the Compose theme
  ui/DubberApp.kt            # top bar, status strip, bottom navigation, file pickers
  ui/DubberState.kt          # screen state, cue model, SRT helpers
  ui/DubberEngine.kt         # Chaquopy bridge + Android media/document APIs
  ui/components/             # UiKit (cards, buttons, pills) + audio preview
  ui/screens/                # Studio, Voice, Subtitle, Edit, Timeline
  ui/theme/                  # navy/violet palette and Material 3 scheme
app/src/main/python/dubber_engine.py   # link resolving, download, SRT, Gemini, edge-tts
```

## ⚠️ Notes

- Speech-to-text (auto subtitle generation from audio) is not part of this build; cues come from
  a pasted script or an imported SRT. Everything else in the pipeline runs on-device.
- Douyin endpoints change regularly — if a link fails, the app reports the engine's error
  verbatim in the status strip and the log.
