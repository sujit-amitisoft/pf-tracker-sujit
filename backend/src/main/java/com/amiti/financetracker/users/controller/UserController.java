package com.amiti.financetracker.users.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.common.BadRequestException;
import com.amiti.financetracker.domain.entity.UserEntity;
import com.amiti.financetracker.domain.repository.UserRepository;
import com.amiti.financetracker.users.dto.UserDtos.MeResponse;
import com.amiti.financetracker.users.dto.UserDtos.UpdateMeRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private static final long MAX_AVATAR_BYTES = 3L * 1024 * 1024;

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public MeResponse me(Authentication authentication) {
        UserEntity user = userRepository.findById(currentUserId(authentication))
                .orElseThrow(() -> new BadRequestException("User not found"));
        boolean hasAvatar = user.getAvatarBytes() != null && user.getAvatarBytes().length > 0;
        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), hasAvatar);
    }

    @PatchMapping
    public MeResponse update(Authentication authentication, @Valid @RequestBody UpdateMeRequest request) {
        UserEntity user = userRepository.findById(currentUserId(authentication))
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setDisplayName(request.displayName().trim());
        UserEntity saved = userRepository.save(user);
        boolean hasAvatar = saved.getAvatarBytes() != null && saved.getAvatarBytes().length > 0;
        return new MeResponse(saved.getId(), saved.getEmail(), saved.getDisplayName(), hasAvatar);
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar(Authentication authentication) {
        UserEntity user = userRepository.findById(currentUserId(authentication))
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getAvatarBytes() == null || user.getAvatarBytes().length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String contentType = user.getAvatarContentType();
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(mediaType)
                .body(user.getAvatarBytes());
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BadRequestException("Avatar must be under 3MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BadRequestException("Avatar must be an image");
        }

        UserEntity user = userRepository.findById(currentUserId(authentication))
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setAvatarContentType(contentType);
        user.setAvatarBytes(file.getBytes());
        user.setAvatarUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @DeleteMapping("/avatar")
    public void deleteAvatar(Authentication authentication) {
        UserEntity user = userRepository.findById(currentUserId(authentication))
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setAvatarContentType(null);
        user.setAvatarBytes(null);
        user.setAvatarUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
