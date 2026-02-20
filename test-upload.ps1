# Test script for RAG PDF upload
Write-Host "Testing RAG PDF Upload Endpoint..." -ForegroundColor Cyan

# Create a simple text file to test (since we don't have a PDF)
$testContent = @"
RAG System Test Document

This is a test document for the Retrieval-Augmented Generation system.

Key Concepts:
- Vector embeddings are numerical representations of text
- Semantic search finds similar content based on meaning
- PostgreSQL with pgvector extension stores vector data
- Cosine similarity measures vector similarity

The system uses local TF-IDF based embeddings with 384 dimensions.
"@

$testFile = "test-document.txt"
$testContent | Out-File -FilePath $testFile -Encoding UTF8

Write-Host "Created test file: $testFile" -ForegroundColor Green

# Upload the file
Write-Host "`nUploading file to RAG system..." -ForegroundColor Cyan

try {
    $uri = "http://localhost:8080/api/rag/upload"
    $filePath = Resolve-Path $testFile
    
    # Create multipart form data
    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"
    
    $fileContent = [System.IO.File]::ReadAllBytes($filePath)
    $fileContentEncoded = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileContent)
    
    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"test-document.pdf`"",
        "Content-Type: application/pdf$LF",
        $fileContentEncoded,
        "--$boundary--$LF"
    ) -join $LF
    
    $response = Invoke-RestMethod -Uri $uri -Method Post -ContentType "multipart/form-data; boundary=$boundary" -Body $bodyLines
    
    Write-Host "`nUpload successful!" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 10
    
} catch {
    Write-Host "`nUpload failed!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

# Clean up
Remove-Item $testFile -ErrorAction SilentlyContinue
Write-Host "`nTest file cleaned up." -ForegroundColor Gray
