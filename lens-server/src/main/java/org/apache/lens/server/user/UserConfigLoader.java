package org.apache.lens.server.user;

/*
 * #%L
 * Lens Server
 * %%
 * Copyright (C) 2014 Apache Software Foundation
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

import org.apache.hadoop.hive.conf.HiveConf;

import java.util.Map;

/**
 * The Class UserConfigLoader.
 */
public abstract class UserConfigLoader {

  /** The hive conf. */
  protected final HiveConf hiveConf;

  /**
   * Instantiates a new user config loader.
   *
   * @param conf
   *          the conf
   */
  protected UserConfigLoader(HiveConf conf) {
    this.hiveConf = conf;
  }

  /**
   * Gets the user config.
   *
   * @param loggedInUser
   *          the logged in user
   * @return the user config
   * @throws UserConfigLoaderException
   *           the user config loader exception
   */
  public abstract Map<String, String> getUserConfig(String loggedInUser) throws UserConfigLoaderException;
}
