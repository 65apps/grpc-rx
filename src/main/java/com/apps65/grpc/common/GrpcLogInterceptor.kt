package com.apps65.grpc.common

import android.util.Log
import io.grpc.*
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall

private const val TAG: String = "GRPC"

internal class GrpcLogInterceptor : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return LogClientCall(
            next.newCall(method, callOptions),
            method.fullMethodName
        )
    }

    private inner class LogClientCall<ReqT, RespT>(
        call: ClientCall<ReqT, RespT>,
        private val fullMethodName: String
    ) : SimpleForwardingClientCall<ReqT, RespT>(call) {

        private fun log(message: String) {
            val s = message.replace("\n ", " ")
                .replace("\n", " ")
            Log.d(TAG, s)
        }

        private fun logWarning(message: String) {
            val s = message.replace("\n", " ")
                .replace("\n", "")
            Log.w(TAG, s)
        }

        override fun sendMessage(message: ReqT) {
            log(fullMethodName)
            log("request ${message.toString().substringAfter("\n")}")
            super.sendMessage(message)
        }

        override fun start(responseListener: Listener<RespT>, headers: Metadata) {
            val decorator = object : Listener<RespT>() {
                override fun onReady() {
                    log(fullMethodName)
                    log("onReady")
                    responseListener.onReady()
                }

                override fun onMessage(message: RespT) {
                    log(fullMethodName)
                    log("response body ${message.toString()}")
                    responseListener.onMessage(message)
                }

                override fun onHeaders(headers: Metadata?) {
                    log(fullMethodName)
                    log("response headers ${headers?.toString()}")
                    responseListener.onHeaders(headers)
                }

                override fun onClose(status: Status, trailers: Metadata?) {
                    if (status.isOk) {
                        log(fullMethodName)
                        log("onClose $status")
                    } else {
                        logWarning(fullMethodName)
                        if (status.code == Status.Code.CANCELLED) {
                            logWarning("onError ${status.code}")
                        } else {
                            logWarning("onError $status ${trailers?.toString()}")
                        }
                    }
                    responseListener.onClose(status, trailers)
                }
            }

            super.start(decorator, headers)
        }
    }
}
