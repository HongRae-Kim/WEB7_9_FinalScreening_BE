package com.back.matchduo.domain.review.dto.response;

public record ReviewDistributionResponse(
        Long userId,
        String nickname,
        Long goodCount,
        Long normalCount,
        Long badCount,
        Long totalCount,
        Double goodRatio,
        Double normalRatio,
        Double badRatio
) {
    public static ReviewDistributionResponse of(Long userId, String nickname, long good, long normal, long bad) {
        long total = good + normal + bad;

        if (total == 0) {
            return new ReviewDistributionResponse(userId, nickname, 0L, 0L, 0L, 0L, 0.0, 0.0, 0.0);
        }

        return new ReviewDistributionResponse(
                userId,
                nickname,
                good,
                normal,
                bad,
                total,
                calculatePercentage(good, total),
                calculatePercentage(normal, total),
                calculatePercentage(bad, total)
        );
    }

    private static double calculatePercentage(long count, long total) {
        double ratio = (count / (double) total) * 100;
        return Math.round(ratio * 10) / 10.0;
    }
}