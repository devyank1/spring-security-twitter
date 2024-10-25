package tech.buildrun.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;
import tech.buildrun.security.controller.dto.LoginRequestDTO;
import tech.buildrun.security.controller.dto.LoginResponseDTO;
import tech.buildrun.security.entities.Role;
import tech.buildrun.security.entities.User;
import tech.buildrun.security.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TokenController {

    //inject the cryptography of the Token JWT.
    private final JwtEncoder jwtEncoder;

    //inject the user repository because, the database will validate if the user exists and compare the passwords
    private final UserRepository userRepository;

    //inject the encrypter (password)
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public TokenController(JwtEncoder jwtEncoder,
                           UserRepository userRepository,
                           BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO){

        var user = userRepository.findByUsername(loginRequestDTO.username());
        //if the user don't exist = incorrect
        //if exists compare the login and password
        if (user.isEmpty() || user.get().isLoginCorrect(loginRequestDTO, bCryptPasswordEncoder)) {
            throw new BadCredentialsException("User or password is invalid!");
        }

        var now = Instant.now();
        var expiresIn = 300L;

        //return the information (user) OR (admin) in the token
        var scopes = user.get().getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.joining(" "));

        //return the token JWT in the requisition
        var claims = JwtClaimsSet.builder()
                .issuer("myfirstsecurity")
                .subject(user.get().getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scopes)
                .build();

        //this token jwt uses the claims above
        var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return ResponseEntity.ok(new LoginResponseDTO(jwtValue, expiresIn));
    }

}
