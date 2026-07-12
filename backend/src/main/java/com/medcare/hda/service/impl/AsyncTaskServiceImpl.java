package com.medcare.hda.service.impl;

import com.medcare.hda.config.AsyncConfig;
import com.medcare.hda.service.AsyncTaskService;
import com.medcare.hda.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private final PointService pointService;

    @Override
    @Async(AsyncConfig.EXECUTOR)
    public void markTaskReadyAsync(Long userId, String type) {
        pointService.markTaskReady(userId, type);
    }
}
