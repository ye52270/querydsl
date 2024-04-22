package hello.querydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;

import java.util.List;

import static hello.querydsl.entity.QMember.member;
import static hello.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Slf4j
public class QueryDSLBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;
    @Autowired
    private OrderedFormContentFilter formContentFilter;

    @BeforeEach
    void setUp() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamA);
        Member member3 = new Member("member3", 10, teamB);
        Member member4 = new Member("member4", 10, teamB);
        Member member5 = new Member(null, 100);
        Member member6 = new Member("member5", 100);
        Member member7 = new Member("member6", 100);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        em.persist(member5);
        em.persist(member6);
        em.persist(member7);
    }

    @Test
    void startJPQL() {
        //member1 을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDSL() {
        queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .selectFrom(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username
                                .eq("member1")
                                .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    void resultFetch() {
/*        List<Member> members = queryFactory
                .selectFrom(member)
                .fetch();

        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member2"))
                .fetchOne();*/

        QueryResults<Member> results = queryFactory
                .select(member)
                .from(member)
                .fetchResults();

        log.info("member count : {}", results.getTotal());

        JPAQuery<Long> count = queryFactory
                .select(member.count())
                .from(member);

        log.info("member count : {}", count);
    }

    @Test
    void sort() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    void paging1() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.username.asc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        Long count = queryFactory
                .select(member.count())
                .from(member)
                .orderBy(member.username.asc())
                .groupBy(member.username)
                .offset(1)
                .limit(2)
                .fetchOne();

        log.info("member count : {}", count);
    }

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        log.info("member.count : {}", tuple.get(member.count()));
        log.info("member.sum : {}", tuple.get(member.age.sum()));
        log.info("member.avg : {}", tuple.get(member.age.avg()));
        log.info("member.max : {}", tuple.get(member.age.max()));
        log.info("member.min : {}", tuple.get(member.age.min()));
    }

    @Test
    void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        log.info("teamA : {}, teamA avg age : {}",
                teamA.get(team.name), teamA.get(member.age.avg()));

        log.info("teamB : {}, teamA avg age : {}",
                teamB.get(team.name), teamB.get(member.age.avg()));

    }

    @Test
    void joinTest() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(members).extracting("username")
                .containsExactly("member1", "member2");

    }

    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> members = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(members).extracting("username")
                .containsExactly("teamA", "teamB");

    }

    //todo : JPQL , select m, t from Member m left join m.team t on t.name = 'teamA'
    @Test
    void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA")) //todo : inner join 일 경우에는 on 대신에 where 을 써도 동일하다
                .fetch();

        for (Tuple tuple : result) {
            log.info("tuple = {}", tuple);
        }
    }

    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            log.info("tuple = {}", tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetch_join_no() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        log.info("---------------------------------------");
        assertThat(isLoaded).as("패치 조회 미적용").isFalse();
    }

    @Test
    void fetch_join() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        log.info("---------------------------------------");
        assertThat(isLoaded).as("패치 조회 적용").isTrue();
    }

    @Test
    void sub_query() {
        QMember memberSub = new QMember("memberSub");

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();
        log.info("나이가 가장 많은 사람은 {}", members);
    }

    @Test
    void sub_query_join2() {
        QMember memberSub = new QMember("memberSub");

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        log.info("나이가 평균 이상인 사람은 {}", members);
        assertThat(members).extracting("age").contains(100);
    }

    @Test
    void sub_query_in() {
        QMember memberSub = new QMember("memberSub");
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.eq(10))
                ))
                .fetch();
        log.info("나이가 10 인 사람은 {}", members);
        assertThat(members).extracting("username")
                .containsExactly("member1", "member2", "member3", "member4");
    }

    //todo : JPA subquery 는 from 절의 subquery 는 지원하지 않는다(where, select 절에서만 사용 가능)
    @Test
    void sub_query_select() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> tuples = queryFactory
                .select(member.username,
                        JPAExpressions //todo : static import 가 가능하다
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();
        for (Tuple tuple : tuples) {
            log.info("tuples = {}", tuple);
        }

    }

    @Test
    void case_sql() {
        List<String> members = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(100).then("백살")
                        .otherwise("기타")
                )
                .distinct()
                .from(member)
                .fetch();

        log.info("멤버의 나이는 {}", members);
    }

    @Test
    void complex_case_sql() {
        List<Tuple> memberTuples = queryFactory
                .select(member.count(),
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0~20살")
                                .when(member.age.between(21, 30)).then("21~30살")
                                .otherwise("기타")
                )
                .from(member)
                .groupBy(member.age)
                .fetch();
        log.info("멤버의 나이 분포 {} : {}", memberTuples.get(0),memberTuples.get(1));
    }

    @Test
    void sql_constant(){
        List<String> members = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        log.info("members concat ----  {}", members);
    }

    @Test
    void sql_concat(){

    }
}

