package com.project.farming.domain.user.service;

import com.project.farming.domain.user.dto.UserMyPageResponseDto;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.entity.UserRole;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.UserNotFoundException;
import com.project.farming.global.image.entity.DefaultImages;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.jwtToken.JwtBlacklistService;
import com.project.farming.global.jwtToken.JwtToken;
import com.project.farming.global.jwtToken.JwtTokenProvider;
import com.project.farming.global.jwtToken.RefreshToken;
import com.project.farming.global.jwtToken.RefreshTokenRepository;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.repository.ImageFileRepository;
import com.project.farming.global.image.service.ImageFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ImageFileRepository imageFileRepository;
    private final ImageFileService imageFileService;

    @Transactional
    public User registerUser(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        // DefaultImages 클래스의 상수를 사용하여 s3Key로 엔티티를 조회합니다.
        ImageFile defaultImageFile = imageFileRepository.findByS3Key(DefaultImages.DEFAULT_USER_IMAGE)
                .orElseThrow(() -> new IllegalStateException("기본 사용자 이미지가 존재하지 않습니다."));

        User newUser = User.builder()
                .email(email)
                .nickname(nickname)
                .password(passwordEncoder.encode(password)) // 비밀번호 암호화
                .oauthProvider("LOCAL") // 자체 회원가입은 "LOCAL"로 구분
                .role(UserRole.USER)
                .subscriptionStatus("FREE")
                .profileImageFile(defaultImageFile)
                .build();
        return userRepository.save(newUser);
    }

    @Transactional
    public JwtToken login(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("인증된 사용자를 찾을 수 없습니다."));

            String accessToken = jwtTokenProvider.generateToken(user.getUserId());
            String refreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUserId());
            long refreshTokenExpirationMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(refreshTokenString);
            Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMillis);

            // delete와 save 대신 단 한 번의 upsert 쿼리 호출
            refreshTokenRepository.saveOrUpdate(user.getUserId(), refreshTokenString, expiresAt);

            return JwtToken.builder()
                    .grantType("Bearer")
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenString)
                    .build();
        } catch (AuthenticationException e) {
            log.warn("로그인 실패: 이메일 또는 비밀번호가 잘못되었습니다. 이메일: {}", email, e.getMessage());
            throw new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다.");
        }
    }

    @Transactional
    public void logout(String accessToken, Long userId) {
        long remainingTimeMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(accessToken);
        if (remainingTimeMillis > 0) {
            jwtBlacklistService.blacklistToken(accessToken, remainingTimeMillis);
            log.info("Access Token 블랙리스트 등록 완료. 토큰: {}", accessToken);
        } else {
            log.warn("만료되었거나 유효하지 않은 Access Token이 로그아웃 요청에 사용되었습니다: {}", accessToken);
        }

        // Refresh Token 삭제
        userRepository.findById(userId).ifPresentOrElse(user -> { // <-- 여기서부터 차이 발생
            refreshTokenRepository.deleteByUser(user);
            log.info("사용자 {} (ID: {})의 Refresh Token 삭제를 시도했습니다.", user.getEmail(), userId);
        }, () -> {
            log.warn("로그아웃 요청: 사용자 ID {}를 찾을 수 없습니다. 해당 사용자는 존재하지 않거나 이미 삭제되었습니다.", userId);
            throw new UserNotFoundException("로그아웃하려는 사용자를 찾을 수 없습니다.");
        });
    }
    /**
     * ✅ 사용자의 FCM 토큰을 업데이트하는 서비스 메서드 추가
     * @param userId 사용자 ID
     * @param fcmToken 새로운 FCM 토큰
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("FCM 토큰을 업데이트할 사용자를 찾을 수 없습니다."));

        user.updateFcmToken(fcmToken);
        log.info("사용자 ID {}의 FCM 토큰이 업데이트되었습니다.", userId);
    }

    @Transactional
    public JwtToken refreshTokens(String refreshTokenString) {
        if (!jwtTokenProvider.validateToken(refreshTokenString)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // JWT에서 직접 userId (Long)를 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshTokenString);

        if (jwtBlacklistService.isBlacklisted(refreshTokenString)) {
            log.warn("블랙리스트에 등록된 리프레시 토큰이 재사용되었습니다. 사용자 ID {}의 모든 리프레시 토큰을 삭제합니다.", userId);
            userRepository.findById(userId)
                    .ifPresent(refreshTokenRepository::deleteByUser);
            throw new IllegalArgumentException("비정상적인 접근입니다. 재로그인이 필요합니다.");
        }

        // userId (Long)로 User 객체 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        RefreshToken storedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshTokenString)
                .orElseThrow(() -> new IllegalArgumentException("DB에 존재하지 않는 리프레시 토큰입니다. 재로그인이 필요합니다."));

        // DB에 저장된 토큰이 현재 사용자에게 할당된 것인지 추가 확인 (userId로 비교)
        if (!storedRefreshToken.getUser().getUserId().equals(userId)) {
            log.warn("리프레시 토큰 소유자 불일치: 요청 토큰의 사용자 ID({})와 DB 토큰의 사용자 ID({})", userId, storedRefreshToken.getUser().getUserId());
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다. 재로그인이 필요합니다.");
        }

        String newAccessToken = jwtTokenProvider.generateToken(userId);
        String newRefreshTokenString = jwtTokenProvider.generateRefreshToken(userId);
        long newRefreshTokenExpirationMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(newRefreshTokenString);

        long oldRefreshTokenRemainingTime = jwtTokenProvider.getExpirationRemainingTimeMillis(refreshTokenString);
        if (oldRefreshTokenRemainingTime > 0) {
            jwtBlacklistService.blacklistToken(refreshTokenString, oldRefreshTokenRemainingTime);
            log.info("이전 Refresh Token 블랙리스트 등록 완료: {}", refreshTokenString);
        } else {
            log.warn("이미 만료된 Refresh Token이 재발급 요청에 사용되었습니다. 재로그인 필요: {}", refreshTokenString);
            throw new IllegalArgumentException("만료된 Refresh Token입니다. 재로그인이 필요합니다.");
        }

        storedRefreshToken.updateRefreshToken(newRefreshTokenString, Instant.now().plusMillis(newRefreshTokenExpirationMillis));
        refreshTokenRepository.save(storedRefreshToken);

        log.info("토큰 재발급 성공. 새로운 Access Token 및 Refresh Token 발급.");

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenString)
                .build();
    }
    @Transactional(readOnly = true)
    public UserMyPageResponseDto getMyPageInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("유저가 없습니다."));

        // DTO 반환 시 프로필 이미지 URL을 안전하게 가져옴
        String profileImageUrl = (user.getProfileImageFile() != null) ? user.getProfileImageFile().getImageUrl() : null;

        return UserMyPageResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(profileImageUrl) // 수정된 부분
                .oauthProvider(user.getOauthProvider())
                .role(user.getRole())
                .subscriptionStatus(user.getSubscriptionStatus())
                .build();
    }

    // 내 정보 삭제 (하드 삭제 예시)
    @Transactional
    public void deleteMyPageInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("삭제할 회원이 없습니다."));

        // 사용자와 연결된 프로필 이미지 파일 삭제 (선택 사항)
        // 사용자가 탈퇴 시 프로필 이미지를 S3에서도 삭제하려면 이 로직을 추가합니다.
        if (user.getProfileImageFile() != null) {
            imageFileService.deleteImage(user.getProfileImageFile().getImageFileId());
        }

        // 사용자가 작성한 다른 도메인(예: Diary, Plant)의 이미지도 함께 삭제하려면
        // ImageFileService.getImagesByDomainAndId(ImageDomainType.JOURNAL, userId) 등을 사용하여 처리해야 합니다.
        // 이는 복잡해질 수 있으므로, 보통 이미지를 S프트 삭제하거나 별도의 정기적인 클리너 작업을 통해 처리하기도 합니다.
        userRepository.deleteById(userId);
    }


    /**
     * 사용자의 닉네임을 변경합니다.
     *
     * @param userId   사용자 ID
     * @param newNickname 새로운 닉네임
     * @return 업데이트된 사용자 정보 DTO
     * @throws UserNotFoundException 사용자를 찾을 수 없을 때
     */
    @Transactional
    public UserMyPageResponseDto updateNickname(Long userId, String newNickname) {
        if (newNickname == null || newNickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        user.updateNickname(newNickname);
        log.info("사용자 ID {}의 닉네임이 {}로 변경되었습니다.", userId, newNickname);

        // 변경된 사용자 정보를 DTO로 변환하여 반환
        return UserMyPageResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl((user.getProfileImageFile() != null) ? user.getProfileImageFile().getImageUrl() : null)
                .oauthProvider(user.getOauthProvider())
                .role(user.getRole())
                .subscriptionStatus(user.getSubscriptionStatus())
                .build();
    }
    @Transactional
    public UserMyPageResponseDto updateProfileImage(Long userId, MultipartFile imageFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("업로드된 이미지 파일이 없습니다.");
        }

        // ✅ 1. 새 이미지 업로드
        ImageFile newProfileImage = imageFileService.uploadImage(imageFile, ImageDomainType.USER, userId);

        // ✅ 2. 기존 이미지 삭제 (기본 이미지가 아닐 경우)
        if (user.getProfileImageFile() != null &&
                !DefaultImages.isDefaultImage(user.getProfileImageFile().getS3Key())) {
            imageFileService.deleteImage(user.getProfileImageFile().getImageFileId());
        }

        // ✅ 3. 유저 객체에 새로운 이미지 설정
        user.updateProfileImageFile(newProfileImage);

        // ✅ 4. 응답 DTO 반환
        return convertToMyPageResponseDto(user);
    }

    @Transactional
    public UserMyPageResponseDto deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        ImageFile defaultImageFile = imageFileRepository.findByS3Key(DefaultImages.DEFAULT_USER_IMAGE)
                .orElseThrow(() -> new IllegalStateException("기본 사용자 이미지가 존재하지 않습니다."));

        // 현재 프로필 이미지가 기본 이미지가 아닌 경우에만 삭제 로직 실행
        if (user.getProfileImageFile() != null &&
                !user.getProfileImageFile().getS3Key().equals(DefaultImages.DEFAULT_USER_IMAGE)) {

            Long currentImageId = user.getProfileImageFile().getImageFileId();
            imageFileService.deleteImage(currentImageId);
            log.info("사용자 ID {}의 기존 프로필 이미지가 삭제되었습니다. 이미지 ID: {}", userId, currentImageId);
        }

        // 사용자 엔티티의 프로필 이미지를 기본 이미지로 업데이트
        user.updateProfileImageFile(defaultImageFile);

        // DTO로 변환하여 반환
        return convertToMyPageResponseDto(user);
    }

    private UserMyPageResponseDto convertToMyPageResponseDto(User user) {
        String profileImageUrl = (user.getProfileImageFile() != null)
                ? user.getProfileImageFile().getImageUrl()
                : null;

        return UserMyPageResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(profileImageUrl)
                .oauthProvider(user.getOauthProvider())
                .role(user.getRole())
                .subscriptionStatus(user.getSubscriptionStatus())
                .build();
    }

}
