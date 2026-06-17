package com.swyp.picke.domain.notification.service;

import com.swyp.picke.domain.notification.dto.request.RegisterDeviceRequest;
import com.swyp.picke.domain.notification.entity.UserDevice;
import com.swyp.picke.domain.notification.repository.UserDeviceRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerDevice(Long userId, RegisterDeviceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userDeviceRepository.findByFcmToken(request.fcmToken())
                .ifPresentOrElse(
                        device -> device.reassign(user, request.platform()),
                        () -> userDeviceRepository.save(
                                UserDevice.builder()
                                        .user(user)
                                        .fcmToken(request.fcmToken())
                                        .platform(request.platform())
                                        .build()
                        )
                );
    }

    @Transactional
    public void unregisterDevice(Long userId, String fcmToken) {
        userDeviceRepository.deleteByUserIdAndFcmToken(userId, fcmToken);
    }
}
