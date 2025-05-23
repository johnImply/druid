/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.k8s.overlord.taskadapter;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import org.apache.druid.indexing.common.task.Task;
import org.apache.druid.k8s.overlord.common.K8sTaskId;

import java.io.IOException;

public interface TaskAdapter
{
  String getAdapterType();

  Job fromTask(Task task) throws IOException;

  Task toTask(Job from) throws IOException;

  K8sTaskId getTaskId(Job from);

  /**
   * Method for exposing to external classes whether the task has its task payload bundled by the adapter or relies on a external system
   */
  boolean shouldUseDeepStorageForTaskPayload(Task task) throws IOException;
}
