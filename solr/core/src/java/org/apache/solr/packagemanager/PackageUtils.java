/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.packagemanager;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.V2Request;
import org.apache.solr.client.solrj.response.V2Response;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Strings;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

public class PackageUtils {
  
  public static Configuration jsonPathConfiguration() {
    MappingProvider provider = new JacksonMappingProvider();
    JsonProvider jsonProvider = new JacksonJsonProvider();
    Configuration c = Configuration.builder().jsonProvider(jsonProvider).mappingProvider(provider).options(com.jayway.jsonpath.Option.REQUIRE_PROPERTIES).build();
    return c;
  }
  
  public static void postFile(SolrClient client, ByteBuffer buffer, String name, String sig)
      throws SolrServerException, IOException {
    String resource = "/api/cluster/files" + name;
    ModifiableSolrParams params = new ModifiableSolrParams();
    if (sig != null) {
      params.add("sig", sig);
    }
    V2Response rsp = new V2Request.Builder(resource)
        .withMethod(SolrRequest.METHOD.PUT)
        .withPayload(buffer)
        .forceV2(true)
        .withMimeType("application/octet-stream")
        .withParams(params)
        .build()
        .process(client);
    if (!name.equals(rsp.getResponse().get(CommonParams.FILE))) {
      throw new SolrException(ErrorCode.BAD_REQUEST, "Mismatch in file uploaded. Uploaded: " +
          rsp.getResponse().get(CommonParams.FILE)+", Original: "+name);
    }
  }

  public static <T> T getJson(HttpClient client, String url, Class<T> klass) {
    try {
      return new ObjectMapper().readValue(getJson(client, url), klass);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } 
  }

  public static String getJson(HttpClient client, String url) {
    try {
      return IOUtils.toString(client.execute(new HttpGet(url)).getEntity().getContent(), "UTF-8");
    } catch (UnsupportedOperationException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  // nocommit javadoc
  public static boolean checkVersionConstraint(String solrVersion, String minInclusive, String maxInclusive) {
    String constraint = ">="+minInclusive + " & <="+maxInclusive;
    System.out.println("Current: "+solrVersion+", constraint: "+constraint);
    return Strings.isNullOrEmpty(constraint) || Version.valueOf(solrVersion).satisfies(constraint);
  }

  // nocommit javadoc
  public static int compareVersions(String v1, String v2) {
    return Version.valueOf(v1).compareTo(Version.valueOf(v2));
  }

}