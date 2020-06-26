package com.youngthree.querydslstudy.entity;

import lombok.*;

import javax.persistence.*;


@ToString(of={"id","username","age"})
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member {

    @Id @GeneratedValue
    private Long id;

    private String username;

    private int age;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="team_id")
    private Team team;

    public Member(String username, int age, Team team) {
        this.username=username;
        this.age=age;
        this.team=team;
    }

    public Member(String username, int age){
        this(username,age,null);
    }

    public Member(String username){
        this(username,0, null);
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
