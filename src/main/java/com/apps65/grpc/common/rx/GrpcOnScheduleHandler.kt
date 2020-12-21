package com.apps65.grpc.common.rx

import io.grpc.Context
import io.reactivex.functions.Function

/**
 * Необходим для предотвращения потери grpc-context'а при переключении между тредами,
 * подробнее в документации к io.grpc.Context
 */
class GrpcOnScheduleHandler : Function<Runnable, Runnable> {

    override fun apply(runnable: Runnable): Runnable = Context.current().wrap(runnable)

}