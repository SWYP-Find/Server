package com.swyp.picke.domain.notification.service;

import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.enums.DevicePlatform;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.UserDeviceRepository;
import com.swyp.picke.global.infra.apns.service.ApnsPushService;
import com.swyp.picke.global.infra.fcm.service.FcmPushService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 앱 내 알림(Notification)과 푸시를 함께 발송하는 알림 정책별 디스패치 서비스.
 * Android는 FCM, iOS는 APNs로 직접 발송한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationService notificationService;
    private final UserDeviceRepository userDeviceRepository;
    private final FcmPushService fcmPushService;
    private final ApnsPushService apnsPushService;

    @Value("${picke.baseUrl}")
    private String baseUrl;

    /**
     * 새로운 배틀 발행 시 전체 사용자에게 인앱 broadcast 알림 + FCM 푸시를 발송한다.
     */
    public void notifyNewBattle(Long battleId, String battleTitle) {
        String body = "\"" + battleTitle + "\"에 지금 참여해보세요!";

        notificationService.createBroadcastNotification(NotificationDetailCode.NEW_BATTLE, body, battleId);

        Map<String, String> data = Map.of(
                "type", "BATTLE",
                "battleId", String.valueOf(battleId),
                "url", baseUrl + "/battle/" + battleId
        );

        String pushTitle = NotificationDetailCode.NEW_BATTLE.getDefaultTitle();
        for (UserDevice device : userDeviceRepository.findAll()) {
            sendPush(device, pushTitle, body, data);
        }
    }

    /**
     * 내가 작성한 답글에 좋아요가 달렸을 때 답글 작성자에게 인앱 알림 + 푸시를 발송한다.
     */
    public void notifyCommentLike(Long commentAuthorId, Long perspectiveId, Long commentId, String battleTitle) {
        String body = "\"" + battleTitle + "\" 배틀에 남긴 내 답글에 좋아요가 달렸어요.";

        notificationService.createNotification(
                commentAuthorId, NotificationDetailCode.COMMENT_LIKE, body, commentId, perspectiveId);

        sendCommentPush(commentAuthorId, NotificationDetailCode.COMMENT_LIKE, body, perspectiveId, commentId);
    }

    /**
     * 내가 작성한 글에 새로운 답글이 달렸을 때 글 작성자에게 인앱 알림 + 푸시를 발송한다.
     */
    public void notifyNewComment(Long perspectiveAuthorId, Long perspectiveId, Long commentId, String battleTitle) {
        String body = "\"" + battleTitle + "\" 배틀에 남긴 내 글에 새로운 답글이 달렸어요.";

        notificationService.createNotification(
                perspectiveAuthorId, NotificationDetailCode.NEW_COMMENT, body, commentId, perspectiveId);

        sendCommentPush(perspectiveAuthorId, NotificationDetailCode.NEW_COMMENT, body, perspectiveId, commentId);
    }

    private void sendCommentPush(Long userId, NotificationDetailCode detailCode, String body, Long perspectiveId, Long commentId) {
        Map<String, String> data = Map.of(
                "type", "COMMENT",
                "perspectiveId", String.valueOf(perspectiveId),
                "commentId", String.valueOf(commentId),
                "url", baseUrl + "/perspective/" + perspectiveId + "?commentId=" + commentId
        );

        String pushTitle = detailCode.getDefaultTitle();
        for (UserDevice device : userDeviceRepository.findAllByUserId(userId)) {
            sendPush(device, pushTitle, body, data);
        }
    }

    private void sendPush(UserDevice device, String title, String body, Map<String, String> data) {
        if (device.getPlatform() == DevicePlatform.IOS) {
            apnsPushService.send(device, title, body, data);
        } else {
            fcmPushService.send(device, title, body, data);
        }
    }
}
