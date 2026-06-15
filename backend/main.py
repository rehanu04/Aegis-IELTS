import os
import io
import math
import logging
import asyncio
import base64
from typing import List, Optional
from fastapi import FastAPI, HTTPException, status, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel, Field, field_validator
from google import genai
from google.genai import types
from google.genai.errors import APIError

# --- Logging Configuration ---
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("aegis-ielts-backend")

app = FastAPI(
    title="Aegis IELTS Evaluation Gateway",
    description="Official 2026 FastAPI gateway for Aegis IELTS Speaking and Writing assessments",
    version="1.0.0"
)

# Enable CORS for local Android Emulator (10.0.2.2) and local debugging
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Initialize Google GenAI SDK Client ---
api_key = os.getenv("GEMINI_API_KEY")
client = None

if not api_key:
    logger.warning("GEMINI_API_KEY environment variable is missing. The gateway will run in LOCAL MOCK FALLBACK mode.")
else:
    try:
        # Initializing the 2026 Google GenAI client
        client = genai.Client(api_key=api_key)
        logger.info("Google GenAI Client successfully initialized.")
    except Exception as e:
        logger.error(f"Failed to initialize Google GenAI Client: {e}. Falling back to local mock.")

# ─── Pydantic Validation Schemas ───────────────────────────────────────────────

class SpeakingGradeRequest(BaseModel):
    audio_base64: str = Field(..., description="Base64 encoded PCM/WAV audio bytes")
    transcript: str = Field(..., description="Candidate STT transcript block")
    prompts: List[str] = Field(..., description="Ordered list of examiner questions presented")

class WritingGradeRequest(BaseModel):
    task_type: int = Field(..., ge=1, le=2, description="1 = Academic Task 1, 2 = Academic Task 2")
    prompt: str = Field(..., description="Prompt instructions shown to candidate")
    essay: str = Field(..., description="Candidate written essay body text")

class BandCalculationRequest(BaseModel):
    scores: List[float] = Field(..., description="Component IELTS scores (0.0 to 9.0)")

    @field_validator("scores")
    @classmethod
    def validate_scores(cls, v: List[float]) -> List[float]:
        if not v:
            raise ValueError("Scores list cannot be empty")
        for score in v:
            if not (0.0 <= score <= 9.0):
                raise ValueError(f"Score {score} must be between 0.0 and 9.0")
            # Verify 0.5 steps
            if (score * 2.0) != int(score * 2.0):
                raise ValueError(f"Score {score} must be in exact 0.5 steps (e.g. 6.0, 6.5, 7.0)")
        return v

# --- Nested Speaking Telemetry Responses ---

class HesitationProfile(BaseModel):
    within_clause_pauses: int = Field(..., alias="withinClausePauses", description="Count of unnatural pauses inside syntactic clauses")
    between_clause_pauses: int = Field(..., alias="betweenClausePauses", description="Count of normal pauses between clauses")
    total_silence_ms: int = Field(..., alias="totalSilenceMs", description="Total silent interval in milliseconds")

    class Config:
        populate_by_name = True

class FluencyCoherenceMetric(BaseModel):
    score: float = Field(..., ge=0.0, le=9.0)
    feedback: str
    hesitation_profile: HesitationProfile = Field(..., alias="hesitationProfile")
    filler_density_index: float = Field(..., ge=0.0, alias="fillerDensityIndex", description="Filler word frequency per 100 words")

    class Config:
        populate_by_name = True

class LexicalAssessmentMetric(BaseModel):
    score: float = Field(..., ge=0.0, le=9.0)
    feedback: str
    lexical_asymmetry_index: float = Field(..., ge=0.0, alias="lexicalAsymmetryIndex")

    class Config:
        populate_by_name = True

class GrammarAssessmentMetric(BaseModel):
    score: float = Field(..., ge=0.0, le=9.0)
    feedback: str

class PronunciationAssessmentMetric(BaseModel):
    score: float = Field(..., ge=0.0, le=9.0)
    feedback: str

class SpeakingAssessmentResponse(BaseModel):
    fluency_coherence: FluencyCoherenceMetric = Field(..., alias="fluencyCoherence")
    coherence_feedback: str = Field(default="Ideas are logically structured with appropriate cohesive devices.", alias="coherenceFeedback")
    lexical_resource: LexicalAssessmentMetric = Field(..., alias="lexicalResource")
    grammatical_range_accuracy: GrammarAssessmentMetric = Field(..., alias="grammaticalRangeAccuracy")
    pronunciation: PronunciationAssessmentMetric = Field(..., alias="pronunciation")
    overall_feedback: str = Field(..., alias="overallFeedback")

    class Config:
        populate_by_name = True

# --- Nested Writing Telemetry Responses ---

class WritingCriteriaScores(BaseModel):
    task_achievement: float = Field(..., ge=0.0, le=9.0, alias="taskAchievement")
    coherence_cohesion: float = Field(..., ge=0.0, le=9.0, alias="coherenceCohesion")
    lexical_resource: float = Field(..., ge=0.0, le=9.0, alias="lexicalResource")
    grammatical_range_accuracy: float = Field(..., ge=0.0, le=9.0, alias="grammaticalRangeAccuracy")

    class Config:
        populate_by_name = True

class TemplateDetectionResult(BaseModel):
    template_detected: bool = Field(..., alias="templateDetected")
    template_similarity_score: float = Field(..., ge=0.0, le=1.0, alias="templateSimilarityScore")
    lexical_asymmetry_index: float = Field(..., ge=0.0, alias="lexicalAsymmetryIndex")

    class Config:
        populate_by_name = True

class GrammarCorrection(BaseModel):
    original: str
    corrected: str
    explanation: str
    error_type: str = Field(..., alias="errorType")

    class Config:
        populate_by_name = True

class WritingAssessmentResponse(BaseModel):
    criteria_scores: WritingCriteriaScores = Field(..., alias="criteriaScores")
    template_detection: TemplateDetectionResult = Field(..., alias="templateDetection")
    grammar_corrections: List[GrammarCorrection] = Field(default=[], alias="grammarCorrections")
    overall_feedback: str = Field(..., alias="overallFeedback")

    class Config:
        populate_by_name = True

# ─── REST Routing Endpoints ───────────────────────────────────────────────────

@app.get("/api/v1/health")
async def health_check():
    """Lightweight server heartbeat for waking up Render."""
    return {"status": "synchronized"}

@app.post("/api/v1/grade/writing", response_model=WritingAssessmentResponse)
async def grade_writing(request: WritingGradeRequest):
    """
    Grades an IELTS essay by stripping templates, checking word count,
    and evaluating task criteria using gemini-2.5-pro.
    """
    logger.info(f"Received writing grade request for Task {request.task_type}")

    words = request.essay.split()
    if not request.essay or len(words) < 10:
        logger.info("Empty or short essay (under 10 words) detected. Returning zero band scores.")
        return WritingAssessmentResponse(
            criteriaScores=WritingCriteriaScores(taskAchievement=0.0, coherenceCohesion=0.0, lexicalResource=0.0, grammaticalRangeAccuracy=0.0),
            templateDetection=TemplateDetectionResult(templateDetected=False, templateSimilarityScore=0.0, lexicalAsymmetryIndex=0.0),
            grammarCorrections=[],
            overallFeedback=f"Band 0.0: Non-attempt / Under 10 rateable words. The response contains only {len(words)} words."
        )

    system_instruction = """
    You are a certified IELTS Writing examiner. Evaluate the essay response.

    First, identify and strip any formulaic essay template sentences (e.g., memorized introductory or transitional sentences like "It is often argued that", "This essay will discuss both sides").
    Count the words in the remaining organic content. If it is below the minimum limit (150 for Task 1, 250 for Task 2), penalize the Task Achievement score.

    Assess template usage: set template_detected=true if formulaic memorized structures are used, and estimate the template_similarity_score (0.0 = completely original, 1.0 = pure template).
    Identify grammar errors and provide corrections.
    """

    user_prompt = f"""
    Evaluate this IELTS Writing Task {request.task_type} response.
    Task Prompt: "{request.prompt}"
    Candidate Essay: "{request.essay}"
    """

    if client:
        try:
            # Enforcing strict Pydantic schema using Google GenAI SDK config
            response = client.models.generate_content(
                model="gemini-2.5-pro",
                contents=user_prompt,
                config=types.GenerateContentConfig(
                    system_instruction=system_instruction,
                    temperature=0.1,
                    response_mime_type="application/json",
                    response_schema=WritingAssessmentResponse
                )
            )

            if response.text:
                parsed = WritingAssessmentResponse.model_validate_json(response.text)
                return JSONResponse(content=parsed.model_dump(by_alias=True))
            else:
                raise HTTPException(
                    status_code=status.HTTP_502_BAD_GATEWAY,
                    detail="Gemini API returned an empty text payload."
                )

        except APIError as api_err:
            logger.error(f"Gemini API Error: {api_err}. Running local fallback.")
        except Exception as e:
            logger.error(f"Error during Gemini generation: {e}. Running local fallback.")

    # Local Fallback
    return local_writing_assessment(request.task_type, request.essay)


@app.post("/api/v1/grade/speaking", response_model=SpeakingAssessmentResponse)
async def grade_speaking(request: SpeakingGradeRequest):
    logger.info("Received speaking grade request")
    words = request.transcript.split()
    if not request.transcript or len(words) < 5:
        logger.info("Empty or short transcript (under 5 words) detected. Returning zero band scores.")
        return SpeakingAssessmentResponse(
            fluencyCoherence=FluencyCoherenceMetric(
                score=0.0,
                feedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {len(words)} words.",
                hesitationProfile=HesitationProfile(withinClausePauses=0, betweenClausePauses=0, totalSilenceMs=0),
                fillerDensityIndex=0.0
            ),
            lexicalResource=LexicalAssessmentMetric(score=0.0, feedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {len(words)} words.", lexicalAsymmetryIndex=0.0),
            grammaticalRangeAccuracy=GrammarAssessmentMetric(score=0.0, feedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {len(words)} words."),
            pronunciation=PronunciationAssessmentMetric(score=0.0, feedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {len(words)} words."),
            overallFeedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {len(words)} words."
        )

    system_instruction = """
    You are a certified IELTS Speaking examiner. Evaluate the candidate's performance
    across all 4 official IELTS Speaking criteria, adhering to the strict 2026 structural metrics.

    Score each criterion on the official 0.0-9.0 scale with 0.5 precision.

    Analyze the hesitation profile:
    - within_clause_pauses: Unnatural pauses that occur inside grammatical clauses (not between clauses).
    - between_clause_pauses: Natural transition pauses between clauses.
    - total_silence_ms: Sum of silence intervals.

    Compute the filler density index: count of filler words (um, ah, like, you know) per 100 words.
    Compute the lexical asymmetry index: imbalance in vocabulary complexity (ratio of complex to simple words).

    Enforce hesitation penalties: if within_clause_pauses is high, fluency score must be penalized.
    """

    user_prompt = f"""
    Evaluate the speaking session:
    Examiner Prompts: {request.prompts}
    Candidate Transcript: "{request.transcript}"
    """

    if client:
        try:
            response = client.models.generate_content(
                model="gemini-1.5-flash",
                contents=user_prompt,
                config=types.GenerateContentConfig(
                    system_instruction=system_instruction,
                    temperature=0.3,
                    response_mime_type="application/json",
                    response_schema=SpeakingAssessmentResponse
                )
            )

            if response.text:
                parsed = SpeakingAssessmentResponse.model_validate_json(response.text)
                return JSONResponse(content=parsed.model_dump(by_alias=True))
            else:
                raise HTTPException(
                    status_code=status.HTTP_502_BAD_GATEWAY,
                    detail="Gemini API returned an empty text payload."
                )

        except APIError as api_err:
            logger.error(f"Gemini API Error: {api_err}. Running local fallback.")
        except Exception as e:
            logger.error(f"Error during Gemini generation: {e}. Running local fallback.")

    # Local Fallback
    return local_speaking_assessment(request.transcript)


@app.post("/api/v1/calculate-band")
async def calculate_band(request: BandCalculationRequest):
    """
    Validates individual component scores and implements the official rounding formula:
    rounded = math.floor((2.0 * raw_average) + 0.5) / 2.0
    """
    logger.info(f"Received band calculation request for scores: {request.scores}")
    raw_average = sum(request.scores) / len(request.scores)
    rounded = math.floor((2.0 * raw_average) + 0.5) / 2.0
    return {
        "raw_average": round(raw_average, 3),
        "band_score": rounded
    }


class ListeningAudioRequest(BaseModel):
    section_number: int = Field(..., ge=1, le=4, description="1 to 4")
    environment_label: str = Field(..., description="Environment label")
    environment_description: str = Field(..., description="Environment description")
    accent_label: str = Field(..., description="Accent label")


class SpeakingNextQuestionRequest(BaseModel):
    audio_base64: Optional[str] = Field(default=None, description="Base64 encoded PCM/WAV audio bytes")
    previous_transcript: Optional[str] = Field(default=None, description="Candidate STT transcript block")
    current_question_index: int = Field(..., description="Index of the question just answered")
    current_part: int = Field(..., description="Active IELTS Part (1, 2, or 3)")
    prompts: Optional[List[str]] = Field(default=[], description="List of previous prompts")
    transcripts: Optional[List[str]] = Field(default=[], description="List of previous transcripts")


class SpeakingNextQuestionResponse(BaseModel):
    transcript: str = Field(description="The decoded transcript or conversational acknowledgment.")
    next_question: str = Field(description="The dynamic, context-aware next question for the candidate.")
    next_part: int = Field(description="The active IELTS Part index (1, 2, or 3).")
    next_question_index: int = Field(description="The index counter for tracking the conversational turn.")
    is_test_complete: bool = Field(description="Flag to mark when all conversational parts are officially concluded.")


@app.get("/api/v1/listening/stream")
async def listening_stream(
    section_number: int,
    accent: str,
    environment_label: Optional[str] = None,
    environment_description: Optional[str] = None,
    is_pool_b: Optional[bool] = False
):
    logger.info(f"Streaming audio request: Section {section_number}, Accent {accent}, Pool B={is_pool_b}")
    audio_bytes = None
    
    if client and os.getenv("GEMINI_API_KEY"):
        env_label = environment_label or "Social Dialogue"
        env_desc = environment_description or "General conversation"
        
        system_instruction = f"""
        You are an IELTS Listening examiner.
        Generate a continuous, realistic IELTS Listening monologue or dialogue script for Section {section_number} ({env_label}: {env_desc}).
        The speaker must speak in a realistic {accent} accent.
        Speak clearly and slowly, introducing the section context and reading a monologue/dialogue of about 150 words.
        Do not output any text or markdown. Generate only the spoken audio.
        """
        
        user_prompt = f"Please read the continuous {accent} narrative for Section {section_number}."
        
        try:
            response = client.models.generate_content(
                model="gemini-1.5-flash",
                contents=user_prompt,
                config=types.GenerateContentConfig(
                    system_instruction=system_instruction,
                    temperature=0.7,
                    response_modalities=["AUDIO"]
                )
            )
            
            if response.candidates and response.candidates[0].content.parts:
                for part in response.candidates[0].content.parts:
                    if part.inline_data:
                        audio_bytes = part.inline_data.data
                        media_type_header = "audio/wav"
                        break
        except Exception as e:
            logger.error(f"Error generating streaming audio via Gemini: {e}")
            
    if not audio_bytes:
        # Fallback to local preset file streaming
        suffix = "_b" if is_pool_b else ""
        filename = f"backend/audio/listening_sec{section_number}{suffix}.mp3"
        if not os.path.exists(filename):
            filename = f"backend/audio/listening_sec{section_number}.mp3"
            
        if os.path.exists(filename):
            with open(filename, "rb") as f:
                audio_bytes = f.read()
            media_type_header = "audio/mpeg"
        else:
            logger.error(f"Local audio asset not found: {filename}")
            raise HTTPException(status_code=404, detail="Audio resource not found")

    # Paced streaming chunks generator (approx 50 KB/s)
    async def paced_generator():
        chunk_size = 4096
        for i in range(0, len(audio_bytes), chunk_size):
            yield audio_bytes[i:i+chunk_size]
            await asyncio.sleep(0.08)
            
    headers = {"Content-Length": str(len(audio_bytes))}
    return StreamingResponse(paced_generator(), media_type=media_type_header, headers=headers)


@app.post("/api/v1/speaking/next-question", response_model=SpeakingNextQuestionResponse)
async def speaking_next_question(request: SpeakingNextQuestionRequest):
    logger.info(f"Speaking next-question request: Index {request.current_question_index}, Part {request.current_part}")
    
    transcript = request.previous_transcript or ""
    if request.audio_base64 and not transcript:
        try:
            audio_bytes = base64.b64decode(request.audio_base64)
            audio_part = types.Part.from_bytes(data=audio_bytes, mime_type="audio/wav")
            
            if client and os.getenv("GEMINI_API_KEY"):
                response = client.models.generate_content(
                    model="gemini-1.5-flash",
                    contents=[
                        audio_part,
                        "Transcribe the spoken words in this audio clearly. Do not add any extra text or comments. Just return the transcription."
                    ]
                )
                transcript = response.text.strip() if response.text else ""
            else:
                transcript = ""
        except Exception as e:
            logger.error(f"Error transcribing audio: {e}")
            transcript = ""

    # Mock fallback for empty transcript
    dummy_transcripts = [
        "My name is John Doe. I am taking the IELTS test to study abroad.",
        "I am from a small town in the countryside, and currently I am working as a junior software engineer.",
        "In my free time, I really enjoy reading books and playing tennis with my friends.",
        "I would like to describe the movie Inception. It had a strong influence on me because of its unique concept of dreams within dreams and how it explores sub-consciousness. I saw it a few years ago, and it really changed the way I think about storytelling.",
        "In my opinion, movies have become much more visual-effects-driven now compared to the past when character development and storyline were more important.",
        "I think films should primarily entertain, but having some educational or thought-provoking value makes them much more memorable and impactful.",
        "Local films often have very limited budgets and tackle cultural themes that might not translate well to global audiences compared to big budget productions."
    ]
    if not transcript:
        if request.current_question_index < len(dummy_transcripts):
            transcript = dummy_transcripts[request.current_question_index]
        else:
            transcript = "Yes, I agree with that point."

    part1_questions = [
        "Welcome to the IELTS speaking test. Can you tell me your full name, please?",
        "Where are you from, and do you work or study?",
        "Let's talk about your free time. What hobbies do you enjoy the most?"
    ]
    part2_question = "Describe a book or a movie that had a strong influence on you. You should say what it is, when you saw/read it, and explain why it influenced you."
    part3_questions = [
        "In your opinion, how has the type of movies people watch changed over the past few decades?",
        "Do you think films should always have educational value, or is entertainment enough?",
        "Why do you think some local films fail to attract a global audience compared to big budget productions?"
    ]
    all_questions = part1_questions + [part2_question] + part3_questions
    
    response_obj = None
    if client and os.getenv("GEMINI_API_KEY"):
        system_instruction = """You are a Certified Senior, Professional IELTS Academic Oral Examiner conducting a true two-way human dialogue.
Your conversation flow must adapt dynamically to the candidate's speech transcript.

The standard exam itinerary contains these 7 questions:
Part 1:
0. "Welcome to the IELTS speaking test. Can you tell me your full name, please?"
1. "Where are you from, and do you work or study?"
2. "Let's talk about your free time. What hobbies do you enjoy the most?"
Part 2:
3. "Describe a book or a movie that had a strong influence on you. You should say what it is, when you saw/read it, and explain why it influenced you."
Part 3:
4. "In your opinion, how has the type of movies people watch changed over the past few decades?"
5. "Do you think films should always have educational value, or is entertainment enough?"
6. "Why do you think some local films fail to attract a global audience compared to big budget productions?"

CRITICAL BEHAVIOR:
1. Clean up and transcribe/normalize the candidate's latest response.
2. Critically read the candidate's full response. If the candidate proactively supplies information (e.g. stating name, city, department, graduation CGPA, and college specialization) that matches upcoming generic introductory prompts, you must dynamically skip/strike off those redundant questions from your itinerary. Never ask a candidate for information they have already provided.
3. You must "catch" something contextually specific from what the candidate said, comment on it naturally (e.g. acknowledging an engineering specialization, or their location in Bangalore), and formulate a tailored follow-up question before routing back to the core thematic modules.
4. Identify the next unanswered question in the sequence:
   - If there are unanswered questions in Part 1 (indices 0, 1, 2), ask the next unanswered one. Set next_part=1, next_question_index=<index>.
   - If all Part 1 questions are answered, transition to Part 2 (index 3). Set next_part=2, next_question_index=3.
   - If Part 2 is answered, transition to Part 3 (indices 4, 5, 6). Ask the next unanswered one. Set next_part=3, next_question_index=<index>.
   - If all questions (including Part 3) are answered, set is_test_complete=true.
5. If is_test_complete is true, set next_question to "Thank you. That is the end of the speaking test." and set next_part=3, next_question_index=6.

Return a JSON object conforming exactly to the response schema."""
        
        try:
            # Reconstruct chat history using types.Content
            history = []
            
            # Match past turns
            for i in range(len(request.transcripts or [])):
                if i < len(request.prompts or []):
                    history.append(types.Content(
                        role="model",
                        parts=[types.Part(text=request.prompts[i])]
                    ))
                history.append(types.Content(
                    role="user",
                    parts=[types.Part(text=request.transcripts[i])]
                ))
                
            # Current prompt asked before the candidate spoke
            if len(request.prompts or []) > len(request.transcripts or []):
                history.append(types.Content(
                    role="model",
                    parts=[types.Part(text=request.prompts[-1])]
                ))

            contents = list(history)
            contents.append(types.Content(
                role="user",
                parts=[types.Part(text=f"Candidate response: {transcript}. Current index: {request.current_question_index}, Part: {request.current_part}.")]
            ))

            response = client.models.generate_content(
                model="gemini-1.5-flash",
                contents=contents,
                config=types.GenerateContentConfig(
                    system_instruction=system_instruction,
                    temperature=0.7,
                    response_mime_type="application/json",
                    response_schema=SpeakingNextQuestionResponse
                )
            )

            if response.text:
                response_obj = SpeakingNextQuestionResponse.model_validate_json(response.text)
        except Exception as e:
            logger.error(f"Error generating dynamic follow-up: {e}")
            
    if response_obj:
        return response_obj
    else:
        # Local fallback
        next_idx = request.current_question_index + 1
        if next_idx >= len(all_questions):
            return SpeakingNextQuestionResponse(
                transcript=transcript,
                next_question="Thank you. That is the end of the speaking test.",
                next_part=3,
                next_question_index=6,
                is_test_complete=True
            )
        else:
            next_part = 1 if next_idx in [0, 1, 2] else (2 if next_idx == 3 else 3)
            return SpeakingNextQuestionResponse(
                transcript=transcript,
                next_question=all_questions[next_idx],
                next_part=next_part,
                next_question_index=next_idx,
                is_test_complete=False
            )


@app.post("/api/v1/generate-listening-audio")
async def generate_listening_audio(request: ListeningAudioRequest):
    logger.info(f"Generating listening audio for Section {request.section_number} ({request.accent_label})")
    
    if not client or not os.getenv("GEMINI_API_KEY"):
        logger.info("GEMINI_API_KEY missing. Returning fallback asset payload.")
        fallback_file = f"audio/listening_sec{request.section_number}.mp3"
        return JSONResponse(
            status_code=200,
            content={
                "fallback_to_local": True,
                "local_asset_path": fallback_file
            }
        )

    system_instruction = f"""
    You are an IELTS Listening examiner.
    Generate a continuous, realistic IELTS Listening monologue or dialogue script for Section {request.section_number} ({request.environment_label}: {request.environment_description}).
    The speaker must speak in a realistic {request.accent_label} accent.
    Speak clearly and slowly, introducing the section context and reading a monologue/dialogue of about 150 words.
    Do not output any text or markdown. Generate only the spoken audio.
    """
    
    user_prompt = f"Please read the continuous {request.accent_label} narrative for Section {request.section_number}."
    
    try:
        # We use gemini-1.5-flash which supports response_modalities=["AUDIO"]
        response = client.models.generate_content(
            model="gemini-1.5-flash",
            contents=user_prompt,
            config=types.GenerateContentConfig(
                system_instruction=system_instruction,
                temperature=0.7,
                response_modalities=["AUDIO"]
            )
        )
        
        audio_bytes = None
        if response.candidates and response.candidates[0].content.parts:
            for part in response.candidates[0].content.parts:
                if part.inline_data:
                    audio_bytes = part.inline_data.data
                    break
        
        if audio_bytes:
            logger.info("Successfully generated listening audio via Gemini API")
            return Response(content=audio_bytes, media_type="audio/mp3")
        else:
            logger.warning("Gemini API did not return audio inline_data. Falling back to local synthesis.")
    except Exception as e:
        logger.error(f"Error generating audio via Gemini: {e}. Falling back to local synthesis.")
            
    # Local fallback
    logger.info("Generating fallback local asset payload due to generation failure")
    fallback_file = f"audio/listening_sec{request.section_number}.mp3"
    return JSONResponse(
        status_code=200,
        content={
            "fallback_to_local": True,
            "local_asset_path": fallback_file
        }
    )


# ─── Mock Fallback Engines ────────────────────────────────────────────────────

def local_writing_assessment(task_type: int, essay: str) -> WritingAssessmentResponse:
    logger.info("Running local mock writing evaluation fallback")
    words = essay.strip().split()
    word_count = len(words)
    min_words = 150 if task_type == 1 else 250

    if word_count < 10:
        return WritingAssessmentResponse(
            criteriaScores=WritingCriteriaScores(taskAchievement=0.0, coherenceCohesion=0.0, lexicalResource=0.0, grammaticalRangeAccuracy=0.0),
            templateDetection=TemplateDetectionResult(templateDetected=False, templateSimilarityScore=0.0, lexicalAsymmetryIndex=0.0),
            grammarCorrections=[],
            overallFeedback=f"Band 0.0: Non-attempt / Under 10 rateable words. The response contains only {word_count} words."
        )

    word_fraction = min(1.0, word_count / min_words) if min_words > 0 else 1.0
    ta_score = round(word_fraction * 7.5 * 2) / 2.0
    ta_score = max(1.0, min(9.0, ta_score))

    unique_words = len(set(w.lower() for w in words))
    diversity = unique_words / word_count if word_count > 0 else 0
    lex_score = round(diversity * 10.0 * 2) / 2.0
    lex_score = max(1.0, min(9.0, lex_score))

    cc_score = 6.0 if word_count > 50 else 3.0
    gra_score = 6.5 if word_count > 100 else 2.5

    # Check template usage
    templates = ["it is often argued that", "on the one hand", "on the other hand", "in conclusion"]
    lower_essay = essay.lower()
    matches = sum(1 for t in templates if t in lower_essay)
    similarity = min(1.0, matches / len(templates))
    template_detected = similarity > 0.35

    feedback = f"Local Mock Evaluation: Essay analyzed with {word_count} words. "
    if word_count < min_words:
        feedback += f"Word count falls short of the required {min_words} minimum limit, which reduced your Task Achievement score."
    else:
        feedback += "Word count requirement met."

    corrections = []
    if word_count > 10:
        corrections.append(
            GrammarCorrection(
                original="is be",
                corrected="is",
                explanation="Auxiliary duplication: avoid placing base form be directly after is.",
                errorType="Verb Agreement"
            )
        )

    return WritingAssessmentResponse(
        criteriaScores=WritingCriteriaScores(
            taskAchievement=ta_score,
            coherenceCohesion=cc_score,
            lexicalResource=lex_score,
            grammaticalRangeAccuracy=gra_score
        ),
        templateDetection=TemplateDetectionResult(
            templateDetected=template_detected,
            templateSimilarityScore=similarity,
            lexicalAsymmetryIndex=0.25
        ),
        grammarCorrections=corrections,
        overallFeedback=feedback
    )


def local_speaking_assessment(transcript: str) -> SpeakingAssessmentResponse:
    logger.info("Running local mock speaking evaluation fallback")
    words = transcript.strip().split()
    word_count = len(words)
    if word_count < 5:
        return SpeakingAssessmentResponse(
            fluencyCoherence=FluencyCoherenceMetric(
                score=0.0,
                feedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {word_count} words.",
                hesitationProfile=HesitationProfile(withinClausePauses=0, betweenClausePauses=0, totalSilenceMs=0),
                fillerDensityIndex=0.0
            ),
            lexicalResource=LexicalAssessmentMetric(score=0.0, feedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {word_count} words.", lexicalAsymmetryIndex=0.0),
            grammaticalRangeAccuracy=GrammarAssessmentMetric(score=0.0, feedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {word_count} words."),
            pronunciation=PronunciationAssessmentMetric(score=0.0, feedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {word_count} words."),
            overallFeedback=f"Band 0.0: Non-attempt / Under 5 rateable words. The response contains only {word_count} words."
        )

    # Detect hesitation pauses and fillers
    fillers = ["um", "ah", "like", "you know"]
    lower_trans = transcript.lower()
    filler_count = sum(lower_trans.count(f) for f in fillers)
    filler_density = (filler_count / word_count * 100) if word_count > 0 else 0.0

    within_pauses = lower_trans.count("[pause]")
    between_pauses = max(0, int(word_count / 15) - within_pauses)

    fluency = max(1.0, min(9.0, round((7.0 - (within_pauses * 0.5)) * 2) / 2.0))
    lexical = 6.5 if word_count > 30 else 3.0
    grammar = 6.0 if word_count > 40 else 2.5
    pron = 7.0 if word_count > 20 else 3.0

    return SpeakingAssessmentResponse(
        fluencyCoherence=FluencyCoherenceMetric(
            score=fluency,
            feedback="Speech exhibits cohesive flow with normal pauses, but hesitation markers are present.",
            hesitationProfile=HesitationProfile(
                withinClausePauses=within_pauses,
                betweenClausePauses=between_pauses,
                totalSilenceMs=within_pauses * 800
            ),
            fillerDensityIndex=filler_density
        ),
        coherenceFeedback="Candidate presents points in a structured format with clear transitions.",
        lexicalResource=LexicalAssessmentMetric(
            score=lexical,
            feedback="Vocabulary is sufficient to discuss general topics, with some repetitive lexical choices.",
            lexicalAsymmetryIndex=0.3
        ),
        grammaticalRangeAccuracy=GrammarAssessmentMetric(
            score=grammar,
            feedback="Maintains reasonable sentence patterns, with minor syntax errors under speech pressure."
        ),
        pronunciation=PronunciationAssessmentMetric(
            score=pron,
            feedback="Vowel clarity is consistent; word stress is accurate overall."
        ),
        overallFeedback="Local Mock Evaluation: Candidate transcript evaluated successfully."
    )
