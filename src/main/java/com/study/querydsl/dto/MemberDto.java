package com.study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection //순수한 모델이 아니라고 판단될 수도 있어서 선택의 문제가 있을 수 있다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
