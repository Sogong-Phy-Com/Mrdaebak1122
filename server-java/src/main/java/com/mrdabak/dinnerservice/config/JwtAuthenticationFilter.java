package com.mrdabak.dinnerservice.config;

import com.mrdabak.dinnerservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String requestPath = request.getRequestURI();
        
        System.out.println("========== [JWT Filter] 요청 처리 시작 ==========");
        System.out.println("[JWT Filter] 요청 경로: " + requestPath);
        System.out.println("[JWT Filter] 요청 메서드: " + request.getMethod());
        System.out.println("[JWT Filter] Authorization 헤더: " + (authHeader != null ? "존재 (길이: " + authHeader.length() + ")" : "없음"));
        if (authHeader != null) {
            System.out.println("[JWT Filter] Authorization 헤더 앞부분: " + (authHeader.length() > 30 ? authHeader.substring(0, 30) + "..." : authHeader));
        }
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JWT Filter] Authorization 헤더가 없거나 Bearer 형식이 아닙니다.");
            System.out.println("[JWT Filter] 요청 경로: " + requestPath);
            
            // 인증이 필요한 경로인 경우 로그만 남기고 계속 진행 (Spring Security가 401 반환)
            if (requestPath.startsWith("/api/") && 
                !requestPath.startsWith("/api/auth/") && 
                !requestPath.startsWith("/api/health") && 
                !requestPath.startsWith("/api/menu/")) {
                System.out.println("[JWT Filter] 경고: 인증이 필요한 경로인데 토큰이 없습니다.");
            }
            
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            System.out.println("[JWT Filter] 토큰 추출 완료 (길이: " + jwt.length() + ")");
            System.out.println("[JWT Filter] 토큰 앞부분: " + (jwt.length() > 20 ? jwt.substring(0, 20) + "..." : jwt));
            
            // 먼저 토큰 유효성 검사
            boolean isValid = false;
            try {
                isValid = jwtService.isTokenValid(jwt);
                System.out.println("[JWT Filter] 토큰 유효성 검사 결과: " + isValid);
            } catch (Exception e) {
                System.out.println("[JWT Filter] 토큰 유효성 검사 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
            
            if (!isValid) {
                System.out.println("[JWT Filter] 경고: 토큰이 유효하지 않습니다. (만료되었거나 서명이 잘못됨)");
                filterChain.doFilter(request, response);
                return;
            }
            
            // 토큰이 유효하면 사용자 정보 추출
            String userId = null;
            String role = null;
            try {
                userId = jwtService.extractUserId(jwt);
                role = jwtService.extractRole(jwt);
                System.out.println("[JWT Filter] 사용자 ID 추출: " + userId);
                System.out.println("[JWT Filter] 역할 추출: " + role);
            } catch (Exception e) {
                System.out.println("[JWT Filter] 사용자 정보 추출 중 오류: " + e.getMessage());
                e.printStackTrace();
                filterChain.doFilter(request, response);
                return;
            }

            if (userId == null || userId.isEmpty()) {
                System.out.println("[JWT Filter] 에러: 사용자 ID가 null이거나 비어있습니다.");
                filterChain.doFilter(request, response);
                return;
            }

            // SecurityContext에 인증 설정
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // role이 null이거나 빈 문자열인 경우 기본값 설정
                String userRole = (role != null && !role.isEmpty()) ? role.toUpperCase() : "CUSTOMER";
                String authority = "ROLE_" + userRole;
                
                System.out.println("[JWT Filter] 설정할 권한: " + authority);
                
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("[JWT Filter] 인증 성공 - SecurityContext에 설정됨");
                System.out.println("[JWT Filter] 인증된 사용자 ID: " + userId);
                System.out.println("[JWT Filter] 인증된 권한: " + authority);
            } else {
                System.out.println("[JWT Filter] 정보: 이미 인증된 상태입니다.");
                Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
                System.out.println("[JWT Filter] 기존 인증 사용자: " + existingAuth.getName());
                System.out.println("[JWT Filter] 기존 인증 권한: " + existingAuth.getAuthorities());
            }
        } catch (Exception e) {
            System.out.println("[JWT Filter] 에러: 토큰 처리 중 예외 발생");
            System.out.println("[JWT Filter] 요청 경로: " + request.getRequestURI());
            System.out.println("[JWT Filter] 예외 타입: " + e.getClass().getName());
            System.out.println("[JWT Filter] 예외 메시지: " + e.getMessage());
            System.out.println("[JWT Filter] 예외 스택 트레이스:");
            e.printStackTrace();
            
            // 인증이 필요한 경로인지 확인
            String path = request.getRequestURI();
            if (path.startsWith("/api/") && 
                !path.startsWith("/api/auth/") && 
                !path.startsWith("/api/health") && 
                !path.startsWith("/api/menu/")) {
                System.out.println("[JWT Filter] 경고: 인증이 필요한 경로인데 토큰 처리 실패");
                System.out.println("[JWT Filter] 경고: 이 요청은 401 Unauthorized로 응답될 수 있습니다.");
            }
            // Token is invalid, continue without authentication
        }

        System.out.println("[JWT Filter] FilterChain.doFilter 호출 전");
        System.out.println("[JWT Filter] SecurityContext 인증 상태: " + 
            (SecurityContextHolder.getContext().getAuthentication() != null ? "인증됨" : "인증 안됨"));
        
        filterChain.doFilter(request, response);
        
        System.out.println("[JWT Filter] FilterChain.doFilter 호출 후");
        System.out.println("========== [JWT Filter] 요청 처리 완료 ==========");
    }
}

