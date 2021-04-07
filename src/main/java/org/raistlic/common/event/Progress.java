/*
 * Copyright 2021 Lei Chen (raistlic@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raistlic.common.event;

import java.util.EventListener;

public final class Progress {

  /**
   * This interface defines the callback methods for monitoring progress.
   */
  public interface Monitor extends EventListener {

    void progressUpdated(int current, int total);

    void progressMessage(String message);

    void progressDone();

    void progressFailed(String message);
  }

  public interface Context<T> {

    Monitor invoke(T task);
  }

  public static Progress of(int progress, int total) {

    if (progress < 0)
      throw new IllegalArgumentException("Invalid progress amount: " + progress);

    return POOL[Math.min(TOTAL, progress * 100 / total)];
  }

  private final int progress;

  private Progress(int progress) {

    this.progress = progress;
  }

  public int progress() {

    return progress;
  }

  public int total() {

    return TOTAL;
  }

  public float percent() {

    return (float) progress / TOTAL;
  }

  private static final int TOTAL;
  private static final Progress[] POOL;
  public static final Progress CANCELED;
  public static final Progress FAILED;

  public static final Progress DONE;

  static {

    TOTAL = 100;

    CANCELED = new Progress(-2);
    FAILED = new Progress(-1);
    DONE = new Progress(TOTAL);

    POOL = new Progress[TOTAL];
    for (int i = 0; i < TOTAL; i++) {

      POOL[i] = new Progress(i);
    }
  }
}
