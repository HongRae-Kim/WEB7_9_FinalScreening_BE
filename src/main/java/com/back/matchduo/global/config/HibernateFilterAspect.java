package com.back.matchduo.global.config;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Session;
import org.hibernate.UnknownFilterException;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class HibernateFilterAspect {

    private final EntityManager entityManager;

    public HibernateFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // 서비스나 레포지토리 등 트랜잭션이 일어나는 곳을 감싸서 실행
    // 보통 Service 계층의 메소드 실행 시점에 적용합니다.
    @Around("execution(* com.back.matchduo..service..*(..))")
    public Object enableFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 현재 트랜잭션의 세션을 가져옴
        Session session = entityManager.unwrap(Session.class);

        // 2. 필터가 정의되어 있는 경우에만 활성화
        // 필터가 없으면 예외를 무시하고 계속 진행
        try {
            session.enableFilter("softDeleteFilter");
        } catch (UnknownFilterException e) {
            // 필터가 정의되지 않은 경우 무시하고 계속 진행
            // GameAccount는 SoftDeletableEntity를 상속받지 않으므로 필터가 필요 없음
        }

        // 3. 원래 메소드 실행
        return joinPoint.proceed();
    }
}