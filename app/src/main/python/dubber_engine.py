import os
import re
import json
import asyncio
import google.generativeai as genai
import edge_tts

# Helper function to convert srt time
def srt_time_to_ms(time_str):
    hours, minutes, seconds = time_str.replace(',', '.').split(':')
    return int((int(hours) * 3600 + int(minutes) * 60 + float(seconds)) * 1000)

def ms_to_srt_time(ms):
    seconds, ms = divmod(ms, 1000)
    minutes, seconds = divmod(seconds, 60)
    hours, minutes = divmod(minutes, 60)
    return f"{int(hours):02}:{int(minutes):02}:{int(seconds):02},{int(ms):03}"

def step2_translate_gemini(srt_content, api_key, target_lang="Khmer"):
    genai.configure(api_key=api_key)
    model = genai.GenerativeModel('gemini-1.5-flash')
    
    prompt = f"""
    You are a professional movie translator. Translate the following SRT subtitle content into {target_lang}.
    Keep the exact same SRT format (numbers, timestamps) but translate the text.
    Only return the translated SRT content, no extra explanations, no markdown blocks.
    
    SRT Content:
    {srt_content}
    """
    
    response = model.generate_content(prompt)
    translated_text = response.text.replace("```srt", "").replace("```", "").strip()
    return translated_text

async def generate_tts_async(text, voice, output_path):
    communicate = edge_tts.Communicate(text, voice)
    await communicate.save(output_path)

def generate_voice(text, voice, output_path):
    asyncio.run(generate_tts_async(text, voice, output_path))
    return os.path.exists(output_path)
