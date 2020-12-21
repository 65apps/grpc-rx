package com.apps65.grpc.common

import javax.inject.Qualifier

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class ChannelSettings(val settings: Settings)

enum class Settings {
    DEVELOPMENT,
    PRE_PRODUCTION,
    PRODUCTION
}