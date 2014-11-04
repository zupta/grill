package com.inmobi.grill.api.schedule;

/*
 * #%L
 * Grill API
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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@XmlRootElement
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Status {
    NEW, SCHEDULED, PAUSED
  }

  @XmlElement
  @Getter
  private Status status;

  public static boolean isValidTransition(Status oldState, Status newState) {
    switch (oldState) {
    case NEW:
      switch (newState) {
      case SCHEDULED:
        return true;
      }
      break;
    case SCHEDULED:
      switch (newState) {
      case PAUSED:
        return true;
      }
      break;
    case PAUSED:
      switch (newState) {
      case SCHEDULED:
        return true;
      }
      break;
    default:
      // fall-through
    }
    return false;
  }
}
