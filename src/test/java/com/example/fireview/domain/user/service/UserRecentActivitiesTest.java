package com.example.fireview.domain.user.service;

import com.example.fireview.domain.review.entity.ReviewFeedback;
import com.example.fireview.domain.review.repository.ReviewFeedbackRepository;
import com.example.fireview.domain.user.dto.UserActivityResponse;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.domain.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRecentActivitiesTest {

    private static final String EMAIL = "user@example.com";

    @Mock UserRepository userRepository;
    @Mock WishlistRepository wishlistRepository;
    @Mock ReviewFeedbackRepository feedbackRepository;
    @InjectMocks UserService userService;

    @Test
    void 외부_리뷰_피드백이_섞여도_최근_활동을_돌려준다() {
        User user = User.builder().id(4L).email(EMAIL).nickname("사용자").build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUser_IdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());

        ReviewFeedback internal = mock(ReviewFeedback.class);
        when(internal.reviewIdOrNull()).thenReturn(11L);
        when(internal.getCreatedAt()).thenReturn(LocalDateTime.now().minusHours(1));

        ReviewFeedback external = mock(ReviewFeedback.class);
        when(external.reviewIdOrNull()).thenReturn(null);
        when(external.getExternalReviewId()).thenReturn("r-99");
        when(external.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(feedbackRepository.findByUserIdWithReview(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(internal, external)));

        List<UserActivityResponse> activities = userService.getRecentActivities(EMAIL);

        assertThat(activities).extracting(UserActivityResponse::targetId)
                .containsExactly("r-99", "11");
    }
}
