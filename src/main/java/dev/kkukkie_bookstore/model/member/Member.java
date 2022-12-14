package dev.kkukkie_bookstore.model.member;

import dev.kkukkie_bookstore.model.base.BaseEntity;
import dev.kkukkie_bookstore.model.file.image.ImageFile;
import dev.kkukkie_bookstore.model.item.book.Book;
import dev.kkukkie_bookstore.model.member.role.MemberRole;
import dev.kkukkie_bookstore.model.team.Team;
import lombok.*;

import javax.crypto.spec.IvParameterSpec;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String loginId;
    private String password;

    private String username;
    private int age;

    private String role = MemberRole.NORMAL;

    @Embedded
    private ImageFile profileImgFile;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "team_id")
    @ToString.Exclude
    private Team team;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private List<Book> books = new ArrayList<>();

    // Only for authentication code (by kakao)
    @Transient
    private String authenticationCode;

    @Transient
    private IvParameterSpec ivParameterSpec;

    ////////////////////////////////////////////////////////

    public Member(String username) {
        this(username, 0);
    }

    public Member(String loginId, String password, String username) {
        this.loginId = loginId;
        this.password = password;
        this.username = username;
        this.age = 0;
        this.team = null;
    }

    public Member(String loginId, String password, String username, int age, Team team) {
        this.loginId = loginId;
        this.password = password;
        this.username = username;
        this.age = age;
        this.team = team;
    }

    public Member(String loginId, String username, int age, Team team) {
        this.loginId = loginId;
        this.password = UUID.randomUUID().toString();
        this.username = username;
        this.age = age;
        this.team = team;
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;

        if (team != null) {
            changeTeam(team);
        }
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        setCreateDateTime(now);
        setLastModifiedDateTime(now);

        setCreatedBy(username);
        setLastModifiedBy(username);
    }

    @PreUpdate
    public void preUpdate() {
        setLastModifiedDateTime(LocalDateTime.now());
        setLastModifiedBy(username);
    }

    private void changeTeam(Team team) {
        this.team = team;
        team.addMember(this);
    }

    @Override
    public String toString() {
        return "Member{" +
                "(" + this.hashCode() + ")" +
                "id=" + id +
                ", imageFile='" + profileImgFile + '\'' +
                ", loginId='" + loginId + '\'' +
                ", password='" + password + '\'' +
                ", username='" + username + '\'' +
                ", age=" + age +
                ", role=" + role +
                ", books=" + books +
                '}';
    }
}
