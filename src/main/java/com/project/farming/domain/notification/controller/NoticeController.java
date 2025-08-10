package com.project.farming.domain.notification.controller;

import com.project.farming.domain.notification.dto.NoticeRequest;
import com.project.farming.domain.notification.dto.NoticeResponse;
import com.project.farming.domain.notification.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notice Admin API", description = "공지사항 알림 관련 **관리자 전용** API")
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
@Controller
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/create")
    @Operation(summary = "새로운 공지사항 등록 페이지 (관리자 전용)",
            description = "새로운 공지사항을 등록하는 페이지 - **관리자만 접근 가능합니다.**")
    public String showCreateNoticePage() {
        return "notice/create-notice";
    }

    @PostMapping("/createProc")
    @Operation(summary = "새로운 공지사항 등록 (관리자 전용)",
            description = "새로운 공지사항을 등록합니다. **관리자만 접근 가능합니다.**")
    public String createNotice(@Valid NoticeRequest request) {
        try {
            noticeService.saveNotice(request);
            return "redirect:/admin/notices";
        } catch (Exception e) {
            return "redirect:/admin/notices/create?error=true";
        }
    }

    @GetMapping
    @Operation(summary = "전체 공지사항 목록 조회 페이지 (관리자 전용)",
            description = "DB에 등록된 모든 공지사항을 ID 순으로 조회합니다. **관리자만 접근 가능합니다.**")
    public String showNoticeListPage(Model model) {
        List<NoticeResponse> noticeList = noticeService.findAllNotices();
        model.addAttribute("noticeList", noticeList);
        return "notice/notice-list";
    }

    @GetMapping("/search")
    @Operation(summary = "공지사항 목록 검색 (관리자 전용)",
            description = """
                    입력한 키워드(제목(title), 내용(content))를 포함하는 모든 공지사항을 ID 순으로 조회합니다.
                    **관리자만 접근 가능합니다.**
                    """)
    public String showSearchNoticeListPage(
            @Parameter(description = "검색 조건: 공지사항 제목(title) 또는 내용(content)")
            @RequestParam(defaultValue = "title") String searchType,
            @RequestParam String keyword, Model model) {
        List<NoticeResponse> noticeList = noticeService.findNoticesByKeyword(searchType, keyword);
        model.addAttribute("noticeList", noticeList);
        return "notice/notice-list";
    }

    @GetMapping("/{noticeId}")
    @Operation(summary = "특정 공지사항 조회 페이지 (관리자 전용)",
            description = "공지사항 ID에 해당하는 공지사항을 조회합니다. **관리자만 접근 가능합니다.**")
    public String showNoticePage(@PathVariable Long noticeId, Model model) {
        NoticeResponse notice = noticeService.findNotice(noticeId);
        model.addAttribute("notice", notice);
        return "notice/notice-detail";
    }

    @GetMapping("/update")
    @Operation(summary = "특정 공지사항 수정 페이지 (관리자 전용)",
            description = "공지사항 ID에 해당하는 공지사항을 수정하는 페이지 - **관리자만 접근 가능합니다.**")
    public String showUpdateNoticePage(@RequestParam Long noticeId, Model model) {
        NoticeResponse notice = noticeService.findNotice(noticeId); // 기존 내용 불러오기
        model.addAttribute("notice", notice);
        return "notice/update-notice";
    }

    @PostMapping("/update/{noticeId}")
    @Operation(summary = "특정 공지사항 수정 (관리자 전용)",
            description = "공지사항 ID에 해당하는 공지사항을 수정합니다. **관리자만 접근 가능합니다.**")
    public String updateNotice(@PathVariable Long noticeId, @Valid NoticeRequest request) {
        try {
            noticeService.updateNotice(noticeId, request);
            return "redirect:/admin/notices/" + noticeId;
        } catch (Exception e) {
            return "redirect:/admin/notices/update?noticeId=" + noticeId + "&error=true";
        }
    }

    @GetMapping("/delete")
    @Operation(summary = "특정 공지사항 삭제 페이지 (관리자 전용)",
            description = "공지사항 ID에 해당하는 공지사항을 삭제하는 페이지 - **관리자만 접근 가능합니다.**")
    public String showDeleteNoticePage() {
        return "notice/delete-notice";
    }

    @GetMapping("/delete/{noticeId}")
    @Operation(summary = "특정 공지사항 삭제 (관리자 전용)",
            description = "공지사항 ID에 해당하는 공지사항을 삭제합니다. **관리자만 접근 가능합니다.**")
    public String deleteNotice(@PathVariable Long noticeId) {
        try {
            noticeService.deleteNotice(noticeId);
            return "redirect:/admin/notices";
        } catch (Exception e) {
            return "redirect:/admin/notices/delete?noticeId=" + noticeId + "&error=true";
        }
    }

    @GetMapping("/send/{noticeId}")
    @Operation(summary = "특정 공지사항 알림 즉시 전송 (관리자 전용)",
            description = "공지사항 ID에 해당하는 공지사항 알림을 즉시 전송합니다. **관리자만 접근 가능합니다.**")
    public String sendNotice(@PathVariable Long noticeId) {
        try {
            noticeService.sendNotice(noticeId);
            return "redirect:/admin/notices/" + noticeId;
        } catch (Exception e) {
            return "redirect:/admin/notices/" + noticeId + "?send_error=true";
        }
    }
}
