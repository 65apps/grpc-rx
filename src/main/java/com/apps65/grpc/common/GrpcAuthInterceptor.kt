package com.apps65.grpc.common

import io.grpc.*
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.Status.Code.UNAUTHENTICATED
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTHORIZATION = "authorization"

@Singleton
class GrpcAuthInterceptor @Inject constructor() : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return AuthClientCall(next.newCall(method, callOptions))
    }

    private inner class AuthClientCall<ReqT, RespT>(
        call: ClientCall<ReqT, RespT>
    ) : SimpleForwardingClientCall<ReqT, RespT>(call) {

        override fun start(responseListener: Listener<RespT>, headers: Metadata) {
            addCredentials(headers)
            val decorator = object : Listener<RespT>() {
                override fun onReady() {
                    responseListener.onReady()
                }

                override fun onMessage(message: RespT) {
                    responseListener.onMessage(message)
                }

                override fun onHeaders(headers: Metadata?) {
                    responseListener.onHeaders(headers)
                }

                override fun onClose(status: Status, trailers: Metadata?) {
                    responseListener.onClose(status, trailers)
                    if (status.code == UNAUTHENTICATED) {
                        //TODO обработка "протухания токена"
                        //переход на экран авторизации
                    }
                }
            }
            super.start(decorator, headers)
        }

        private fun addCredentials(headers: Metadata) {
            try {
                val auth = Metadata.Key.of(AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER)
                //TODO добавление токена авторизации
                //headers.put(auth, user.getToken())
            } catch (e: Throwable) {
                //TODO обработка "протухания токена"
                //переход на экран авторизации

            }
        }
    }
}