package com.example.fireview.domain.review.entity;

public enum TrustGrade {
    SAFE("안전", "#22C55E"),
    SUSPICIOUS("의심", "#EAB308"),
    DANGER("위험", "#EF4444");

    private final String label;
    private final String color;

    TrustGrade(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }

    public static TrustGrade fromScore(double score) {
        if (score >= 80) return SAFE;
        if (score >= 50) return SUSPICIOUS;
        return DANGER;
    }
}