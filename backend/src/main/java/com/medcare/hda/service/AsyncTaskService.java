package com.medcare.hda.service;

/** 异步任务：非主链路耗时操作收敛到这里，未来切 MQ 只换实现，业务代码不动 */
public interface AsyncTaskService {

    /** 异步标记任务"待领取"（条件达成时调用，不阻塞主链路） */
    void markTaskReadyAsync(Long userId, String type);
}
