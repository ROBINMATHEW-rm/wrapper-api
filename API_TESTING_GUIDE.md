# API Testing Guide - Step by Step

## Prerequisites

1. **Start the application:**
   ```bash
   mvn spring-boot:run
   ```
   
2. **Wait for this message:**
   ```
   Started WrapperApiApplication in X seconds
   ```

3. **Keep the application running** while testing

---

## Test 1: Upload a PDF Document

### Using Postman:
1. Open Postman
2. Create a new request
3. Set method to **POST**
4. URL: `http://localhost:8080/api/rag/upload`
5. Go to **Body** tab
6. Select **form-data**
7. Add key: `file` (change type to **File**)
8. Click "Select Files" and choose a PDF
9. Click **Send**

### Using curl (PowerShell):
```powershell
curl -X POST http://localhost:8080/api/rag/upload -F "file=@C:\path\to\your\document.pdf"
```

### Expected Response:
```json
{
  "message": "Document uploaded successfully",
  "documentId": "abc-123-def-456",
  "chunksStored": 25
}
```

**✅ Save the `documentId` - you'll need it for the next tests!**

---

## Test 2: List All Documents

### Using Postman:
1. Create a new request
2. Set method to **GET**
3. URL: `http://localhost:8080/api/rag/documents`
4. Click **Send**

### Using curl:
```powershell
curl http://localhost:8080/api/rag/documents
```

### Expected Response:
```json
{
  "documents": [
    {
      "documentId": "abc-123-def-456",
      "filename": "document.pdf",
      "chunkCount": 25,
      "uploadedAt": "2026-02-20T10:30:00"
    }
  ]
}
```

---

## Test 3: Ask a Question (Main Endpoint)

### Using Postman:
1. Create a new request
2. Set method to **POST**
3. URL: `http://localhost:8080/api/rag/ask`
4. Go to **Body** tab
5. Select **raw** and **JSON**
6. Paste this JSON (replace `documentId` with yours):
```json
{
  "question": "What is this document about?",
  "documentId": "abc-123-def-456",
  "topK": 5,
  "threshold": 0.3
}
```
7. Click **Send**

### Using curl:
```powershell
curl -X POST http://localhost:8080/api/rag/ask -H "Content-Type: application/json" -d '{\"question\": \"What is this document about?\", \"documentId\": \"abc-123-def-456\", \"topK\": 5, \"threshold\": 0.3}'
```

### Expected Response:
```json
{
  "answer": "Based on the document, it discusses...",
  "sources": [
    "Relevant chunk 1 from the document...",
    "Relevant chunk 2 from the document..."
  ]
}
```

---

## Test 4: Ask Without Specifying Document (Search All)

### Using Postman:
1. Same as Test 3, but use this JSON:
```json
{
  "question": "What are the main topics?",
  "topK": 5,
  "threshold": 0.3
}
```

### Using curl:
```powershell
curl -X POST http://localhost:8080/api/rag/ask -H "Content-Type: application/json" -d '{\"question\": \"What are the main topics?\", \"topK\": 5, \"threshold\": 0.3}'
```

---

## Test 5: Simple Question (Legacy Endpoint)

### Using Postman:
1. Create a new request
2. Set method to **POST**
3. URL: `http://localhost:8080/api/rag/ask-simple`
4. Go to **Body** tab
5. Select **raw** and **Text**
6. Type your question: `What is the summary?`
7. Click **Send**

### Using curl:
```powershell
curl -X POST http://localhost:8080/api/rag/ask-simple -H "Content-Type: text/plain" -d "What is the summary?"
```

---

## Test 6: Delete a Specific Document

### Using Postman:
1. Create a new request
2. Set method to **DELETE**
3. URL: `http://localhost:8080/api/rag/documents/abc-123-def-456`
   (Replace with your documentId)
4. Click **Send**

### Using curl:
```powershell
curl -X DELETE http://localhost:8080/api/rag/documents/abc-123-def-456
```

### Expected Response:
```json
{
  "message": "Document deleted successfully"
}
```

---

## Test 7: Clear All Data (Use with Caution!)

### Using Postman:
1. Create a new request
2. Set method to **DELETE**
3. URL: `http://localhost:8080/api/rag/clear`
4. Click **Send**

### Using curl:
```powershell
curl -X DELETE http://localhost:8080/api/rag/clear
```

### Expected Response:
```json
{
  "message": "All data cleared successfully"
}
```

---

## Testing Checklist

- [ ] Test 1: Upload PDF ✅
- [ ] Test 2: List documents ✅
- [ ] Test 3: Ask question with documentId ✅
- [ ] Test 4: Ask question without documentId ✅
- [ ] Test 5: Simple question endpoint ✅
- [ ] Test 6: Delete specific document ✅
- [ ] Test 7: Clear all data ✅

---

## Common Issues & Solutions

### Issue 1: "Connection refused"
**Solution:** Make sure the application is running (`mvn spring-boot:run`)

### Issue 2: "404 Not Found"
**Solution:** Check the URL - it should start with `http://localhost:8080/api/`

### Issue 3: "File too large"
**Solution:** The max file size is 100MB. Use a smaller PDF.

### Issue 4: "Invalid file format"
**Solution:** Only PDF files are supported currently.

### Issue 5: "Document not found"
**Solution:** Check the documentId - use the one from the upload response.

### Issue 6: "Groq API error"
**Solution:** Check that GROQ_API_KEY environment variable is set correctly.

---

## Sample Test PDFs

If you don't have a PDF handy, you can:
1. Create a simple text document
2. Save it as PDF
3. Or download a sample PDF from the internet

---

## Monitoring the Application

While testing, watch the console output for:
- ✅ "Stored embedding for doc: ..." (upload working)
- ✅ "Searching top X vectors..." (search working)
- ✅ "Retrieved X candidates from database" (retrieval working)
- ✅ "Found X results above threshold" (filtering working)

---

## Next Steps After Testing

Once all tests pass:
1. ✅ System is working correctly
2. ✅ Ready for production use
3. ✅ Can integrate with frontend
4. ✅ Can add authentication
5. ✅ Can deploy to server

---

**Happy Testing! 🚀**

If you encounter any issues, check the console logs for detailed error messages.
