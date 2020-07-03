package com.youngthree.querydslstudy;


import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.youngthree.querydslstudy.entity.Member;
import com.youngthree.querydslstudy.entity.QMember;
import com.youngthree.querydslstudy.entity.QTeam;
import com.youngthree.querydslstudy.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import java.util.List;

import static com.youngthree.querydslstudy.entity.QMember.*;
import static com.youngthree.querydslstudy.entity.QTeam.*;

@SpringBootTest
@Transactional
class QuerydslBasicTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private EntityManagerFactory emf;

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

    //AND 조건을파라미터로처리
    @Test
    public void querydslTest2(){
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1").and(member.age.between(10, 20)))
                .fetchOne();
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //결과 조회
    @Test
    public void 결과조회_리스트(){
        List<Member> members = queryFactory.selectFrom(member)
                .fetch();
        Assertions.assertThat(members.size()).isEqualTo(4);
    }

    @Test
    public void 결과조회_처음하나(){
        Member member = queryFactory.selectFrom(QMember.member)
                .fetchFirst();
        Assertions.assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    public void 정렬(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory.select(member)
                .from(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member1 = members.get(0);
        Member member2 = members.get(1);
        Member member3 = members.get(2);

        Assertions.assertThat(member1.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member2.getUsername()).isEqualTo("member6");
        Assertions.assertThat(member3.getUsername()).isNull();
    }

    @Test
    public void paging(){
        List<Member> members = queryFactory.select(member)
                .from(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetch();
        Assertions.assertThat(members.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
        Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
        Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
        Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        List<Tuple> tuples = queryFactory.select(member.count(), member.age.sum(), member.age.avg(), member.age.min(), member.age.max())
                .from(member)
                .fetch();
        Tuple tuple = tuples.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
    }

    @Test
    public void join(){
        List<Member> members = queryFactory.select(member)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(members).extracting("username").containsExactly("member1","member2");
    }

    @Test
    public void join_no_fetch(){
        em.flush();
        em.clear();
        Member member = queryFactory.select(QMember.member)
                .from(QMember.member)
                .join(QMember.member.team, team)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();
        Assertions.assertThat(emf.getPersistenceUnitUtil().isLoaded(member.getTeam())).isFalse();
    }

    @Test
    public void fetch_join(){
        em.flush();
        em.clear();
        Member member = queryFactory.select(QMember.member)
                .from(QMember.member)
                .join(QMember.member.team, team).fetchJoin()
                .where(QMember.member.username.eq("member1"))
                .fetchOne();
        Assertions.assertThat(emf.getPersistenceUnitUtil().isLoaded(member.getTeam())).isTrue();
    }

    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg()).from(memberSub)
                ))
                .fetch();
        Assertions.assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }
}

