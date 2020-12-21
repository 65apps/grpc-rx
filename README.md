# Конвертер Grpc -> RxJava

## Не забудьте добавить в onCreate Application
```kotlin
 RxJavaPlugins.setScheduleHandler(GrpcOnScheduleHandler())
```

## Пример использования extensions
```kotlin
internal class SomeService @Inject constructor(private val channel: Channel) {

    fun getUserStatus(): Single<GetUserStatusResp> =
        channel.newUnaryCall(GetUserStatusReq.getDefaultInstance(), BrokerAccountCoreServiceGrpc::getGetUserStatusMethod)
}
```

## В классе GrpcAuthInterceptor - обработка подстановки токена + протухание токена

## В классе GrpcLogInterceptor - логирование событий gprc
