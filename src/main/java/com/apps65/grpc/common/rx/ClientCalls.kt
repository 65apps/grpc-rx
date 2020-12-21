package com.apps65.grpc.common.rx

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.MethodDescriptor.MethodType.*
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import io.reactivex.*
import io.reactivex.BackpressureStrategy.LATEST
import io.reactivex.annotations.SchedulerSupport
import io.reactivex.annotations.SchedulerSupport.NONE
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.subscribeBy

@SchedulerSupport(NONE)
fun <C : Channel, T : Any, R : Any> C.newUnaryCall(
    request: T,
    method: MethodDescriptor<T, R>,
    callOptions: CallOptions = CallOptions.DEFAULT
): Single<R> {
    require(method.type == UNARY) {
        "Expected a UNARY type, but got $method"
    }
    return Single.create { emitter ->
        val call = newCall(method, callOptions)
        emitter.setCancellable {
            call.cancel(null, null)
        }
        val streamObserver = emitter.asStreamObserver()
        ClientCalls.asyncUnaryCall(call, request, streamObserver)
    }
}

@SchedulerSupport(NONE)
inline fun <C : Channel, T : Any, R : Any> C.newUnaryCall(
    request: T,
    crossinline method: () -> MethodDescriptor<T, R>,
    callOptions: CallOptions = CallOptions.DEFAULT
): Single<R> = Single.defer { newUnaryCall(request, method(), callOptions) }

@SchedulerSupport(NONE)
fun <C : Channel, T : Any, R : Any> C.newServerStreamingCall(
    request: T,
    method: MethodDescriptor<T, R>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    mode: BackpressureStrategy = LATEST
): Flowable<R> {
    require(method.type == SERVER_STREAMING) {
        "Expected a SERVER_STREAMING type, but got $method"
    }
    return Flowables.create<R>(mode) { emitter ->
        val call = newCall(method, callOptions)
        emitter.setCancellable {
            call.cancel(null, null)
        }
        val streamObserver = emitter.asStreamObserver()
        ClientCalls.asyncServerStreamingCall(call, request, streamObserver)
    }
}

@SchedulerSupport(NONE)
inline fun <C : Channel, T : Any, R : Any> C.newServerStreamingCall(
    request: T,
    crossinline method: () -> MethodDescriptor<T, R>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    mode: BackpressureStrategy = LATEST
): Flowable<R> = Flowable.defer { newServerStreamingCall(request, method(), callOptions, mode) }

@SchedulerSupport(NONE)
fun <C : Channel, T : Any, R : Any> C.newBidiStreamingCall(
    requests: Flowable<T>,
    method: MethodDescriptor<T, R>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    mode: BackpressureStrategy = LATEST
): Flowable<R> {
    require(method.type == BIDI_STREAMING) {
        "Expected a BIDI_STREAMING type, but got $method"
    }
    return Flowables.create<R>(mode) { emitter ->
        val call = newCall(method, callOptions)
        val outputStreamObserver = emitter.asStreamObserver()
        val inputStreamObserver = ClientCalls.asyncBidiStreamingCall(call, outputStreamObserver)
        val inputDisposable = requests.subscribeBy(
            onNext = { value ->
                if (!emitter.isCancelled) {
                    inputStreamObserver.onNext(value)
                }
            },
            onError = { t ->
                if (!emitter.isCancelled) {
                    inputStreamObserver.onError(t)
                }
            },
            onComplete = {
                if (!emitter.isCancelled) {
                    inputStreamObserver.onCompleted()
                }
            }
        )
        emitter.setCancellable {
            call.cancel(null, null)
            inputDisposable.dispose()
        }
    }
}

@SchedulerSupport(NONE)
inline fun <C : Channel, T : Any, R : Any> C.newBidiStreamingCall(
    requests: Flowable<T>,
    crossinline method: () -> MethodDescriptor<T, R>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    mode: BackpressureStrategy = LATEST
): Flowable<R> = Flowable.defer { newBidiStreamingCall(requests, method(), callOptions, mode) }

private fun <T : Any> SingleEmitter<T>.asStreamObserver(): StreamObserver<T> {
    return object : StreamObserver<T> {
        override fun onNext(value: T) = this@asStreamObserver.onSuccess(value)

        override fun onError(t: Throwable) {
            this@asStreamObserver.tryOnError(t)
        }

        override fun onCompleted() {
            // some workaround, if either no onNext or onError called, so there are no elements
            if (!this@asStreamObserver.isDisposed) {
                this@asStreamObserver.tryOnError(NoSuchElementException())
            }
        }
    }
}

private fun <T : Any> FlowableEmitter<T>.asStreamObserver() = object : StreamObserver<T> {
    override fun onNext(value: T) = this@asStreamObserver.onNext(value)
    override fun onCompleted() = this@asStreamObserver.onComplete()
    override fun onError(t: Throwable) {
        this@asStreamObserver.tryOnError(t)
    }
}
