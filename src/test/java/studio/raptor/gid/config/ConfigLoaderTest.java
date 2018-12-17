/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package studio.raptor.gid.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.common.Type;
import studio.raptor.gid.def.BreadcrumbDef;
import studio.raptor.gid.def.SequenceDef;
import studio.raptor.gid.def.SnowflakeDef;
import studio.raptor.gid.def.TicktockDef;

/**
 * 配置加载测试用例。
 *
 * @author bruce
 * @since 0.1
 */
public final class ConfigLoaderTest {

  private static final String NOT_EXIST_FILE_PATH = "/xml/not_exist_file.xml"; // 不存在的配置文件

  private static final String BAD_FORMED_FILE_PATH1 = "/xml/seq_test_bad_formed1.xml";// 错误的元素

  private static final String BAD_FORMED_FILE_PATH2 = "/xml/seq_test_bad_formed2.xml";// 错误的属性值

  private static final String WELL_FORMED_FILE_PATH = "/xml/seq_test_well_formed_all.xml"; // 合法的配置文件


  @Rule
  public ExpectedException expectedEx = ExpectedException.none();


  @Test
  public void testValidateFail() {
    assertFalse(ConfigLoader.validate(NOT_EXIST_FILE_PATH));
    assertFalse(ConfigLoader.validate(BAD_FORMED_FILE_PATH1));
    assertFalse(ConfigLoader.validate(BAD_FORMED_FILE_PATH2));
  }

  @Test
  public void testValidateSuccess() {
    assertTrue(ConfigLoader.validate(WELL_FORMED_FILE_PATH));
  }

  @Test
  public void testLoadSuccess() throws GidException {

    List<SequenceDef> sequenceDefs = ConfigLoader.load(WELL_FORMED_FILE_PATH);
    assertThat(sequenceDefs.size(), equalTo(7));

    for (SequenceDef sequenceDef : sequenceDefs) {
      String name = sequenceDef.name();
      Type type = sequenceDef.type();

      assertThat(name, startsWith(type.name.toLowerCase()));

      if (Type.SNOWFLAKE == type) {
        SnowflakeDef snowflakeDef = (SnowflakeDef) sequenceDef;
        assertTrue(snowflakeDef.sequenceWidth() > 0);
        assertTrue(snowflakeDef.workerIdWidth() > 0);

      } else if (Type.TICKTOCK == type) {
        TicktockDef ticktockDef = (TicktockDef) sequenceDef;
        assertTrue(ticktockDef.sequenceWidth() > 0);
        assertTrue(ticktockDef.workerIdWidth() > 0);

      } else if (Type.BREADCRUMB == type) {

        BreadcrumbDef breadcrumbDef = (BreadcrumbDef) sequenceDef;
        assertThat(breadcrumbDef.start(), notNullValue());
        assertThat(breadcrumbDef.incr(), notNullValue());
        assertThat(breadcrumbDef.cache(), notNullValue());

      }
    }
  }


}
