package com.project.farming.domain.user.service;

import com.project.farming.domain.user.dto.UserAdminRequest;
import com.project.farming.domain.user.dto.UserAdminResponse;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.entity.UserRole;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.UserNotFoundException;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.service.ImageFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final ImageFileService imageFileService;

    /**
     * 전체 사용자 목록 조회(별명순)
     *
     * @return 각 사용자 정보의 Response DTO 리스트
     */
    public List<UserAdminResponse> findAllUsers() {
        List<User> userList = userRepository.findAllByOrderByNicknameAsc();
        if (userList.isEmpty()) {
            throw new UserNotFoundException("등록된 사용자가 없습니다.");
        }
        return userList.stream()
                .map(user -> toUserAdminResponseBuilder(user).build())
                .collect(Collectors.toList());
    }

    /**
     * 사용자 목록 검색(별명/이메일 순)
     * - 사용자의 별명 또는 이메일로 검색
     *
     * @param searchType 검색 조건(name 또는 email) - 기본값은 name
     * @param keyword 검색어(별명 또는 이메일)
     * @return 검색된 사용자 정보의 Response DTO 리스트
     */
    public List<UserAdminResponse> findUsersByKeyword(String searchType, String keyword) {
        List<User> foundUsers = switch (searchType) {
            case "name" -> userRepository.findByNicknameContainingOrderByNicknameAsc(keyword);
            case "email" -> userRepository.findByEmailContainingOrderByEmailAsc(keyword);
            default -> throw new IllegalArgumentException("지원하지 않는 검색 조건입니다: " + searchType);
        };
        return foundUsers.stream()
                .map(user -> toUserAdminResponseBuilder(user).build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자 조회
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 Response DTO
     */
    public UserAdminResponse findUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        return toUserAdminResponseBuilder(user).build();
    }

    /**
     * 특정 사용자 정보 수정
     *
     * @param userId 수정할 사용자 ID
     * @param request 새로 저장할 사용자 정보
     * @param newFile 새로 업로드할 프로필 이미지 파일 (선택적)
     */
    public void updateUser(Long userId, UserAdminRequest request, MultipartFile newFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        if (newFile != null && !newFile.isEmpty()) {
            // 새로운 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.updateImage(
                    user.getProfileImageFile().getImageFileId(), // 기존 이미지 파일
                    newFile, ImageDomainType.USER, userId);
            user.updateProfileImageFile(imageFile);
        }
        user.updateEmail(request.getEmail());
        user.updateNickname(request.getNickname());
        user.setOauthProvider(request.getOauthProvider());
        user.setOauthId(request.getOauthId());
        user.updateRole(UserRole.valueOf(request.getRole()));
        user.updateFcmToken(request.getFcmToken());
        user.updateSubscriptionStatus(request.getSubscriptionStatus());
        userRepository.save(user);
    }

    /**
     * Response DTO로 변환
     *
     * @param user Response DTO로 변환할 사용자 정보 엔티티
     * @return 사용자 정보 Response DTO
     */
    private UserAdminResponse.UserAdminResponseBuilder toUserAdminResponseBuilder(User user) {
        return UserAdminResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .oauthProvider(user.getOauthProvider())
                .oauthId(user.getOauthId())
                .role(user.getRole().toString())
                .fcmToken(user.getFcmToken())
                .subscriptionStatus(user.getSubscriptionStatus())
                .profileImageUrl(user.getProfileImageFile().getImageUrl());
    }
}
