import os
import re
import asyncio
import whisper
import google.generativeai as genai
import edge_tts
from pydub import AudioSegment
from pydub.effects import speedup
import ffmpeg

# ==========================================
# កំណត់ការកំណត់ទូទៅ (Configuration)
# ==========================================
GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"  # ដាក់ API Key របស់អ្នកនៅទីនេះ
TTS_VOICE = "km-KH-PisethNeural"            # សំឡេងខ្មែរ (ប្រុស) | សំឡេងស្រី៖ km-KH-SreymomNeural

# ==========================================
# មុខងារជំនួយ (Helper Functions)
# ==========================================
def srt_time_to_ms(time_str):
    """បំលែងពេលវេលា SRT (00:00:01,500) ទៅជាមិល្លីវិនាទី"""
    hours, minutes, seconds = time_str.replace(',', '.').split(':')
    return int((int(hours) * 3600 + int(minutes) * 60 + float(seconds)) * 1000)

def ms_to_srt_time(ms):
    """បំលែងមិល្លីវិនាទីទៅជាទម្រង់ SRT"""
    seconds, ms = divmod(ms, 1000)
    minutes, seconds = divmod(seconds, 60)
    hours, minutes = divmod(minutes, 60)
    return f"{int(hours):02}:{int(minutes):02}:{int(seconds):02},{int(ms):03}"

# ==========================================
# ជំហានទី ១៖ Transcribe (បំលែងសំឡេងជាអក្សរ)
# ==========================================
def step1_transcribe(video_path, output_srt="original.srt"):
    print("\n⏳ ជំហានទី១៖ កំពុង Transcribe (អាចចំណាយពេល ៥-១០ នាទី)...")
    model = whisper.load_model("base") # ប្រើ "small" ឬ "medium" ដើម្បីត្រឹមត្រូវជាង
    result = model.transcribe(video_path, task="transcribe")
    
    with open(output_srt, "w", encoding="utf-8") as f:
        for i, segment in enumerate(result["segments"], start=1):
            start = ms_to_srt_time(segment['start'] * 1000)
            end = ms_to_srt_time(segment['end'] * 1000)
            f.write(f"{i}\n{start} --> {end}\n{segment['text'].strip()}\n\n")
            
    print(f"✅ ជំហានទី១ បញ្ចប់! ឯកសាររក្សាទុកនៅ៖ {output_srt}")
    return output_srt

# ==========================================
# ជំហានទី ២៖ បកប្រែដោយ Gemini
# ==========================================
def step2_translate(srt_path, target_lang="Khmer", output_srt="translated.srt"):
    print("\n🚀 ជំហានទី២៖ កំពុងបកប្រែដោយ Google Gemini...")
    genai.configure(api_key=GEMINI_API_KEY)
    model = genai.GenerativeModel('gemini-1.5-flash') # ប្រើ flash សម្រាប់ល្បឿនលឿន
    
    with open(srt_path, "r", encoding="utf-8") as f:
        srt_content = f.read()

    prompt = f"""
    You are a professional movie translator. Translate the following SRT subtitle content into {target_lang}.
    Keep the exact same SRT format (numbers, timestamps) but translate the text.
    Only return the translated SRT content, no extra explanations, no markdown blocks.
    
    SRT Content:
    {srt_content}
    """
    
    try:
        response = model.generate_content(prompt)
        # សម្អាតកូដប្លុក (បើមាន) ចេញពីលទ្ធផល
        translated_text = response.text.replace("```srt", "").replace("```", "").strip()
        
        with open(output_srt, "w", encoding="utf-8") as f:
            f.write(translated_text)
        print(f"✅ ជំហានទី២ បញ្ចប់! ឯកសាររក្សាទុកនៅ៖ {output_srt}")
        return output_srt
    except Exception as e:
        print(f"❌ មានបញ្ហាក្នុងការបកប្រែ៖ {e}")
        return None

# ==========================================
# ជំហានទី ៣៖ បង្កើតសំឡេង និងដោះស្រាយ Audio Sync
# ==========================================
async def generate_tts_for_segment(text, output_path):
    """បង្កើតសំឡេងសម្រាប់អត្ថបទមួយប្រយោគ"""
    communicate = edge_tts.Communicate(text, TTS_VOICE)
    await communicate.save(output_path)

def step3_dub_audio(translated_srt, output_audio="dubbed.mp3"):
    print("\n🎙️ ជំហានទី៣៖ កំពុងបង្កើតសំឡេង និងដោះស្រាយ Audio Sync...")
    
    # អានឯកសារ SRT
    with open(translated_srt, "r", encoding="utf-8") as f:
        content = f.read()
    
    # បំបែក SRT ជា Blocks
    blocks = re.split(r'\n\s*\n', content.strip())
    
    # បង្កើតសំឡេងទទេសម្រាប់ដាក់ជាមូលដ្ឋាន
    final_audio = AudioSegment.silent(duration=0)
    
    temp_dir = "temp_tts"
    os.makedirs(temp_dir, exist_ok=True)
    
    for block in blocks:
        lines = block.strip().split('\n')
        if len(lines) >= 3:
            # ទាញយកពេលវេលា និងអត្ថបទ
            time_line = lines[1]
            text = " ".join(lines[2:])
            
            start_str, end_str = time_line.split(' --> ')
            start_ms = srt_time_to_ms(start_str)
            end_ms = srt_time_to_ms(end_str)
            target_duration = end_ms - start_ms
            
            # បង្កើតសំឡេងបណ្តោះអាសន្ន
            temp_file = os.path.join(temp_dir, f"seg_{start_ms}.mp3")
            asyncio.run(generate_tts_for_segment(text, temp_file))
            
            # អានសំឡេងដែលបានបង្កើត
            segment_audio = AudioSegment.from_file(temp_file)
            actual_duration = len(segment_audio)
            
            # ដោះស្រាយ Audio Sync
            if actual_duration > target_duration:
                # បើសំឡេងវែងជាងពេលវេលាកំណត់ -> បង្កើនល្បឿន
                speed_factor = actual_duration / target_duration
                if speed_factor > 1.5: # កុំបង្កើនល្បឿនខ្លាំងពេក នាំឱ្យសំឡេងខូច
                    speed_factor = 1.5
                segment_audio = speedup(segment_audio, playback_speed=speed_factor)
                
            # បន្ថែមសំឡេងទៅនឹង Timeline តាមពេលវេលាកំណត់
            # បើមានចន្លោះទទេ យើងបន្ថែមសំឡេងស្ងាត់
            if start_ms > len(final_audio):
                silence = AudioSegment.silent(duration=start_ms - len(final_audio))
                final_audio += silence
                
            final_audio = final_audio.overlay(segment_audio, position=start_ms)
            
            # លុបឯកសារបណ្តោះអាសន្ន
            os.remove(temp_file)
            
    # រក្សាទុកសំឡេងចុងក្រោយ
    final_audio.export(output_audio, format="mp3")
    
    # លុប Folder បណ្តោះអាសន្ន
    os.rmdir(temp_dir)
    
    print(f"✅ ជំហានទី៣ បញ្ចប់! ឯកសារសំឡេងរក្សាទុកនៅ៖ {output_audio}")
    return output_audio

# ==========================================
# ជំហានទី ៤៖ ភ្ជាប់វីដេអូ សំឡេង និងអក្សររត់
# ==========================================
def step4_merge_video(video_path, audio_path, srt_path, output_path="final_dubbed.mp4"):
    print("\n🎬 ជំហានទី៤៖ កំពុងភ្ជាប់វីដេអូ សំឡេង និងអក្សររត់...")
    
    try:
        input_video = ffmpeg.input(video_path)
        input_audio = ffmpeg.input(audio_path)
        
        # បញ្ចូល Subtitle ជា Filter (Hardcode) ដើម្បីឱ្យវាដិតនៅនឹងវីដេអូ
        # ចំណាំ៖ បើអ្នកចង់ដាក់អក្សររត់ជា Track ដោយឡែក (Softsub) កូដនឹងខុសគ្នាបន្តិច
        video_with_subs = input_video.video.filter('subtitles', srt_path)
        
        # ភ្ជាប់វីដេអូជាមួយសំឡេងថ្មី
        ffmpeg.output(
            video_with_subs, 
            input_audio, 
            output_path, 
            vcodec='libx264',  # Video Codec
            acodec='aac',      # Audio Codec
            shortest=None,     # បញ្ចប់នៅពេលវីដេអូខ្លីជាង
            overwrite_output=True
        ).run()
        
        print(f"🎉 ជំហានទី៤ បញ្ចប់! វីដេអូចុងក្រោយរក្សាទុកនៅ៖ {output_path}")
        return output_path
    except ffmpeg.Error as e:
        print(f"❌ FFmpeg Error: {e.stderr.decode() if e.stderr else str(e)}")
        return None

# ==========================================
# មុខងារចម្បង (Main Function)
# ==========================================
if __name__ == "__main__":
    # កំណត់ផ្លូវឯកសារ
    VIDEO_INPUT = "my_movie.mp4"  # ដាក់ឈ្មោះវីដេអូរបស់អ្នកនៅទីនេះ
    
    print("=" * 50)
    print("🎬 ចាប់ផ្តើមដំណើរការបកប្រែរឿងភាគ 🎬")
    print("=" * 50)
    
    # ជំហានទី ១: Transcribe
    srt_original = step1_transcribe(VIDEO_INPUT, "movie_original.srt")
    
    # ជំហានទី ២: បកប្រែជាភាសាខ្មែរ
    srt_translated = step2_translate(srt_original, target_lang="Khmer", output_srt="movie_khmer.srt")
    
    # ជំហានទី ៣: បង្កើតសំឡេងខ្មែរ
    if srt_translated:
        audio_dubbed = step3_dub_audio(srt_translated, "movie_khmer_dubbed.mp3")
        
        # ជំហានទី ៤: ភ្ជាប់វីដេអូចុងក្រោយ
        if audio_dubbed:
            final_video = step4_merge_video(
                video_path=VIDEO_INPUT,
                audio_path=audio_dubbed,
                srt_path=srt_translated,
                output_path="final_khmer_movie.mp4"
            )
            
            if final_video:
                print("\n" + "=" * 50)
                print("🎉 បញ្ចប់ដំណើរការទាំងអស់ដោយជោគជ័យ! 🎉")
                print(f"📁 វីដេអូចុងក្រោយ៖ {final_video}")
                print("=" * 50)