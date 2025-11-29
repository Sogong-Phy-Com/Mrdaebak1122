# Spring Boot 설정 가이드

## 1. 프로젝트 구조
```
src/main/java/com/mrdinner/
├── api/controller/          # REST API 컨트롤러
│   ├── CustomerController.java
│   ├── MenuController.java
│   ├── OrderController.java
│   └── AdminController.java
├── domain/                  # 도메인 모델 (기존)
├── service/                 # 서비스 레이어
│   ├── CustomerService.java
│   ├── MenuService.java
│   └── (기존 서비스들)
└── config/                  # 설정 클래스
    └── WebConfig.java
```

## 2. Maven 의존성 (pom.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.14</version>
        <relativePath/>
    </parent>
    
    <groupId>com.mrdinner</groupId>
    <artifactId>mr-dinner-service</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <java.version>8</java.version>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <!-- Development Tools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## 3. Spring Boot 메인 클래스
```java
package com.mrdinner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.mrdinner.domain")
@EnableJpaRepositories("com.mrdinner.repository")
public class MrDinnerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MrDinnerServiceApplication.class, args);
    }
}
```

## 4. 데이터베이스 설정 (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mrdaeBak_dinner_service?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
    database-platform: org.hibernate.dialect.MySQL8Dialect

server:
  port: 8080
  servlet:
    context-path: /api

logging:
  level:
    com.mrdinner: DEBUG
    org.springframework.web: DEBUG
```

## 5. CORS 설정 (WebConfig.java)
```java
package com.mrdinner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

## 6. API 엔드포인트 목록

### 고객 관련 API
- `POST /api/customers/register` - 회원가입
- `POST /api/customers/login` - 로그인
- `GET /api/customers/{customerId}` - 고객 정보 조회
- `GET /api/customers/{customerId}/orders` - 고객 주문 내역

### 메뉴 관련 API
- `GET /api/menu/dinners` - 모든 디너 메뉴 조회
- `GET /api/menu/dinners/{dinnerType}` - 특정 디너 상세 정보
- `GET /api/menu/serving-styles` - 서빙 스타일 목록
- `POST /api/menu/calculate-price` - 가격 계산
- `GET /api/menu/champagne-feast/constraints` - 샴페인 축제 디너 제약사항

### 주문 관련 API
- `POST /api/orders` - 새 주문 생성
- `GET /api/orders/{orderId}` - 주문 조회
- `GET /api/orders/customer/{customerId}` - 고객 주문 내역
- `PUT /api/orders/{orderId}/status` - 주문 상태 업데이트
- `DELETE /api/orders/{orderId}` - 주문 취소
- `PUT /api/orders/{orderId}/items` - 주문 수정

### 관리자 API
- `GET /api/admin/dashboard` - 대시보드 통계
- `GET /api/admin/orders` - 모든 주문 목록
- `GET /api/admin/customers` - 고객 목록
- `GET /api/admin/inventory` - 재고 현황
- `GET /api/admin/deliveries` - 배달 현황
- `PUT /api/admin/deliveries/{deliveryId}/assign` - 배달원 배정

## 7. 실행 방법

### 개발 환경 실행
```bash
# 프로젝트 빌드
mvn clean install

# Spring Boot 애플리케이션 실행
mvn spring-boot:run

# 또는 JAR 파일로 실행
java -jar target/mr-dinner-service-1.0.0.jar
```

### Docker 실행 (선택사항)
```dockerfile
FROM openjdk:8-jdk-alpine
COPY target/mr-dinner-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 8. 테스트 방법

### API 테스트 예시 (curl)
```bash
# 회원가입
curl -X POST http://localhost:8080/api/customers/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "김철수",
    "email": "kim@email.com",
    "phoneNumber": "010-1234-5678",
    "password": "password123",
    "streetAddress": "서울시 강남구 테헤란로 123",
    "city": "서울시",
    "state": "강남구",
    "postalCode": "06292"
  }'

# 메뉴 조회
curl -X GET http://localhost:8080/api/menu/dinners

# 가격 계산
curl -X POST http://localhost:8080/api/menu/calculate-price \
  -H "Content-Type: application/json" \
  -d '{
    "dinnerType": "발렌타인 디너",
    "servingStyle": "그랜드",
    "quantity": 1
  }'
```

이제 앱/웹 프론트엔드에서 이 REST API를 호출하여 백엔드와 연동할 수 있습니다!
