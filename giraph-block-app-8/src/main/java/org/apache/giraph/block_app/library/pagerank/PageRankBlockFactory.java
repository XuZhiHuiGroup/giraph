/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.block_app.library.pagerank;

import org.apache.giraph.block_app.framework.AbstractBlockFactory;
import org.apache.giraph.block_app.framework.block.Block;
import org.apache.giraph.comm.messages.MessageEncodeAndStoreType;
import org.apache.giraph.conf.GiraphConfiguration;
import org.apache.giraph.conf.GiraphConstants;
import org.apache.giraph.edge.LongDoubleArrayEdges;
import org.apache.giraph.edge.LongNullArrayEdges;
import org.apache.giraph.graph.Vertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

/**
 * Block factory for pagerank
 */
public class PageRankBlockFactory extends AbstractBlockFactory<Object> {
  @Override
  protected Class<? extends WritableComparable> getVertexIDClass(
      GiraphConfiguration conf) {
    return LongWritable.class;
  }

  @Override
  protected Class<? extends Writable> getVertexValueClass(
      GiraphConfiguration conf) {
    return DoubleWritable.class;
  }

  @Override
  protected Class<? extends Writable> getEdgeValueClass(
      GiraphConfiguration conf) {
    return PageRankSettings.isWeighted(conf) ?
        DoubleWritable.class : NullWritable.class;
  }

  @Override
  public Block createBlock(GiraphConfiguration conf) {
    if (PageRankSettings.isWeighted(conf)) {
      return PageRankBlockUtils.<LongWritable, DoubleWritable>weightedPagerank(
          (vertex, value) -> vertex.getValue().set(value.get()),
          Vertex::getValue,
          conf);
    } else {
      return
          PageRankBlockUtils.<LongWritable, DoubleWritable>unweightedPagerank(
              (vertex, value) -> vertex.getValue().set(value.get()),
              Vertex::getValue,
              conf);
    }
  }

  @Override
  public Object createExecutionStage(GiraphConfiguration conf) {
    return new Object();
  }

  @Override
  protected void additionalInitConfig(GiraphConfiguration conf) {
    conf.setVertexValueFactoryClass(PageRankVertexValueFactory.class);
    if (PageRankSettings.isWeighted(conf)) {
      conf.setOutEdgesClass(LongDoubleArrayEdges.class);
    } else {
      // Save on network traffic by only sending one message value per worker
      GiraphConstants.MESSAGE_ENCODE_AND_STORE_TYPE.setIfUnset(
          conf, MessageEncodeAndStoreType.EXTRACT_BYTEARRAY_PER_PARTITION);
      conf.setOutEdgesClass(LongNullArrayEdges.class);
    }
  }
}
