# RAG API Test Script
Write-Host "=== RAG System API Test ===" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "1. Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/rag/health" -Method Get
    Write-Host "   Status: $($health.status)" -ForegroundColor Green
    Write-Host "   Total Documents: $($health.totalDocuments)" -ForegroundColor Green
    Write-Host "   Total Chunks: $($health.totalChunks)" -ForegroundColor Green
} catch {
    Write-Host "   FAILED: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: List Documents
Write-Host "2. Testing List Documents Endpoint..." -ForegroundColor Yellow
try {
    $docs = Invoke-RestMethod -Uri "http://localhost:8080/api/rag/documents" -Method Get
    Write-Host "   Document Count: $($docs.count)" -ForegroundColor Green
    Write-Host "   Total Chunks: $($docs.totalChunks)" -ForegroundColor Green
    if ($docs.documents) {
        Write-Host "   Documents:" -ForegroundColor Green
        $docs.documents | ForEach-Object { Write-Host "     - $_" -ForegroundColor Gray }
    }
} catch {
    Write-Host "   FAILED: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "=== Basic Tests Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: PDF upload test requires an actual PDF file." -ForegroundColor Yellow
Write-Host "You can test upload manually using:" -ForegroundColor Yellow
Write-Host '  curl -X POST http://localhost:8080/api/rag/upload -F "file=@yourfile.pdf"' -ForegroundColor Gray
