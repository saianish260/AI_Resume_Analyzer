import streamlit as st
import pdfplumber
from groq import Groq
from dotenv import load_dotenv
import os

# -----------------------------
# Load API Key
# -----------------------------
load_dotenv()

client = Groq(
    api_key=os.getenv("GROQ_API_KEY")
)

# -----------------------------
# App Title
# -----------------------------
st.title("🤖 AI Resume Analyzer & ATS Checker")

st.write(
    "Upload your resume and compare it with a job description using AI."
)

# -----------------------------
# Upload Resume
# -----------------------------
resume = st.file_uploader(
    "📄 Upload your Resume (PDF)",
    type=["pdf"]
)

# -----------------------------
# Job Description
# -----------------------------
job_description = st.text_area(
    "📝 Paste the Job Description"
)

# -----------------------------
# Analyze Resume
# -----------------------------
if st.button("🚀 Analyze Resume"):

    if resume is None:
        st.error("Please upload your resume.")
        st.stop()

    if job_description.strip() == "":
        st.error("Please paste the job description.")
        st.stop()

    # Read Resume PDF
    with pdfplumber.open(resume) as pdf:

        resume_text = ""

        for page in pdf.pages:

            extracted_text = page.extract_text()

            if extracted_text:
                resume_text += extracted_text + "\n"

    # AI Processing
    with st.spinner("Analyzing Resume..."):

        prompt = f"""
You are an expert ATS (Applicant Tracking System) Resume Analyzer.

Compare the following resume with the job description.

Resume:
{resume_text}

Job Description:
{job_description}

Generate a professional report in the following format:

# ATS Match Score
(Give score out of 100)

# Matching Skills

# Missing Skills

# Resume Strengths

# Resume Weaknesses

# Suggestions to Improve

# Final Verdict
Should the candidate apply for this job? Explain briefly.
"""

        try:

            response = client.chat.completions.create(
                model="llama-3.3-70b-versatile",
                messages=[
                    {
                        "role": "user",
                        "content": prompt
                    }
                ]
            )

            st.success("✅ Resume analyzed successfully!")

            st.markdown(response.choices[0].message.content)

        except Exception as e:
            st.error(f"AI Error: {e}")

# -----------------------------
# Footer
# -----------------------------
st.markdown("---")
st.caption("Developed by Sai Anish | Powered by Groq AI")