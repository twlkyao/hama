/**
 * Copyright 2007 The Apache Software Foundation
 *
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
package org.apache.hama.algebra;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hama.Constants;
import org.apache.hama.DenseMatrix;
import org.apache.hama.HamaConfiguration;
import org.apache.hama.SubMatrix;
import org.apache.hama.io.BlockID;
import org.apache.hama.io.BlockPosition;
import org.apache.hama.io.BlockWritable;
import org.apache.hama.mapred.BlockInputFormat;
import org.apache.log4j.Logger;

public class BlockCyclicMultiplyMap extends MapReduceBase implements
    Mapper<BlockID, BlockPosition, IntWritable, BlockWritable> {
  static final Logger LOG = Logger.getLogger(BlockCyclicMultiplyMap.class);
  protected DenseMatrix matrix_a;
  public static final String MATRIX_A = "hama.multiplication.matrix.a";
  protected DenseMatrix matrix_b;
  public static final String MATRIX_B = "hama.multiplication.matrix.b";

  public void configure(JobConf job) {
    try {
      matrix_a = new DenseMatrix(new HamaConfiguration(), job.get(MATRIX_A, ""));
      matrix_b = new DenseMatrix(new HamaConfiguration(), job.get(MATRIX_B, ""));
    } catch (IOException e) {
      LOG.warn("Load matrix_b failed : " + e.getMessage());
    }
  }

  public static void initJob(String matrix_a, String matrix_b,
      Class<BlockCyclicMultiplyMap> map, Class<IntWritable> outputKeyClass,
      Class<BlockWritable> outputValueClass, JobConf jobConf) {

    jobConf.setMapOutputValueClass(outputValueClass);
    jobConf.setMapOutputKeyClass(outputKeyClass);
    jobConf.setMapperClass(map);
    jobConf.set(MATRIX_A, matrix_a);
    jobConf.set(MATRIX_B, matrix_b);

    jobConf.setInputFormat(BlockInputFormat.class);
    FileInputFormat.addInputPaths(jobConf, matrix_a);

    jobConf.set(BlockInputFormat.COLUMN_LIST, Constants.BLOCK_POSITION);
  }

  @Override
  public void map(BlockID key, @SuppressWarnings("unused") BlockPosition value,
      OutputCollector<IntWritable, BlockWritable> output, Reporter reporter)
      throws IOException {
    int blockSize = matrix_b.getBlockSize();
    SubMatrix a = matrix_a.getBlock(key.getRow(), key.getColumn());
    for (int j = 0; j < blockSize; j++) {
      SubMatrix b = matrix_b.getBlock(key.getColumn(), j);
      SubMatrix c = a.mult(b);
      output.collect(new IntWritable(key.getRow()), 
          new BlockWritable(key.getRow(), j, c));
    }
  }
}