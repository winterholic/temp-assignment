package com.realteeth.hw.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 10
            maxPoolSize = 50
            queueCapacity = 200
            setThreadNamePrefix("job-worker-")
            initialize()
        }
}
