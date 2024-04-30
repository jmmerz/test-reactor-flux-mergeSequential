package test.merge_sequential;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import test.merge_sequential.service.DelayedToUppercaseService;
import test.merge_sequential.service.DelayedToUppercaseService.Data;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public class MergeSequentialUnitTest {

  /**
   * Demonstrate that the merged Flux never completes when the problematic conditions are present.
   */
  @Test
  public void test_mergeSequential_failsWhenDelayed() {
    boolean proceedByDefault = false;
    boolean useDoOnSubscribeWorkaround = false;
    runTest(proceedByDefault, useDoOnSubscribeWorkaround);
  }

  /**
   * Demonstrate that the merged Flux completes as expected if the first Monos are able to complete
   * execution without waiting.
   */
  @Test
  public void test_mergeSequential_succeedsWhenNotDelayed() {
    boolean proceedByDefault = true;
    boolean useDoOnSubscribeWorkaround = false;
    runTest(proceedByDefault, useDoOnSubscribeWorkaround);
  }

  /**
   * Demonstrate that the merged Flux completes as expected if the third Mono is given a No-Op
   * {@link Mono#doOnSubscribe(Consumer)} handler.
   */
  @Test
  public void test_mergeSequential_succeedsWhenDelayedWithDoOnSubscribeWorkaround() {
    boolean proceedByDefault = false;
    boolean useDoOnSubscribeWorkaround = true;
    runTest(proceedByDefault, useDoOnSubscribeWorkaround);
  }

  /**
   * Run the test code to demonstrate configurations that succeed or fail based on two parameters.
   *
   * @param proceedByDefault If true, the {@link DelayedToUppercaseService} will be configured to
   *                        immediately dispatch commands rather than waiting to be signaled.
   * @param useDoOnSubscribeWorkaround If true, the {@link Mono} sending the proceed signal will be
   *                                   supplied with a No-Op {@link Mono#doOnSubscribe(Consumer)}
   *                                   handler to demonstrate the workaround.
   */
  private void runTest(boolean proceedByDefault, boolean useDoOnSubscribeWorkaround) {
    DelayedToUppercaseService delayedToUppercaseService = new DelayedToUppercaseService();
    delayedToUppercaseService.setProceed(proceedByDefault);

    Data request1 = new Data(1, "first");
    Mono<Data> toUppercaseMono1 = delayedToUppercaseService.addInputTask(request1);

    Data request2 = new Data(2, "second");
    Mono<Data> toUppercaseMono2 = delayedToUppercaseService.addInputTask(request2);

    Mono<String> proceedMono = Mono.fromCallable(
          () -> {
            // Print so we can see when this is executed
            // (This is never printed in the "broken" cases)
            System.out.println("Holding execution for 3s...");
            // Sleep for 3s to see that the other tasks have begun and are waiting
            // Note that commenting this line out in order to execute the code below immediately
            // does not result in success
            Thread.sleep(3_000);
            System.out.println("Releasing hold");
            delayedToUppercaseService.setProceed(true);
            return "PROCEEDED";
          });
          if(useDoOnSubscribeWorkaround) {
            proceedMono = proceedMono.doOnSubscribe(s -> {/* No-Op */});
            // NOTE: proceedMono.log() also succeeds as a workaround, though other signal handlers
            // like doOnTerminate() or doOnError() do not. It seems specific to the subscribe
            // signal.
          }


    Flux<String> mergedFlux = Flux.mergeSequential(
          toUppercaseMono1.map(output -> output.value),
          toUppercaseMono2.map(output -> output.value),
          proceedMono
    );

    // mergeSequential should subscribe to all three Monos eagerly
    // and then merge their results in the original order.
    StepVerifier.create(mergedFlux)
          .expectNext("FIRST")
          .expectNext("SECOND")
          .expectNext("PROCEEDED")
          .expectComplete()
          .verify(Duration.of(6, ChronoUnit.SECONDS));
  }

}
