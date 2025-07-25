package com.project.farming.global.image.controller;

import com.project.farming.global.exception.ImageFileNotFoundException;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.jwtToken.CustomUserDetails;
import com.project.farming.global.image.dto.ErrorResponseDto;
import com.project.farming.global.image.dto.ImageUploadResponseDto;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.service.ImageFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "이미지 (Image)", description = "이미지 업로드, 삭제 등 이미지 파일 관리 API")
public class ImageFileController {

    private final ImageFileService imageFileService;

    @Operation(summary = "이미지 업로드", description = "단일 이미지를 S3에 업로드하고 ImageFile 정보를 DB에 저장합니다. 업로드된 이미지의 ID를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "이미지 업로드 성공",
                    content = @Content(schema = @Schema(implementation = ImageUploadResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 없음, 지원하지 않는 파일 형식 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))), // ErrorResponseDto는 예시
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponseDto> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "이미지가 속할 도메인 유형 (예: USER, PLANT, JOURNAL)", required = true)
            @RequestParam("domainType") ImageDomainType domainType,
            @Parameter(description = "이미지가 속할 도메인 엔티티의 ID (예: 사용자 ID, 식물 ID)", required = true)
            @RequestParam("domainId") Long domainId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails // 사용자 ID를 자동으로 가져오기 위해 추가
    ) {
        // 실제 비즈니스 로직에 따라 domainId가 customUserDetails.getUser().getUserId()와 일치하는지 검증할 수 있습니다.
        // 예를 들어, USER 도메인 타입의 이미지는 반드시 해당 사용자의 ID와 연결되어야 할 수 있습니다.
        if (domainType == ImageDomainType.USER && !domainId.equals(customUserDetails.getUser().getUserId())) {
            throw new IllegalArgumentException("사용자 프로필 이미지는 본인의 ID에만 연결될 수 있습니다.");
        }

        ImageFile uploadedImage = imageFileService.uploadImage(file, domainType, domainId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ImageUploadResponseDto.builder()
                        .imageFileId(uploadedImage.getImageFileId())
                        .imageUrl(uploadedImage.getImageUrl())
                        .message("이미지 업로드 성공")
                        .build()
        );
    }

    @Operation(summary = "이미지 삭제", description = "지정된 ImageFile ID에 해당하는 이미지를 S3와 DB에서 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "이미지 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (존재하지 않는 이미지 ID)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사용자의 이미지 삭제 시도)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth")
    @DeleteMapping("/{imageFileId}")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "삭제할 이미지 파일의 ID", required = true)
            @PathVariable Long imageFileId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        // 이미지 소유권 확인 로직 (중요!)
        ImageFile imageFile = imageFileService.getImageFileById(imageFileId)
                .orElseThrow(() -> new ImageFileNotFoundException("존재하지 않는 이미지 파일입니다: " + imageFileId));

        // 예시: USER 도메인 이미지인 경우, 해당 이미지의 domainId가 현재 로그인한 사용자의 ID와 일치하는지 확인
        if (imageFile.getDomainType() == ImageDomainType.USER && !imageFile.getDomainId().equals(customUserDetails.getUser().getUserId())) {
            throw new IllegalArgumentException("본인의 프로필 이미지만 삭제할 수 있습니다.");
        }
        // TODO: 다른 도메인 타입에 대한 삭제 권한 로직 추가 (예: 게시글 이미지는 게시글 작성자만 삭제 가능)

        imageFileService.deleteImage(imageFileId);
        return ResponseEntity.noContent().build();
    }
}
