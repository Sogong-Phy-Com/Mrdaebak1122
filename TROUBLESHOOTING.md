# 500 Error Troubleshooting Guide

## Error Cause

The 500 error on `/api/voice-orders/start` endpoint is usually caused by:

1. **OpenAI API key not configured** (most common)
2. Invalid OpenAI API key
3. Network connection issues

## Solution

### 1. Check Environment Variables in Render Dashboard

1. Go to Render Dashboard: https://dashboard.render.com
2. Select `mrdabak-dinner-service` service
3. Go to **Environment** tab
4. Verify these environment variables are set:
   - `VOICE_LLM_API_KEY`: OpenAI API key (REQUIRED!)
   - `VOICE_LLM_API_URL`: `https://api.openai.com/v1/chat/completions` (default)
   - `VOICE_LLM_MODEL`: `gpt-4o-mini` (default)

### 2. Set OpenAI API Key

**If you don't have an API key:**
1. Go to OpenAI Dashboard: https://platform.openai.com/api-keys
2. Click "Create new secret key"
3. Enter a name and create
4. Copy the generated key (shown only once!)

**Set in Render:**
1. Render Dashboard → Environment tab
2. Click "Add Environment Variable"
3. Key: `VOICE_LLM_API_KEY`
4. Value: Paste your OpenAI API key
5. Click "Save Changes"

### 3. Redeploy Service

After adding/updating environment variables:
1. Click "Manual Deploy" in Render Dashboard
2. Select "Deploy latest commit"
3. Wait for deployment to complete

### 4. Verify

After deployment:
1. Open voice order page in browser
2. Click "Start Voice Order" button
3. Verify 500 error is gone and it works normally

## Current APIs in Use

- **STT (Speech Recognition)**: Web Speech API (browser-side, no server needed)
- **LLM (Conversational AI)**: OpenAI GPT-4o-mini (external API, requires API key)

## Notes

- API key must start with `sk-`
- Make sure there are no spaces or special characters in the API key
- Verify your OpenAI account has sufficient credits

## 429 Error (Rate Limit / Quota Exceeded)

If you see a 429 error with "insufficient_quota":

1. **Check OpenAI Billing**: https://platform.openai.com/account/billing
2. **Add Credits**: Go to "Payment methods" and add credits to your account
3. **Check Usage**: Go to "Usage" to see your current usage and limits
4. **Wait**: If it's a rate limit (not quota), wait a few minutes and try again

Common causes:
- **No payment method added** - Even if you set a budget, you MUST add a payment method to use the API
- Free tier credits exhausted
- Monthly spending limit reached
- API key belongs to a different account than the one you're viewing

**Important**: If you see $0 usage but still get "insufficient_quota" error:
1. Check if you have a payment method added (not just a budget set)
2. Verify the API key in Render matches the account you're viewing
3. Try creating a new API key and updating it in Render

