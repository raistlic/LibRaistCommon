/*
 * Copyright 2015 Lei CHEN (raistlic@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raistlic.common.taskqueue;

import org.raistlic.common.util.ExceptionHandler;
import org.raistlic.common.util.Factory;

/**
 * 
 *
 * @author Lei Chen (2015-11-24)
 */
public interface TaskQueueBuilder extends Factory<TaskQueue.Controller> {

  TaskQueueBuilder withThreadAsDaemon(boolean daemon);

  TaskQueueBuilder withThreadPriority(int priority);

  TaskQueueBuilder withThreadName(String name);

  TaskQueueBuilder withExceptionHandler(ExceptionHandler exceptionHandler);
}
