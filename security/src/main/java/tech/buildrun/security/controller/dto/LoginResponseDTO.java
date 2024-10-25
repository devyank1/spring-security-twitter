package tech.buildrun.security.controller.dto;

public record LoginResponseDTO(String accessToken, Long expiresIn) {
}
