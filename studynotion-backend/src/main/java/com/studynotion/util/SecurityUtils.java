
package com.studynotion.util;

import com.studynotion.entity.User;
import com.studynotion.exception.AppException;
import com.studynotion.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private static UserRepository staticUserRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        SecurityUtils.staticUserRepository = userRepository;
    }

    public static Long getCurrentUserId() {
        String email = getCurrentUserEmail();
        if (staticUserRepository == null) {
            throw new AppException("Repository not initialized", 500);
        }
        User user = staticUserRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", 404));
        return user.getId();
    }

    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException("Not authenticated", 401);
        }
        return auth.getName();
    }

    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }
}