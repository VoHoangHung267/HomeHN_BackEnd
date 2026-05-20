package com.homehn.backend;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.homehn.backend.config.GeminiProperties;
import com.homehn.backend.config.VnpayProperties;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication
@EnableConfigurationProperties({VnpayProperties.class, GeminiProperties.class})
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
@Configuration
class CloudinaryConfig {
    @Value("${cloudinary.cloud-name}") private String cloudName;
    @Value("${cloudinary.api-key}")    private String apiKey;
    @Value("${cloudinary.api-secret}") private String apiSecret;

    @Bean
    Cloudinary cloudinary() {
        if (cloudName == null || cloudName.isBlank()
                || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException(
                    "Cloudinary config is missing. Set cloudinary.cloud-name, cloudinary.api-key, cloudinary.api-secret."
            );
        }
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));
    }
}

@Service
class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepo;
    CustomUserDetailsService(UserRepository userRepo) { this.userRepo = userRepo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + email));
        return new UserPrincipal(user);
    }
}
