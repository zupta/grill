package com.inmobi.grill.server.api.events;

/*
 * #%L
 * Grill API for server and extensions
 * %%
 * Copyright (C) 2014 Inmobi
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.inmobi.grill.api.GrillException;

import java.util.concurrent.*;

/**
 * Event listeners should implement this class if they wish to process events asynchronously.
 * This should be used when event processing can block, or is computationally intensive.
 * @param <T>
 */
public abstract class AsyncEventListener<T extends GrillEvent> implements GrillEventListener<T> {
  protected final ThreadPoolExecutor processor;
  protected final BlockingQueue<Runnable> eventQueue;

  /**
   * Create a single threaded event listener with an unbounded queue, with daemon threads
   */
  public AsyncEventListener() {
    this(1);
  }

  /**
   * Create a event listener with poolSize threads with an unbounded queue and daemon threads
   * @param poolSize
   */
  public AsyncEventListener(int poolSize) {
    this(poolSize, -1, 10, true);
  }

  /**
   * Create an asynchronous event listener which uses a thread poool to process events
   * @param poolSize size of the event processing pool
   * @param maxQueueSize max size of the event queue, if this is non positive, then the queue is unbounded
   * @param timeOutSeconds time out in seconds when an idle thread is destroyed
   * @param isDaemon if the threads used to process should be daemon threads, if false, then implementation should
   *                 call stop() to stop the thread pool
   */
  public AsyncEventListener(int poolSize, int maxQueueSize, long timeOutSeconds, final boolean isDaemon) {
    if (maxQueueSize <= 0) {
      eventQueue = new LinkedBlockingQueue<Runnable>();
    } else {
      eventQueue = new ArrayBlockingQueue<Runnable>(maxQueueSize);
    }

    processor = new ThreadPoolExecutor(poolSize, poolSize, timeOutSeconds, TimeUnit.SECONDS, eventQueue, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        Thread th = new Thread(runnable);
        th.setName("event_processor_thread");
        th.setDaemon(isDaemon);
        return th;
      }
    });
    processor.allowCoreThreadTimeOut(true);
  }

  /**
   * Creates a new runnable and calls the process method in it
   * @param event
   * @throws GrillException
   */
  @Override
  public void onEvent(final T event) throws GrillException {
    try {
      processor.execute(new Runnable() {
        @Override
        public void run() {
          process(event);
        }
      });
    } catch (RejectedExecutionException rejected) {
      throw new GrillException(rejected);
    }
  }

  /**
   * Should implement the actual event handling
   */
  public abstract void process(T event);

  /**
   * Should be called to stop the event processor thread
   */
  public void stop() {
    processor.shutdownNow();
  }

  public BlockingQueue<Runnable> getEventQueue() {
    return eventQueue;
  }
}
