package test.merge_sequential.service;

import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * "Service" to accept some number of requests for data to be converted to uppercase and hold
 * them until signaled to proceed at which point any held tasks will be executed.
 */
public class DelayedToUppercaseService {
  private final AtomicBoolean proceed = new AtomicBoolean(false);

  private final ConcurrentHashMap<Integer, Consumer<Data>> dataIdToOutputConsumer =
      new ConcurrentHashMap<>();

  public static class Data {
    public final int id;
    public final String value;

    public Data(int id, String value) {
      this.id = id;
      this.value = value;
    }
  }

  public void setProceed(boolean proceed) {
    this.proceed.set(proceed);
  }

  /**
   * Add a Data value that will be converted to uppercase as soon as the service is signaled to
   * proceed.
   *
   * @return Mono that will publish the uppercased Data object.
   */
  public Mono<Data> addInputTask(Data input) {
    Mono<Data> result = Mono.just(input)
      .flatMap(
          data -> Mono.fromRunnable(() -> this.nonBlockingToUppercaseOnceReleased(data))
                  .then(Mono.create(sink -> dataIdToOutputConsumer.put(data.id, sink::success)))
      );
    return result;
  }

  /**
   * Create a new thread that will hold until released, then produce an uppercased
   * version of the input Data onto the appropriate dataIdToOutputConsumer. Control is
   * meanwhile returned to the caller.
   */
  private void nonBlockingToUppercaseOnceReleased(Data input) {
    Thread t = new Thread(() -> {
      try {
        while (!proceed.get()) {
          System.out.println("Input " + input.id + ": Waiting to proceed");
          Thread.sleep(1000);
        }

        Data output = new Data(input.id, input.value.toUpperCase());
        System.out.println("Producing output: " + output.id + " (" + output.value + ")");
        dataIdToOutputConsumer.get(output.id).accept(output);

      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
    t.setDaemon(true); // Allow thread to stop when main thread dies.
    t.start();
  }
}
