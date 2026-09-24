"""
KEMSININ / Phorn Dubber - on-device dubbing engine.

Every public function returns a JSON string so the Kotlin layer can read
structured results without any extra serialization dependency.
"""

import os
import re
import json
import asyncio

# ---------------------------------------------------------------------------
# Optional dependencies (Chaquopy pip installs them into the APK)
# ---------------------------------------------------------------------------
try:
    import requests
except Exception as exc:  # pragma: no cover
    requests = None
    _REQUESTS_ERROR = str(exc)
else:
    _REQUESTS_ERROR = None

try:
    import google.generativeai as genai
except Exception as exc:  # pragma: no cover
    genai = None
    _GENAI_ERROR = str(exc)
else:
    _GENAI_ERROR = None

try:
    import edge_tts
except Exception as exc:  # pragma: no cover
    edge_tts = None
    _EDGE_TTS_ERROR = str(exc)
else:
    _EDGE_TTS_ERROR = None


MOBILE_UA = (
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
)

SHARE_URL_RE = re.compile(r"https?://[^\s]+")

SRT_BLOCK_RE = re.compile(
    r"(\d+)\s*\n\s*(\d{2}:\d{2}:\d{2}[,.]\d{1,3})\s*-->\s*"
    r"(\d{2}:\d{2}:\d{2}[,.]\d{1,3})\s*\n(.*?)(?=\n\s*\n|\Z)",
    re.DOTALL,
)

VIDEO_EXTENSIONS = (".mp4", ".mkv", ".mov", ".webm", ".avi", ".m4v", ".3gp")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def _ok(**payload):
    payload["ok"] = True
    return json.dumps(payload, ensure_ascii=False)


def _err(message, **payload):
    payload["ok"] = False
    payload["error"] = str(message)
    return json.dumps(payload, ensure_ascii=False)


def _require_requests():
    if requests is None:
        raise RuntimeError("Python 'requests' is unavailable: %s" % _REQUESTS_ERROR)


def srt_time_to_ms(time_str):
    hours, minutes, seconds = time_str.replace(",", ".").strip().split(":")
    return int((int(hours) * 3600 + int(minutes) * 60 + float(seconds)) * 1000)


def ms_to_srt_time(ms):
    ms = max(0, int(ms))
    seconds, millis = divmod(ms, 1000)
    minutes, seconds = divmod(seconds, 60)
    hours, minutes = divmod(minutes, 60)
    return "%02d:%02d:%02d,%03d" % (hours, minutes, seconds, millis)


def ms_to_clock(ms):
    ms = max(0, int(ms))
    seconds = ms // 1000
    return "%02d:%02d" % (seconds // 60, seconds % 60)


# ---------------------------------------------------------------------------
# Environment / diagnostics
# ---------------------------------------------------------------------------
def engine_info():
    return _ok(
        python=os.sys.version.split()[0],
        requests=None if requests is None else getattr(requests, "__version__", "?"),
        edge_tts=None if edge_tts is None else "ready",
        gemini=None if genai is None else "ready",
        errors={
            "requests": _REQUESTS_ERROR,
            "edge_tts": _EDGE_TTS_ERROR,
            "gemini": _GENAI_ERROR,
        },
    )


def extract_url(text):
    """Pull the first http(s) URL out of pasted share text."""
    if not text:
        return ""
    match = SHARE_URL_RE.search(text.strip())
    return match.group(0).rstrip("。.,，)") if match else text.strip()


def is_video_file(path):
    return path.lower().endswith(VIDEO_EXTENSIONS)


# ---------------------------------------------------------------------------
# Douyin / TikTok source resolving
# ---------------------------------------------------------------------------
def _douyin_item_id(url):
    for pattern in (r"/video/(\d+)", r"modal_id=(\d+)", r"item_ids=(\d+)", r"/note/(\d+)"):
        found = re.search(pattern, url)
        if found:
            return found.group(1)
    return None


def fetch_video_info(raw_url):
    """Resolve a Douyin share link into title/author/cover/duration/play url."""
    try:
        _require_requests()
        url = extract_url(raw_url)
        if not url:
            return _err("Empty link")

        headers = {"User-Agent": MOBILE_UA, "Referer": "https://www.douyin.com/"}

        # Follow v.douyin.com short links to the canonical page.
        if "v.douyin.com" in url or "vm.tiktok" in url or "vt.tiktok" in url:
            response = requests.get(url, headers=headers, allow_redirects=True, timeout=25)
            url = response.url

        item_id = _douyin_item_id(url)
        if not item_id:
            return _err("Could not find a Douyin video id in this link", resolved_url=url)

        api = (
            "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/"
            "?item_ids=%s" % item_id
        )
        response = requests.get(api, headers=headers, timeout=25)
        payload = response.json()
        items = payload.get("item_list") or []
        if not items:
            return _err(
                "Douyin returned no video metadata (the link may be private or expired)",
                item_id=item_id,
            )

        item = items[0]
        video = item.get("video") or {}
        play_addr = (video.get("play_addr") or {}).get("url_list") or []
        play_url = play_addr[0].replace("playwm", "play") if play_addr else ""

        cover_addr = (video.get("cover") or {}).get("url_list") or []
        music = item.get("music") or {}
        author = item.get("author") or {}

        return _ok(
            item_id=item_id,
            title=(item.get("desc") or "").strip() or "Douyin video",
            author=(author.get("nickname") or "").strip(),
            cover=cover_addr[0] if cover_addr else "",
            duration_ms=int(video.get("duration") or 0),
            duration_clock=ms_to_clock(int(video.get("duration") or 0)),
            video_url=play_url,
            music_title=(music.get("title") or "").strip(),
            watermark_free=True,
        )
    except Exception as exc:
        return _err(exc)


def download_video(raw_url, dest_dir, file_name="douyin.mp4"):
    """Download a watermark-free Douyin video into the app storage folder."""
    try:
        _require_requests()
        info = json.loads(fetch_video_info(raw_url))
        if not info.get("ok"):
            return json.dumps(info, ensure_ascii=False)

        play_url = (info.get("video_url") or "").replace("playwm", "play")
        if not play_url:
            return _err("No downloadable stream found for this link")

        os.makedirs(dest_dir, exist_ok=True)
        safe_name = re.sub(r"[^A-Za-z0-9._-]", "_", file_name) or "douyin.mp4"
        if not safe_name.lower().endswith(".mp4"):
            safe_name += ".mp4"
        target = os.path.join(dest_dir, safe_name)

        headers = {"User-Agent": MOBILE_UA, "Referer": "https://www.douyin.com/"}
        with requests.get(play_url, headers=headers, stream=True, timeout=60) as response:
            response.raise_for_status()
            with open(target, "wb") as handle:
                for chunk in response.iter_content(chunk_size=1024 * 256):
                    if chunk:
                        handle.write(chunk)

        size = os.path.getsize(target)
        if size <= 0:
            return _err("Downloaded file is empty")

        return _ok(
            file_path=target,
            size_bytes=size,
            size_label="%.1f MB" % (size / (1024.0 * 1024.0)),
            title=info.get("title"),
            author=info.get("author"),
            duration_ms=info.get("duration_ms", 0),
            duration_clock=info.get("duration_clock", "00:00"),
            cover=info.get("cover", ""),
        )
    except Exception as exc:
        return _err(exc)


# ---------------------------------------------------------------------------
# Subtitles
# ---------------------------------------------------------------------------
def parse_srt(srt_content):
    """Parse SRT text into [{index, start_ms, end_ms, text}]."""
    try:
        segments = []
        for match in SRT_BLOCK_RE.finditer((srt_content or "").replace("\r\n", "\n")):
            segments.append(
                {
                    "index": int(match.group(1)),
                    "start_ms": srt_time_to_ms(match.group(2)),
                    "end_ms": srt_time_to_ms(match.group(3)),
                    "text": " ".join(match.group(4).split()),
                }
            )
        return _ok(segments=segments, count=len(segments))
    except Exception as exc:
        return _err(exc)


def build_srt(segments_json):
    """Turn [{start_ms, end_ms, text}] back into SRT text."""
    try:
        segments = json.loads(segments_json) if isinstance(segments_json, str) else segments_json
        lines = []
        for position, segment in enumerate(segments, start=1):
            lines.append(str(position))
            lines.append(
                "%s --> %s"
                % (ms_to_srt_time(segment.get("start_ms", 0)), ms_to_srt_time(segment.get("end_ms", 0)))
            )
            lines.append((segment.get("text") or "").strip())
            lines.append("")
        return _ok(srt="\n".join(lines).strip() + "\n")
    except Exception as exc:
        return _err(exc)


TRANSLATE_INSTRUCTIONS = (
    "You are a professional subtitle translator for a video dubbing studio.\n"
    "Translate the SRT subtitle content into {lang}.\n"
    "Rules:\n"
    "1. Keep the exact same SRT structure (index, timestamp line, then text).\n"
    "2. Never merge or split cues, never renumber them.\n"
    "3. Keep each translation short enough to be read aloud in the cue duration.\n"
    "4. Return ONLY the translated SRT. No markdown fences, no commentary."
)


def translate_srt(srt_content, api_key, target_lang="Khmer"):
    try:
        if genai is None:
            return _err("Gemini library unavailable: %s" % _GENAI_ERROR)
        if not api_key or not api_key.strip():
            return _err("Missing Gemini API key")
        if not srt_content or not srt_content.strip():
            return _err("Nothing to translate")

        genai.configure(api_key=api_key.strip())
        model = genai.GenerativeModel("gemini-1.5-flash")
        prompt = "%s\n\nSRT Content:\n%s" % (
            TRANSLATE_INSTRUCTIONS.format(lang=target_lang),
            srt_content,
        )
        response = model.generate_content(prompt)
        text = (response.text or "").strip()
        text = text.replace("```srt", "").replace("```", "").strip()
        if not text:
            return _err("Gemini returned an empty translation")
        return _ok(srt=text + "\n")
    except Exception as exc:
        return _err(exc)


def translate_text(text, api_key, target_lang="Khmer"):
    try:
        if genai is None:
            return _err("Gemini library unavailable: %s" % _GENAI_ERROR)
        if not api_key or not api_key.strip():
            return _err("Missing Gemini API key")
        genai.configure(api_key=api_key.strip())
        model = genai.GenerativeModel("gemini-1.5-flash")
        response = model.generate_content(
            "Translate this video title into %s. Reply with the translation only:\n%s"
            % (target_lang, text)
        )
        return _ok(text=(response.text or "").strip())
    except Exception as exc:
        return _err(exc)


def test_gemini(api_key):
    try:
        if genai is None:
            return _err("Gemini library unavailable: %s" % _GENAI_ERROR)
        genai.configure(api_key=api_key.strip())
        model = genai.GenerativeModel("gemini-1.5-flash")
        response = model.generate_content("Reply with the single word: OK")
        return _ok(reply=(response.text or "").strip())
    except Exception as exc:
        return _err(exc)


# ---------------------------------------------------------------------------
# Text to speech (edge-tts neural voices)
# ---------------------------------------------------------------------------
def list_voices(prefix=""):
    try:
        if edge_tts is None:
            return _err("edge-tts unavailable: %s" % _EDGE_TTS_ERROR)

        async def collect():
            voices = await edge_tts.list_voices()
            return [
                {
                    "name": voice.get("ShortName", ""),
                    "gender": voice.get("Gender", ""),
                    "locale": voice.get("Locale", ""),
                }
                for voice in voices
                if not prefix or prefix in voice.get("Locale", "")
            ]

        voices = asyncio.run(collect())
        return _ok(voices=voices, count=len(voices))
    except Exception as exc:
        return _err(exc)


def _synthesize_sync(text, voice, out_path, rate, pitch, volume):
    async def run():
        communicate = edge_tts.Communicate(
            text, voice, rate=rate, pitch=pitch, volume=volume
        )
        await communicate.save(out_path)

    asyncio.run(run())
    return os.path.exists(out_path) and os.path.getsize(out_path) > 0


def synthesize(text, voice, out_path, rate="+0%", pitch="+0Hz", volume="+0%"):
    """Synthesize one passage of speech into an audio file."""
    try:
        if edge_tts is None:
            return _err("edge-tts unavailable: %s" % _EDGE_TTS_ERROR)
        if not text or not text.strip():
            return _err("Nothing to speak")

        parent = os.path.dirname(out_path)
        if parent:
            os.makedirs(parent, exist_ok=True)
        if os.path.exists(out_path):
            os.remove(out_path)

        if not _synthesize_sync(text.strip(), voice, out_path, rate, pitch, volume):
            return _err("Voice synthesis produced no audio")

        return _ok(
            file_path=out_path,
            size_bytes=os.path.getsize(out_path),
            voice=voice,
        )
    except Exception as exc:
        return _err(exc)


def synthesize_segments(segments_json, voice, out_dir, rate="+0%", pitch="+0Hz", volume="+0%"):
    """Synthesize one file per subtitle cue so the dubbing stays in sync."""
    try:
        if edge_tts is None:
            return _err("edge-tts unavailable: %s" % _EDGE_TTS_ERROR)

        segments = json.loads(segments_json) if isinstance(segments_json, str) else segments_json
        os.makedirs(out_dir, exist_ok=True)

        results = []
        for position, segment in enumerate(segments, start=1):
            text = (segment.get("text") or "").strip()
            if not text:
                continue
            target = os.path.join(out_dir, "seg_%04d.mp3" % position)
            ok = False
            try:
                ok = _synthesize_sync(text, voice, target, rate, pitch, volume)
            except Exception:
                ok = False
            results.append(
                {
                    "index": position,
                    "start_ms": segment.get("start_ms", 0),
                    "end_ms": segment.get("end_ms", 0),
                    "text": text,
                    "file_path": target if ok else "",
                    "ok": ok,
                }
            )

        succeeded = len([item for item in results if item["ok"]])
        return _ok(
            segments=results,
            total=len(results),
            succeeded=succeeded,
            failed=len(results) - succeeded,
            folder=out_dir,
        )
    except Exception as exc:
        return _err(exc)


# ---------------------------------------------------------------------------
# Export
# ---------------------------------------------------------------------------
def export_srt(srt_content, dest_path):
    try:
        parent = os.path.dirname(dest_path)
        if parent:
            os.makedirs(parent, exist_ok=True)
        with open(dest_path, "w", encoding="utf-8") as handle:
            handle.write(srt_content or "")
        return _ok(file_path=dest_path, size_bytes=os.path.getsize(dest_path))
    except Exception as exc:
        return _err(exc)
