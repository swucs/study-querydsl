package com.study.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.QTeam;
import com.study.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import java.util.List;

import static com.study.querydsl.entity.QMember.member;
import static com.study.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {

        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("Member1", 10, teamA);
        Member member2 = new Member("Member2", 20, teamA);
        Member member3 = new Member("Member3", 30, teamB);
        Member member4 = new Member("Member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }


    @Test
    public void startJPQL() {
        String qlString = "select m from Member m where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "Member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("Member1");

    }

    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("Member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("Member1");

    }

    @Test
    public void search() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.username.eq("Member1")
                        , member.age.eq(10)
                )
                .fetch();

        assertThat(members.size()).isEqualTo(1);
    }


    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("Member5", 100));
        em.persist(new Member("Member6", 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);
        assertThat(member5.getUsername()).isEqualTo("Member5");
        assertThat(member6.getUsername()).isEqualTo("Member6");
        assertThat(memberNull.getUsername()).isNull();
    }


    @Test
    public void paging1() {

        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {

        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(memberQueryResults.getTotal()).isEqualTo(4);
        assertThat(memberQueryResults.getLimit()).isEqualTo(2);
        assertThat(memberQueryResults.getOffset()).isEqualTo(1);
        assertThat(memberQueryResults.getResults().size()).isEqualTo(2);
    }



    @Test
    public void aggregation() {
        List<Tuple> members = queryFactory
                .select(
                        member.count()
                        , member.age.sum()
                        , member.age.avg()
                        , member.age.max()
                        , member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = members.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    @Test
    public void groupBy() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);


        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    @Test
    public void join() {

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("Member1", "Member2");
    }

    /**
     * 연관관계가 없는 조인
     * cross join
     * 카테시안 곱이라고 했었음.
     * 이 세타 조인은 외부조인은 되지 않음
     */
    @Test
    public void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");


    }

    /**
     * 회원과 팀을 조회하면서 팀이름이 teamA인 팀만 조회, 멤버는 모두 조회
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> tuples = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : tuples) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계 없는 외부 조인
     * 회원의 이름과 팀의 이름이 같은 대상 외부조인
     */
    @Test
    public void join_on_no_relation() {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamA"));

        List<Tuple> tuples = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : tuples) {
            System.out.println("tuple = " + tuple);
        }

    }

}
