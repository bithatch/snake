package uk.co.bithatch.snake.ui;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SchedulerManager implements Closeable {

	public enum Queue {
		DEVICE_IO, APP_IO, MACROS, TIMER, AUDIO
	}

	private Map<Queue, ScheduledExecutorService> schedulers = Collections.synchronizedMap(new HashMap<>());

	public ScheduledExecutorService get(Queue queue) {
		synchronized (schedulers) {
			var s = schedulers.get(queue);
			if (s == null) {
				s = Executors.newScheduledThreadPool(1, (r) -> new Thread(r, "snake-" + queue.name()));
				schedulers.put(queue, s);
			}
			return s;
		}
	}

	public void clear(Queue queue) {
		synchronized (schedulers) {
			var s = schedulers.get(queue);
			if (s != null) {
				s.shutdown();
				schedulers.remove(queue);
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (schedulers) {
			for (Map.Entry<Queue, ScheduledExecutorService> s : schedulers.entrySet()) {
				s.getValue().shutdownNow();
			}
			schedulers.clear();
		}
	}
}
