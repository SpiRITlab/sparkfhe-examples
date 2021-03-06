/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spiritlab.sparkfhe.example.debug;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import java.util.ArrayList;
import java.util.List;

public final class JavaPi {

  public static void main(String[] args) throws Exception {

    // Creating a session to Spark. The session allows the creation of the
    // various data abstractions such as RDDs, DataFrame, and more.
    SparkSession spark = SparkSession
            .builder()          // create a sparkSession.builder for construction a Spark Session
            .appName("SparkFHE-JavaPi") // session name, optional, will be autogenerated if not specified
            .getOrCreate();     // reuse existing spark context or created new one

    // Creating spark context which allows the communication with worker nodes
    JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());

    // Creating number of slices depending on the arguments
    int slices = (args.length == 1) ? Integer.parseInt(args[0]) : 2;
    int n = 100000 * slices;

    // Creating a large array of integers
    List<Integer> l = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      l.add(i);
    }

    // Creating an RDD object named dataSet, and initialize it with
    // distributed scala collection, aka the large array in this case.
    // parallelized with the number of slices
    JavaRDD<Integer> dataSet = jsc.parallelize(l, slices);

    // For every integer object in the Java RDD dataSet, generate 2
    // random number, x and y, then assign 0 or 1 to each integer object
    // based on the formula below. lastly, add up the 0s and 1s 2 integer
    // objects at a time.
    int count = dataSet.map(integer -> {
      double x = Math.random() * 2 - 1;
      double y = Math.random() * 2 - 1;
      return (x * x + y * y <= 1) ? 1 : 0;
    }).reduce((integer, integer2) -> integer + integer2);

    // Print out the result
    System.out.println("[SparkFHE] Pi is roughly " + 4.0 * count / n);

    // Stop existing spark context
    spark.stop();
  }
}
