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

import com.google.common.base.Strings;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.common.Type;
import studio.raptor.gid.def.BreadcrumbDef;
import studio.raptor.gid.def.DefaultBreadcrumbDef;
import studio.raptor.gid.def.DefaultSnowflakeDef;
import studio.raptor.gid.def.DefaultTicktockDef;
import studio.raptor.gid.def.SequenceDef;
import studio.raptor.gid.def.SnowflakeDef;
import studio.raptor.gid.def.TicktockDef;

/**
 * 序列配置加载。
 *
 * @author Charley
 * @author bruce
 * @since 0.1
 */
public final class ConfigLoader {

  private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

  private static final String XSD_PATH = "/gid.xsd";


  /**
   * 加载序列定义文件
   *
   * @param filePath 文件路径(类路径下)
   * @return 序列定义列表
   * @throws GidException 验证不通过或解析等异常
   */
  public static List<SequenceDef> load(String filePath) throws GidException {
    List<SequenceDef> seqDefs = new ArrayList<SequenceDef>();

    if (validate(filePath)) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      try {
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(ConfigLoader.class.getResourceAsStream(filePath));
        NodeList sequences = document.getElementsByTagName("sequences");
        Element sequencesElement = (Element) sequences.item(0);
        //
        seqDefs.addAll(unmarshal(sequencesElement, Type.SNOWFLAKE));
        seqDefs.addAll(unmarshal(sequencesElement, Type.TICKTOCK));
        seqDefs.addAll(unmarshal(sequencesElement, Type.BREADCRUMB));
        //
      } catch (ParserConfigurationException | SAXException | IOException e) {
        throw new GidException("Load sequence definition xml failure", e);
      }
    } else {
      throw new GidException("Xml validation is not passed @" + filePath);
    }
    return seqDefs;
  }


  /**
   * 解编成序列定义对象列表
   *
   * @param sequencesElement 对应xml中的sequences元素
   * @param seqType 序列类型
   * @return 序列定义列表
   * @throws GidException 未知序列类型 或 序列定义不合法
   */
  private static List<SequenceDef> unmarshal(Element sequencesElement, Type seqType)
      throws GidException {
    List<SequenceDef> seqDefs = new ArrayList<SequenceDef>();
    // <breadcrumb ......./>  <snowflake ....../>
    NodeList nodes = sequencesElement.getElementsByTagName(seqType.name);

    int nodeNum = nodes.getLength();
    for (int i = 0; i < nodeNum; i++) {
      final Element e = (Element) nodes.item(i);
      final String seqName = e.getAttribute("name");
      switch (seqType) {
        case SNOWFLAKE:
          seqDefs.add(new SnowflakeDef() {
            @Override
            public String name() {
              return seqName;
            }

            @Override
            public int workerIdWidth() {
              return Strings.isNullOrEmpty(e.getAttribute("workerIdWidth"))
                  ? DefaultSnowflakeDef.DEFAULT_WORKERID_BITS
                  : Integer.valueOf(e.getAttribute("workerIdWidth"));
            }

            @Override
            public int sequenceWidth() {
              return Strings.isNullOrEmpty(e.getAttribute("sequenceWidth"))
                  ? DefaultSnowflakeDef.DEFAULT_SEQUENCE_BITS
                  : Integer.valueOf(e.getAttribute("sequenceWidth"));
            }
          });
          break;

        case TICKTOCK:
          seqDefs.add(new TicktockDef() {

            @Override
            public String name() {
              return seqName;
            }

            @Override
            public int workerIdWidth() {
              return Strings.isNullOrEmpty(e.getAttribute("workerIdWidth"))
                  ? DefaultTicktockDef.DEFAULT_WORKERID_BITS
                  : Integer.valueOf(e.getAttribute("workerIdWidth"));
            }

            @Override
            public int sequenceWidth() {
              return Strings.isNullOrEmpty(e.getAttribute("sequenceWidth"))
                  ? DefaultTicktockDef.DEFAULT_SEQUENCE_BITS
                  : Integer.valueOf(e.getAttribute("sequenceWidth"));
            }
          });
          break;
        case BREADCRUMB:
          seqDefs.add(new BreadcrumbDef() {
            @Override
            public String name() {
              return seqName;
            }

            @Override
            public int cache() {
              return Strings.isNullOrEmpty(e.getAttribute("cache"))
                  ? DefaultBreadcrumbDef.DEFAULT_CACHE
                  : Integer.valueOf(e.getAttribute("cache"));
            }

            @Override
            public long incr() {
              return Strings.isNullOrEmpty(e.getAttribute("incr"))
                  ? DefaultBreadcrumbDef.DEFAULT_INCR
                  : Long.valueOf(e.getAttribute("incr"));
            }

            @Override
            public long start() {
              return Strings.isNullOrEmpty(e.getAttribute("start"))
                  ? DefaultBreadcrumbDef.DEFAULT_START
                  : Long.valueOf(e.getAttribute("start"));
            }
          });
          break;
        default:
          throw new GidException("Unkonwn sequence name");
      }
    }

    return seqDefs;
  }

  /**
   * xml文档合法性校验
   *
   * @param xmlPath xml文件所在路径（类路径下）
   * @return xml文档校验合法返回true，否则返回false
   */
  public static boolean validate(String xmlPath) {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    Schema schema;
    Validator validator;
    try {
      URL xsd = ConfigLoader.class.getResource(XSD_PATH);
      if (null == xsd) {
        throw new FileNotFoundException("Sequence xsd file is not found in classpath '" + XSD_PATH+"'");
      }

      URL xml = ConfigLoader.class.getResource(xmlPath);
      if (null == xml) {
        throw new FileNotFoundException("Sequence definition xml file is not found in classpath '" + xmlPath+"'");
      }

      schema = schemaFactory.newSchema(xsd);
      validator = schema.newValidator();
      Source source = new StreamSource(xml.getFile());
      validator.validate(source);
      return true;
    } catch (Exception e) {
      log.error("Validate sequence definition xml fail", e);
      return false;
    }
  }
}