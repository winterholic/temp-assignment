package com.realteeth.hw.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class JobRequest(
    @field:NotBlank(message = "imageUrl이 필요합니다.")
    @Schema(description = "처리할 이미지 URL", example = "https://example.com/tooth.jpg")
    val imageUrl: String,
)
