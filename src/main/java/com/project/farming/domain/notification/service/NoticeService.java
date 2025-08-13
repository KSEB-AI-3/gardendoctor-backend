package com.project.farming.domain.notification.service;

import com.project.farming.global.fcm.FcmService;
import com.project.farming.domain.notification.dto.NoticeRequest;
import com.project.farming.domain.notification.dto.NoticeResponse;
import com.project.farming.domain.notification.entity.Notice;
import com.project.farming.domain.notification.repository.NoticeRepository;
import com.project.farming.domain.user.service.UserAdminService;
import com.project.farming.global.exception.NoticeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserAdminService userAdminService;
    private final FcmService fcmService;
    private final NotificationService notificationService;

    /**
     * 새로운 공지사항 등록
     *
     * @param request 등록할 공지사항 내용
     */
    @Transactional
    public void saveNotice(NoticeRequest request) {
        if (noticeRepository.existsByTitleAndContent(request.getTitle(), request.getContent())) {
            log.error("이미 등록된 공지사항입니다: 제목 - {}, 내용 - {}", request.getTitle(), request.getContent());
            throw new IllegalArgumentException(
                    "이미 등록된 공지사항입니다: 제목 - " + request.getTitle() +  ", 내용 - " + request.getContent());
        }
        Notice newNotice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isSent(false)
                .sentAt(request.getSentAt())
                .build();
        noticeRepository.save(newNotice);
        // TODO: 설정한 시간에 공지사항 알림 자동 전송 로직
    }

    /**
     * 전체 공지사항 목록 조회(ID 순)
     *
     * @return 각 공지사항의 Response DTO 리스트
     */
    public List<NoticeResponse> findAllNotices() {
        List<Notice> noticeList = noticeRepository.findAllByOrderByNoticeIdAsc();
        if (noticeList.isEmpty()) {
            log.info("등록된 공지사항이 없습니다.");
        }
        return noticeList.stream()
                .map(notice -> toNoticeResponseBuilder(notice).build())
                .collect(Collectors.toList());
    }

    /**
     * 공지사항 목록 검색(ID 순)
     * - 공지사항의 제목 또는 내용으로 검색
     *
     * @param searchType 검색 조건(title 또는 content) - 기본값은 title
     * @param keyword 검색어(제목 또는 내용)
     * @return 검색된 공지사항의 Response DTO 리스트
     */
    public List<NoticeResponse> findNoticesByKeyword(String searchType, String keyword) {
        List<Notice> noticeList = switch (searchType) {
            case "title" -> noticeRepository.findByTitleContainingOrderByNoticeIdAsc(keyword);
            case "content" -> noticeRepository.findByContentContainingOrderByNoticeIdAsc(keyword);
            default -> {
                log.error("지원하지 않는 검색 조건입니다: {}", searchType);
                throw new IllegalArgumentException("지원하지 않는 검색 조건입니다: " + searchType);
            }
        };
        return noticeList.stream()
                .map(notice -> toNoticeResponseBuilder(notice).build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 공지사항 조회
     *
     * @param noticeId 조회할 공지사항의 ID
     * @return 해당 공지사항의 Response DTO
     */
    public NoticeResponse findNotice(Long noticeId) {
        Notice foundNotice = findNoticeById(noticeId);
        return toNoticeResponseBuilder(foundNotice).build();
    }

    /**
     * 특정 공지사항 내용 수정
     *
     * @param noticeId 수정할 공지사항의 ID
     * @param request 새로 저장할 공지사항 내용
     */
    @Transactional
    public void updateNotice(Long noticeId, NoticeRequest request) {
        Notice notice = findNoticeById(noticeId);
        notice.updateNotice(
                request.getTitle(), request.getContent(), request.getSentAt());
        noticeRepository.save(notice);
    }

    /**
     * 특정 공지사항 삭제
     *
     * @param noticeId 삭제할 공지사항의 ID
     */
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = findNoticeById(noticeId);
        notificationService.deleteNotice(notice.getTitle(), notice.getContent());
        noticeRepository.delete(notice);
    }

    /**
     * 공지사항 알림 즉시 전송(전체 사용자 대상)
     *
     * @param noticeId 전송할 공지사항의 ID
     */
    public void sendNotice(Long noticeId) {
        Notice notice = findNoticeById(noticeId);
        List<String> targetTokens = userAdminService.getUserFcmTokenList();
        fcmService.sendMessagesTo(targetTokens, notice.getTitle(), notice.getContent());
        notice.markAsSent();
        noticeRepository.save(notice);
        // 각 사용자 별 공지 저장
        notificationService.saveNotice(notice.getTitle(), notice.getContent());
    }

    /**
     * ID로 공지사항 조회
     *
     * @param noticeId 조회할 공지사항의 ID
     * @return 조회한 공지사항 내용
     */
    private Notice findNoticeById(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> {
                    log.error("해당 공지사항이 존재하지 않습니다: {}", noticeId);
                    return new NoticeNotFoundException("해당 공지사항이 존재하지 않습니다: " + noticeId);
                });
    }

    /**
     * Response DTO로 변환
     *
     * @param notice Response DTO로 변환할 공지사항 엔티티
     * @return 공지사항 Response DTO
     */
    private NoticeResponse.NoticeResponseBuilder toNoticeResponseBuilder(Notice notice) {
        return NoticeResponse.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .isSent(notice.isSent())
                .sentAt(notice.getSentAt())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt());
    }
}
