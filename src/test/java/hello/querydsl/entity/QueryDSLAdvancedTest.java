package hello.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.querydsl.dto.MemberDTO;
import hello.querydsl.dto.QMemberDTO;
import hello.querydsl.dto.UserDTO;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;

import java.util.List;

import static hello.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
@Slf4j
public class QueryDSLAdvancedTest {
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

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void simpleProjection() {
        List<String> members = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : members) {
            log.info("s = {}", s);
        }
    }

    @Test
    void tupleProjection() {
        List<Tuple> tuples = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : tuples) {
            log.info("이름 : {}, 나이 : {} ", tuple.get(member.username), tuple.get(member.age));
        }
    }

    //todo : JPA 에서 DTO 사용하기
    @Test
    void find_dto_by_jpql() {
        List<MemberDTO> memberDTO = em.createQuery(
                "select new hello.querydsl.dto.MemberDTO(m.username, m.age)  from Member m",
                MemberDTO.class).getResultList();

        for (MemberDTO dto : memberDTO) {
            log.info("DTO = {}", dto);
        }
    }

    //todo : QueryDSL 에서 DTO 사용하기(setter 를 사용한 property 접근 방법)
    @Test
    void find_dto_by_query_dsl_1() {
        em.flush();
        em.clear();

        List<MemberDTO> memberDTOs = queryFactory
                .select(
                        Projections.bean(
                                MemberDTO.class,
                                member.username,
                                member.age
                        )
                )
                .from(member)
                .fetch();


        for (MemberDTO memberDTO : memberDTOs) {
            log.info("setter DTO = {}", memberDTO);
        }
    }

    //todo : QueryDSL 에서 DTO 사용하기(field 를 사용한 property 접근 방법, Getter/Setter 가 없어도 된다)
    @Test
    void find_dto_by_query_dsl_2() {
        em.flush();
        em.clear();

        List<MemberDTO> memberDTOs = queryFactory
                .select(
                        Projections.fields(
                                MemberDTO.class,
                                member.username,
                                member.age
                        )
                )
                .from(member)
                .fetch();


        for (MemberDTO memberDTO : memberDTOs) {
            log.info("field DTO = {}", memberDTO);
        }
    }

    //todo : QueryDSL 에서 DTO 사용하기(생성자 접근 방법, Getter/Setter 가 없어도 된다)
    @Test
    void find_dto_by_query_dsl_3() {
        em.flush();
        em.clear();

        List<MemberDTO> memberDTOs = queryFactory
                .select(
                        Projections.constructor(
                                MemberDTO.class,
                                member.username,
                                member.age
                        )
                )
                .from(member)
                .fetch();


        for (MemberDTO memberDTO : memberDTOs) {
            log.info("field DTO = {}", memberDTO);
        }
    }

    //todo : alias 사용하기 (field 를 사용한 property 접근 방법, Getter/Setter 가 없어도 된다)
    @Test
    void find_user_dto_by_query_dsl_4() {

        List<UserDTO> userDTOs = queryFactory
                .select(
                        Projections.fields(
                                UserDTO.class,
                                member.username.as("name"), //todo : as 를 사용한 alias 사용
                                member.age
                        )
                )
                .from(member)
                .fetch();


        for (UserDTO userDTO : userDTOs) {
            log.info("user DTO = {}", userDTO);
        }
    }

    //todo : subquery 에서 alias 사용하기 (field 를 사용한 property 접근 방법, Getter/Setter 가 없어도 된다)
    @Test
    void find_user_dto_by_query_dsl_5() {
        QMember memberSub = new QMember("memberSub");

        List<UserDTO> userDTOs = queryFactory
                .select(
                        Projections.fields(
                                UserDTO.class,
                                member.username.as("name"), //todo : as 를 사용한 alias 사용
                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(memberSub.age.avg().intValue())
                                                .from(memberSub)

                                , "age")
                        )
                )
                .from(member)
                .fetch();


        for (UserDTO userDTO : userDTOs) {
            log.info("user DTO = {}", userDTO);
        }
    }

    @Test //todo : MemberDTO 에 @QueryProjection 을 명시해 주면 된다.DTO 도 Q파일로 생성이 된다
    void find_by_query_projection() {
        List<MemberDTO> memberDTOs = queryFactory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : memberDTOs) {
            log.info("memberDTO DTO = {}", memberDTO);
        }
    }

    //todo : 동적 쿼리 BooleanBuilder
    @Test
    void dynamic_query_boolean_builder(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();

        if(usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

        return members;
    }

    //todo : 동적 쿼리 Where 절에서 사용
    @Test
    void dynamic_query_where_param(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        List<Member> members = queryFactory
                .selectFrom(member)
                //.where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();

        return members;
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if(usernameCond == null) {
            return null;
        }

        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if(ageCond == null) {
            return null;
        }
        return member.age.eq(ageCond);
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
}
