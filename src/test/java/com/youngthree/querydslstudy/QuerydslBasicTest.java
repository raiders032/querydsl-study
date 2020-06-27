package com.youngthree.querydslstudy;


import com.querydsl.jpa.impl.JPAQueryFactory;
import com.youngthree.querydslstudy.entity.Member;
import com.youngthree.querydslstudy.entity.QMember;
import com.youngthree.querydslstudy.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static com.youngthree.querydslstudy.entity.QMember.*;

@SpringBootTest
@Transactional
class QuerydslBasicTest {

    @Autowired
    private EntityManager em;

    private JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void jpqlTest1(){
        Member findMember = em.createQuery("select m from Member m where m.username=:username", Member.class)
                .setParameter("username", "member1").getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void querydslTest1(){
        QMember m = new QMember("m");
        Member findMember = queryFactory.select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void querydslTest1V2(){
        //같은 테이블을 조인해야 하는 경우가 아니면 기본 인스턴스를 사용하자
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void querydslTest2(){
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1").and(member.age.between(10, 20)))
                .fetchOne();
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }
}