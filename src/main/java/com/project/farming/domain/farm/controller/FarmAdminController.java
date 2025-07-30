package com.project.farming.domain.farm.controller;

import com.project.farming.domain.farm.dto.FarmAdminRequest;
import com.project.farming.domain.farm.dto.FarmResponse;
import com.project.farming.domain.farm.service.FarmAdminService;
import com.project.farming.domain.farm.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Farm Admin API", description = "텃밭 관련 **관리자 전용** API")
@RequestMapping("/admin/farms")
@RequiredArgsConstructor
@Controller
public class FarmAdminController {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private final FarmAdminService farmAdminService;
    private final FarmService farmService;

    @GetMapping("/create")
    @Operation(summary = "새로운 텃밭 정보 등록 페이지 (관리자 전용)",
            description = "새로운 텃밭 정보를 등록하는 페이지 - **관리자만 접근 가능합니다.**"
    )
    public String showCreateFarmPage() {
        return "farm/create-farm";
    }

    @PostMapping("/createProc")
    @Operation(summary = "새로운 텃밭 정보 등록 (관리자 전용)",
            description = """
                    새로운 텃밭 정보를 등록합니다. **관리자만 접근 가능합니다.**
                    텃밭 정보는 DTO로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    enctype은 multipart/form-data입니다.
                    """
    )
    public String createFarm(
            @Parameter(description = "텃밭 정보") @Valid @ModelAttribute FarmAdminRequest request,
            @Parameter(description = "업로드할 텃밭 이미지 파일")
            @RequestParam("imageFile") MultipartFile imageFile) {
        try {
            farmAdminService.saveFarm(request, imageFile);
            return "redirect:/admin/farms";
        } catch (Exception e) {
            return "redirect:/admin/farms/create?error=true";
        }
    }

    @GetMapping
    @Operation(summary = "전체 텃밭 목록 조회 페이지 (관리자 전용)",
            description = "DB에 등록된 모든 텃밭을 고유번호순으로 조회합니다. **관리자만 접근 가능합니다.**")
    public String showFarmListPage(Model model) {
        List<FarmResponse> farmList = farmService.findAllFarms();
        model.addAttribute("farmList", farmList);
        return "farm/farm-list";
    }

    @GetMapping("/search")
    @Operation(summary = "텃밭 목록 검색 (관리자 전용)",
            description = """
                    사용자가 입력한 키워드(텃밭명(name) 또는 주소(address))를 포함하는 모든 텃밭을 고유번호순으로 조회합니다.
                    **관리자만 접근 가능합니다.**
                    """)
    public String showSearchFarmListPage(
            @Parameter(description = "검색 조건: 텃밭명(name) 또는 주소(address)")
            @RequestParam(defaultValue = "name") String searchType,
            @RequestParam String keyword, Model model) {
        List<FarmResponse> farmList = farmAdminService.findFarmsByKeyword(searchType, keyword);
        model.addAttribute("farmList", farmList);
        return "farm/farm-list";
    }

    @GetMapping("/{farmId}")
    @Operation(summary = "특정 텃밭 정보 조회 페이지 (관리자 전용)",
            description = "텃밭 ID에 해당하는 텃밭의 상세 정보를 조회합니다. **관리자만 접근 가능합니다.**")
    public String showFarmPage(@PathVariable Long farmId, Model model) {
        FarmResponse farm = farmService.findFarm(farmId);
        model.addAttribute("farm", farm);
        return "farm/farm-detail";
    }

    @GetMapping("/update")
    @Operation(summary = "특정 텃밭 정보 수정 페이지 (관리자 전용)",
            description = "텃밭 ID에 해당하는 텃밭의 정보를 수정하는 페이지 - **관리자만 접근 가능합니다.**"
    )
    public String showUpdateFarmPage(@RequestParam Long farmId, Model model) {
        FarmResponse farm = farmService.findFarm(farmId); // 기존 내용 불러오기
        model.addAttribute("farm", farm);
        return "farm/update-farm";
    }

    @PostMapping("/update/{farmId}")
    @Operation(summary = "특정 텃밭 정보 수정 (관리자 전용)",
            description = """
                    텃밭 ID에 해당하는 텃밭의 정보를 수정합니다. **관리자만 접근 가능합니다.**
                    텃밭 정보는 DTO로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    enctype은 multipart/form-data입니다.
                    """
    )
    public String updateFarm(@PathVariable Long farmId,
            @Parameter(description = "텃밭 정보") @Valid @ModelAttribute FarmAdminRequest request,
            @Parameter(description = "업로드할 텃밭 이미지 파일")
            @RequestParam("imageFile") MultipartFile imageFile) {
        try {
            farmAdminService.updateFarm(farmId, request, imageFile);
            return "redirect:/admin/farms/" + farmId;
        } catch (Exception e) {
            return "redirect:/admin/farms/update?farmId=" + farmId + "&error=true";
        }
    }

    @GetMapping("/delete")
    @Operation(summary = "특정 텃밭 정보 삭제 페이지 (관리자 전용)",
            description = "텃밭 ID에 해당하는 텃밭의 정보를 삭제하는 페이지 - **관리자만 접근 가능합니다.**")
    public String showDeleteFarmPage() {
        return "farm/delete-farm";
    }

    @GetMapping("/delete/{farmId}")
    @Operation(summary = "특정 텃밭 정보 삭제 (관리자 전용)",
            description = "텃밭 ID에 해당하는 텃밭의 정보를 삭제합니다. **관리자만 접근 가능합니다.**")
    public String deleteFarm(@PathVariable Long farmId) {
        try {
            farmAdminService.deleteFarm(farmId);
            return "redirect:/admin/farms";
        } catch (Exception e) {
            return "redirect:/admin/farms/" + farmId + "?error=true";
        }
    }

    @GetMapping("/map")
    @Operation(summary = "주변 텃밭 정보 조회 테스트 페이지 (관리자 전용)",
            description = """
                    사용자의 현재 위치(위도, 경도)를 기준으로 원하는 반경(km) 내에 위치한 텃밭 정보를 조회하는 페이지 테스트 -
                    **관리자만 접근 가능합니다.**
                    """)
    public String showMapPage(Model model) {
        model.addAttribute("kakaoApiKey", kakaoApiKey);
        return "farm/map";
    }
}
