//
// Copyright SpiRITlab - The SparkFHE project.
// https://github.com/SpiRITlab
//

package spiritlab.sparkfhe.example.packing;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.spiritlab.sparkfhe.SparkFHEPlugin;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import spiritlab.sparkfhe.api.*;
import spiritlab.sparkfhe.example.Config;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * This is an example for SparkFHE project. Created to test the functionality
 * of the doc product operation on both plaintext and cipher-text, in local and distributed environment.
 */
public class TotalSumExample {

    // declare variable to hold a ciphertext vector
    private static String ctxt_vec;
    private static String CTXT_Vector_FILE;

    /**
     * This method performs the total sum operation on a plaintext vector and print out the result
     * @param jsc spark context which allows the communication with worker nodes
     * @param slices the number of time a task is split up
     */
    public static void test_basic_total_sum(JavaSparkContext jsc, int slices) {
        System.out.println("test_basic_total_sum");

        // distribute a local Scala collection (lists in this case) to form 2 RDDs
        JavaRDD<Integer> values_RDD = jsc.parallelize(
                Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1), slices);

        // sum up the values and display
        System.out.println("values_RDD:"+values_RDD.reduce((x, y) -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            return SparkFHE.do_basic_op(x, y, SparkFHE.ADD);
        }));
    }

    /**
     * This method performs the total sum operation on a ciphertext vector and print out the result
     * @param spark the spark session which allows the creation of the various data abstractions such
     *              as RDDs, DataFrame, and more.
     * @param slices the number of time a task is split up
     * @param library the HE library name
     * @param scheme  the HE scheme name
     * @param pk_b broadcast variable for public key
     * @param sk_b broadcast variable for secret key
     * @param rlk_b broadcast variable for relin keys
     * @param glk_b boradcast variable for galois keys
     */
    public static void test_FHE_total_sum_via_lambda(SparkSession spark, int slices, String library, String scheme, Broadcast<String> pk_b,
                                                     Broadcast<String> sk_b, Broadcast<String> rlk_b, Broadcast<String> glk_b) {
        System.out.println("test_FHE_total_sum_via_lambda");

        // Encoders are created for Java beans
        Encoder<SerializedCiphertext> ctxtJSONEncoder = Encoders.bean(SerializedCiphertext.class);

        // Create rdd with json line file.
        JavaRDD<SerializedCiphertext> ctxt_vec_rdd = spark.read().json(CTXT_Vector_FILE).as(ctxtJSONEncoder).javaRDD();

        long startTime, endTime;
        startTime = System.currentTimeMillis();
        ctxt_vec_rdd.reduce((x, y) -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue(), rlk_b.getValue(), glk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_add(x.getCtxt(), y.getCtxt()));
        });

        // sum up the slots of the result
        Ciphertext total_sum_ctxt = new Ciphertext(SparkFHE.getInstance().fhe_total_sum(ctxt_vec_rdd.first().getCtxt()));
        endTime = System.currentTimeMillis();
        System.out.println("TIMEINFO:batch_FHE_total_sum_via_lambda:" + (endTime - startTime) + ":ms");

        if (Config.DEBUG) {
            System.out.println("Partitions:"+ctxt_vec_rdd.partitions().size());
            // print results for debugging purposes
            Util.decrypt_and_print(scheme, "Total Sum", total_sum_ctxt, false, 0);
        }
    }


    /**
     This method performs the total sum operation on a ciphertext vector and print out the result naively
     * @param spark the spark session which allows the creation of the various data abstractions such
     *              as RDDs, DataFrame, and more.
     * @param slices the number of time a task is split up
     * @param library the HE library name
     * @param scheme  the HE scheme name
     * @param pk_b broadcast variable for public key
     * @param sk_b broadcast variable for secret key
     * @param rlk_b broadcast variable for relin keys
     * @param glk_b boradcast variable for galois keys
     */
    public static void test_FHE_total_sum_via_native_code(SparkSession spark, int slices, String library, String scheme, Broadcast<String> pk_b,
                                                          Broadcast<String> sk_b, Broadcast<String> rlk_b, Broadcast<String> glk_b) {
        System.out.println("test_FHE_total_sum_via_native_code");

        /* Encoders are created for Java beans */
        Encoder<SerializedCiphertext> ctxtJSONEncoder = Encoders.bean(SerializedCiphertext.class);

        // Create rdd with json line file.
        JavaRDD<SerializedCiphertext> ctxt_vec_rdd = spark.read().json(CTXT_Vector_FILE).as(ctxtJSONEncoder).javaRDD();

        long startTime, endTime;
        startTime = System.currentTimeMillis();
        // call homomorphic array sum operator on the rdd
        JavaRDD<SerializedCiphertext> collection = ctxt_vec_rdd.mapPartitions(records -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue(), rlk_b.getValue(), glk_b.getValue());

            LinkedList<SerializedCiphertext> sum = new LinkedList<SerializedCiphertext>();
            StringVector vec = new StringVector();
            while (records.hasNext()) {
                SerializedCiphertext rec = records.next();
                vec.add(rec.getCtxt());
            }
            sum.add(new SerializedCiphertext(SparkFHE.getInstance().fhe_total_sum(vec)));
            return sum.iterator();
        });

        // sum up the results from the previous operation
        SerializedCiphertext res = collection.reduce((x, y) -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue(), rlk_b.getValue(), glk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_add(x.getCtxt(), y.getCtxt()));
        });

        // sum up the slots of the result and display to verify it
        Ciphertext total_sum_ctxt = new Ciphertext(SparkFHE.getInstance().fhe_total_sum(res.getCtxt()));
        endTime = System.currentTimeMillis();
        System.out.println("TIMEINFO:batch_FHE_total_sum_via_native_code:" + (endTime - startTime) + ":ms");

        if (Config.DEBUG) {
            // print out the ciphertext vectors after decryption for verification purposes
            System.out.println("ctxt_vec_rdd.count() = " + ctxt_vec_rdd.count());
            ctxt_vec_rdd.foreach(data -> {
                // we need to load the shared library and init a copy of SparkFHE on the executor
                SparkFHEPlugin.setup();
                SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue(), rlk_b.getValue(), glk_b.getValue());
                Util.decrypt_and_print(scheme, "", new Ciphertext(data.getCtxt()), true, 100);
            });

            // print out the results for debugging purposes
            Util.decrypt_and_print(scheme, "Total Sum", total_sum_ctxt, false, 0);
        }
    }


    /**
     This method performs the Total sum operation on cipher-text vectors and print out the results as SparkSQL
     * @param spark the spark session which allows the creation of the various data abstractions such
     *              as RDDs, DataFrame, and more.
     * @param slices the number of time a task is split up
     * @param library the HE library name
     * @param scheme  the HE scheme name
     * @param pk_b broadcast variable for public key
     * @param sk_b broadcast variable for secret key
     * @param rlk_b broadcast variable for relin keys
     * @param glk_b boradcast variable for galois keys
     */
    public static void test_FHE_total_sum_via_sql(SparkSession spark, int slices, String library, String scheme, Broadcast<String> pk_b,
                                                  Broadcast<String> sk_b, Broadcast<String> rlk_b, Broadcast<String> glk_b) {
        System.out.println("test_FHE_total_sum_via_sql");
        // Spark example for FHE calculations //
        // Encoders are created for Java beans
        Encoder<SerializedCiphertext> ctxtJSONEncoder = Encoders.bean(SerializedCiphertext.class);
        Dataset<SerializedCiphertext> ctxt_vec_ds = spark.read().json(CTXT_Vector_FILE).as(ctxtJSONEncoder);

        // col - Returns a Column based on the given column name, ctxt.
        // explode - Creates a new row for each element in the given array or map column
        // select - select the newly created column, and alias it accordingly
        Dataset<String> ctxt_vec_ds2 = ctxt_vec_ds.select(ctxt_vec_ds.col("ctxt").as("ctxt")).as(Encoders.STRING());

        // withColumn - create a new DataFrame with a column added or renamed.
        // monotonically_increasing_id - A column that generates monotonically increasing 64-bit integers
        Dataset<Row> ctxt_a_ds3 = ctxt_vec_ds2.withColumn("id", functions.monotonically_increasing_id());
        // orderBy - creates a window specification that defines the partitioning, ordering, and frame boundaries with
        // the ordering defined. add this column as "id" and same it to the dataFrame ctxt_a_ds3
        ctxt_a_ds3 = ctxt_a_ds3.withColumn("id", functions.row_number().over(Window.orderBy("id")));

        StructType structType = new StructType();
        // Creates a new StructType by adding a new field with no metadata where the dataType is specified as a String.
        structType = structType.add("ctxt", DataTypes.StringType, false);

        // RowEncoder - is part of the Encoder framework and acts as the encoder for DataFrames, i.e. Dataset[Row] 
        // — Datasets of Rows.
        // ExpressionEncoder[T] - is a generic Encoder of JVM objects of the type T to and from internal binary rows.
        ExpressionEncoder<Row> encoder = RowEncoder.apply(structType);
        ExpressionEncoder<Row> encoder2 = RowEncoder.apply(structType);

        long startTime, endTime;
        startTime = System.currentTimeMillis();
        // mapPartition - converts each partition of the source RDD into multiple elements of the result
        // perform dot product on each pair (StringVector) of the dataFrame, and saving the rcesults to a LinkedList
        Dataset<SerializedCiphertext> collection = ctxt_a_ds3.mapPartitions((MapPartitionsFunction<Row, SerializedCiphertext>) iter -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue(), rlk_b.getValue(), glk_b.getValue());

            LinkedList<SerializedCiphertext> v = new LinkedList<SerializedCiphertext>();
            StringVector a = new StringVector();
            while (iter.hasNext()) {
                Row row = iter.next();
                a.add((String)row.getAs("ctxt"));
            }
            v.add(new SerializedCiphertext(SparkFHE.getInstance().fhe_total_sum(a)));
            return v.iterator();
        }, Encoders.kryo(SerializedCiphertext.class));

        // sum up the results from the previous operation and display
        SerializedCiphertext res = collection.javaRDD().reduce((x, y) -> {
            // we need to load the shared library and init a copy of SparkFHE on the executor
            SparkFHEPlugin.setup();
            SparkFHE.init(library, scheme, pk_b.getValue(), sk_b.getValue(), rlk_b.getValue(), glk_b.getValue());
            return new SerializedCiphertext(SparkFHE.getInstance().fhe_add(x.getCtxt(), y.getCtxt()));
        });

        // sum up the slots of the result and display to verify it
        Ciphertext total_sum_ctxt = new Ciphertext(SparkFHE.getInstance().fhe_total_sum(res.getCtxt()));
        endTime = System.currentTimeMillis();
        System.out.println("TIMEINFO:batch_FHE_total_sum_via_sql:" + (endTime - startTime) + ":ms");

        if (Config.DEBUG) {
            // print out the results for debugging purposes
            Util.decrypt_and_print(scheme, "Total Sum", total_sum_ctxt, false, 0);
        }
    }


    public static void main(String[] args) {
        String scheme="", library = "", pk="", sk="", rlk="", glk="", master="";

        // The variable slices represent the number of time a task is split up
        int slices=2;

        // Create a SparkConf that loads defaults from system properties and the classpath
        SparkConf sparkConf = new SparkConf();
        //Provides the Spark driver application a name for easy identification in the Spark or Yarn UI
        sparkConf.setAppName("TotalSumWithBatchingExample");

        // Decide whether to run the task locally or on the clusters
        Config.setExecutionEnvironment(args[0]);
        switch (Config.currentExecutionEnvironment) {
            case CLUSTER:
                slices = Integer.parseInt(args[0]);
                Config.set_HDFS_NAME_NODE(args[1]);
                library = args[2];
                scheme = args[3];
                pk = args[4];
                sk = args[5];
                if (library.equalsIgnoreCase(FHELibrary.SEAL)){
                    rlk = args[6];
                    glk = args[7];
                }
                break;
            case LOCAL:
                if (args[1] == "") {
                    master = "local";
                } else {
                    master = "local["+args[1]+"]";
                }
                sparkConf.setMaster(master);
                library = args[2];
                scheme = args[3];
                pk = args[4];
                sk = args[5];
                if (library.equalsIgnoreCase(FHELibrary.SEAL)){
                    rlk = args[6];
                    glk = args[7];
                }
                break;
            default:
                break;
        }
        System.out.println("CURRENT_DIRECTORY = "+ Config.get_current_directory());

        // Creating a session to Spark. The session allows the creation of the
        // various data abstractions such as RDDs, DataFrame, and more.
        SparkSession spark = SparkSession.builder().config(sparkConf).getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        // Creating spark context which allows the communication with worker nodes
        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());

        // required to load our shared library
        SparkFHEPlugin.setup();
        // create SparkFHE object
        SparkFHE.init(library, scheme, pk, sk, rlk, glk);

        // set ctxt file name
        CTXT_Vector_FILE = Config.get_records_directory()+"/packed_vec_a_"+SparkFHE.getInstance().generate_crypto_params_suffix()+ ".jsonl";

        Broadcast<String> pk_b = jsc.broadcast(pk);
        Broadcast<String> sk_b = jsc.broadcast(sk);
        Broadcast<String> rlk_b = jsc.broadcast(rlk);
        Broadcast<String> glk_b = jsc.broadcast(glk);

        // testing the total sum operation on plaintext vector.
        test_basic_total_sum(jsc, slices);

        long main_startTime, main_endTime;
        // testing the dot product operation in HE libraries on cipher text vector.
        main_startTime = System.currentTimeMillis();
        test_FHE_total_sum_via_lambda(spark, slices, library, scheme, pk_b, sk_b, rlk_b, glk_b);
        main_endTime = System.currentTimeMillis();
        System.out.println("TIMEINFO:batch_total_Spark_total_sum_job_via_lambda:" + (main_endTime - main_startTime) + ":ms");

        main_startTime = System.currentTimeMillis();
        test_FHE_total_sum_via_native_code(spark, slices, library, scheme, pk_b, sk_b, rlk_b, glk_b);
        main_endTime = System.currentTimeMillis();
        System.out.println("TIMEINFO:batch_total_Spark_total_sum_job_via_native_code:" + (main_endTime - main_startTime) + ":ms");

        main_startTime = System.currentTimeMillis();
        test_FHE_total_sum_via_sql(spark, slices, library, scheme, pk_b, sk_b, rlk_b, glk_b);
        main_endTime = System.currentTimeMillis();
        System.out.println("TIMEINFO:batch_total_Spark_total_sum_job_via_sql:" + (main_endTime - main_startTime) + ":ms");
        
//        try {
//            System.out.println("Paused to allow checking the Spark server log, press enter to continue.");
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Stop existing spark context
        jsc.close();

        // Stop existing spark session
        spark.close();
    }

}