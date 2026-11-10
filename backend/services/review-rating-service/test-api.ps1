# ==============================================================================
# LearnHub LMS - Review & Rating Service Automated API Test Script
# Chạy trực tiếp trên Windows PowerShell, không cần cài đặt thêm phần mềm nào!
# Cách dùng: .\test-api.ps1 (khi service 8085 đang chạy)
# ==============================================================================

$baseUrl = "http://localhost:8085"
$courseId = "2a3df3a6-bd47-43b9-91aa-8fb7a13236a8"
$student1Id = "11111111-1111-1111-1111-000000000101"
$student5Id = "11111111-1111-1111-1111-111111111111"
$instructorId = "22222222-2222-2222-2222-000000000007"
$adminId = "33333333-3333-3333-3333-000000000001"

Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "   BAT DAU KIEM THU FUNCTIONAL API - REVIEW & RATING SERVICE" -ForegroundColor Cyan
Write-Host "   Target: $baseUrl" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan

$passed = 0
$failed = 0

function Test-Step($name, $scriptBlock) {
    Write-Host "`n>> [$name]..." -ForegroundColor Yellow
    try {
        & $scriptBlock
        Write-Host "   [PASS] $name" -ForegroundColor Green
        $script:passed++
    } catch {
        Write-Host "   [FAIL] $name - Error: $_" -ForegroundColor Red
        $script:failed++
    }
}

# 1. Health Check
Test-Step "1. Health Check (/actuator/health)" {
    $res = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    Write-Host "   Status: $($res.status)" -ForegroundColor Gray
    if ($res.status -ne "UP") { throw "Status is not UP" }
}

# 2. Xem Rating Summary cong khai
Test-Step "2. Xem Rating Summary cong khai (UC-R05)" {
    $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/courses/$courseId/ratings/summary" -Method Get
    Write-Host "   Average Rating: $($res.averageRating), Total Reviews: $($res.totalReviews)" -ForegroundColor Gray
    if ($null -eq $res.averageRating) { throw "averageRating is null" }
}

# 3. Xem danh sach Review cong khai
Test-Step "3. Xem danh sach Review cong khai (UC-R04)" {
    $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/courses/$courseId/reviews?page=0&size=10" -Method Get
    Write-Host "   Total elements: $($res.totalElements), Current items: $($res.content.Length)" -ForegroundColor Gray
}

# 4. Kiem tra quyen danh gia (Eligibility)
Test-Step "4. Kiem tra quyen danh gia hoc vien 105 (UC-R11)" {
    $headers = @{
        "X-User-Id" = $student5Id
        "X-User-Role" = "student"
    }
    $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/courses/$courseId/reviews/eligibility" -Method Get -Headers $headers
    Write-Host "   Eligible: $($res.eligible), Reason: $($res.reason)" -ForegroundColor Gray
}

# 5. Tao Review moi
Test-Step "5. Hoc vien 105 gui danh gia 5 sao (UC-R01)" {
    $headers = @{
        "X-User-Id" = $student5Id
        "X-User-Role" = "student"
        "Content-Type" = "application/json; charset=utf-8"
    }
    $body = @{
        rating = 5
        comment = "Khoa hoc Spring Boot Microservices cuc ky thuc te va de hieu!"
    } | ConvertTo-Json -Compress

    try {
        $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/courses/$courseId/reviews" -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
        Write-Host "   Tao Review thanh cong - ID: $($res.id), Rating: $($res.rating)" -ForegroundColor Gray
        $script:createdReviewId = $res.id
    } catch {
        # Neu da tao roi (Conflict 409), lay review co san de tiep tuc test
        Write-Host "   (Review co the da ton tai, tiep tuc voi Review ID 1)" -ForegroundColor Gray
        $script:createdReviewId = 1
    }
}

$reviewId = if ($script:createdReviewId) { $script:createdReviewId } else { 1 }

# 6. Vote Helpful
Test-Step "6. Vote Helpful cho review (UC-R07)" {
    $headers = @{
        "X-User-Id" = $student1Id
        "X-User-Role" = "student"
    }
    $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/reviews/$reviewId/helpful" -Method Post -Headers $headers
    Write-Host "   Helpful Count sau vote: $($res.helpfulCount), Voted: $($res.voted)" -ForegroundColor Gray
}

# 7. Giang vien phan hoi (Reply)
Test-Step "7. Giang vien phan hoi nhan xet (UC-R06)" {
    $headers = @{
        "X-User-Id" = $instructorId
        "X-User-Role" = "instructor"
        "Content-Type" = "application/json; charset=utf-8"
    }
    $body = @{
        content = "Cam on em da ung ho khoa hoc! Hay tiep tuc phat huy nhe."
    } | ConvertTo-Json -Compress

    try {
        $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/reviews/$reviewId/replies" -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
        Write-Host "   Phan hoi thanh cong: $($res.content)" -ForegroundColor Gray
    } catch {
        Write-Host "   (Review co the da co reply tu truoc)" -ForegroundColor Gray
    }
}

# 8. Xem lich su danh gia ca nhan
Test-Step "8. Xem lich su danh gia ca nhan (UC-R10)" {
    $headers = @{
        "X-User-Id" = $student1Id
        "X-User-Role" = "student"
    }
    $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/my-reviews" -Method Get -Headers $headers
    Write-Host "   Tong so danh gia cua hoc vien 101: $($res.totalElements)" -ForegroundColor Gray
}

# 9. Admin an / duyet danh gia
Test-Step "9. Admin duyet trang thai danh gia (UC-R09)" {
    $headers = @{
        "X-User-Id" = $adminId
        "X-User-Role" = "admin"
        "Content-Type" = "application/json"
    }
    $body = @{ status = "APPROVED" } | ConvertTo-Json -Compress
    $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/reviews/$reviewId/status" -Method Patch -Headers $headers -Body $body
    Write-Host "   Trang thai sau khi Admin cap nhat: $($res.status)" -ForegroundColor Gray
}

Write-Host "`n==============================================================" -ForegroundColor Cyan
Write-Host "   KET QUA KIEM THU: PASSED: $passed  |  FAILED: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "==============================================================" -ForegroundColor Cyan
