package com.swyp.picke.domain.notification.repository;

import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.enums.DevicePlatform;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByFcmToken(String fcmToken);

    List<UserDevice> findAllByUserId(Long userId);

    List<UserDevice> findAllByUserIdAndPlatform(Long userId, DevicePlatform platform);

    void deleteByUserIdAndFcmToken(Long userId, String fcmToken);
}
