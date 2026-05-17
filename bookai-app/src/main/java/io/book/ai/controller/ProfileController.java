package io.book.ai.controller;

import io.book.ai.api.UserProfileRequest;
import io.book.ai.repository.UserProfileRepository;
import io.book.ai.repository.entity.UserProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileRepository profileRepository;

    @GetMapping
    public List<UserProfileEntity> listProfiles() {
        return profileRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileEntity createProfile(@RequestBody UserProfileRequest request) {
        UserProfileEntity entity = new UserProfileEntity(
                request.profileId(), request.displayName(),
                request.style(), request.responseFormat(), request.constraints());
        return profileRepository.save(entity);
    }

    @PutMapping("/{profileId}")
    public UserProfileEntity updateProfile(@PathVariable String profileId,
                                           @RequestBody UserProfileRequest request) {
        UserProfileEntity entity = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        entity.update(request.displayName(), request.style(), request.responseFormat(), request.constraints());
        return profileRepository.save(entity);
    }

    @DeleteMapping("/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable String profileId) {
        profileRepository.deleteById(profileId);
    }
}
