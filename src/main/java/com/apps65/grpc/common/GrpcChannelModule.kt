package com.apps65.grpc.common

import android.content.Context
import com.apps65.grpc.common.Settings.*
import dagger.Module
import dagger.Provides
import io.grpc.android.AndroidChannelBuilder
import javax.inject.Singleton
import io.grpc.Channel as GrpcChannel

@Module
class GrpcChannelModule {

    @Provides
    @Singleton
    fun provideFlavorChannel(
        authInterceptor: GrpcAuthInterceptor,
        context: Context
    ): GrpcChannel = channelFor(null, authInterceptor, context)

    @Provides
    @Singleton
    @ChannelSettings(settings = DEVELOPMENT)
    fun provideDevChannel(
        authInterceptor: GrpcAuthInterceptor,
        context: Context
    ): GrpcChannel = channelFor(DEVELOPMENT, authInterceptor, context)

    @Provides
    @Singleton
    @ChannelSettings(settings = PRE_PRODUCTION)
    fun providePreprodChannel(
        authInterceptor: GrpcAuthInterceptor,
        context: Context
    ): GrpcChannel = channelFor(PRE_PRODUCTION, authInterceptor, context)

    @Provides
    @Singleton
    @ChannelSettings(settings = PRODUCTION)
    fun provideProdChannel(
        authInterceptor: GrpcAuthInterceptor,
        context: Context
    ): GrpcChannel = channelFor(PRODUCTION, authInterceptor, context)

    private fun channelFor(
        type: Settings?,
        authInterceptor: GrpcAuthInterceptor,
        context: Context
    ): GrpcChannel {
        val port = when (type) {
            DEVELOPMENT -> DEV_GRPC_PORT
            PRE_PRODUCTION -> PREDPROD_GRPC_PORT
            PRODUCTION -> PROD_GRPC_PORT
            else -> getPort()
        }
        return AndroidChannelBuilder.forAddress(getAddress(), port)
            .intercept(listOf(authInterceptor, GrpcLogInterceptor()))
            .context(context.applicationContext)
            .build()
    }

    private fun getAddress() = DEV_GRPC_ADDRESS

    private fun getPort(): Int = PROD_GRPC_PORT

    //TODO - адрес и порт сервера
    companion object {
        private const val DEV_GRPC_ADDRESS = "your api"
        private const val DEV_GRPC_PORT = 9020
        private const val PREDPROD_GRPC_PORT = 9010
        private const val PROD_GRPC_PORT = 9000
    }

}