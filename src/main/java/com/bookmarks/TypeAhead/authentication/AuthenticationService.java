package com.bookmarks.TypeAhead.authentication;

import com.bookmarks.TypeAhead.Exceptions.UserDoesNotExists;
import com.bookmarks.TypeAhead.dto.SignUpUserDto;
import com.bookmarks.TypeAhead.dto.UserResponseDto;
import com.bookmarks.TypeAhead.entity.Users;
import com.bookmarks.TypeAhead.repository.UserRepository;
import com.bookmarks.TypeAhead.service.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(UserRepository userRepository,
                                 BCryptPasswordEncoder bCryptPasswordEncoder,
                                 JwtService jwtService) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponseDto signUp(SignUpUserDto signUpUserDto) {
        Optional<Users> user = this.userRepository.findByEmail(signUpUserDto.getEmail());

        if (user.isPresent()) {
            throw new RuntimeException("User Already Exists");
        }

        String hashedPassword = this.bCryptPasswordEncoder.encode(signUpUserDto.getPassword());

        Users userForDb = new Users();
        userForDb.setDisplayName(signUpUserDto.getDisplayName());
        userForDb.setEmail(signUpUserDto.getEmail());
        userForDb.setPassword(hashedPassword);
        userRepository.save(userForDb);

        String token = this.jwtService.generateToken(userForDb.getEmail());

        return buildResponse(userForDb, token);
    }

    public UserResponseDto signIn(String email, String password) {
        Optional<Users> user = this.userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new UserDoesNotExists("User doesn't exists");
        }

        Users userForDb = user.get();

        if (!bCryptPasswordEncoder.matches(password, userForDb.getPassword())){
            throw new IllegalArgumentException("Invalid Credentials");
        }

        String token = this.jwtService.generateToken(userForDb.getEmail());

        return buildResponse(userForDb, token);
    }

    public UserResponseDto buildResponse (Users user, String token ) {
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setDisplayName(user.getDisplayName());
        userResponseDto.setEmail(user.getEmail());
        userResponseDto.setToken(token);
        return userResponseDto;
    }
}
