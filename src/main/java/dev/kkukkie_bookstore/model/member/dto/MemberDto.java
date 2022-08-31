package dev.kkukkie_bookstore.model.member.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberDto {

    private long id;
    private String username;
    private int age;

    private String loginId;

    private String role;

    private String teamName;

    @QueryProjection
    public MemberDto(long id, String username, int age, String loginId, String role, String teamName) {
        this.id = id;
        this.username = username;
        this.age = age;
        this.loginId = loginId;
        this.role = role;
        this.teamName = teamName;
    }

}