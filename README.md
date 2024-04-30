# test-reactor-flux-mergeSequential
Test to demonstrate an apparent problem (or possibly a misunderstanding of mine) with Reactor's `Flux.mergeSequential()`

## Building
```shell
./gradlew build
```

## Running the tests
### Unit Test
The unit test is run automatically with the build and may also be run via:
```shell
./gradlew test
```

## About the Test
In the event that [Flux.mergeSequential(Publisher<? extends I>... sources)](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#mergeSequential-org.reactivestreams.Publisher...-) is invoked with a group of Producers (`mono1`, `mono2`, ..., `monoN`, `monoFinal`) for which the earlier ones are waiting on a signal from the last one, it appears that the last one is never executed, and thus the first N never complete.

The test class demonstrating this is `MergeSequentialUnitTest` which has three test methods in it:
1. **test_mergeSequential_failsWhenDelayed:** Demonstrates the error case.
2. **test_mergeSequential_succeedsWhenNotDelayed:** Demonstrates that all Producers complete if the initial producers are not dependent on the last producer to complete.
3. **test_mergeSequential_succeedsWhenDelayedWithDoOnSubscribeWorkaround:** Demonstrates the workaround of applying [Mono.doOnSubscribe(Consumer<? super Subscription> onSubscribe)](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#doOnSubscribe-java.util.function.Consumer-) to `monoFinal`. ([Mono.log()](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#log--) works as well.)

The test code is currently configured to run with Reactor 3.6.5 which does not work. The [build.gradle.kts] also includes commented lines for Reactor 3.5.0 (first version I found that does not work) and Reactor 3.4.37 (last version that **does** work).
