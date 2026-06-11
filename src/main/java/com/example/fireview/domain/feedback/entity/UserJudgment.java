package com.example.fireview.domain.feedback.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserJudgment {
    MORE_TRUSTWORTHY("신뢰도가 더 높아요"),
    MORE_RISKY("위험도가 더 높아요"),
    UNDECIDED("판단 보류");

    private final String description;
}
